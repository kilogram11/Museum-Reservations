# 预约 Agent 联调验收用例（阶段 2.6）

> 契约：[ai-agent-design.md](./ai-agent-design.md)  
> 执行口径：[OPTIMIZATION_PLAN.md](../OPTIMIZATION_PLAN.md)  
> Base URL：`http://127.0.0.1:8081`  
> 鉴权头：`Token: <JWT>`（注意字段名是 `Token`，不是 `Authorization`）

## 0. 验收强度（定稿）

`/ai/chat` 只返回 `{ "reply": "..." }`，**无法从响应严格证明 Tool 调用序列**。

| 项 | 本阶段 |
| --- | --- |
| 行为级验收（reply + HTTP 对照 / 副作用快照） | **必须** |
| 参考 Tool 意图 | 仅排障提示，**不作为 Pass/Fail** |
| 给 `/ai/chat` 增加 tool trace | **不做**（另开任务） |

每条用例的 Pass/Fail 只看「行为判据」与「副作用观测」。

**环境硬前置**：`/app/booking/times` 与写路径依赖真实 Redis（`localhost:6379`）。Redis 未启动时 times 会返回 `code=500`，联调前须先拉起 Redis。

---

## 1. 可复用前置数据

### 1.1 登录

```json
POST /app/user/login
{ "mobile": "13800009901", "code": "1234" }
```

演示验证码固定 `1234`。手机号可换，下文记为「测试用户」。

### 1.2 游客样例（硬前置）

`POST /app/identity/save` 要求非空 `identityName` + **校验位合法**的 `identityCard`（Hutool `IdcardUtil.isValidCard`），否则直接拒绝。

**主用样例**

```json
{
  "identityName": "联调游客A",
  "identityCard": "110101199001010015",
  "identityMobile": "13900009901"
}
```

**备用样例**（主用卡号已占用 / 与压测种子冲突时）

```json
{
  "identityName": "联调游客B",
  "identityCard": "110101199001010023",
  "identityMobile": "13900009902"
}
```

证件号仅用于本地联调，勿当真实身份。

### 1.3 准备脚本顺序

1. `POST /app/user/login` → 保存 `token`
2. `GET /app/identity/list` + Header `Token`
3. 若列表为空：`POST /app/identity/save` 用主用样例 → 再 `list`，记录 `identityId`
4. `GET /app/booking/days` → 选一个 `status=1` 的日期 `day`
5. `GET /app/booking/times?day={day}` → 选一个 `surplus` > 0 的下午时段，记录 `timeMark`

下单对话须**显式写出** `timeMark` 与 `identityId`，避免模型猜测。

---

## 2. 用例表

### TC-01 查时段（有票吗）

| 项 | 内容 |
| --- | --- |
| 前置 | 可匿名；已从 HTTP 拿到目标 `day` 的 times |
| 话术 | `{day} 下午还有票吗` |
| 参考 Tool 意图 | `queryDays`（可选）→ `queryTimes` |
| 行为判据 | `reply` 中关于开闭馆/有无票与 `GET /app/booking/times?day=...` 一致；不得瞎报具体余票数字，除非数字能被 times 对上 |
| 副作用观测 | 无 |
| 结果 | **Pass**（2026-08-17） |

### TC-02 登录下单

| 项 | 内容 |
| --- | --- |
| 前置 | Token；已有 `identityId`；目标 `timeMark` 余票 > 0；下单前记录 `GET /app/record/list` |
| 话术 | `请用 identityId={identityId} 预约 timeMark={timeMark}，帮我提交预约` |
| 参考 Tool 意图 | `submitBooking`（可先 `queryTimes`） |
| 行为判据 | `reply` 表示成功或给出真实失败原因；若提到预约号，必须与之后 `list` 中的 `joinId` 一致，禁止编造 |
| 副作用观测 | 下单后同 Token `GET /app/record/list` **新增**对应时段记录；同 `day` 目标时段 `surplus` 减 1 |
| 结果 | **Pass**（2026-08-17） |

### TC-03 查单（非空）

| 项 | 内容 |
| --- | --- |
| 前置 | Token；TC-02 已成功留下至少 1 条预约 |
| 话术 | `我的预约` |
| 参考 Tool 意图 | `listRecords` |
| 行为判据 | `reply` 与当前用户 `GET /app/record/list` 在日期/时段/状态上一致 |
| 副作用观测 | 无 |
| 结果 | **Pass**（2026-08-17） |

### TC-04 取消

| 项 | 内容 |
| --- | --- |
| 前置 | Token；从 list 取真实 `joinId` |
| 话术 | `取消预约 joinId={joinId}` |
| 参考 Tool 意图 | `listRecords` → `cancelBooking` |
| 行为判据 | `reply` 表示取消成功；不得在失败时假装成功 |
| 副作用观测 | 取消后 `GET /app/record/list`：该单消失或 `joinStatus=2`（已取消）；可选 times 余票回补 |
| 结果 | **Pass**（2026-08-17） |

### TC-05 无 Token 下单（无副作用）

| 项 | 内容 |
| --- | --- |
| 前置 | **不**带 Token 调 `/ai/chat`；但用测试用户 Token 做 list 快照；目标 `day`/`timeMark` 已知 |
| 话术 | `请用 identityId={identityId} 预约 timeMark={timeMark}，帮我提交预约` |
| 参考 Tool 意图 | `submitBooking` → `UNAUTHORIZED` |
| 行为判据 | `reply` 明确需登录 / 未登录 / UNAUTHORIZED；**不得**声称已预约成功或给出假预约号 |
| 副作用观测 | **前后各一次**：`GET /app/booking/times?day={day}` 目标时段余票不变；带测试用户 Token 的 `GET /app/record/list` 记录集合不变 |
| 结果 | **Pass**（2026-08-17） |

### TC-06 防幻觉（库存数字）

| 项 | 内容 |
| --- | --- |
| 前置 | 可带 Token；先用 HTTP 记下某日真实 times |
| 话术 | `{day} 下午还剩多少票？请给出具体数字` |
| 参考 Tool 意图 | 应走 `queryTimes`，禁止裸编 |
| 行为判据 | 若给出具体余票，必须与 `GET /app/booking/times` 一致 |
| 副作用观测 | 无写操作 |
| 结果 | **Pass**（2026-08-17） |

---

## 3. 联调记录

| 日期 | 环境 | 执行人 | 总评 |
| --- | --- | --- | --- |
| 2026-08-17 | 本机 8081 + Redis/MySQL + DeepSeek | Agent | **TC-01～06 全部 Pass** |

### 快照备忘（本次实机）

```text
mobile=13800009901
identityId=identity_load_001
day=2026-08-18
timeMark=museum_load_2026-08-18_14:00
list_count_before=0
remain_before=50
TC-05: remain 50→50，list 空→空，reply 提示未登录/UNAUTHORIZED
TC-02: new joinId=join_e550bb25bb854ac299901b427d40bee9，remain 50→49
TC-04: joinStatus=2（已取消），goneOrCancelled=true
TC-06: reply 余票 50 与 HTTP surplus 一致
```

联调中曾遇 Redis 未启动导致 `/app/booking/times` 全部 500；拉起本机 `redis-server` 后恢复。**非 Agent 代码缺陷**，属环境前置。

---

## 4. 与设计文档的关系

[ai-agent-design.md](./ai-agent-design.md) §8 话术表仍有效，但其中「期望 Tool 序列」在 2.6 仅作参考；正式 Pass/Fail 以本文「行为判据 + 副作用观测」为准。
