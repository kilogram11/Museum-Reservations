# 核心业务流程 · FLOWS

> 本文档用 Mermaid 描述两大核心子系统：**预约库存**（面试托底）与 **AI Agent + RAG**（主叙事）。
> 配套脚本/联调材料：`docs/jmeter/`、`docs/ai-agent-test-case.md`、`docs/ai-rag-test-case.md`。

---

## 流程 1：预约提交（Redis Lua 预扣 + MySQL 事务落单 + 补偿）

### 1.1 系统架构

```mermaid
graph LR
    APP[小程序端<br/>POST /app/booking/submit] --> CTRL[AppBookingController]
    CTRL --> SVC[JoinService.submitBooking]
    SVC --> STOCK[BookingStockService]
    STOCK -- Lua 原子 --> REDIS[(Redis<br/>stock / booked)]
    SVC -- 事务 --> MYSQL[(MySQL<br/>join + time.SUCC_CNT)]
    SVC -. 失败/取消补偿 .-> REDIS
```

### 1.2 提交时序

```mermaid
sequenceDiagram
    participant U as 小程序用户
    participant C as AppBookingController
    participant J as JoinService
    participant S as BookingStockService
    participant R as Redis
    participant M as MySQL

    U->>C: POST /app/booking/submit {timeMark, identityIds[]}
    Note over C: JWT 取 userId（@RequireLogin）
    C->>J: submitBooking(userId, timeMark, identityIds)
    J->>J: 请求内去重（identityIds 不重复）
    J->>S: warmUpIfAbsent（SETNX 预热 stock）
    S->>R: SETNX booking:stock:{timeMark} = LIMIT_CNT
    J->>S: tryReserve（Lua 预扣）
    S->>R: EVAL booking_reserve.lua
    Note over R: 1. 请求内重复→-3<br/>2. stock 不足→-1<br/>3. 已 booked→-2<br/>4. DECRBY stock<br/>5. SET booked + TTL
    R-->>S: 1（成功）/ 负数错误码
    alt Lua 失败
        S-->>J: 抛业务异常（库存不足/一证一约/重复）
        J-->>C: Result.error(对应 ErrorCode)
    else Lua 成功
        J->>M: 事务：insert join + SUCC_CNT+=delta
        alt MySQL 成功
            M-->>J: 提交
            J-->>C: Result.success
        else MySQL 失败
            J->>S: compensate（Lua 回补）
            S->>R: INCRBY stock + DEL booked
            J-->>C: Result.error
        end
    end
```

### 1.3 Redis Key 设计


| Key                                 | 形式       | 用途         | TTL             |
| ----------------------------------- | ---------- | ------------ | --------------- |
| `booking:stock:{timeMark}`          | 数值字符串 | 时段剩余库存 | 随排期          |
| `booking:booked:{day}:{identityId}` | `1`        | 一证一约标记 | 参观日结束 + 2h |

`timeMark` 形如 `museum_001_2025-12-30_09:00`（场馆+日期+时段），与 `time` 表唯一键一致。

### 1.4 Lua 脚本契约（`resources/lua/booking_reserve.lua`）

执行顺序对正确性至关重要：

1. 请求内去重：同一 `bookedKey` 出现两次 → 返回 `-3`
2. 库存校验：`GET stockKey`，对比 `need` → 不足返回 `-1`
3. 一证一约：`EXISTS` 每个 `bookedKey` → 已存在返回 `-2`
4. 原子预扣：`DECRBY stockKey need`
5. 写标记：`SET` 每个 `bookedKey = '1' EX ttl`
6. 返回 `1`

返回码：`1` 成功 / `-1` 库存不足 / `-2` 当日已约 / `-3` 请求内重复。

### 1.5 补偿（`resources/lua/booking_compensate.lua`）

