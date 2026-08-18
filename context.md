# Museum-Reservations 上下文（同步记忆用）

> 换机器 / 新会话时先读本文件 + [OPTIMIZATION_PLAN.md](./OPTIMIZATION_PLAN.md)。

## 目标

简历向改造：主叙事 **预约 Agent**，面试托底 **预约库存**。执行口径以 `OPTIMIZATION_PLAN.md` 为准。

## 进度

| 阶段 | 内容 | 状态 |
| --- | --- | --- |
| 0 | 去污：Key 外置、开 Redis、去 JVM 锁降级、JWT 拦截器 + `@RequireLogin`、README 写明 mock 验证码 | **本机完成**（含实机：无 Token 下单 401、Redis 可连） |
| 1 | Redis 预扣 + MySQL 事务 + 补偿（Lua；请求内去重；原子 succCnt；booked TTL） | **完成**（2026-08-16 JMeter：80 并发 0 超卖；一证一约仅 1 成功） |
| 2 | 单 Agent + 5 Tool（Spring AI OpenAI-compatible + DeepSeek） | **2.0/2.5 已完成**（ChatClient + @Tool + Interceptor→UserContext）；下一步 2.6 联调验收 |
| 3.1 | 轻量 RAG（公告/须知） | 未开始 |
| 3.2 | 小程序 AI 页对接 | 未开始 |

## 环境约束

- 本机已装 MySQL 8.0 + Redis（路径见对话/本机安装）；库名 `museum_book`，初始化见 `docs/sql/`。
- 验证码固定演示用 **`1234`**（非真实短信）。
- DeepSeek：在 [`ZDYZ/src/main/resources/application.yml`](ZDYZ/src/main/resources/application.yml) 的 `spring.ai.openai.api-key` **直接填写** Key（本项目不用环境变量）；勿把真实 Key 提交进 Git。
- 第一版不做：RabbitMQ、多 Agent、GEO/Feed/签到、Milvus/ES。
- 压测：`docs/jmeter/`（JMeter 5.6.3）；结论已写入 README。

## 关键路径

- 后端：`ZDYZ/`（端口 8081）
- 管理端：`admin-front/ZDYZ/`
- 小程序：`app-front/ZDYZ/`
- App Token 头：`Token`；管理端：`Authorization: Bearer` 或 `Token`

## 阶段 1 库存要点

- Key：`booking:stock:{timeMark}`、`booking:booked:{day}:{identityId}`（TTL=参观日结束+2h）
- 下单：请求内去重 → warm SETNX → Lua 预扣 → MySQL 事务（insert + `SUCC_CNT=SUCC_CNT+delta`）→ 失败 compensate
- 取消：MySQL 成功后 compensate；Redis 失败不回滚 MySQL
- 脚本：`ZDYZ/src/main/resources/lua/booking_reserve.lua`、`booking_compensate.lua`

## 阶段 2 计划要点

- 固定只有 5 个 Tool：`queryDays`、`queryTimes`、`submitBooking`、`listRecords`、`cancelBooking`（**2.4 `BookingTools` + `@Tool` 已落地**）
- Tool 层只做 DTO / Converter / Service 适配，不写 SQL，不碰 Redis，不承载库存事务逻辑
- AI 暴露语义化字段，不直接暴露数据库状态码
- 写 Tool 绑定当前登录用户；不接受外部传入 `userId`（`AppAuthInterceptor` → `UserContext`）
- `AiChatService` 已切 Spring AI OpenAI-compatible `ChatClient`（DeepSeek）；无 Key 时可选注入降级
- 下一步：2.6 Agent 联调与验收用例
- 阶段 2 详细设计：业务契约 [`docs/api-contract.md`](./docs/api-contract.md)，AI Tool 契约 [`docs/ai-agent-design.md`](./docs/ai-agent-design.md)

## 实机勾验（2026-08-16 本机）

**阶段 0**：无 Token 下单返回 `code=401`；Redis PING 正常；验证码 1234。

**阶段 1**：JMeter 80 并发同 `timeMark` → `SUCC_CNT=50` + `stock=0` = `LIMIT_CNT`，0 超卖；同 identity 10 并发仅 1 成功。详见 README。
