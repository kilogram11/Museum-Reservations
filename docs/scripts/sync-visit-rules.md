# 同步参观须知镜像（visit-rules.md）

源文件：`app-front/ZDYZ/miniprogram/pages/noticeReservation/noticeReservation.wxml`  
镜像：`ZDYZ/src/main/resources/rag/visit-rules.md`

## 何时同步

修改 wxml 中任一须知文案（开放时间、预约规则、文明参观、禁限带、存包、拍照）后，必须更新镜像。

## 步骤

1. 打开两侧文件对照标题与条款编号。
2. 将 wxml 中可见中文抽成纯 Markdown（去掉 `<view>` / `<text>` 标签）。
3. 保留 `visit-rules.md` 文首镜像声明注释。
4. 重启后端或触发 RAG rebuild，使内存索引加载新文本。
5. 用 `/ai/chat` 问「可以带背包吗」「几点停止入馆」等核对答复是否反映新文案。

3.1 不提供自动解析 wxml 的脚本；本清单为最低可执行同步要求。
