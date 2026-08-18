# Museum-Reservations 简历优化计划

本文档是后续改代码的唯一执行口径。目标：把本项目优化到可以写进简历，主叙事是预约 Agent，面试托底是预约库存。

**当前进度（2026-08-17）：阶段 0/1/2 已验收；阶段 3.1 轻量 RAG与阶段 3.2 小程序 AI 页已完成；阶段 3 全部完成。** 详见各阶段「进度」。

已确认约束：

- 第一版不做 RabbitMQ、不做令牌桶、不做 GEO / Feed / 签到
- 不把 `sping-ai/examples` 整仓搬进来
- 验证码继续 mock `1234`，README 写明仅演示
- 阶段 2 只有 **1 个 Agent + 5 个 Tool**，不做多 Agent
- 阶段 1 库存方案固定为 **Redis 预扣 + MySQL 事务 + 补偿**，不保留其它路径
- RAG、小程序 AI 页放在最后

---

## 原则

1. 只改本仓库，业务闭环（预约 / 核销 / 黑名单 / 看板）保持不变。
2. 先去污，再库存，再单 Agent，最后 RAG 和小程序 AI 页。
3. 每个阶段有可验收结果，完成后再进入下一阶段。

---

## 明确不做（第一版）

| 不做 | 原因 |
| --- | --- |
| RabbitMQ / 令牌桶 / GEO / Feed / 签到 | 不是预约主链路，容易做成点评课设拼盘 |
| 多 Agent / LangGraph / MCP / NL2SQL / 视频多模态 | 阶段 2 只保留一个助手 |
| Milvus / Elasticsearch / 百炼知识库 | RAG 用内存或 Redis 向量即可 |
| 真实短信服务 | 验证码保持 mock，文档标明演示 |

---

## 阶段 0：去污

目的：去掉简历减分项。本阶段不新增业务功能。

| 项 | 改什么 | 验收 |
| --- | --- | --- |
| 密钥 | `AiChatService` 中的模型 Key 改为环境变量或 `application.yml` 占位，不入库明文 | 仓库检索不到真实 Key |
| Redis | 打开 `application.yml` 的 Redis 配置，预约锁依赖真实 Redis | 无 Redis 时启动失败或明确报错，不再静默降级为唯一路径 |
| 鉴权 | 实现并注册 App / Admin 拦截器，JWT 真正校验；登录态接口使用 `RequireLogin` | 未带 Token 调用预约 / 下单返回 401 |
| 验证码 | 保持 mock `1234` | 根 README 写明「演示用固定验证码」 |
| 空拦截器 | 删除或实现 `AppAuthInterceptor` / `AdminAuthInterceptor` 的空 `return true` | 不再存在未注册的空壳拦截器 |

涉及文件（预期）：

- `ZDYZ/src/main/resources/application.yml`
- `ZDYZ/src/main/java/com/museum/service/AiChatService.java`
- `ZDYZ/src/main/java/com/museum/security/*`
- `ZDYZ/src/main/java/com/museum/config/*`
- `README.md`

### 进度（2026-08-14）

**状态：本机代码已完成；另一台有 MySQL/Redis 的机器拉取后做实机勾验。**

| 项 | 状态 | 说明 |
| --- | --- | --- |
| 密钥 | 已完成 | **现用** `application.yml` → `spring.ai.openai.api-key` 直接填写 DeepSeek Key（本项目不用环境变量）；勿把真实 Key 提交仓库 |
| Redis 配置 + 去降级 | 已完成 | `application.yml` 已启用 Redis；`JoinServiceImpl` 已删 `LOCAL_LOCKS` 静默降级 |
| 鉴权拦截器 | 已完成 | `AppAuthInterceptor` / `AdminAuthInterceptor` 已实现并在 `WebMvcConfig` 注册；登录态已标 `@RequireLogin` |
| 验证码文档 | 已完成 | 根 README 写明演示用固定验证码 `1234` |
| 本机验证 | 已完成 | 静态检索 + 编译；拦截器 Mock 单测 7 通过 |
| 实机验收 | 已完成（2026-08-16） | 无 Token 下单 401；有 Redis+MySQL 可启动并冒烟 |

下一阶段：阶段 2 正式实现（先 Tool Contract，再 DTO/Converter/ChatClient）。

---

## 阶段 1：预约库存（固定方案）

方案固定为：**Redis 预扣 + MySQL 事务 + 补偿**。不使用「只写库」「只写 Redis」「MQ 削峰」等替代路径。

改动中心：`BookingStockService` / `JoinServiceImpl.submit` / 取消预约。

补充口径（实现已遵守）：

- 请求内 `identityIds` 去重/拒绝（Java 前置 + Lua `-3`）
- MySQL `SUCC_CNT` 仅原子 SQL：`SUCC_CNT = SUCC_CNT + delta`（禁止 `updateById` 整行回写）
- `booking:booked` 设参观日结束 + 缓冲 TTL（不永久堆积）

### Redis Key

| Key | 含义 |
| --- | --- |
| `booking:stock:{timeMark}` | 该时段剩余名额 |
| `booking:booked:{day}:{identityId}` | 该游客该日已预约标记（一证一约） |