- 触发：MySQL 落单失败 或 用户取消（`POST /app/record/cancel`）
- 动作：原子 `INCRBY stockKey` 还原库存 + `DEL` 全部 `bookedKey`
- `compensate()` 错误仅记录日志不抛出（fail-silent），由定时审计任务兜底对账
- 取消时：**MySQL 成功后**才补偿 Redis；Redis 失败不回滚 MySQL

### 1.6 一证一约并发

```mermaid
sequenceDiagram
    participant G1 as 请求A（同一 identityId）
    participant G2 as 请求B（同一 identityId）
    participant R as Redis

    par
        G1->>R: EVAL booking_reserve.lua
        G1->>R: EXISTS booked:{day}:{identityId} = 0
        G1->>R: SET booked = 1
    and
        G2->>R: EVAL booking_reserve.lua
        G2->>R: EXISTS booked = 1（A 已写入）
        G2-->>G2: 返回 -2
    end
    Note over R: Lua 原子性保证同一 identity<br/>10 并发仅 1 成功
```

### 1.7 压测结论（2026-08-16 JMeter，本机真 Redis/MySQL）

- 场景 A 超卖：80 并发同 `timeMark`（`LIMIT_CNT=50`）→ 成功 50、业务失败 ~30；`SUCC_CNT=50`、`booking:stock=0`、**0 超卖**
- 场景 B 一证一约：同一 `identityId` 10 并发 → 成功 1；Redis 写入 `booking:booked:{day}:{identityId}`
- 不变量：`succCnt + Redis剩余 = limitCnt`
- 脚本：`docs/jmeter/`

### 1.8 爽约闭环（`BookingScheduler`，每 5 分钟）

```mermaid
flowchart TD
    A[扫描过 JOIN_COMPLETE_END_TIME 且 JOIN_IS_CHECKIN=0 的记录] --> B[置 JOIN_IS_CHECKIN=3 爽约]
    B --> C[IdentityMapper.updateBanStatistics<br/>子查询统计近 7 日 status=3 次数]
    C --> D{USER_BAN_NUM > 阈值?}
    D -- 是 --> E[doBan: IDENTITY_STATUS=0<br/>USER_CHECK_TYPE=1 自动]
    D -- 否 --> F[保持]
    G[扫描 BLACK_END_TIME 已过] --> H[autoUnban: IDENTITY_STATUS=1]
    E --> I[发 sys_message: 预约爽约已入黑名单]
```

---

## 流程 2：AI 对话（双 ChatClient + 5 Tool + 轻量 RAG）

### 2.1 系统架构

```mermaid
graph TB
    MP[小程序 AI 页<br/>POST /ai/chat] --> ICPT[AppAuthInterceptor<br/>软注入 UserContext]
    ICPT --> CTRL[AiChatController]
    CTRL --> SVC[AiChatService]
    SVC --> IR[IntentRouter 关键词路由]
    IR -- BOOKING/MIXED --> C1[museumBookingChatClient<br/>+ BookingTools]
    IR -- RULES --> C2[museumRulesChatClient<br/>无 Tool]
    C2 --> RAG[RagService 检索]
    RAG --> STORE[InMemoryRagStore<br/>预计算向量]
    STORE --> CORPUS[语料: visible news<br/>+ visit-rules.md]
    C1 --> TOOLS[5 @Tool]
    TOOLS --> JSVC[JoinService / DayService / TimeService]
    C1 --> LLM[DeepSeek]
    C2 --> LLM
    SVC --> COLLECT[AiChatBlockCollector<br/>组装 blocks/suggestions]
    COLLECT --> RESP[AiChatResponse]
```

### 2.2 意图路由时序

