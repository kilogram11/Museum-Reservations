# 博物馆预约管理系统 · 项目文档

> 本目录由 `project-dumper` 技能从源码自动生成，仅做设计与结构描述，不包含源码副本。
> 生成时间：2026-08-21。项目主叙事与阶段口径以根目录 `OPTIMIZATION_PLAN.md` 为准。

## 文档导航


| 文档                                 | 内容                                        |
| ------------------------------------ | ------------------------------------------- |
| [ARCHITECTURE.md](./ARCHITECTURE.md) | 分层架构、技术栈选型、设计模式、关键决策    |
| [API.md](./API.md)                   | 全量接口清单（管理端 / 小程序端 / AI 对话） |
| [DATABASE.md](./DATABASE.md)         | 数据库表结构、ER 关系、索引与设计取舍       |
| [FLOWS.md](./FLOWS.md)               | 核心业务流程与 Mermaid 时序图               |

## 项目定位

软件工程实训项目——**博物馆预约管理系统**。三端一体：

- **后端**（`ZDYZ/`）：Spring Boot 3 + Java 17，对外 REST API，端口 `8081`
- **管理端**（`admin-front/ZDYZ/`）：Vue 3 + Vite + Element Plus 后台
- **小程序端**（`app-front/ZDYZ/`）：微信小程序原生开发

简历向两条主线：

1. **预约 Agent**（主叙事）—— 单 Agent + 5 Tool + 轻量 RAG
2. **预约库存**（面试托底）—— Redis Lua 预扣 + MySQL 事务落单 + 补偿

## 技术栈速览


| 层       | 技术                                                                 |
| -------- | -------------------------------------------------------------------- |
| 后端框架 | Spring Boot 3.4.5 / Java 17 / Maven                                  |
| ORM      | MyBatis-Plus 3.5.7                                                   |
| 数据库   | MySQL 8.0（库名`museum_book`）                                       |
| 缓存/锁  | Redis（Lua 脚本原子预扣）                                            |
| 鉴权     | JWT（jjwt 0.11.5）+ 拦截器 +`@RequireLogin`                          |
| AI       | Spring AI 1.0.0（OpenAI-compatible → DeepSeek`deepseek-v4-flash`）  |
| 工具     | Hutool、ZXing（二维码）、POI（Excel 导出）、ONNX Runtime（文物识别） |
| 管理端   | Vue 3.5 / Vite 7 / Element Plus 2 / Axios / ECharts 6                |
| 小程序   | 微信原生 / 21 页 / 4 TabBar                                          |

## 快速启动

### 后端

```bash
cd ZDYZ
mvnw.cmd spring-boot:run      # Windows
# ./mvnw spring-boot:run      # *nix
```

前置依赖：**MySQL 8.0 + Redis** 均需本地可连。配置见 `ZDYZ/src/main/resources/application.yml`。默认端口 `8081`。

DeepSeek Key 直接写入 `application.yml` 的 `spring.ai.openai.api-key`（本项目不用环境变量），**提交前清除真实 Key**。

### 管理端

```bash
cd admin-front/ZDYZ
npm install
npm run dev        # Vite 默认 3000，dev 代理 /api → localhost:8081
```

### 小程序

用微信开发者工具打开 `app-front/ZDYZ`，`project.config.json` 中 AppID `wx50f6b69dfee0a8b3`。URL 合法域校验已关闭，允许本地调试。

### 数据库初始化

SQL 脚本位于 `docs/sql/`：

- `museum_book_schema.sql` —— 表 DDL
- `museum_book_seed_base.sql` —— 头像、文物、消息模板、演示管理员
- `museum_book_seed_loadtest.sql` —— 压测数据（80 用户 + 7 天排期）
- `generate_loadtest_seed.py` —— 按当前日期重新生成排期

## 重要约束

- **演示用固定验证码 `1234`**（非真实短信）。
- 预约库存依赖**真实 Redis**；无 Redis 时启动/运行会明确失败。
- 压测结论（2026-08-16 JMeter）：80 并发同 `timeMark`（`LIMIT_CNT=50`）→ 成功 50、0 超卖；同一 `identityId` 10 并发仅 1 成功。脚本见 `docs/jmeter/`。
- AI 写操作（`submitBooking` / `cancelBooking`）只允许当前登录用户触发，Tool 不接受外部 `userId`。
- 第一版不做：RabbitMQ、多 Agent、LangGraph、MCP、Milvus/ES、真实短信、NL2SQL。

## 目录结构

```text
Museum-Reservations
├── ZDYZ/                       # Spring Boot 后端
│   └── src/main/java/com/museum/
│       ├── controller/{admin,app}/   # 控制器分层
│       ├── service/ + service/impl/   # 业务层
│       ├── entity/                    # 14 张表实体
│       ├── mapper/                   # MyBatis-Plus
│       ├── ai/                       # AI Agent + RAG
│       │   ├── config/  context/  converter/  dto/
│       │   ├── rag/  support/  tool/  trace/
│       ├── security/                 # JWT 拦截器
│       └── common/ config/ job/
├── admin-front/ZDYZ/           # Vue 3 管理端
├── app-front/ZDYZ/             # 微信小程序
├── docs/                       # 设计文档 + jmeter + sql + scripts
├── 原型/  故宫图片/  *.docx/*.png  # 需求/设计/流程素材
└── project-docs/               # 本目录（自动生成）
```

## 相邻文档（项目原有）

根目录 `docs/` 下已有更细粒度的设计与测试文档，本文档不重复其内容，仅在相关处引用：

