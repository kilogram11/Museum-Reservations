# 博物馆预约管理系统

本项目是软件工程实训项目，包含后端服务、管理端前端、微信小程序端、原型设计、测试材料和项目文档。

简历向优化计划见 [OPTIMIZATION_PLAN.md](./OPTIMIZATION_PLAN.md)。按阶段执行：去污 → Redis 预扣库存 → 单 Agent 五 Tool → RAG 与小程序 AI 页。

## 项目结构

```text
big-work
├── ZDYZ/                 # Spring Boot 后端服务
├── admin-front/ZDYZ/     # Vue 管理端前端
├── app-front/ZDYZ/       # 微信小程序端
├── 原型/                 # 用户端原型页面
├── 测试/                 # 测试用例、测试报告和缺陷统计
├── 故宫图片/             # 页面和原型素材
└── *.docx / *.png / *.json # 需求、设计、数据库和流程文档
```

## 技术栈

- 后端：Spring Boot 3、Java 17、MyBatis-Plus、MySQL、Redis、JWT、Maven
- 管理端：Vue 3、Vite、Element Plus、Axios、ECharts
- 小程序端：微信小程序原生开发、云函数目录结构
- 文档与测试：需求规格、数据库设计、业务流程、测试用例、缺陷统计

## 后端启动

```bash
cd ZDYZ
./mvnw spring-boot:run
```

Windows 环境可使用：

```bash
cd ZDYZ
mvnw.cmd spring-boot:run
```

默认后端端口为 `8081`。启动前请准备 **MySQL** 与 **Redis**，并根据本地环境检查 `ZDYZ/src/main/resources/application.yml`。

DeepSeek 对话能力：在 `ZDYZ/src/main/resources/application.yml` 配置

```yaml
spring:
  ai:
    model:
      chat: openai
    openai:
      api-key: "sk-你的DeepSeekKey"   # 直接写字符串，不要用 ${}；本项目不用环境变量
      base-url: https://api.deepseek.com
      chat:
        options:
          model: deepseek-v4-flash
```

真实 Key 仅用于本地演示，**不要提交到 Git**。

## 管理端启动

```bash
cd admin-front/ZDYZ
npm install
npm run dev
```

## 微信小程序端

使用微信开发者工具打开 `app-front/ZDYZ` 目录，确认 `project.config.json` 中的小程序配置与本地开发环境一致。

## AI Agent（阶段 2 + 3）

- 目标：单 Agent + 5 Tool，复用现有预约服务，不新增第二套预约主链路
- Chat：Spring AI `ChatClient`（OpenAI-compatible → DeepSeek，`deepseek-v4-flash`）
- 双 Client：`museumBookingChatClient`（+ BookingTools）与 `museumRulesChatClient`（无 Tool，纯馆规）
- 固定 Tool：`queryDays`、`queryTimes`、`submitBooking`、`listRecords`、`cancelBooking`
- 轻量 RAG：本地哈希向量 + 内存库；语料 = 全量可见 `news`（`listVisibleForRag`）+ [`visit-rules.md`](ZDYZ/src/main/resources/rag/visit-rules.md)（小程序须知镜像）
- 配置：`museum.ai.rag.*`（见 `application.yml`）；改 `noticeReservation.wxml` 须同步 `visit-rules.md`（[docs/scripts/sync-visit-rules.md](docs/scripts/sync-visit-rules.md)）
- 安全约束：写操作只允许当前登录用户触发，Tool 不接受外部 `userId`；纯 RULES 路径不绑定预约 Tool
- 小程序 AI 页：`pages/index/index` 已有首页悬浮入口，进入 `pages/aiChat/aiChat`
- AI 页协议：`/ai/chat` 现返回 `reply + intent + blocks + suggestions`；旧前端仍兼容 `reply`
- 当前状态：阶段 2（含 2.6）、阶段 3.1 与阶段 3.2 已完成
- 联调：预约见 [docs/ai-agent-test-case.md](./docs/ai-agent-test-case.md)；RAG 见 [docs/ai-rag-test-case.md](./docs/ai-rag-test-case.md)
- 示例问句：`可以带背包吗` / `几点停止入馆` / `后天下午有票吗` / `我的预约`
- 详细口径见 [OPTIMIZATION_PLAN.md](./OPTIMIZATION_PLAN.md) / [docs/ai-rag-design.md](./docs/ai-rag-design.md)

## 注意事项

- **演示用固定验证码 `1234`**（非真实短信服务，仅本地/演示登录使用）。
- 预约库存方案：**Redis Lua 预扣**（`booking:stock` / `booking:booked`）+ **MySQL 事务落单** + 失败/取消 **补偿**；`SUCC_CNT` 使用原子增减，依赖真实 **Redis**。
- **压测结论（阶段 1，2026-08-16 本机真 Redis/MySQL + JMeter）**：
  - 场景 A 超卖：80 并发同 `timeMark`（`LIMIT_CNT=50`）→ 成功预约 **50**、业务失败约 **30**；`SUCC_CNT=50`，`booking:stock=0`，**0 超卖**，满足 `succCnt + Redis剩余 = limitCnt`。
  - 场景 B 一证一约：同一 `identityId` 10 并发 → 成功 **1**；Redis 写入 `booking:booked:{day}:{identityId}`。
  - 脚本与复现步骤见 [docs/jmeter/README.md](./docs/jmeter/README.md)；库初始化见 [docs/sql/README.md](./docs/sql/README.md)。
- 阶段 2、3.1、3.2 已完成。
- DeepSeek Key 写在 `application.yml` 的 `spring.ai.openai.api-key`（本项目不用环境变量）；提交前请清除真实 Key。
- `AccessKey.csv`、`project.private.config.json`、`node_modules/`、`target/`、IDE 配置和本地上传文件已通过 `.gitignore` 排除。
- 如需公开部署，请不要提交真实密钥、数据库密码、云服务 AccessKey 或私有环境配置。
- 上传 GitHub 前建议先检查 `git status` 和暂存清单，确认没有敏感文件被加入。