库存预热：时段首次被预约或应用启动时，用 `Time.limitCnt - Time.succCnt` 写入 `booking:stock:{timeMark}`，SETNX 避免覆盖。

### 下单主路径（顺序固定）

```text
1. Lua 预扣 Redis
   - 余票 < 人数 → 失败，不进 MySQL
   - 任一 identity 已在 booking:booked:{day}:{id} → 失败，不进 MySQL
   - 通过则 DECR 库存，并写入 booked 标记
2. 开启 MySQL 事务
   - 插入 join 记录
   - 更新 Time.succCnt
   - 事务提交
3. 事务失败或抛错 → 立即走补偿，不加「或者改成别的写法」
```

Lua 在单次脚本内完成：判余票、判一证一约、扣库存、写 booked。保证 Redis 侧原子。

### 补偿（顺序固定）

MySQL 事务失败时执行补偿 Lua：

1. `INCRBY booking:stock:{timeMark} {人数}`
2. 删除本次写入的 `booking:booked:{day}:{identityId}`

补偿失败：打 error 日志，记录 `timeMark` / `identityIds` / 人数，便于人工或定时对账。本阶段不对账任务做独立系统，但必须打日志。

### 取消预约（顺序固定）

1. MySQL 事务：校验可取消，更新 join 状态
2. 事务提交成功后执行补偿 Lua：回补 Redis 库存，删除 booked 标记
3. Redis 补偿失败：打 error 日志，不回滚已取消的 MySQL 状态（避免用户以为没取消）；依赖日志对账

### 压测验收

同 `timeMark` 并发预约，断言：

- `succCnt + Redis 剩余 = limitCnt`
- 不出现超卖
- 同一 `identityId` 同一天不能两单成功

README 记录压测结论（例如并发数、是否 0 超卖）。本阶段不做 MQ。

### 进度（2026-08-14）

**状态：已完成（2026-08-16 本机 JMeter + 真 Redis/MySQL）。**

| 项 | 状态 |
| --- | --- |
| Lua 预扣 / 补偿 + booked TTL | 已完成 |
| 请求内 identity 去重 | 已完成 |
| MySQL 原子 `incrSuccCnt` | 已完成 |
| JoinServiceImpl 下单/取消顺序 | 已完成 |
| 余票展示读 Redis | 已完成 |
| Mock 单测 | 已完成 |
| 真 Redis 并发压测 + README 结论 | **已完成**：80 并发 / LIMIT=50 → 成功 50、0 超卖；一证一约 10 并发仅 1 成功 |

---

## 阶段 2：单 Agent + 五个 Tool

只做一个预约助手，使用 Spring AI `ChatClient`（OpenAI-compatible → DeepSeek）+ `@Tool`。阶段 2 的实施顺序固定为：

```text
先确认现有业务能力
  → 再定义 Tool Contract（AI 接口契约）
    → 再做 DTO / Converter / Tool 适配层
      → 最后接 ChatClient / Prompt / 登录态
```

参考：Spring AI Method Tool 写法（历史参考过 `spring-ai-alibaba-tool-calling-example` 的 Tool 形态）；不参考多 Agent / Graph / MCP 示例。当前模型提供商为 DeepSeek（OpenAI-compatible）。

### 总原则

- 先定义业务能力契约，再定义 AI 能力契约，最后接 Agent；不要一上来直接写 Tool
- Tool 层只做参数接收、结果转换、调用现有 Service；不写 SQL，不碰 Redis，不承载库存或事务逻辑
- AI 暴露的是语义化字段，不直接暴露数据库状态码或表字段含义
- 阶段 2 只复用现有预约主链路：阶段 1 的 Redis 预扣 + MySQL 事务 + 补偿，不新增并行库存方案
- 阶段 1 真 Redis / MySQL 并发压测通过前，不把阶段 2 状态改成“开始开发”

### 2.0 AI 基础设施接入

目标：把聊天实现替换为 Spring AI `ChatClient` 调用链路（OpenAI-compatible 提供商，现用 DeepSeek）。

涉及内容：

- `pom.xml` 增加 Spring AI（`spring-ai-starter-model-openai`）相关依赖
- 新增 `ChatClient` Bean 与模型配置
- 在 `application.yml` 的 `spring.ai.openai.api-key` 直接填写 DeepSeek Key（本项目不用环境变量），不回退到明文入库的公共仓库实践
- `AiChatService` 从直连 HTTP 改为 `chatClient.prompt()...call()`

验收：

- 本机无 Redis / MySQL 时，允许完成依赖接入、编译、Mock/单测
- 有环境的机器上，`/ai/chat` 能走通新链路

### 2.1 现状分析与业务能力盘点

目标：先搞清楚当前预约系统已经有什么能力可复用，再决定 Tool 边界。

建议产出：

- `docs/ai-agent-design.md`

至少盘点以下对象：

- 现有 `AiChatService` / `AiChatController`
- `JoinService` / `JoinServiceImpl`
- `AppBookingController` / `AppRecordController`
- `Join`、`Time`、`Day`、`Identity` 等核心实体
- 阶段 1 Redis 预扣逻辑与错误码语义

