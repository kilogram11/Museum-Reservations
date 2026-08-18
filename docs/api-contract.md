# 预约业务接口契约（阶段 2 调研）

> 依据：现有 Controller / Service / Entity / `ErrorCode` 实装（2026-08-14）  
> 用途：阶段 2「先确认现有业务能力 → 再定义 Tool Contract」的输入材料  
> 范围：与五个 Tool 直接相关的 App 预约主链路 + 登录/游客支撑接口；不含管理端核销/看板

---

## 1. 约定

| 项 | 说明 |
| --- | --- |
| Base URL | `http://{host}:8081` |
| 统一响应 | `{ code, msg, data }`（`Result`） |
| 成功码 | `code = 200` |
| App 鉴权头 | `Token: <JWT>` |
| 登录态来源 | `@RequireLogin` → `AppAuthInterceptor` 校验；Controller 内 `getUserId()` 从 Token 解析 |
| 业务异常 | `BusinessException` → `Result.error(code, message)` |
| 未登录 | `401` / `未登录或Token无效`（HTTP 仍可能是 200，看 body.code） |

### 统一响应壳

**脑图**

```text
Result
├─ code : Integer   # 200 成功；其它见 ErrorCode
├─ msg  : String
└─ data : Object|null
```

**JSON**

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": null
}
```

---

## 2. 与阶段 2 五个 Tool 的映射

| Tool（计划） | 现有 HTTP | 现有 Service | 读写 |
| --- | --- | --- | --- |
| `queryDays` | `GET /app/booking/days` | `JoinService.getBookingDays()` | 读 |
| `queryTimes` | `GET /app/booking/times` | `JoinService.getBookingTimes(day)` | 读 |
| `submitBooking` | `POST /app/booking/submit` | `JoinService.submitBooking(userId, timeMark, identityIds)` | 写 |
| `listRecords` | `GET /app/record/list` | `JoinService.getMyBookings(userId)` | 读 |
| `cancelBooking` | `POST /app/record/cancel` | `JoinService.cancelBooking(userId, joinId)` | 写 |

支撑接口（非 Tool，但 Agent 链路需要）：

| 用途 | HTTP |
| --- | --- |
| 登录拿 Token | `POST /app/user/login` |
| 常用游客列表（下单前选人） | `GET /app/identity/list` |
| 现状 AI 文本聊天（阶段 2 已接 ChatClient+Tool） | `POST /ai/chat` |

---

## 3. 可预约日期 — `queryDays`

### 说明

返回当前启用场馆下、今天起最多 7 天（`BOOKING_DAYS_TO_SHOW`）的排期日期。匿名可调，无需登录。

| 项 | 值 |
| --- | --- |
| Method / Path | `GET /app/booking/days` |
| 登录 | 否 |
| Service | `JoinService.getBookingDays()` |
| 成功 msg | `获取成功` |

### 请求

无 Query / Body。

### 响应 `data`：`List<Map>`

**脑图**

```text
data[] DayItem
├─ day    : String   # yyyy-MM-dd
├─ week   : String   # 如「周一」
└─ status : Integer  # 1 可预约/开馆；0 不可预约/闭馆
```

**JSON 示例**

```json
{
  "code": 200,
  "msg": "获取成功",
  "data": [
    {
      "day": "2026-08-15",
      "week": "周六",
      "status": 1
    },
    {
      "day": "2026-08-16",
      "week": "周日",
      "status": 0
    }
  ]
}
```

### 内部语义（AI 适配时需转换）

| 字段 | 内部值 | 建议 AI 语义 |
| --- | --- | --- |
| `status` | `1` / `0` | `OPEN` / `CLOSED` |

无活跃场馆时返回空数组 `[]`。

---

## 4. 可预约时段 — `queryTimes`

### 说明

按日期查启用中的时段；余票优先读 Redis `booking:stock:{timeMark}`，缺失则用 DB `limitCnt - succCnt` 预热后再读。

| 项 | 值 |
| --- | --- |
| Method / Path | `GET /app/booking/times` |
| 登录 | 否 |
| Query | `day`（必填，`yyyy-MM-dd`） |
| Service | `JoinService.getBookingTimes(dayStr)` |
| 成功 msg | `获取成功` |

### 请求

**脑图**

```text
Query
└─ day : String  # yyyy-MM-dd，匹配 TIME_MARK like day
```

**JSON（Query 等价表达）**

```json
{
  "day": "2026-08-15"
}
```

### 响应 `data`：`List<Map>`

**脑图**

```text
data[] TimeItem
├─ timeMark  : String   # 时段唯一键，下单必传
├─ startTime : String   # HH:mm / H:mm
├─ endTime   : String
├─ total     : Integer  # 名额上限 LIMIT_CNT
├─ used      : Integer  # total - surplus（展示用已用）
└─ surplus   : Integer  # 剩余名额（Redis/DB）
```

**JSON 示例**

```json
{
  "code": 200,
  "msg": "获取成功",
  "data": [
    {
      "timeMark": "museum_001_2026-08-15_09:00",
      "startTime": "09:00",
      "endTime": "11:00",
      "total": 50,
      "used": 12,
      "surplus": 38
    },
    {
      "timeMark": "museum_001_2026-08-15_14:00",
      "startTime": "14:00",
      "endTime": "16:00",
      "total": 50,
      "used": 50,
      "surplus": 0
    }
  ]
}
```

### 内部语义（AI 适配时需转换）

| 字段 | 说明 | 建议 AI 语义 |
| --- | --- | --- |
| `surplus` | 剩余票 | 可直接暴露为 `remain` |
| `startTime` 上午/下午 | 业务未给枚举，需按时间推断 | `MORNING` / `AFTERNOON` |
| `surplus == 0` | 满 | `FULL` |

仅返回 `STATUS = 1` 的时段。

---

## 5. 提交预约 — `submitBooking`

### 说明

主链路：请求内去重 → Redis Lua 预扣 → MySQL 事务落单（insert + 原子 `SUCC_CNT`）→ 失败补偿。  
**成功时 `data` 为 null**，不直接返回预约号（需再查「我的预约」）。

| 项 | 值 |
| --- | --- |
| Method / Path | `POST /app/booking/submit` |
| 登录 | **是**（`@RequireLogin`） |
| Header | `Token` |
| Body | `BookingSubmitDTO` |
| Service | `JoinService.submitBooking(userId, timeMark, identityIds)` |
| 成功 msg | `预约成功` |

### 请求

**脑图**

```text
BookingSubmitDTO
├─ timeMark     : String        # 必填，来自 /times
└─ identityIds  : String[]      # 必填，1~3 个游客 ID，不可重复
     └─ 注：userId 不由客户端传入，由 Token 注入
