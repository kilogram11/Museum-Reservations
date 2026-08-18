# 预约 Agent 设计（阶段 2.1 / 2.2）

> 输入材料：[api-contract.md](./api-contract.md)  
> 执行口径：[OPTIMIZATION_PLAN.md](../OPTIMIZATION_PLAN.md)  
> 范围：单 Agent + **仅 5 个 Tool**；不写 SQL、不碰 Redis、不重写库存  
> 状态：Tool Contract **已定稿**；2.0–2.6 **已完成**（行为级联调见 [ai-agent-test-case.md](./ai-agent-test-case.md)）；下一步阶段 3.1 轻量 RAG

---

## 1. 目标与边界

做一个博物馆预约助手：用户用自然语言查票 / 下单 / 查单 / 取消。模型只能通过 Tool 获取业务真相，禁止编造余票、预约号、取消结果。

| 做 | 不做（阶段 2） |
| --- | --- |
| 5 个固定 Tool | 多 Agent / Planner / LangGraph / MCP |
| 语义化 request / response | 直接暴露 `joinStatus=1` 等库内数字 |
| 复用 `JoinService` 主链路 | 第二套库存方案、游客 CRUD Tool、核销 Tool |
| 写操作绑定登录用户 | Tool 入参接受外部 `userId` |
| 文档 + 后续 DTO/Converter | 本文件不包含实现代码 |

---

## 2. 能力盘点（2.1）

### 2.1 可直接复用

| 能力 | Service | 说明 |
| --- | --- | --- |
| 可预约日期 | `JoinService.getBookingDays()` | 返回 `List<Map>`：day / week / status |
| 时段余票 | `JoinService.getBookingTimes(day)` | 余票已读 Redis；字段 surplus / total / timeMark |
| 下单 | `JoinService.submitBooking(userId, timeMark, identityIds)` | 阶段 1 Redis 预扣 + MySQL + 补偿；成功 `void` |
| 我的预约 | `JoinService.getMyBookings(userId)` | `List<Join>`，含 enrich 场馆与时段 |
| 取消 | `JoinService.cancelBooking(userId, joinId)` | MySQL 成功后 Redis 补偿 |

Tool 层**绕过 HTTP**，直接调上述方法。

### 2.2 必须经适配层（Converter）

| 业务/DB | AI 语义 | 原因 |
| --- | --- | --- |
| `day.status` 1/0 | `OPEN` / `CLOSED` | 禁止数字语义 |
| `surplus` | `remain`；且 `remain==0` → 槽位 `FULL` | 模型友好 |
| `startTime` | `period=MORNING\|AFTERNOON` | 业务无枚举，按小时推断 |
| `joinStatus` 1/2 | `BOOKED` / `CANCELLED` | 禁止暴露表字段码 |
| `joinIsCheckin` 0/1/3 | `UNCHECKED` / `CHECKED_IN` / `EXPIRED` | 同上 |
| `joinForms` JSON 字符串 | 展开为 `visitorName`（可选脱敏证件） | 勿把原始 JSON 字符串塞给模型 |
| `ErrorCode` 整型 | Tool `error` 枚举字符串 | 见 §5 |
| Service 的 `userId` | **不进 Tool 入参**，由 UserContext / Token 注入 | 安全硬约束 |

### 2.3 暂不进 Tool

| 能力 | 原因 |
| --- | --- |
| `IdentityService` 增删改游客 | 阶段 2 固定 5 Tool；下单假定已有 `identityIds` |
| 管理端核销 / 看板 / 黑名单管理 | 非用户预约助手范围 |
| `GET /app/record/detail` | 可用 `listRecords` 覆盖 |
| 现状 Hutool `/ai/chat` 纯文本 | 将被 ChatClient + Tool 替换 |

### 2.4 现状 AI 入口（2.5 已落地）

- [`AiChatController`](../ZDYZ/src/main/java/com/museum/controller/AiChatController.java)：`POST /ai/chat`，`{message}` → `{reply}`，**无强制登录**、**无 Result 壳**（可选 Token；写 Tool 靠 UserContext）
- [`AiChatService`](../ZDYZ/src/main/java/com/museum/service/AiChatService.java)：Spring AI `ChatClient`（OpenAI 兼容 → DeepSeek）