要求记录清楚：

- 预约表、时段表、库存字段、状态字段、用户关联字段
- 哪些字段是数据库内部值，哪些可以转成 AI 语义字段
- 哪些能力可以直接复用，哪些必须做适配层隔离

### 2.2 Tool 边界与 Tool Contract

目标：固定 AI 允许调用的能力范围，并定义稳定的 AI 接口契约。

固定只做这五个 Tool：

| Tool | 调用 | 说明 |
| --- | --- | --- |
| `queryDays` | 现有查可预约日期 | 只读 |
| `queryTimes` | 现有查时段余票 | 只读，余票以 Redis/DB 为准 |
| `submitBooking` | 现有下单（阶段 1 的 Redis 预扣链路） | 写 |
| `listRecords` | 现有我的预约 | 只读，仅当前用户 |
| `cancelBooking` | 现有取消（含 Redis 补偿） | 写 |

Tool Contract 约束：

- 不直接暴露数据库字段名与数值语义，如避免直接返回 `type=1`
- 改为语义化表达，如 `status=FULL`
- 每个 Tool 都要明确 request / response / 可抛业务错误 / 对应 Service 调用
- 写 Tool 不接受外部 `userId` 参数，用户身份由系统上下文注入

建议在 `docs/ai-agent-design.md` 中按 Tool 写清：

- 名称
- 作用
- 输入字段
- 输出字段
- 对应业务调用
- 错误映射

### 2.3 AI DTO 与 Converter 适配层

目标：隔离数据库模型、现有业务返回结构和 AI 返回结构，避免模型直接依赖内部字段。

分层原则：

- 数据库模型：Entity / Mapper / 表字段
- 业务模型：现有 Service 参数与返回结构
- AI 模型：Tool Request / Tool Response DTO

适配层职责：

- `AI Request -> 业务参数`
- `业务结果 -> AI Response`
- `ErrorCode -> Tool 语义错误`

映射示例：

- `上午 -> MORNING`
- `下午 -> AFTERNOON`
- `BOOKING_SLOT_FULL -> FULL`
- `IDENTITY_DUPLICATE_BOOKING -> DUPLICATE_BOOKING`

建议：

- 阶段 2 先引入 `AI DTO + Converter`
- 不为了 Agent 先全面重构整个 `JoinService` 为完整 DTO 体系
- 避免把阶段 2 做成一次大规模业务重构

### 2.4 Tool 实现层

建议目录：

```text
ZDYZ/src/main/java/com/museum/ai/
  ├─ tool/
  ├─ dto/
  └─ converter/
```

建议核心类：

- `BookingTools.java`
- `BookingToolRequest/Response` 等 DTO
- `BookingToolConverter.java`

Tool 层只做三件事：

1. 接收模型参数
2. 调用 Converter 做转换
3. 调用现有 `JoinService`

明确不做：

- 不写 SQL
- 不操作 Redis
- 不重写库存逻辑
- 不新增第二套预约主链路

### 2.5 Agent 接入与登录态

目标：把 Tool 接到单 Agent，并把写操作和当前登录用户绑定。

实现要求：

- 一个 `ChatClient` Bean
- 一个系统提示词：博物馆预约助手，余票和下单只信 Tool 返回，禁止编造库存/预约号
- 绑定当前登录用户，写 Tool 内部只使用服务端解析出的 `userId`
- Tool 禁止暴露 `submitBooking(userId, ...)` 这种外部可传用户身份的接口

推荐链路：

```text
HTTP Request
  → JWT Token
    → Interceptor / UserContext
      → AiChatController / AiChatService
        → ChatClient
          → BookingTools
            → JoinService
```

约束：

- 无登录 Token 时，拒绝执行写 Tool
- 查询类 Tool 可按业务需要决定是否允许匿名调用；若允许，也不能泄露他人记录
- 本阶段只改后端 Agent 接口，小程序 AI 页不在本阶段改版

### 2.6 联调测试与验收

建议产出：

- `docs/ai-agent-test-case.md`

至少覆盖：

- 查询：如“后天下午有票吗”
- 下单：如“有票就帮我订”
- 查单：查询当前用户自己的预约记录
- 取消：取消当前用户自己的预约
- 防幻觉：不能编造预约号、库存、取消结果
- 权限：无 Token 时拒绝写操作

验收示例：

用户说「后天下午有票吗，有就帮我订」时：

1. 先调 `queryTimes`
2. 有票再调 `submitBooking`
3. 返回真实预约号或真实失败原因

### 阶段 2 明确不做

- 不做多 Agent / Planner / LangGraph
- 不做额外 Tool，固定只有 5 个
- 不做独立 Memory / 知识库
- 不做 NL2SQL
- 不改小程序 AI 页
- 不改阶段 1 库存主链路

### 进度（2026-08-17）

**状态：2.0–2.6 已完成（行为级联调 TC-01～06 全绿）。**