- `docs/api-contract.md` —— 接口契约（最权威）
- `docs/ai-agent-design.md` / `ai-agent-test-case.md` —— Agent 5 Tool 设计与联调
- `docs/ai-rag-design.md` / `ai-rag-test-case.md` —— RAG 设计与用例
- `docs/ai-miniapp-page.md` —— 小程序 AI 页交互
- `docs/ai-tool-trace-plan.md` —— Tool 执行追踪
- `docs/jmeter/README.md`、`docs/sql/README.md` —— 压测与库初始化
- 根 `OPTIMIZATION_PLAN.md` —— 阶段化简历向改造计划（0~3 阶段）
- 根 `context.md` —— 跨会话记忆同步

---


实习经历

业务介绍：参与酒店业务定时调度平台建设，面向价格刷新、库存同步、策略计算等异步/定时任务场景，提供统一任务注册、调度推进、执行回调和运维监控能力。系统基于 Spring Boot + Trip FX + CDubbo 暴露 SOA 服务，底层使用 CRedis ZSET/HASH 构建轻量级调度内核，支持 HTTP/gRPC 多协议执行与策略扩展。
主要工作：

1. 调度内核设计：参与建设 Redis 双队列调度模型，将任务生命周期拆分为 timeline 等待触发队列与 executeline 待执行优先级队列，实现“触发时间”和“执行优先级”解耦。
2. 高吞吐任务推进：负责/参与 TimeLineTrigger 到期扫描与任务推进链路，使用 Redis ZSET score 表达触发时间，通过 MULTI/EXEC 保证任务从 timeline 到 executeline 的状态迁移一致性，并结合 Pipeline 支持单批 5000 条任务推进，降低批量调度场景下的 Redis RTT 成本。
3. 多类型调度能力：实现 once、interval、cron、fixed_delay 四类调度语义；针对 fixed_delay 设计 backup score + 成功 callback 覆盖机制，保证任务执行失败时仍可按 timeout 自动重试，避免长周期任务断链。
4. 执行器抽象建设：参与 TaskWorker 执行链路开发，基于策略工厂封装 HTTP/gRPC ActionInvoker，统一执行入参、超时、异常映射和重试分类；默认 8 线程并发消费 executeline，支持最多 3 次内联重试。
5. 策略扩展与容灾降级：接入外部 Policy 服务动态计算 executeline_score，支持按业务策略调整执行优先级；当策略服务超时或异常时降级为内置默认分值，保障调度主链路不被弱依赖阻塞。
6. 可观测与运维闭环：建设 Admin REST 监控能力，提供任务注册/取消、任务详情、timeline/executeline 查询和 dashboard 聚合；通过 CAT Transaction/Event、TripLog 和调度事件体系追踪注册、推进、入队、执行、失败、回调等完整生命周期，提升问题定位效率。

   ---
7. 项目经历
8. 博物馆预约管理与 AI 助手平台
   项目介绍：面向博物馆预约、核销、馆规咨询和运营管理的一体化平台，包含微信小程序、Vue 管理端与 Spring Boot 后端。用户侧支持场馆/活动浏览、预约下单、二维码入馆、预约记录、AI 咨询与文物识别；管理侧支持排期配置、预约核销、黑名单、公告、统计看板和 Excel 导出。
   技术栈：Spring Boot 3、Java 17、MyBatis-Plus、MySQL、Redis、Lua、Spring AI、DeepSeek、Vue 3、微信小程序、ONNX Runtime。
   主要工作：
9. AI 预约 Agent 架构：设计并落地单 Agent + 双 ChatClient + 5 Tool 的预约助手架构，将“预约事实查询/写操作”和“馆规问答”拆分为不同对话链路，避免一个模型上下文同时承担业务执行与规则问答导致边界不清。
10. Tool Calling 业务闭环：封装 queryDays、queryTimes、submitBooking、listRecords、cancelBooking 五个固定 Tool，Tool 层只做 DTO 转换和 Service 适配，复用真实预约主链路完成查票、下单、查询和取消，避免 AI 绕过业务校验直接读写数据。
11. RAG 馆规问答：实现轻量级 RAG 检索链路，使用本地 384 维字符 n-gram 哈希向量和内存向量库索引公告/馆规文本，按 Top-K 与 min-score 召回后拼接给模型；未命中时返回固定兜底话术，降低馆规问答编造风险。
12. AI 写操作权限隔离：通过 AppAuthInterceptor 将 JWT 登录态注入 UserContext(ThreadLocal)，AI 写 Tool 仅允许读取当前会话用户，不接受模型传入 userId，防止提示词注入或参数伪造导致越权预约/取消。
13. 高并发库存扣减：设计 Redis Lua 原子预扣 + MySQL 事务落单的预约库存模型，使用 booking:stock:{timeMark} 管理时段库存、booking:booked:{day}:{identityId} 实现一证一约，MySQL 侧通过 SUCC_CNT += delta 原子记账，避免应用层加锁。
14. 一致性补偿机制：针对 MySQL 落单失败和用户取消预约场景，使用 Lua 回补 Redis 库存并清理 booked 标记，维护 succCnt + Redis剩余 = limitCnt 不变量，保证缓存库存、数据库预约数和用户占位状态一致。
15. 压测与可靠性验证：基于 JMeter 构造真实 Redis/MySQL 并发测试，80 并发抢同一 timeMark、LIMIT_CNT=50 时成功 50 单且 0 超卖；同一 identityId 10 并发仅 1 单成功，验证库存扣减和一证一约约束有效性。
16. 预约风控闭环：实现二维码核销、预约成功消息、爽约扫描、自动拉黑与自动解禁流程；管理端提供核销并发冲突提示、预约导出和统计看板，形成从预约、履约、核销到风控运营的完整闭环。