```

**JSON**

```json
{
  "timeMark": "museum_001_2026-08-15_09:00",
  "identityIds": [
    "identity_xxx",
    "identity_yyy"
  ]
}
```

### 响应

**脑图**

```text
Result
├─ code : 200
├─ msg  : "预约成功"
└─ data : null
```

**JSON**

```json
{
  "code": 200,
  "msg": "预约成功",
  "data": null
}
```

### 落库副作用（非 HTTP 返回，供 Converter 知晓）

每人一条 `Join`：

```text
Join（创建）
├─ joinId          : "join_" + uuid
├─ userId          : Token 用户
├─ identityId      : 游客
├─ joinMeetDay     : 参观日
├─ timeMark        : 时段键
├─ joinStatus      : 1（SUCCESS）
├─ joinIsCheckin   : 0（UNCHECKED）
├─ joinForms       : {"name","card","mobile"} 快照 JSON 字符串
└─ joinQr          : Base64 二维码
```

### 常见业务错误

| code | ErrorCode | 含义 | 建议 AI 语义 |
| --- | --- | --- | --- |
| 401 | UNAUTHORIZED | 未登录 | `UNAUTHORIZED` |
| 2001 | IDENTITY_NOT_FOUND | 游客不存在 | `IDENTITY_NOT_FOUND` |
| 2002 | IDENTITY_BLACKLISTED | 黑名单 | `BLACKLISTED` |
| 2003 | IDENTITY_DUPLICATE_BOOKING | 该日已约 | `DUPLICATE_BOOKING` |
| 2004 | IDENTITY_DUPLICATE_IN_REQUEST | 请求内重复游客 | `DUPLICATE_IN_REQUEST` |
| 3001 | BOOKING_SLOT_INVALID | 时段无效 | `SLOT_INVALID` |
| 3002 | BOOKING_SLOT_FULL | 余量不足 | `FULL` |
| 3003 | BOOKING_TOO_MANY | 超过 3 人 | `TOO_MANY` |
| 3004 | BOOKING_NO_VISITORS | 未选人 | `NO_VISITORS` |
| 3011 | BOOKING_SCHEDULE_ERROR | 排期异常 | `SCHEDULE_ERROR` |
| 3012 | BOOKING_STOCK_REDIS_ERROR | Redis 不可用 | `STOCK_UNAVAILABLE` |

---

## 6. 我的预约 — `listRecords`

### 说明

按当前用户查预约列表（含取消），按下单时间倒序；关联时段起止与场馆展示字段。

| 项 | 值 |
| --- | --- |
| Method / Path | `GET /app/record/list` |
| 登录 | **是**（类级 `@RequireLogin`） |
| Service | `JoinService.getMyBookings(userId)` |
| 成功 msg | `获取成功` |

### 请求

无参数；`userId` 仅来自 Token。

### 响应 `data`：`List<Join>`

**脑图**

```text
data[] Join
├─ id                 : String    # 表主键 _id
├─ joinId             : String    # 业务预约号（取消用这个）
├─ identityId         : String
├─ userId             : String
├─ joinMeetDay        : String    # yyyy-MM-dd
├─ timeMark           : String
├─ joinStartTime      : Long|null
├─ joinCompleteEndTime: String|null
├─ joinStatus         : Integer   # 1 成功；2 已取消
├─ joinForms          : String    # JSON 字符串快照
│    ├─ name
│    ├─ card
│    └─ mobile
├─ joinIsCheckin      : Integer   # 0 未核销；1 已核销；3 爽约
├─ joinQr             : String    # Base64
├─ joinAddTime        : Long
├─ joinEditTime       : Long
├─ pid                : String
├─ joinMeetTimeStart  : String    # 非表字段，来自 time.TIME_START
├─ joinMeetTimeEnd    : String    # 非表字段，来自 time.TIME_END
├─ museumTitle        : String    # 服务端 enrich
├─ museumAddress      : String
├─ latitude           : Double
└─ longitude          : Double
```

**JSON 示例**

```json
{
  "code": 200,
  "msg": "获取成功",
  "data": [
    {
      "id": "a1b2c3",
      "joinId": "join_d4e5f6",
      "identityId": "identity_xxx",
      "userId": "user_001",
      "joinMeetDay": "2026-08-15",
      "timeMark": "museum_001_2026-08-15_09:00",
      "joinStartTime": null,
      "joinCompleteEndTime": null,
      "joinStatus": 1,
      "joinForms": "{\"name\":\"张三\",\"card\":\"1101...\",\"mobile\":\"13800000000\"}",
      "joinIsCheckin": 0,
      "joinQr": "data:image/png;base64,...",
      "joinAddTime": 1723612800000,
      "joinEditTime": 1723612800000,
      "pid": "1",
      "joinMeetTimeStart": "09:00",
      "joinMeetTimeEnd": "11:00",
      "museumTitle": "示例博物馆",
      "museumAddress": "北京市...",
      "latitude": 39.9,
      "longitude": 116.4
    }
  ]
}
```

### 状态码语义（AI 必须转换，勿原样暴露数字）

| 字段 | 值 | 业务含义 | 建议 AI 语义 |
| --- | --- | --- | --- |
| `joinStatus` | 1 | 预约成功 | `BOOKED` |
| `joinStatus` | 2 | 已取消 | `CANCELLED` |
| `joinIsCheckin` | 0 | 未核销 | `UNCHECKED` |
| `joinIsCheckin` | 1 | 已核销 | `CHECKED_IN` |
| `joinIsCheckin` | 3 | 爽约 | `EXPIRED` |

---

## 7. 取消预约 — `cancelBooking`

### 说明

校验归属与可取消状态 → MySQL 改状态并回减 `SUCC_CNT` → 成功后 Redis 补偿（回库存、删 booked）。Redis 失败不回滚 MySQL。

| 项 | 值 |
| --- | --- |
| Method / Path | `POST /app/record/cancel` |
| 登录 | **是** |
| Body | `{ "joinId": "..." }` |
| Service | `JoinService.cancelBooking(userId, joinId)` |
| 成功 msg | `取消成功` |

### 请求

**脑图**

```text
Body
└─ joinId : String  # 业务预约号；也可用主键 _id（Service 兼容）
```

**JSON**

```json
{
  "joinId": "join_d4e5f6"
}
```

`joinId` 为空时 Controllers 直接返回 `code=500, msg=参数错误`（未走 ErrorCode 枚举）。

### 响应

```json
{
  "code": 200,
  "msg": "取消成功",
  "data": null
}
```

### 常见业务错误

| code | ErrorCode | 含义 | 建议 AI 语义 |
| --- | --- | --- | --- |
| 3005 | BOOKING_NOT_FOUND | 无此记录或不属于当前用户 | `NOT_FOUND` |
| 3006 | BOOKING_STATUS_INVALID | 非「预约成功」态 | `STATUS_INVALID` |
| 3007 | BOOKING_ALREADY_CHECKED_IN | 已核销/已失效不可取消 | `ALREADY_CHECKED_IN` |

---

## 8. 预约详情（非 Tool，可选复用）

| 项 | 值 |
| --- | --- |
| Method / Path | `GET /app/record/detail?joinId=` |
| 登录 | 是 |
| 说明 | 在「我的预约」列表中按 `joinId` 过滤；不存在返回 `500 记录不存在` |

响应结构同单条 `Join`（见 §6）。

---

## 9. 支撑接口

### 9.1 登录

| 项 | 值 |
| --- | --- |
| Method / Path | `POST /app/user/login` |
| 登录 | 否 |
| 演示验证码 | 固定 `1234` |

**请求脑图**

```text
Body
├─ mobile : String
└─ code   : String  # 演示固定 1234
```

**请求 JSON**

```json
{
  "mobile": "13800000000",
  "code": "1234"
}
```

**响应脑图**

```text
data
└─ token : String  # 后续请求放 Header Token
```

**响应 JSON**

```json
{
  "code": 200,
  "msg": "登录成功",
  "data": {
    "token": "<JWT>"
  }
}
```

### 9.2 常用游客列表（下单依赖）

| 项 | 值 |
| --- | --- |
| Method / Path | `GET /app/identity/list` |
| 登录 | 是 |
| Service | `IdentityService.listMyIdentity(userId)` |

**响应脑图（核心字段）**

```text
data[] Identity
├─ identityId     : String
├─ identityName   : String
├─ identityCard   : String
├─ identityMobile : String
└─ identityStatus : Integer  # 1 正常；0 黑名单
```

**JSON 示例**

```json
{
  "code": 200,
  "msg": "获取成功",
  "data": [
    {
      "identityId": "identity_xxx",
      "identityName": "张三",
      "identityCard": "1101************",
      "identityMobile": "13800000000",
      "identityStatus": 1
    }
  ]
}
```

保存/删除：`POST /app/identity/save`、`POST /app/identity/del`（body 含 `identityId`），阶段 2 Tool **不覆盖**。

### 9.3 AI 聊天（ChatClient + DeepSeek）

| 项 | 值 |
| --- | --- |
| Method / Path | `POST /ai/chat` |
| 登录 | 未强制 `@RequireLogin`（写 Tool 靠 `UserContext`；可选 Token） |
| 实现 | `AiChatService` → 意图分流 → 预约 Tool Client / 馆规 RAG Client |
| 响应类型 | `AiChatResponse`（**不是** `Result` 壳） |

**请求 / 响应**

```json
// request
{ "message": "后天下午有票吗" }