| 项 | 状态 |
| --- | --- |
| Spring AI OpenAI-compatible（DeepSeek）接入 | **已完成**（Boot 3.4.5 + spring-ai-bom 1.0.0 + starter-model-openai；Key 写在 `application.yml`） |
| 5 个 Tool 边界 | 已定稿 |
| Tool Contract / DTO / Converter | **已完成** |
| Tool 实现层 `BookingTools` + `@Tool` | **已完成** |
| `ChatClient` + System Prompt | **已完成**（`AiChatClientConfig`；无 Key 条件装配） |
| `AiChatService` ObjectProvider 降级 | **已完成**（含 `AiChatServiceTest`） |
| Interceptor→UserContext | **已完成**（`/app/**`+`/ai/**`；preHandle 开头 + afterCompletion clear） |
| Agent 联调与验收（2.6） | **已完成**：见 [docs/ai-agent-test-case.md](docs/ai-agent-test-case.md)；行为级（reply + HTTP 对照 / times+list 副作用快照），不验严格 Tool 序列 |

---

## 阶段 3（最后）：轻量 RAG，然后小程序 AI 页

本阶段在阶段 2 验收通过之后才做。顺序固定：先 RAG，再小程序 AI 页。

### 3.1 轻量 RAG

目标：在不改预约主链路、不引入多 Agent 的前提下，为当前单 Agent 增加「馆规 / 须知 / 公告」类问答能力，降低模型对参观规则、开放说明、活动通知等信息的幻觉率。

本阶段只解决「知识问答」，不把预约事务逻辑改成 RAG。阶段 2 已完成的 5 个 Tool（查票 / 下单 / 查单 / 取消）继续作为唯一业务真相来源；阶段 3.1 只是给模型补一层检索能力，让它在回答「能不能带包」「几点闭馆」「最近公告说了什么」这类问题时，先查资料再回答。

#### 3.1.1 目标与边界

要做：
- 语料范围固定为「公告、参观须知、活动通知」等现有内容，优先复用 `news` / `notice` 及现有静态文案
- 为 AI 提供一条独立于预约 Tool 的检索链路：召回相关片段 → 组装上下文 → 回答
- 未命中时明确说不知道或未查询到相关规定，不编造馆规
- 保持与阶段 2 单 Agent 共存：预约类问题仍走 Tool，须知类问题优先走 RAG
- 输出一套最小可验收的测试用例与运行说明

不做：
- 不上 Milvus / Elasticsearch / 外部知识库平台
- 不做多轮对话记忆、长期 Memory、用户画像
- 不做 NL2SQL，不让模型直接查业务库表
- 不把游客管理、库存、预约记录改造成可检索知识
- 不在本阶段改小程序 AI 页 UI；页面接入留给 3.2

#### 3.1.2 问题范围

阶段 3.1 只覆盖这三类问题：
- 馆规 / 须知：如「可以带背包吗」「迟到还能进吗」「需要身份证吗」
- 开放信息：如「今天开馆吗」「周一闭馆吗」「临时闭馆公告是什么」
- 公告 / 通知 / 活动说明：如「最近有什么通知」「暑期活动怎么参加」

不覆盖这三类问题：
- 实时预约事务：如「后天下午有票吗」「帮我订一张」；这些继续走 5 个 Tool
- 个人态问题：如「我的预约」「帮我取消刚才那单」；这些继续依赖登录态与 Tool
- 后台管理问题：如黑名单、核销、看板统计；不进入当前助手能力范围

#### 3.1.3 数据源与优先级

数据源优先级固定为：
1. 后台已维护且对用户可见的 `notice`
2. 后台已维护且对用户可见的 `news`
3. 仓库内已有的固定参观须知文案（若存在）

执行要求：
- 先做最小闭环，只接 1~2 类最稳定的数据源，优先 `notice` + `news`
- 每条入库语料都要保留来源元信息：来源类型、来源 ID、标题、发布时间/更新时间、可选跳转链接
- 过滤明显不适合检索的脏数据：空标题、空正文、仅图片、仅占位内容
- 若 `news` / `notice` 正文带 HTML，需在切片前做纯文本清洗

#### 3.1.4 技术路线（固定方案）

技术路线固定为「离线/启动时构建知识片段 + 运行时向量检索 + 单次注入上下文」：

1. 采集：
   从 `notice` / `news` Service 或 Mapper 拉取可见内容，统一转成 `RagDocument`
2. 清洗：
   去 HTML、去多余空白、保留标题/摘要/正文/发布时间
3. 切片：
   按段落或定长窗口切分，避免把整篇公告一次性塞给模型
4. 向量化：
   使用 Spring AI Embedding 能力生成向量
5. 存储：
   优先内存向量库；若本地已有 Redis 且接入成本低，可选 Redis 向量，但不引入新基础设施
6. 检索：
   用户问题先做相似检索，取 TopK 片段
7. 生成：
   将检索片段作为上下文拼进提示词，要求模型「只依据检索内容回答」
8. 兜底：
   若 TopK 为空或相似度过低，明确回复「我目前没有查到相关馆规/公告」

固定约束：
- 不让模型直接访问 `news` / `notice` 原始表结构
- 不让 RAG 参与预约写操作
- 不在本阶段引入重排序模型；先做最小可用召回
- 不做混合检索（关键词 + 向量）的大而全方案，除非最小方案明显不够用