已落地链路：

```text
HTTP + Token
  → Interceptor / UserContext
    → AiChatController / AiChatService
      → ChatClient + System Prompt
        → BookingTools（仅 5 个）
          → Converter
            → JoinService
```

---

## 3. 全局约定

### 3.1 Tool 统一结果壳（AI 层，非 HTTP Result）

每个 Tool 返回同一结构，便于模型与后续前端解析：

```text
ToolResult
├─ ok      : boolean
├─ error   : string|null   # 失败时语义码，见 §5；成功为 null
├─ message : string|null   # 人类可读短句（可给用户看）
└─ data    : object|null   # 成功载荷；失败一般为 null
```

**JSON**

```json
{
  "ok": true,
  "error": null,
  "message": null,
  "data": {}
}
```

### 3.2 登录态

| Tool | 是否要求登录 | 说明 |
| --- | --- | --- |
| `queryDays` | 否 | 公开排期 |
| `queryTimes` | 否 | 公开余票；不得泄露他人记录 |
| `submitBooking` | **是** | 无 Token → `UNAUTHORIZED`，不调 Service |
| `listRecords` | **是** | 仅当前用户；无 Token → `UNAUTHORIZED` |
| `cancelBooking` | **是** | 无 Token → `UNAUTHORIZED` |

写 Tool **禁止**声明 `userId` / `user_id` 参数。实现内从 `UserContext`（由 JWT 解析）取值。

### 3.3 时段 period 推断规则（定稿）

对 `startTime`（`HH:mm` 或 `H:mm`）：

- 规范化后小时 `< 12` → `MORNING`
- 小时 `>= 12` → `AFTERNOON`

不引入 `EVENING`（当前排期模板为上下午两段）。

### 3.4 身份证件展示

`listRecords` 可返回 `visitorName`；`visitorCard` **默认脱敏**（保留前 4 后 4，中间 `*`），避免模型回显完整身份证。下单入参只用 `identityIds`，不传证件号。

---

## 4. 五个 Tool Contract（2.2）

### 4.1 `queryDays`

| 项 | 内容 |
| --- | --- |
| 作用 | 查询近几日可预约日期 |
| 读写 | 读 |
| 登录 | 否 |
| 业务调用 | `JoinService.getBookingDays()` |

**Request**

```text
（无参数）
```

```json
{}
```

**Response `data`**

```text
data
└─ days[] DaySlot
     ├─ day    : string   # yyyy-MM-dd
     ├─ week   : string   # 如「周六」
     └─ status : string   # OPEN | CLOSED
```

```json
{
  "ok": true,
  "error": null,
  "message": null,
  "data": {
    "days": [
      { "day": "2026-08-16", "week": "周日", "status": "OPEN" },
      { "day": "2026-08-17", "week": "周一", "status": "CLOSED" }
    ]
  }
}
```

**映射**

| 业务 | AI |
| --- | --- |
| `status=1` | `OPEN` |
| `status=0` | `CLOSED` |
| 空列表 | `ok=true`，`days=[]` |

**可抛错误**：一般无业务错误；底层异常 → `INTERNAL_ERROR`。

---

### 4.2 `queryTimes`

| 项 | 内容 |
| --- | --- |
| 作用 | 查询某日各时段余票 |
| 读写 | 读 |
| 登录 | 否 |
| 业务调用 | `JoinService.getBookingTimes(day)` |

**Request**

```text
QueryTimesRequest
└─ day : string  # 必填，yyyy-MM-dd
```

```json
{
  "day": "2026-08-16"
}
```

**Response `data`**

```text
data
├─ day   : string
└─ times[] TimeSlot
     ├─ timeMark  : string
     ├─ startTime : string
     ├─ endTime   : string
     ├─ period    : string   # MORNING | AFTERNOON
     ├─ total     : int
     ├─ remain    : int      # ← 业务 surplus
     ├─ used      : int
     └─ status    : string   # AVAILABLE | FULL
```

