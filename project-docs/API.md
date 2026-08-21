# 接口文档 · API

> 后端端口默认 `8081`。所有响应统一 `Result<T>`：`{ code, msg, data }`，HTTP 恒 200，业务结果由 `code` 表达。
> 鉴权头：管理端 `Authorization: Bearer <token>` 或 `Token: <token>`；小程序端 `Token: <token>`。
> 最权威契约见 `docs/api-contract.md`，本文档为结构化索引。

## 0. 响应格式与错误码

### 统一响应
```json
{ "code": 200, "msg": "操作成功", "data": <object|array|null> }
```

### 错误码分段

| 区间 | 含义 |
|------|------|
| 200 | SUCCESS |
| 400 | BAD_REQUEST |
| 401 | UNAUTHORIZED（无/无效 Token） |
| 403 | FORBIDDEN（权限不足） |
| 500 | INTERNAL_ERROR |
| 1001-1003 | 用户类（未找到 / 手机号空 / 验证码错） |
| 2001-2004 | 身份/访客类（未找到 / 已拉黑 / 重复预约） |
| 3001-3012 | 预约类（时段无效 / 已满 / 人数超限 / 状态非法 / 已过期 等） |
| 4001 | CHECKIN_BUSY（核销并发冲突） |

### 鉴权注解
- `@RequireLogin`（类/方法级）：无有效 Token 返回 401
- `/ai/chat` 无注解：有效 Token 软注入 `UserContext`，无 Token 匿名放行（写 Tool 自行失败）

---

## 1. 管理端 API（`/admin` + `/stats`）

> 除 `login` / `register` 外，均需类级 `@RequireLogin`。

### 1.1 管理员账号 `/admin/auth`

| 方法 | 路径 | 说明 | 入参 | 出参 |
|------|------|------|------|------|
| POST | `/admin/auth/login` | 登录 | `AdminLoginDTO{username,password}` | `{token, adminId}` |
| POST | `/admin/auth/register` | 注册（需 secretKey） | `AdminLoginDTO` + `secretKey="e7g6s679Ty67N9fTh98"` | Admin |
| GET | `/admin/auth/info` | 当前管理员信息 | — | Admin |
| POST | `/admin/auth/logout` | 登出 | — | msg |
| GET | `/admin/auth/profile` | 个人资料（名/简介/头像） | — | `{userName,userIntro,currentAvatar}` |
| POST | `/admin/auth/profile/update` | 更新名 & 简介 | `{userName,userIntro}` | msg |
| POST | `/admin/auth/profile/update-avatar` | 更新头像 | `{avatarUrl}` | `{avatarUrl}` |

### 1.2 场馆管理 `/admin/museum`

| 方法 | 路径 | 说明 | 入参 | 出参 |
|------|------|------|------|------|
| POST | `/admin/museum/list` | 分页列表 | `{keyword?,page,limit}` | Page<Museum> |
| POST | `/admin/museum/add` | 新增场馆+排期 | `MuseumAddDTO`（含 lat/lng/address） | msg |
| POST | `/admin/museum/edit` | 编辑 | `MuseumEditDTO` | msg |
| POST | `/admin/museum/del` | 删除 | `{id}` | msg |
| POST | `/admin/museum/status` | 上下架 | `{id,status}` | msg |
| GET | `/admin/museum/detail?id=` | 详情 | `id` | Museum |
| GET | `/admin/museum/all` | 全量下拉 | — | Museum[] |

### 1.3 活动管理 `/admin/activity`

| 方法 | 路径 | 说明 | 入参 | 出参 |
|------|------|------|------|------|
| POST | `/admin/activity/list` | 分页搜索 | `{keyword?,page,limit}` | Page<Activity> |
| POST | `/admin/activity/add` | 新增 | `ActivityAddDTO` | msg |
| POST | `/admin/activity/edit` | 编辑 | `ActivityEditDTO` | msg |
| POST | `/admin/activity/del` | 删除 | `{id}` | msg |
| POST | `/admin/activity/status` | 发布/下架 | `{id,status}` | msg |
| GET | `/admin/activity/detail?id=` | 详情 | `id` | Activity |

### 1.4 预约核销 `/admin/join`

| 方法 | 路径 | 说明 | 入参 | 出参 |
|------|------|------|------|------|
| POST | `/admin/join/list` | 预约列表（可按姓名/身份证搜） | `{keyword?,name?,idCard?,page,limit}` | Page<Join> |
| POST | `/admin/join/checkin` | 核销 | `{id}` | msg |
| GET | `/admin/join/export` | 导出 Excel | — | Excel 流 |

### 1.5 公告管理 `/admin/news`