#### 3.1.5 与当前 Agent 的集成方式

当前单 Agent 继续保留一个入口 `/ai/chat`。阶段 3.1 集成策略固定为：

- 预约事务问题：
  继续按阶段 2 的 System Prompt + 5 个 Tool 处理，RAG 不参与
- 须知类问题：
  先做 RAG 检索，再把检索结果提供给模型生成回复
- 混合问题：
  如「明天上午有票吗，另外可以带水吗」，允许同一轮里同时使用 Tool 与 RAG，但要保证：
  先从问题中拆出预约事务和馆规问答两部分；预约信息只信 Tool，馆规信息只信 RAG 片段

实现上优先选下面两种方式之一，保持简单：
- 方式 A：在 `AiChatService` 内先做意图分流，须知问题走 RAG，上下文拼接后再调 `ChatClient`
- 方式 B：保留统一 `ChatClient`，通过单独的 RAG 上下文构建器在 prompt 前注入检索结果

推荐优先做方式 A，因为：
- 改动边界更清晰
- 更容易证明「RAG 不影响预约 Tool」
- 联调时更容易定位是检索问题还是 Tool 问题

#### 3.1.6 目录与模块建议

建议新增独立 RAG 模块目录，避免把逻辑散落在 `AiChatService` 里：

```text
ZDYZ/src/main/java/com/museum/ai/rag/
  ├─ config/                 # RAG 开关、TopK、相似度阈值、数据源开关
  ├─ model/                  # RagDocument / RagChunk / RagHit
  ├─ loader/                 # news / notice 数据加载
  ├─ store/                  # 向量存储适配（memory / redis）
  ├─ service/                # 检索与组装上下文
  └─ support/                # 清洗、切片、来源格式化
```

当前业务模块的预期改动点：
- `AiChatService`：接入 RAG 分流或上下文注入
- `AiChatClientConfig`：补充 RAG 相关 prompt 片段或 advisor（若选用）
- `news` / `notice` 相关 Service/Mapper：提供最小数据读取能力
- `application.yml`：增加 RAG 配置项（开关、TopK、阈值、数据源）

#### 3.1.7 配置约定

阶段 3.1 配置必须满足：
- 默认可在本机直接启动，不依赖额外云组件
- 保持 DeepSeek Chat 配置继续写在 `application.yml`
- RAG 配置单独放在 `application.yml` 新节点下，避免污染阶段 2 的 Chat 配置

建议配置项：
- `museum.ai.rag.enabled`
- `museum.ai.rag.top-k`
- `museum.ai.rag.min-score`
- `museum.ai.rag.sources.news-enabled`
- `museum.ai.rag.sources.static-rules-enabled`
- `museum.ai.rag.rebuild-on-startup`

若阶段 3.1 需要 Embedding 模型：
- 先选一个可本地/当前供应商直接用的最小方案
- 不允许因此把 Chat Key 切回环境变量
- 若 Embedding 供应商与 Chat 供应商不同，必须在文档中明确分开配置，不混写

#### 3.1.8 提示词约束

阶段 3.1 的提示词必须新增这几条约束：
- 预约类事实只信 Tool，不信模型猜测
- 馆规 / 公告类事实只信检索结果，不信模型记忆
- 检索未命中时明确说不知道，不补全、不脑补
- 回复里尽量点明信息来源类型，如「根据最新公告 / 参观须知」
- 若用户同时问预约和馆规，分段回答，避免把两类真相源混在一起

#### 3.1.9 实施顺序（交给 Cursor 的执行顺序）

顺序固定为：

1. 盘点现有 `news` / `notice` 字段与可见性规则
2. 先接一个最小数据源（推荐 `notice`）
3. 完成文本清洗 + 切片 + 向量化 + 内存存储闭环
4. 在 `AiChatService` 增加最小 RAG 分流
5. 加入第二个数据源（`news`）
6. 补充开关、TopK、阈值等配置
7. 完成联调用例与文档
8. 全部通过后，才进入 3.2 小程序 AI 页

明确禁止：
- 一上来同时接 4 种数据源
- 一边做 RAG 一边重构阶段 2 Tool
- 在 3.1 顺手改前端页面

#### 3.1.10 交付物

至少产出：
- `docs/ai-rag-design.md`
  说明数据源、切片策略、检索策略、提示词约束、失败兜底
- `docs/ai-rag-test-case.md`
  记录实机测试用例与 Pass/Fail
- `README.md`
  补充 3.1 的启动方式、配置说明、示例问句
- 必要的单测 / 集成测试

代码交付至少应包含：
- RAG 配置
- 文档加载与切片
- 检索服务
- `AiChatService` 集成点
- 最小测试

#### 3.1.11 验收标准

至少覆盖以下测试：
- 命中类：
  「可以带背包吗」「周一开馆吗」「最近有什么公告」
- 未命中类：
  一个当前语料中没有答案的问题，回复必须明确不知道
- 混合类：
  「明天下午有票吗？另外可以带水吗」
  预约部分与 Tool 结果一致，馆规部分来自检索结果