```json
{
  "ok": true,
  "error": null,
  "message": null,
  "data": {
    "day": "2026-08-16",
    "times": [
      {
        "timeMark": "museum_load_2026-08-16_09:00",
        "startTime": "09:00",
        "endTime": "11:00",
        "period": "MORNING",
        "total": 50,
        "remain": 38,
        "used": 12,
        "status": "AVAILABLE"
      },
      {
        "timeMark": "museum_load_2026-08-16_14:00",
        "startTime": "14:00",
        "endTime": "16:00",
        "period": "AFTERNOON",
        "total": 50,
        "remain": 0,
        "used": 50,
        "status": "FULL"
      }
    ]
  }
}
```

**映射**

| 业务 | AI |
| --- | --- |
| `surplus` | `remain` |
| `remain > 0` | `status=AVAILABLE` |
| `remain == 0` | `status=FULL` |

**可抛错误**

| 条件 | error |
| --- | --- |
| `day` 为空或格式非法 | `BAD_REQUEST` |

余票以 Tool 返回为准；模型不得臆造 `remain`。

---

### 4.3 `submitBooking`

| 项 | 内容 |
| --- | --- |
| 作用 | 为当前登录用户提交预约 |
| 读写 | 写 |
| 登录 | **是** |
| 业务调用 | `JoinService.submitBooking(userIdFromContext, timeMark, identityIds)` |

**Request（禁止 userId）**

```text
SubmitBookingRequest
├─ timeMark     : string     # 必填，来自 queryTimes
└─ identityIds  : string[]   # 必填，1~3，不可重复
```

```json
{
  "timeMark": "museum_load_2026-08-16_09:00",
  "identityIds": ["identity_load_001"]
}
```

**Response `data`（成功）— 定稿口径**

当前业务 `submitBooking` 成功返回 `void`，HTTP `data=null`，**不返回预约号**。

阶段 2 **不改** `JoinService` 签名去硬塞 joinId。成功时：

```json
{
  "ok": true,
  "error": null,
  "message": "预约成功",
  "data": {
    "booked": true,
    "timeMark": "museum_load_2026-08-16_09:00",
    "visitorCount": 1,
    "joinIds": null
  }
}
```

**System Prompt / Agent 行为约定**：若用户需要预约号，成功后**再调** `listRecords`（可按 `timeMark` / 当日过滤由模型在对话中完成），禁止编造 `joinId`。

后续若业务层扩展为返回 joinId 列表，仅扩展 `data.joinIds`，不改 Tool 名称。

**可抛错误**（见 §5 全表；常用如下）

| ErrorCode | error |
| --- | --- |
| 401 | `UNAUTHORIZED` |
| 2001 | `IDENTITY_NOT_FOUND` |
| 2002 | `BLACKLISTED` |
| 2003 | `DUPLICATE_BOOKING` |
| 2004 | `DUPLICATE_IN_REQUEST` |
| 3001 | `SLOT_INVALID` |
| 3002 | `FULL` |
| 3003 | `TOO_MANY` |
| 3004 | `NO_VISITORS` |
| 3011 | `SCHEDULE_ERROR` |
| 3012 | `STOCK_UNAVAILABLE` |

失败示例：

```json
{
  "ok": false,
  "error": "FULL",
  "message": "该时段余量不足",
  "data": null
}
```

---

### 4.4 `listRecords`

| 项 | 内容 |
| --- | --- |
| 作用 | 列出当前用户预约记录 |
| 读写 | 读 |
| 登录 | **是** |
| 业务调用 | `JoinService.getMyBookings(userIdFromContext)` |

**Request**

```text
ListRecordsRequest
├─ day    : string|null   # 可选，yyyy-MM-dd，过滤 joinMeetDay
└─ status : string|null   # 可选，BOOKED | CANCELLED；默认全部
```

```json
{
  "day": "2026-08-16",
  "status": "BOOKED"
}
```

过滤在 Converter / Tool 内对 Service 结果做内存过滤（不新增 Service 方法）。