// response（向后兼容：旧客户端只读 reply 即可）
{
  "reply": "...",
  "intent": "BOOKING|RULES|MIXED",
  "blocks": [
    {
      "type": "time_slots|booking_records|rules_source|tips",
      "title": "...",
      "items": [],
      "source": "tool:queryTimes|tool:listRecords|static_rules|news|config|rag_miss|..."
    }
  ],
  "suggestions": ["后天下午有票吗", "我的预约", "可以带背包吗"]
}
```

| 字段 | 说明 |
| --- | --- |
| `reply` | 模型自然语言回复（必有） |
| `intent` | `IntentRouter` 结果：`BOOKING` / `RULES` / `MIXED` |
| `blocks` | 确定性结构化块；由 Tool 旁路收集（`AiChatBlockCollector`）或 RAG `RagHit` 生成，**禁止**从 `reply` 正则解析 |
| `suggestions` | 按 intent 的静态追问建议，供小程序 chips |

`blocks[].type` 常见取值：

| type | 来源 |
| --- | --- |
| `time_slots` | `BookingTools.queryTimes` 成功 |
| `booking_records` | `BookingTools.listRecords` 成功 |
| `rules_source` | RAG 命中（`source` 为 `static_rules` / `news` 等） |
| `tips` | 配置缺失、RAG 未命中、提交/取消成功提示、异常提示等 |

未配置 `spring.ai.openai.api-key` 时 `reply` 与 `tips` 返回配置提示；已填写 Key 时可走 Tool Calling（`BookingTools`）。本项目 Key **写在 yml**，不用环境变量。

小程序消费约定见 [ai-miniapp-page.md](./ai-miniapp-page.md)。

---

## 10. Service 层契约（Tool 应直接调用）

Tool 层应绕过 HTTP，直接调 Service：

```text
JoinService
├─ getBookingDays()                              → List<Map>
├─ getBookingTimes(dayStr)                       → List<Map>
├─ submitBooking(userId, timeMark, identityIds)  → void
├─ getMyBookings(userId)                         → List<Join>
└─ cancelBooking(userId, joinId)                 → void
```

**强制约束（与 OPTIMIZATION_PLAN 一致）**

- 写方法的 `userId` 只能来自服务端登录上下文，禁止作为 Tool 入参暴露给模型
- Tool 不写 SQL、不碰 Redis、不重写库存；库存与补偿留在 `BookingStockService` / `JoinServiceImpl`

---

## 11. 字段分层备忘（给 2.3 Converter）

| 层级 | 例子 |
| --- | --- |
| DB / Entity | `JOIN_STATUS=1`、`JOIN_IS_CHECKIN=0`、`SUCC_CNT` |
| 业务 HTTP 返回 | `joinStatus`、`surplus`、`timeMark` |
| AI Tool 应暴露 | `status=BOOKED`、`remain=38`、`period=MORNING`、`error=FULL` |

HTTP 契约是「业务真相」；AI Contract 是其上的语义投影，不要把表字段数字直接喂给模型。

---

## 12. 相关源码索引

| 能力 | 路径 |
| --- | --- |
| 预约 Controller | `ZDYZ/.../controller/app/AppBookingController.java` |
| 记录 Controller | `ZDYZ/.../controller/app/AppRecordController.java` |
| 游客 Controller | `ZDYZ/.../controller/app/AppIdentityController.java` |
| 下单 DTO | `ZDYZ/.../common/dto/BookingSubmitDTO.java` |
| 业务实现 | `ZDYZ/.../service/impl/JoinServiceImpl.java` |
| 错误码 | `ZDYZ/.../common/exception/ErrorCode.java` |
| 统一结果 | `ZDYZ/.../common/result/Result.java` |
| AI 聊天 | `ZDYZ/.../controller/AiChatController.java` → `AiChatResponse` |
| AI 块收集 | `ZDYZ/.../ai/support/AiChatBlockCollector.java` |
| 小程序 AI 页 | `app-front/ZDYZ/miniprogram/pages/aiChat`；约定见 [ai-miniapp-page.md](./ai-miniapp-page.md) |

---

## 13. 下一步

阶段 0～3 已完成（含 3.1 RAG、3.2 小程序 AI 页）。设计与验收见 [ai-agent-design.md](./ai-agent-design.md)、[ai-rag-design.md](./ai-rag-design.md)、[ai-miniapp-page.md](./ai-miniapp-page.md)、[OPTIMIZATION_PLAN.md](../OPTIMIZATION_PLAN.md)。