- 防幻觉类：
  删除或关闭某条公告后，不应继续稳定复述旧结论

判据固定为：
- 回复中的馆规事实能在检索命中内容中找到依据
- 未命中时不编造
- 不影响阶段 2 已通过的 5 个 Tool 行为
- `compile` / 相关测试 / 必要联调全部通过

#### 3.1.12 风险与优先排查项

优先关注这些风险：
- `news` / `notice` 正文太脏，导致切片质量差
- 语料太短或太模板化，向量检索区分度不足
- RAG 分流误判，把预约事务问题错误送进知识问答
- 检索命中旧公告，回答过期信息

若联调失败，优先排查：
- 数据是否真的被加载
- 切片后是否为空
- 向量是否成功写入存储
- TopK / 阈值是否过严
- 提示词是否允许模型在未命中时胡编

#### 3.1.13 本阶段完成后的状态更新

**状态（2026-08-17）：3.1 已完成。** 实现要点：双 ChatClient（rules 无 Tool）、`listVisibleForRag` 全量装库、本地哈希向量 + 内存库、`visit-rules.md` 镜像须知。详见 [docs/ai-rag-design.md](docs/ai-rag-design.md)、[docs/ai-rag-test-case.md](docs/ai-rag-test-case.md)。

### 3.2 小程序 AI 页

目标：把现有小程序 `pages/aiChat` 从“一个直接 POST `/ai/chat` 的文本聊天壳”升级为真正可用的预约助手页，稳定承接阶段 2 的预约 Tool 能力和阶段 3.1 的 RAG 能力，并在移动端提供清晰、可信、可继续追问的交互体验。

#### 进度（2026-08-17）

**状态：已完成（代码 + 文档收尾）。**

| 项 | 状态 | 说明 |
| --- | --- | --- |
| 后端响应协议 | 已完成 | `/ai/chat` → `AiChatResponse`：`reply` + `intent` + `blocks` + `suggestions` |
| Tool/RAG → blocks | 已完成 | `AiChatBlockCollector`；RAG → `rules_source`；`queryDays` 不出块（可接受） |
| 小程序请求收口 | 已完成 | `api/ai.js` + `utils/request.js`（带 Token）；无页面硬编码 host |
| 三段式 AI 页 | 已完成 | 欢迎语 / chips / 结构化卡片 / 登录提示 / 快捷跳转 / 重试 |
| 文档 | 已完成 | [docs/ai-miniapp-page.md](docs/ai-miniapp-page.md)、README、[docs/api-contract.md](docs/api-contract.md) §9.3 |
| 单测 | 已完成 | `AiChatServiceTest`、`AiChatBlockCollectorTest` |

仓库现状（完成后）：
- 页面：`app-front/ZDYZ/miniprogram/pages/aiChat`（首页悬浮球进入）
- 请求统一走 `api/ai.js` → `utils/request.js`，不再硬编码 `localhost`
- 消息按 `role/text/blocks/suggestions/status` 渲染；支持余票卡、预约记录卡、须知来源块、tips
- 后端结构化块由 Tool 旁路收集与 RAG 命中生成，前端禁止正则解析 `reply`

#### 3.2.1 目标与边界

要做：
- 复用现有单 Agent 接口，支持预约问答与须知问答
- 复用小程序现有 Token 体系与 `utils/request.js`，不再在页面里硬编码 `localhost`
- 优化 AI 页的消息流：欢迎语、输入态、加载态、错误态、重试
- 将“预约事务结果”和“馆规/RAG 结果”做前端分区展示，避免全靠长文本堆叠
- 支持推荐问句、快捷追问、常见意图入口
- 在不引入流式输出的前提下，做到移动端可用、可读、可验证

不做：
- 不改预约主链路、不改 BookingTools / JoinService
- 不做多 Agent、多会话记忆、语音输入、富媒体生成
- 不做 WebSocket/流式输出，先用普通请求闭环
- 不做复杂埋点平台，只保留必要日志和调试信息
- 不在本阶段重做整个小程序视觉系统，只围绕 `pages/aiChat` 做增强

#### 3.2.2 页面现状问题（必须在实施前明确）

当前 `pages/aiChat` 的主要问题：
- 直接写死 `http://localhost:8081/ai/chat`，与项目已存在的 `utils/request.js` 不一致
- 没有统一复用 `request.js` 中的 `Token` 注入逻辑
- 只把响应当作 `{ reply }` 文本，不支持结果卡片化
- 错误提示过于粗糙，无法区分“未登录”“网络失败”“AI 未配置”“未命中馆规”
- 没有示例问句，也没有快捷跳转到预约、记录、须知等页面的能力

因此，3.2 不是简单换皮，而是：
1. 页面层重构
2. 请求层收口到统一工具
3. 与后端 AI 返回协议对齐

#### 3.2.3 固定交互方案

页面交互固定为“三段式助手页”：

1. 顶部引导区
- 显示助手定位：预约、查单、取消、参观须知
- 显示 3~5 个推荐问句按钮
- 用户未登录时显示轻提示，但允许先问公开信息