**Response `data`**

```text
data
└─ records[] BookingRecord
     ├─ joinId        : string
     ├─ day           : string          # joinMeetDay
     ├─ timeMark      : string
     ├─ startTime     : string|null
     ├─ endTime       : string|null
     ├─ period        : string|null     # 由 startTime 推断
     ├─ status        : string          # BOOKED | CANCELLED
     ├─ checkin       : string          # UNCHECKED | CHECKED_IN | EXPIRED
     ├─ visitorName   : string|null
     ├─ visitorCardMasked : string|null
     ├─ museumTitle   : string|null
     └─ museumAddress : string|null
```

**不返回**：`joinQr` Base64、完整证件号、原始 `joinForms` 字符串、表主键 `_id`（取消统一用 `joinId`）。

```json
{
  "ok": true,
  "error": null,
  "message": null,
  "data": {
    "records": [
      {
        "joinId": "join_d4e5f6",
        "day": "2026-08-16",
        "timeMark": "museum_load_2026-08-16_09:00",
        "startTime": "09:00",
        "endTime": "11:00",
        "period": "MORNING",
        "status": "BOOKED",
        "checkin": "UNCHECKED",
        "visitorName": "张三",
        "visitorCardMasked": "1101**********1234",
        "museumTitle": "压测博物馆",
        "museumAddress": "北京市..."
      }
    ]
  }
}
```

**映射**

| 业务 | AI |
| --- | --- |
| `joinStatus=1` | `BOOKED` |
| `joinStatus=2` | `CANCELLED` |
| `joinIsCheckin=0/1/3` | `UNCHECKED` / `CHECKED_IN` / `EXPIRED` |

**可抛错误**：`UNAUTHORIZED`；其它 → `INTERNAL_ERROR`。

---

### 4.5 `cancelBooking`

| 项 | 内容 |
| --- | --- |
| 作用 | 取消当前用户的一条预约 |
| 读写 | 写 |
| 登录 | **是** |
| 业务调用 | `JoinService.cancelBooking(userIdFromContext, joinId)` |

**Request（禁止 userId）**

```text
CancelBookingRequest
└─ joinId : string  # 必填，业务预约号
```

```json
{
  "joinId": "join_d4e5f6"
}
```

**Response `data`（成功）**

```json
{
  "ok": true,
  "error": null,
  "message": "取消成功",
  "data": {
    "cancelled": true,
    "joinId": "join_d4e5f6"
  }
}
```

**可抛错误**

| ErrorCode / 条件 | error |
| --- | --- |
| 未登录 | `UNAUTHORIZED` |
| `joinId` 空白 | `BAD_REQUEST` |
| 3005 | `NOT_FOUND` |
| 3006 | `STATUS_INVALID` |
| 3007 | `ALREADY_CHECKED_IN` |

---

## 5. ErrorCode → Tool 错误枚举（全表）

| HTTP/业务 code | ErrorCode | Tool `error` | 典型 Tool |
| --- | --- | --- | --- |
| 401 | UNAUTHORIZED | `UNAUTHORIZED` | 写 / list |
| 400 | （参数） | `BAD_REQUEST` | 各 Tool |
| 2001 | IDENTITY_NOT_FOUND | `IDENTITY_NOT_FOUND` | submit |
| 2002 | IDENTITY_BLACKLISTED | `BLACKLISTED` | submit |
| 2003 | IDENTITY_DUPLICATE_BOOKING | `DUPLICATE_BOOKING` | submit |
| 2004 | IDENTITY_DUPLICATE_IN_REQUEST | `DUPLICATE_IN_REQUEST` | submit |
| 3001 | BOOKING_SLOT_INVALID | `SLOT_INVALID` | submit |
| 3002 | BOOKING_SLOT_FULL | `FULL` | submit |
| 3003 | BOOKING_TOO_MANY | `TOO_MANY` | submit |
| 3004 | BOOKING_NO_VISITORS | `NO_VISITORS` | submit |
| 3005 | BOOKING_NOT_FOUND | `NOT_FOUND` | cancel |
| 3006 | BOOKING_STATUS_INVALID | `STATUS_INVALID` | cancel |
| 3007 | BOOKING_ALREADY_CHECKED_IN | `ALREADY_CHECKED_IN` | cancel |
| 3011 | BOOKING_SCHEDULE_ERROR | `SCHEDULE_ERROR` | submit |
| 3012 | BOOKING_STOCK_REDIS_ERROR | `STOCK_UNAVAILABLE` | submit |
| 500 | INTERNAL_ERROR 等 | `INTERNAL_ERROR` | 全部 |