| 方法 | 路径 | 说明 | 入参 | 出参 |
|------|------|------|------|------|
| POST | `/admin/news/list` | 分页 | `?keyword=&page=&limit=` | Page<News> |
| POST | `/admin/news/add` | 新增 | `NewsAddDTO` | msg |
| POST | `/admin/news/edit` | 编辑 | `NewsEditDTO` | msg |
| POST | `/admin/news/del?id=` | 删除 | `id` | msg |
| POST | `/admin/news/view?id=` | 详情 | `id` | News |

### 1.6 黑名单 `/admin/blacklist`

| 方法 | 路径 | 说明 | 入参 | 出参 |
|------|------|------|------|------|
| POST | `/admin/blacklist/list` | 分页（可按状态过滤） | `{keyword?,page,limit,status?}` | Page<Identity> |
| POST | `/admin/blacklist/add` | 加入黑名单 | `{identityId,reason?,endTime}` | msg |
| POST | `/admin/blacklist/updateTime` | 改结束时间 | `{identityId,endTime}` | msg |
| POST | `/admin/blacklist/remove` | 移出黑名单 | `{identityId}` | msg |
| GET | `/admin/blacklist/export` | 导出 Excel | — | Excel 流 |

### 1.7 文件上传 `/admin/upload`

| 方法 | 路径 | 说明 | 入参 | 出参 |
|------|------|------|------|------|
| POST | `/admin/upload/image` | 上传图片 | `file`（multipart） | `{url:"/files/<uuid>.<ext>"}` |

### 1.8 统计看板 `/stats`

| 方法 | 路径 | 说明 | 出参 |
|------|------|------|------|
| GET | `/stats/home` | 4 个统计卡 | 计数集合 |
| GET | `/stats/trend` | 近 7 日预约趋势 | `[{date,count,...}]` |
| GET | `/stats/checkin` | 今日核销分布 | `{checked,notChecked,...}` |
| GET | `/stats/popular-news` | 阅读量 Top5 公告 | `[{newsId,title,viewCount,...}]` |
| GET | `/stats/noshow-comparison` | 爽约对比（4 周） | `[{week,noShowCount,...}]` |

### 1.9 消息模板 `/admin/message/template`

| 方法 | 路径 | 说明 | 出参 |
|------|------|------|------|
| GET | `/admin/message/template/list` | 分页列表 | Page<MessageTemplate> |
| GET | `/admin/message/template/{id}` | 详情 | MessageTemplate |
| POST | `/admin/message/template/update` | 更新 | msg |

---

## 2. 小程序端 API（`/app`）

### 2.1 首页/公开信息 `/app/home`（无鉴权）

| 方法 | 路径 | 说明 | 入参 | 出参 |
|------|------|------|------|------|
| GET | `/app/home/index` | 首页聚合（banner/场馆/今日/活动/公告） | — | 聚合对象 |
| POST | `/app/home/activity/list` | 活动列表 | `{page?,limit?}` | Page<Activity> |
| GET | `/app/home/activity/detail?id=` | 活动详情（须已发布） | `id` | Activity |
| POST | `/app/home/notice/list` | 公告列表 | `{page?,limit?}` | Page<News> |
| GET | `/app/home/notice/detail?id=` | 公告详情（浏览量+1） | `id` | News |

### 2.2 文物识别 `/app/relic`（无鉴权）

| 方法 | 路径 | 说明 | 入参 | 出参 |
|------|------|------|------|------|
| POST | `/app/relic/identify` | ONNX 文物识别 | `file`（multipart） | `{recognition:{id,label}, detail:Relic, modelUrl?}` |

### 2.3 预约 `/app/booking`（GET 开放，POST 需登录）

| 方法 | 路径 | 说明 | 入参 | 出参 |
|------|------|------|------|------|
| GET | `/app/booking/days` | 可预约日期 | — | 日期列表 |
| GET | `/app/booking/times?day=` | 某日时段（含余量） | `day=yyyy-MM-dd` | 时段列表 |
| POST | `/app/booking/submit` | 提交预约 | `BookingSubmitDTO{timeMark,identityIds[]}` | msg |

> `submit` 的 `userId` 从 JWT 取（`BaseAppController.getUserId()`），不走请求体。

### 2.4 访客身份 `/app/identity`（类级 `@RequireLogin`）

| 方法 | 路径 | 说明 | 入参 | 出参 |
|------|------|------|------|------|
| GET | `/app/identity/list` | 我的访客列表 | — | Identity[] |
| POST | `/app/identity/save` | 新增/更新访客 | Identity | msg |
| POST | `/app/identity/del` | 删除访客 | `{identityId}` | msg |

### 2.5 预约记录 `/app/record`（类级 `@RequireLogin`）

