# 轻量 RAG 设计（阶段 3.1）

> 执行口径：[OPTIMIZATION_PLAN.md](../OPTIMIZATION_PLAN.md)  
> 状态：已落地（本地向量 + 内存库 + 意图分流 + 双 ChatClient）

## 1. 目标

为单 Agent 增加馆规 / 须知 / 公告问答，降低幻觉。预约事务仍只信 5 个 Tool。

## 2. 数据源

| sourceType | 来源 | 装载方式 |
| --- | --- | --- |
| `news` | 表 `news`，`NEWS_STATUS=1` | `NoticeService.listVisibleForRag()` **全量**；**禁止** `appList` 分页 |
| `static_rules` | [`visit-rules.md`](../ZDYZ/src/main/resources/rag/visit-rules.md) | classpath 读取 |

正文：`news.newsDesc`。空标题/空正文过滤。HTML 切片前清洗。

表无「公告/资讯」分类字段，3.1 不拆子类。

## 3. visit-rules 镜像同步

`visit-rules.md` 是后端 RAG **专用镜像**，源文件为小程序 [`noticeReservation.wxml`](../app-front/ZDYZ/miniprogram/pages/noticeReservation/noticeReservation.wxml)。

**规则**：修改 wxml 中的开放时间、预约规则、禁带、存包、拍照等文案时，**必须**同步更新 `visit-rules.md`。核对步骤见 [scripts/sync-visit-rules.md](./scripts/sync-visit-rules.md)。

## 4. 向量与检索

- Embedding：`LocalHashingEmbeddingModel`（字符 n-gram 哈希，固定维，无外网）
- 存储：进程内 `InMemoryRagStore`（chunk + float 向量）
- 检索：余弦相似度 TopK，低于 `min-score` 视为未命中
- 启动：`rebuild-on-startup=true` 时重建索引

## 5. 双 ChatClient（强制）

| Bean | Tools | 用途 |
| --- | --- | --- |
| `museumBookingChatClient` | `BookingTools` | BOOKING / MIXED |
| `museumRulesChatClient` | **无** | 纯 RULES |

纯 RULES **不得**走 booking client，避免「预约规则」等词误触发 Tool。

## 6. 意图分流

- BOOKING → booking client
- RULES → RAG → rules client（未命中直接兜底文案，不调模型）
- MIXED → RAG 上下文 + booking client；分段：预约信 Tool，馆规信检索

## 7. 配置

```yaml
museum.ai.rag.enabled
museum.ai.rag.top-k
museum.ai.rag.min-score
museum.ai.rag.rebuild-on-startup
museum.ai.rag.sources.news-enabled
museum.ai.rag.sources.static-rules-enabled
```

Chat Key 仍在 `spring.ai.openai.api-key`（yml，不用环境变量）。

## 8. 验收

见 [ai-rag-test-case.md](./ai-rag-test-case.md)。