2. 会话消息区
- 用户消息：普通气泡
- 机器人消息：按内容类型渲染
  - 普通文本说明
  - 预约结果卡片
  - 余票列表卡片
  - 须知来源块 / 公告来源块
  - 未命中提示块

3. 底部输入区
- 文本输入 + 发送按钮
- 常见快捷动作：查余票、我的预约、参观须知
- 发送中禁重复提交

交互要求：
- 每次发送后滚到底部
- 首次进入显示欢迎语 + 示例问句
- AI 回复后若可继续操作，显示快捷追问 chips
  - 例如“查看明天时段”“查询我的预约”“查看参观须知”

#### 3.2.4 前后端接口口径（先对齐再开工）

阶段 3.2 若想实现“结果卡片化”，后端返回不能只停留在纯 `reply` 文本。计划固定要求：

现有兼容口径：
```json
{ "reply": "..." }
```

3.2 目标口径（向后兼容）：
```json
{
  "reply": "...",
  "intent": "BOOKING|RULES|MIXED",
  "blocks": [
    {
      "type": "text|time_slots|booking_records|rules_source|tips",
      "title": "...",
      "items": []
    }
  ],
  "suggestions": ["后天下午有票吗", "我的预约", "可以带背包吗"]
}
```

说明：
- `reply` 继续保留，保证旧前端不挂
- `intent` 供页面做样式和交互策略判断
- `blocks` 供小程序做结构化渲染
- `suggestions` 供快捷追问

如果后端暂时来不及补全 `blocks`：
- 3.2 第一版允许先消费 `reply + intent + suggestions`
- 但计划目标仍是补到结构化 `blocks`，否则“展示 Tool 结果，不只吐纯文本”无法完成

#### 3.2.5 页面数据结构建议

建议将前端消息从当前简单 `{ type, content }` 升级为：

```text
ChatMessage
├─ id: string
├─ role: user | assistant | system
├─ text: string
├─ blocks?: ChatBlock[]
├─ suggestions?: string[]
├─ status?: sending | success | error
└─ meta?: { intent, sourceTypes, createdAt }
```

```text
ChatBlock
├─ type: text | time_slots | booking_records | rules_source | tips
├─ title?: string
├─ items?: object[]
└─ source?: string
```

这样可以保证：
- 老的纯文本回复还能显示
- 新增结构化结果时不用再重写整页

#### 3.2.6 UI 与视觉方向

页面继续延用当前项目的“博物馆 / 米黄 / 故宫红”方向，但不保留现在那种仅气泡聊天的单调形态。

建议视觉方案：
- 顶部加入“助手身份卡”，明确它能做什么
- 推荐问句做成可点击标签，而不是写在 placeholder 里
- 预约结果与馆规结果用不同卡片样式区分
- `rules_source` 块显示“来源：参观须知 / 公告”
- 余票卡片按上午 / 下午分组；有余票和已约满状态颜色明确区分
- 未登录但可查公开信息时，页面不要强制打断；只有写操作才提示登录

移动端要求：
- 兼容底部安全区
- 消息列表和输入栏不互相遮挡
- 长文本能折行，来源块能展开/收起

#### 3.2.7 与现有小程序基础设施的集成

3.2 必须复用项目已有能力，不另起炉灶：

- 请求：
  使用 `app-front/ZDYZ/miniprogram/utils/request.js`
- Token：
  继续使用本地 `wx.getStorageSync('token')`
- 登录：
  复用当前 `app.js` 的登录逻辑与登录页
- 路由：
  保留 `pages/aiChat/aiChat` 页面路径，不新增第二个 AI 页面

明确禁止：
- 在 `aiChat.js` 里继续手写 `wx.request` 调 `localhost`
- 在页面里重复实现 Token 注入
- 前端自己猜测“有没有票”“预约是否成功”

#### 3.2.8 功能拆分

阶段 3.2 建议按以下功能拆分：

1. 基础重构
- 改为使用统一 `request.js`
- 抽离发送消息、渲染消息、错误提示逻辑

2. 首屏与推荐问句
- 默认欢迎语
- 常见问句 chips
- 历史消息初始化

3. 结构化消息渲染
- 文本块
- 余票卡片
- 预约记录卡片
- 来源块

4. 登录态与写操作提示
- 未登录问公开信息可直接查
- 未登录写操作给出跳登录提示
- 支持从 AI 页跳转登录 / 我的预约 / 预约页

5. 混合问句体验
- 同一轮消息里可同时显示预约结果块和须知来源块
- 页面上分区展示，避免一大段文本混在一起

#### 3.2.9 需要的后端最小配合

3.2 前端计划默认需要以下后端配合项：
- `/ai/chat` 返回 `intent`
- `/ai/chat` 可选返回 `blocks`
- `/ai/chat` 可选返回 `suggestions`

如果后端暂不返回结构化块，则 Cursor 在前端侧只做兼容渲染，不做 brittle 的文本正则解析。明确禁止靠解析自然语言文案来拼余票卡片或预约卡片，因为这会极易被模型表述变化打断。

#### 3.2.10 实施顺序（交给 Cursor 的执行顺序）

顺序固定为：