| 方法 | 路径 | 说明 | 入参 | 出参 |
|------|------|------|------|------|
| GET | `/app/record/list` | 我的预约 | — | Join[] |
| POST | `/app/record/cancel` | 取消预约 | `{joinId}` | msg |
| GET | `/app/record/detail?joinId=` | 详情（校验归属） | `joinId` | Join |

### 2.6 用户账号 `/app/user`

| 方法 | 路径 | 说明 | 鉴权 | 入参 | 出参 |
|------|------|------|------|------|------|
| POST | `/app/user/login` | 手机号+验证码登录 | 无 | `{mobile,code}` | `{token}` |
| GET | `/app/user/info` | 用户资料 | 需登录 | — | User |
| POST | `/app/user/update` | 更新资料 | 需登录 | User | msg |
| GET | `/app/user/heads` | 可用头像列表 | 需登录 | — | 头像 URL[] |

> 演示用固定验证码 **`1234`**。

---

## 3. 消息中心

### 3.1 用户消息 `/message`（类级 `@RequireLogin`）

| 方法 | 路径 | 说明 | 入参 | 出参 |
|------|------|------|------|------|
| GET | `/message/my?userId=` | 我的消息 | `userId` | Message[] |
| POST | `/message/read/{id}` | 标记已读 | `id`（路径） | msg |
| GET | `/message/unread/count?userId=` | 未读数 | `userId` | `{count}` |

### 3.2 管理端消息模板
见 §1.9。

---

## 4. AI 对话 `/ai`

### 4.1 对话接口

| 方法 | 路径 | 说明 | 鉴权 | 入参 | 出参 |
|------|------|------|------|------|------|
| POST | `/ai/chat` | AI 对话 | 可选（软注入 UserContext） | `{message}` | `AiChatResponse` |

可选请求头 `X-AI-Debug: 1` 开启调试字段。

### 4.2 响应结构 `AiChatResponse`

```json
{
  "reply": "文本回复，兼容旧前端",
  "intent": "BOOKING | RULES | MIXED",
  "blocks": [
    {
      "type": "time_slots | booking_records | rules_source | tips",
      "title": "可选标题",
      "items": [],
      "source": "news | static_rules"
    }
  ],
  "suggestions": ["后天下午有票吗", "我的预约", "可以带背包吗"],
  "debug": {
    "intent": "BOOKING",
    "ragHits": [{ "title": "...", "score": 0.42, "source": "static_rules" }],
    "tools": [{ "name": "queryTimes", "input": {...}, "output": {...} }],
    "elapsed": 234
  }
}
```

- `reply`：向后兼容，旧前端只需读此字段
- `intent`：`IntentRouter` 关键词路由结果
- `blocks`：结构化卡片，前端按 `type` 渲染
- `suggestions`：快捷追问
- `debug`：仅 `X-AI-Debug: 1` 时返回

### 4.3 五个固定 Tool 契约

| Tool | 类型 | 入参 | 返回 DTO | 说明 |
|------|------|------|----------|------|
| `queryDays` | 读 | — | `QueryDaysData` | 全部可预约日 + 开/闭馆状态 |
| `queryTimes` | 读 | `day: yyyy-MM-dd` | `QueryTimesData` | 某日时段 + 余量 + AVAILABLE/FULL |
| `submitBooking` | 写 | `timeMark`, `identityIds[]` | `SubmitBookingData` | 需登录；调 `JoinService.submitBooking` |
| `listRecords` | 读 | `day?`, `status?` | `ListRecordsData` | 需登录；返回含 `joinId` 的记录 |
| `cancelBooking` | 写 | `joinId` | `CancelBookingData` | 需登录；`joinId` 须来自 `listRecords` |

写 Tool 取 `UserContext.get()`，无登录返回 `ToolError.UNAUTHORIZED`；不接受外部传入 `userId`。

### 4.4 意图路由（`IntentRouter`）

- **RULES 关键词**：须知 / 背包 / 禁带 / 开馆 / 闭馆 / 公告 / 开放时间 / 停止入馆 / 馆规 / 规定 / 谢绝 / 允许带 …
- **BOOKING 关键词**：有票 / 余票 / 下单 / 帮我订 / 我的预约 / 预约记录 / 还有票吗 / 取消预约 / 能订吗 …
- 同时命中 → `MIXED`；仅命中 RULES → `RULES`；仅命中 BOOKING 或泛指“预约” → `BOOKING`；默认 → `BOOKING`

### 4.5 示例问句
- 馆规：`可以带背包吗` / `几点停止入馆` → 走 `museumRulesChatClient` + RAG
- 预约：`后天下午有票吗` / `我的预约` → 走 `museumBookingChatClient` + Tool
- 混合：`明天有票吗，能带水吗` → MIXED，分段回答