```mermaid
sequenceDiagram
    participant U as 用户
    participant C as AiChatController
    participant I as IntentRouter
    participant AI as AiChatService
    participant R as RagService
    participant LLM as DeepSeek
    participant T as BookingTools

    U->>C: POST /ai/chat {message}
    C->>I: route(message)
    alt 命中 RULES 且 BOOKING 关键词
        I-->>C: MIXED
        C->>AI: chatMixed(message)
        AI->>R: retrieve(message)
        R-->>AI: Top-K hits
        AI->>LLM: museumBookingChatClient + RAG 片段 + 系统 prompt（分段回答）
        LLM->>T: 调用 Tool（预约事实）
        T->>AI: ToolResult
        AI->>LLM: Tool 结果
        LLM-->>AI: 最终回复
    else 仅 RULES
        I-->>C: RULES
        C->>AI: chatRules(message)
        AI->>R: retrieve(message)
        alt 有命中
            R-->>AI: hits
            AI->>LLM: museumRulesChatClient（无 Tool）+ 片段
            LLM-->>AI: 回复
        else 无命中
            AI-->>C: MISS_REPLY（不编造）
        end
    else BOOKING / 默认
        I-->>C: BOOKING
        C->>AI: chatBooking(message)
        AI->>LLM: museumBookingChatClient（+ Tools）
        LLM->>T: 调用 Tool
        T-->>AI: ToolResult
        AI-->>C: 回复 + blocks
    end
    C-->>U: AiChatResponse{reply, intent, blocks, suggestions, debug?}
```

### 2.3 五个 Tool 契约


| Tool            | 类型 | 入参                        | 返回 DTO            | 备注                                  |
| --------------- | ---- | --------------------------- | ------------------- | ------------------------------------- |
| `queryDays`     | 读   | —                          | `QueryDaysData`     | 全部可预约日 + 开/闭馆                |
| `queryTimes`    | 读   | `day`                       | `QueryTimesData`    | 某日时段 + 余量 + AVAILABLE/FULL      |
| `submitBooking` | 写   | `timeMark`, `identityIds[]` | `SubmitBookingData` | 需登录；调`JoinService.submitBooking` |
| `listRecords`   | 读   | `day?`, `status?`           | `ListRecordsData`   | 需登录；返回含`joinId`                |
| `cancelBooking` | 写   | `joinId`                    | `CancelBookingData` | 需登录；`joinId` 须来自 `listRecords` |

**写 Tool 绑定登录用户**：取 `UserContext.get()`，无登录返回 `ToolError.UNAUTHORIZED`；**不接受外部 `userId`**。

### 2.4 UserContext 注入与写操作安全

```mermaid
sequenceDiagram
    participant U as 用户
    participant ICPT as AppAuthInterceptor
    participant UC as UserContext (ThreadLocal)
    participant T as submitBooking Tool
    participant J as JoinService

    U->>ICPT: POST /ai/chat（Token 头可选）
    ICPT->>UC: clear()
    alt Token 有效
        ICPT->>UC: set(userId)
    else 无 Token
        UC->>UC: userId=null（匿名放行）
    end
    ICPT->>T: （AI 调用 Tool）
    T->>UC: get()
    alt userId == null
        T-->>AI: ToolError.UNAUTHORIZED
    else userId 存在
        T->>J: submitBooking(userId, ...)
        Note over J: 复用主链路库存逻辑<br/>不新增第二套预约
    end
    Note over ICPT: afterCompletion: clear() 防泄漏
```

### 2.5 轻量 RAG 检索

```mermaid
flowchart LR
    Q[用户问句] --> EM[LocalHashingEmbeddingModel<br/>字符 n-gram 哈希 384 维]
    EM --> V[查询向量]
    V --> S[InMemoryRagStore.search]
    S --> COS[与全量预计算向量余弦相似度]
    COS --> F[Top-K 且 score ≥ min-score]
    F --> FMT[RagService.formatContext]
    FMT --> P[拼入 LLM prompt: [1] source=...]
```

