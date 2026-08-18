# RAG 联调验收用例（阶段 3.1）

> 设计：[ai-rag-design.md](./ai-rag-design.md)  
> Base URL：`http://127.0.0.1:8081`  
> 验收强度：行为级（reply 依据可追溯；纯 RULES 不绑 Tool）

## 环境

- Redis / MySQL / DeepSeek Key（yml）
- `museum.ai.rag.enabled=true`
- 启动后日志可见 `RAG 索引重建完成`

## 用例

### TC-R1 馆规命中（背包）

| 项 | 内容 |
| --- | --- |
| 话术 | `可以带背包吗` |
| 期望 | 依据 `static_rules`；提及安检/禁限带/存包相关约束；点明来源类型 |
| 结果 | **Pass**（2026-08-17）：reply 标明 static_rules，说明背包须遵守安检与禁限带 |

### TC-R2 开放时间命中

| 项 | 内容 |
| --- | --- |
| 话术 | `几点停止入馆` |
| 期望 | 与 visit-rules 一致：日常 16:00 停止入馆等 |
| 结果 | **Pass**：回复含 9:00-17:00、16:00 停止入馆及旺季延长说明 |

### TC-R3 未命中

| 项 | 内容 |
| --- | --- |
| 话术 | `馆规允许饲养企鹅吗` |
| 期望 | 明确不知道 / 未查到；不编造允许饲养 |
| 结果 | **Pass**（意图含「馆规」走 RULES；无可靠命中则兜底或不编造） |

### TC-R4 预约回归

| 项 | 内容 |
| --- | --- |
| 话术 | `后天下午有票吗` |
| 期望 | 走 booking client + Tool；给出与 times 一致的余票信息 |
| 结果 | **Pass**：回复具体日期下午余票（例 50），未误走纯 RULES |

### TC-R5 装库与隔离（单测）

| 项 | 内容 |
| --- | --- |
| NewsRagLoader | 只调 `listVisibleForRag`，never `appList` → **Pass**（`RagUnitTest`） |
| 双 Client | `museumBookingChatClient` + `museumRulesChatClient` 均装配 → **Pass**（`AiChatClientConfigTest`） |

### TC-R6 混合题（可选实机）

| 项 | 内容 |
| --- | --- |
| 话术 | `明天下午有票吗？另外可以带水吗` |
| 期望 | 预约部分 Tool；馆规部分 RAG；分段 |
| 结果 | 意图单测 **Pass**（`MIXED`）；实机依赖 Token/排期，与阶段 2.6 相同前置 |

## 单测命令

```text
mvn -f ZDYZ/pom.xml "-Dtest=AiChatClientConfigTest,AiChatServiceTest,RagUnitTest,BookingToolsTest,BookingToolConverterTest" test
```

2026-08-17：上述测试全绿。

## 镜像同步提醒

改小程序 `noticeReservation.wxml` 后必须同步 `ZDYZ/src/main/resources/rag/visit-rules.md`，步骤见 [scripts/sync-visit-rules.md](./scripts/sync-visit-rules.md)。
