# 小程序 AI 页（阶段 3.2）

> 页面：`app-front/ZDYZ/miniprogram/pages/aiChat`  
> 后端接口：`POST /ai/chat`  
> 执行口径：`OPTIMIZATION_PLAN.md` §3.2

## 1. 目标

把现有 AI 页从“只发一条文本到 `/ai/chat`，再把 `reply` 原样显示”的聊天壳，升级成真正可用的预约助手页：

- 预约问题继续复用阶段 2 的 Tool 能力
- 馆规/须知问题继续复用阶段 3.1 的 RAG 能力
- 页面可消费 `intent / blocks / suggestions`
- 统一复用小程序已有 `request.js` 与 `Token`

## 2. 页面结构

页面采用三段式：

1. 顶部引导区
- 助手身份说明
- 推荐问句 chips
- 未登录轻提示

2. 中部消息区
- 用户气泡
- 助手文本
- 结构化 blocks
  - `time_slots`
  - `booking_records`
  - `rules_source`
  - `tips`
- 回复后的 suggestions chips

3. 底部操作区
- 快捷跳转：查余票 / 我的预约 / 参观须知
- 输入框 + 发送按钮

## 3. 协议

后端响应兼容旧字段 `reply`，并新增：

```json
{
  "reply": "...",
  "intent": "BOOKING|RULES|MIXED",
  "blocks": [
    {
      "type": "time_slots|booking_records|rules_source|tips",
      "title": "...",
      "items": [],
      "source": "tool:queryTimes|tool:listRecords|static_rules|news|..."
    }
  ],
  "suggestions": ["后天下午有票吗", "我的预约", "可以带背包吗"]
}
```

前端兼容策略：
- 有 `blocks` 就渲染卡片
- 没有 `blocks` 仍显示 `reply`
- 禁止正则解析 `reply` 去拼卡片

## 4. 登录策略

- 页面请求统一走 `utils/request.js`
- 若本地有 `token`，自动带 `Token` 请求头
- 未登录也允许提问公开信息
- 写操作若触发“需登录”，页面显示轻提示并提供去登录入口

## 5. 错误与兜底

页面显式区分：
- `config`
  - 后端 AI 未配置
- `rag-miss`
  - 馆规未命中，明确说不知道
- `login-required`
  - 未登录写操作
- `network-error`
  - 网络失败，可重试上一条

## 6. 验收

行为级用例：

1. 「可以带背包吗」
- 有 `reply`
- 有 `rules_source`
- 来源指向 `static_rules` 或 `news`

2. 「后天下午有票吗」
- 有 `reply`
- 若 Tool 收集成功，出现 `time_slots`

3. 未登录「帮我订明天上午」
- 明确提示需登录
- 页面不假装成功

4. 混合问句
- 同时出现预约结果区与馆规来源区

5. 网络/未配置/未命中
- 页面稳定兜底，不白屏

**代码侧验收（2026-08-17）：** `AiChatServiceTest` / `AiChatBlockCollectorTest` 覆盖 intent、`rules_source`、tips、suggestions 与 Tool→block 映射；HTTP 契约见 [api-contract.md](./api-contract.md) §9.3。真机/开发者工具勾验按需补做。

## 7. 入口

首页已有悬浮入口组件：
- `components/floating-ai-btn`

路径：
- `pages/index/index` → `<floating-ai-btn />`
- 点击进入 `pages/aiChat/aiChat`