1. 盘点现有 `pages/aiChat`、`utils/request.js`、登录态逻辑
2. 先把 `aiChat.js` 改为统一请求封装，不再硬编码地址
3. 重构消息数据结构与基础样式
4. 加入首屏欢迎语、推荐问句、发送中状态
5. 对接后端 `intent`
6. 若后端已支持，则接入 `blocks` 和 `suggestions`
7. 加入登录提示、快捷跳转与常见追问
8. 完成移动端联调与文档

明确禁止：
- 未完成请求收口就直接堆 UI
- 用正则从 `reply` 猜结构化数据
- 在 3.2 顺手返工 3.1 RAG 或 2.x Tool

#### 3.2.11 交付物

至少产出：
- `app-front/ZDYZ/miniprogram/pages/aiChat/aiChat.js`
- `app-front/ZDYZ/miniprogram/pages/aiChat/aiChat.wxml`
- `app-front/ZDYZ/miniprogram/pages/aiChat/aiChat.wxss`
- 如有必要的子组件，可新增 `app-front/ZDYZ/miniprogram/components/ai-*`
- `docs/ai-miniapp-page.md`
  说明页面结构、消息协议、交互约定、登录态策略
- `README.md`
  补充小程序 AI 页的使用说明

#### 3.2.12 验收标准

至少覆盖以下行为级用例：
- 公开须知问句：
  「可以带背包吗」
  页面能显示回答正文，并展示来源块（如 `static_rules` / `news`）
- 预约问句：
  「后天下午有票吗」
  页面能正常显示回复；若后端有 `blocks`，则展示余票卡片
- 写操作问句：
  未登录发送「帮我订明天上午」
  页面明确提示需登录，不假装成功
- 混合问句：
  「明天下午有票吗？另外可以带水吗」
  页面能分区呈现预约信息和须知信息
- 异常问句：
  网络失败 / AI 未配置 / RAG 未命中
  页面有稳定兜底，不白屏

验收判据：
- 不再直接写死 `localhost`
- 统一走 `request.js`
- 预约类问题与阶段 2/3.1 后端行为一致
- 须知类问题能体现来源
- 页面在真机与开发者工具中都能正常滚动、输入、回显

#### 3.2.13 风险与优先排查项

优先关注这些风险：
- 后端仍只返回纯 `reply`，导致前端无法真正结构化展示
- 小程序端消息结构升级后，旧消息渲染逻辑残留导致空白或错位
- Token 状态与页面提示不一致，出现“已登录却提示未登录”
- 长文本和来源块在小屏设备上溢出

若联调失败，优先排查：
- `request.js` 是否正确带上 `Token`
- `/ai/chat` 响应结构是否与页面消费逻辑一致
- `intent` 是否正确返回
- `blocks` 是否缺失或字段名不一致
- 页面滚动与输入区是否被安全区/键盘遮挡

#### 3.2.14 本阶段完成后的状态更新

**状态（2026-08-17）：3.2 已完成；阶段 3 全部完成。**

已回写：
- 本文件进度表与「仓库现状」
- `README.md`：AI 页入口与示例问句
- `docs/ai-miniapp-page.md`：页面结构、协议、登录策略、验收用例
- `docs/api-contract.md` §9.3：与 `AiChatResponse` 对齐

验收对照 3.2.12：代码侧已具备（单测覆盖 intent/blocks/suggestions 与收集器）；真机/开发者工具行为勾验按需补做。

### 文档

根 README 补充：启动方式、mock 验证码、Redis 依赖、压测结论、Agent 示例问句。

---

## 阶段顺序

```text
阶段 0 去污                    ← 已完成
  → 阶段 1 Redis 预扣 + MySQL 事务 + 补偿   ← 已完成（含 JMeter 压测）
    → 阶段 2 一个 Agent + 五个 Tool         ← 已完成（含 2.6）
      → 阶段 3.1 轻量 RAG                    ← 已完成
        → 阶段 3.2 小程序 AI 页              ← 已完成
```

不可并行把 3.2 提前到阶段 2：小程序 AI 页必须等 Agent 和 RAG 都就绪。

---

## 简历写法（全部完成后再用）

- 预约库存：Redis 预扣，Lua 原子扣减；MySQL 事务落单；失败补偿，避免超卖与一证多约
- 预约 Agent：Spring AI 单助手（DeepSeek / OpenAI-compatible），五个业务 Tool 完成查票 / 下单 / 查单 / 取消
- 须知问答：公告向量检索，降低幻觉
- 管理端：核销、黑名单、看板（现有能力，保持）

---

## 涉及模块

| 阶段 | 主要改动 |
| --- | --- |
| 0 | 配置、JWT 拦截器、密钥、README |
| 1 | `BookingStockService`、`JoinServiceImpl`、Lua 脚本、并发测试 |
| 2 | 重写 `AiChat*`、新增 5 个 Tool、ChatClient 配置 |
| 3 | 公告向量、`pages/aiChat`、README 示例 |

后端测试：阶段 1 必须有并发不超卖测试；阶段 2 测五个 Tool 的权限与调用，不测「多个 Agent 协作」。
