# `/ai/chat` Tool Trace 规划

> 目标：只补可观测性，不改现有预约主链路；本阶段只出计划，不直接改代码。

## 1. 背景

当前 `/ai/chat` 已能返回：

```json
{
  "reply": "...",
  "intent": "BOOKING|RULES|MIXED",
  "blocks": [],
  "suggestions": []
}
```

阶段 2.6 的联调验收目前是**行为级**，只能通过 `reply`、HTTP 对照和副作用快照来判断是否符合预期；无法从接口响应中直接看到：

- 本轮到底调用了哪些 Tool
- 是否命中了 RAG
- 调用顺序是什么
- 某次失败发生在 Tool 前、Tool 中还是 RAG 未命中

因此，补一套可选的 `toolTrace` / `ragTrace` 调试信息，会明显提升：

- 联调效率
- 回归测试可解释性
- 简历项目展示完整度
- 面试时对 Agent 工程化能力的说服力

## 2. 目标与边界

### 2.1 要做

- 为 `/ai/chat` 设计**可选调试字段**
- 能记录本轮：
  - 意图路由结果
  - Tool 调用清单
  - Tool 输入摘要
  - Tool 输出摘要 / 错误码
  - 是否命中 RAG、命中来源与条数
  - 总耗时与分段耗时
- 默认不影响现有小程序消费 `reply + intent + blocks + suggestions`
- 文档中明确生产/演示场景的开关策略

### 2.2 不做

- 不改 5 个 Tool 的业务语义
- 不引入多 Agent / LangGraph / MCP
- 不让前端依赖 trace 才能工作
- 不把完整敏感参数、完整证件号、完整 Token、完整原始 prompt 直接透出给前端

## 3. 推荐方案

推荐采用：

**服务端旁路收集 trace → 可选挂到 `/ai/chat` 响应的 `debug` 字段**

而不是把 trace 混入 `reply` 文本，也不是让前端去猜 Tool 调用。

### 3.1 响应结构建议

在现有响应基础上，增加一个**可选字段**：

```json
{
  "reply": "...",
  "intent": "BOOKING",
  "blocks": [],
  "suggestions": [],
  "debug": {
    "route": {
      "intent": "BOOKING",
      "routerVersion": "v1"
    },
    "ragTrace": {
      "enabled": true,
      "queried": false,
      "hitCount": 0,
      "sources": []
    },
    "toolTrace": [
      {
        "name": "queryTimes",
        "status": "OK",
        "startedAt": 1723970000000,
        "durationMs": 42,
        "inputSummary": {
          "day": "2026-08-20"
        },
        "outputSummary": {
          "slotCount": 2
        },
        "error": null
      }
    ],
    "timing": {
      "totalMs": 1086
    }
  }
}
```

### 3.2 开关建议

至少保留一种开关方式：

1. 请求头开关：
   - `X-AI-Debug: 1`
2. 或请求参数开关：
   - `debug=true`

建议优先请求头，原因是：

- 不污染现有请求体契约
- 小程序默认不需要带
- Postman / curl / 联调脚本里更好控

### 3.3 安全约束

`debug` 只返回**摘要信息**，不返回：

- 完整身份证号
- 完整 `identityIds`
- 完整 JWT / userId 来源
- 完整 Prompt 原文
- 内部异常堆栈

建议输出脱敏摘要，例如：

- `identityCount: 2`
- `hasJoinId: true`
- `filterDay: 2026-08-20`

## 4. 实现分层建议

### 4.1 新增 Trace 上下文

建议新增：

```text
com.museum.ai.trace/
  ├─ AiDebugTraceContext.java
  ├─ ToolTraceEntry.java
  ├─ RagTraceEntry.java
  └─ AiDebugTrace.java
```

职责：

- `AiDebugTraceContext`
  - ThreadLocal 管理本轮 trace 生命周期
- `ToolTraceEntry`
  - 记录单个 Tool 调用
- `RagTraceEntry`
  - 记录检索是否命中、来源类型、命中数
- `AiDebugTrace`
  - 聚合到最终响应里的 `debug`

### 4.2 Tool 层接入点

最佳接入点：`BookingTools`

原因：

- 五个 Tool 已集中在一个类中
- 这里最容易拿到“方法名 + 输入 + 输出/错误”
- 不需要侵入 `JoinService`

建议每个 Tool 统一记录：

- Tool 名称
- 开始/结束时间
- 输入摘要
- `ok/error/message`
- 结果摘要

### 4.3 RAG 接入点

最佳接入点：`AiChatService`

建议记录：

- 是否进入 RULES / MIXED 路径
- 是否执行 `rag.retrieve`
- 命中数量
- 命中来源类型集合（`news` / `static_rules`）
- 是否走 `rag_miss`

### 4.4 最终响应拼装

仍由 `AiChatService` 统一组装 `AiChatResponse`。

策略：

- 未开启 debug：完全不返回 `debug`
- 开启 debug：在不影响现有字段的前提下附加 `debug`

## 5. 验收口径

补完后，至少应能支持这些场景：

1. 预约问句：
   - `后天下午有票吗`
   - 可看到 `queryTimes`
2. 连续事务问句：
   - `后天下午有票就帮我订`
   - 可看到 `queryTimes`、`submitBooking`
3. 规则问句：
   - `可以带背包吗`
   - 可看到 `ragTrace.hitCount > 0`，且 `toolTrace=[]`
4. 混合问句：
   - `明天下午有票吗，另外可以带水吗`
   - 可同时看到 `ragTrace` 和 `toolTrace`
5. 未登录写操作：
   - 可看到 `submitBooking` 返回 `UNAUTHORIZED`
6. RAG 未命中：
   - 可看到 `ragTrace.queried=true` 且 `hitCount=0`

## 6. 风险点

### 6.1 主要风险

- Spring AI Tool 调用过程不一定天然暴露完整回调点
- 若直接记录原始参数，容易把敏感信息带出去
- 若前端开始依赖 trace，会把调试字段变成正式协议负担

### 6.2 规避策略

- 只在 `BookingTools` 侧记录业务可控摘要
- `debug` 默认关闭
- 文档明确：`debug` 是联调辅助，不是正式业务字段

## 7. 推荐实施顺序

1. 先定 `debug` 响应模型
2. 接 `AiDebugTraceContext`
3. 在 `BookingTools` 五个方法补 trace
4. 在 `AiChatService` 补 route / rag trace
5. 给 `/ai/chat` 增加 debug 开关
6. 更新 `docs/ai-agent-test-case.md`
7. 再补自动化回归脚本

## 8. 交付物

本任务正式实施时，建议至少交付：

- `docs/ai-tool-trace-plan.md`（本文件）
- `docs/ai-agent-test-case.md` 增补 debug 验收段
- `docs/api-contract.md` 增补 `debug` 字段说明
- 对应的后端 trace 代码与单测

## 9. 预期收益

做完这项后，这个项目在简历和面试里的表达会更完整：

- 不只是“接了 LLM + Tool”
- 而是“做了可观测、可解释、可回归的 Agent 接口”

这对单 Agent 项目非常加分。