- **Embedding**：本地哈希，无网络、不依赖 DeepSeek embeddings；unigram/bigram/trigram 加权哈希到 384 维后 L2 归一化
- **存储**：`CopyOnWriteArrayList` + 预计算向量，启动时 `rebuild-on-startup`
- **语料**：`NewsRagLoader`（`NoticeService#listVisibleForRag` 全量可见公告，HTML 清洗）+ `StaticRulesLoader`（`classpath:rag/visit-rules.md`）
- **配置**：`museum.ai.rag.{enabled,top-k=4,min-score=0.12,rebuild-on-startup=true}`
- **未命中**：返回 `MISS_REPLY`（"我目前没有查到相关馆规…"），**零编造**

### 2.6 响应协议与调试追踪

`AiChatResponse` = `reply + intent + blocks + suggestions + debug?`：

- `reply`：兼容旧前端
- `blocks`：`time_slots` / `booking_records` / `rules_source` / `tips`，前端按 `type` 渲染卡片
- `suggestions`：按 intent 给快捷追问
- `debug`：仅 `X-AI-Debug: 1` 时返回，含 `intent / ragHits / tools / elapsed`，由 `AiDebugTraceContext`（ThreadLocal）收集

---

## 流程 3：用户端预约全链路（小程序）

```mermaid
sequenceDiagram
    participant U as 用户
    participant MP as 小程序
    participant API as 后端
    participant R as Redis
    participant M as MySQL

    U->>MP: 手机号 + 验证码(1234)
    MP->>API: POST /app/user/login
    API-->>MP: {token}
    U->>MP: 添加访客（身份证）
    MP->>API: POST /app/identity/save
    Note over API: 唯一约束 uk_identity_card<br/>拉黑检测 IDENTITY_STATUS
    U->>MP: 查可约日期
    MP->>API: GET /app/booking/days
    U->>MP: 选日查时段
    MP->>API: GET /app/booking/times?day=
    API->>M: SELECT time WHERE day<br/>返回余量
    U->>MP: 选时段 + 访客提交
    MP->>API: POST /app/booking/submit
    Note over API: 见流程 1：Lua 预扣 + MySQL 落单
    API->>M: insert join + SUCC_CNT+=delta
    API->>M: insert sys_message（预约成功通知）
    API-->>MP: 成功 + 二维码
    Note over U: 参观日到场
    U->>API: 出示二维码
    API->>API: POST /admin/join/checkin（管理端核销）
    API->>M: JOIN_IS_CHECKIN=1
    Note over M: 过期末核销 → 定时任务置 3<br/>→ 累计 USER_BAN_NUM → 自动拉黑
```

---

## 流程 4：管理端核销与对账（管理端）

```mermaid
flowchart LR
    A[管理员登录] --> B[进核销页 Check.vue]
    B --> C[搜索预约<br/>姓名/身份证]
    C --> D[扫码核销<br/>POST /admin/join/checkin]
    D --> E{JOIN_IS_CHECKIN}
    E -- 0→1 --> F[核销成功]
    E -- 已核销 --> G[CHECKIN_BUSY 并发提示]
    D --> H[写 log 表<br/>LOG_TYPE=99]
    B --> I[导出 Excel<br/>GET /admin/join/export]
    B --> J[看板 /stats/*<br/>趋势/核销分布/爽约对比]
```

---

## 关键不变量与边界


| 不变量                           | 保证机制                                                                   |
| -------------------------------- | -------------------------------------------------------------------------- |
| `succCnt + Redis剩余 = limitCnt` | Lua 原子预扣 + MySQL 原子自增 + 补偿对称                                   |
| 同一身份证当日仅 1 约            | `booking:booked:{day}:{identityId}` SET + TTL                              |
| AI 不编造预约事实                | 写 Tool 必须调真实 Service；RULES 路径不绑 Tool；RAG 未命中返回 MISS_REPLY |
| AI 写操作不可伪造他人 userId     | `UserContext` 取自 JWT，Tool 不接受参数 userId                             |
| 预约记录不可变审计               | `JOIN_STATUS` 不删除；`JOIN_FORMS` 落单快照                                |
| 爽约必拉黑                       | 定时任务闭环：置 3 → 累加`USER_BAN_NUM` → `doBan()` → 发消息            |