Converter：`BusinessException.getCode()` → 上表；未命中 → `INTERNAL_ERROR`，`message` 可保留业务原文短句。

---

## 6. System Prompt 要点（给 2.5）

1. 你是博物馆预约助手；余票、预约结果、预约号**只信 Tool 返回**。
2. 用户问「有没有票」→ 先 `queryDays` / `queryTimes`，再回答；禁止编造 `remain`。
3. 「有票就帮我订」→ 确认时段与游客后 `submitBooking`；成功若需预约号再 `listRecords`。
4. 取消必须使用真实 `joinId`（来自 `listRecords`），禁止猜测。
5. 未登录时写操作失败：提示用户先登录；不要假装已下单。
6. 不做馆规 RAG（阶段 3）；不知道的规定说不知道。

---

## 7. 工程落点

```text
ZDYZ/src/main/java/com/museum/ai/
  ├─ config/AiChatClientConfig.java  # 2.0/2.5：ChatClient + System Prompt + defaultTools
  ├─ tool/BookingTools.java          # 2.4/2.5：5 方法 + @Tool
  ├─ dto/                            # 2.3：*Request / ToolResult / *Data / 语义枚举
  ├─ converter/BookingToolConverter.java
  └─ context/UserContext.java        # ThreadLocal；AppAuthInterceptor set/clear
```

**2.3 进度（2026-08-16）**：DTO + Converter + `UserContext` 已落地；单测 `BookingToolConverterTest` 10 例通过。

**2.4 进度（2026-08-17）**：`BookingTools` 编排已落地；单测 `BookingToolsTest` 11 例通过。

**2.0 / 2.5 进度（2026-08-17）**：Boot 3.4.5 + `spring-ai-starter-model-openai`（DeepSeek：`base-url=https://api.deepseek.com`，模型 `deepseek-v4-flash`）；Key 在 `application.yml` 的 `spring.ai.openai.api-key` 直接配置（**不用环境变量**）；`AiChatService` 经 `ObjectProvider<ChatClient>` 可选注入；`BookingTools` 已加 `@Tool`；`AppAuthInterceptor` 覆盖 `/ai/**` 并管理 `UserContext`。

职责固定：

1. 收模型参数  
2. Converter 转换  
3. 调 `JoinService`  

---

## 8. 验收话术（参考意图；正式用例见 [ai-agent-test-case.md](./ai-agent-test-case.md)）

> `/ai/chat` 仅返回 `reply`，**不能**严格证明 Tool 序列。下表为排障参考；Pass/Fail 以 test-case 的行为判据与副作用观测为准。

| 用户说法 | 参考 Tool 意图（非严格验收） |
| --- | --- |
| 后天下午有票吗 | `queryDays`（可选）→ `queryTimes` |
| 有票就帮我订 | `queryTimes` →（有票）`submitBooking` →（若要号）`listRecords` |
| 我的预约 | `listRecords` |
| 取消刚才那单 | `listRecords` → `cancelBooking` |
| 无 Token 下单 | `submitBooking` → `UNAUTHORIZED` |

---

## 9. 与 api-contract 的关系

| 文档 | 角色 |
| --- | --- |
| [api-contract.md](./api-contract.md) | 现有 HTTP / Service / DB 真相 |
| **本文件** | AI 语义投影与 Tool 边界；实现与联调的契约源 |

实现时以本文件字段名为准；业务变更先改 Service，再同步 Converter 映射，避免模型直接依赖表结构。
