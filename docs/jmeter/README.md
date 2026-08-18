# JMeter 预约并发压测

目标：验收阶段 1 Redis 预扣 —— **不超卖**、**一证一约**。

## 前置

1. MySQL `museum_book` 已按 `docs/sql/README.md` 初始化
2. Redis 已启动（默认 `6379` 无密码）
3. 后端已启动：`http://localhost:8081`
4. 本机 JMeter：`D:\Users\peiyaowan\apache-jmeter-5.6.3\apache-jmeter-5.6.3\bin\jmeter.bat`

## 一键准备

```bat
python docs\sql\generate_loadtest_seed.py
REM 再导入 museum_book_seed_loadtest.sql（若日期变更）
python docs\jmeter\prepare_tokens.py
python docs\jmeter\reset_loadtest.py
python docs\jmeter\generate_jmx.py
```

## 场景 A：超卖防护

- 线程：80；Ramp-Up：1s；同一 `timeMark`；每人不同 `identityId`
- `LIMIT_CNT=50` → 期望成功 50、失败约 30（多为 `3002`）

```bat
jmeter -n -t docs\jmeter\booking-oversell.jmx -l docs\jmeter\results-oversell.jtl -j docs\jmeter\jmeter-oversell.log
python docs\jmeter\assert_loadtest.py
```

断言：`SUCC_CNT + Redis(booking:stock) = LIMIT_CNT`，且成功预约行数 = `SUCC_CNT`，无超卖。

## 场景 B：一证一约

- 10 线程同一 `identityId` 并发 submit
- 期望成功 **1**；Redis 存在 `booking:booked:{day}:{identityId}`

```bat
python docs\jmeter\reset_loadtest.py
python docs\jmeter\generate_jmx.py
jmeter -n -t docs\jmeter\booking-duplicate.jmx -l docs\jmeter\results-duplicate.jtl
```

## 产物说明

| 文件 | 说明 |
| --- | --- |
| `mobiles.csv` | 手机号 / userId / identityId |
| `users.csv` | Token / identityId（含 JWT，勿提交仓库） |
| `booking-oversell.jmx` / `booking-duplicate.jmx` | 测试计划 |
| `results-*.jtl` | 运行结果（本地） |

## 2026-08-16 实跑摘要

见根目录 [README.md](../../README.md)「压测结论」。
