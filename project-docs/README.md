# SchedulerTest — Hotel Timed Scheduler

> **appId:** `100062091` · **BU:** HTL (Hotel) · **Owner:** peiyaowan

A Redis-backed distributed timed task scheduler for the Hotel business unit. Tasks wait on a **timeline** → advance to an **executeline** → get executed by **TaskWorker** via HTTP or gRPC → optionally reschedule via **fixed_delay** callback.

---

## Tech Stack

| Layer | Technology | Version |
|-------|-----------|---------|
| Runtime | Java | 21 |
| Framework | Spring Boot + Trip FX | 8.33.4-Java21 |
| Build | Maven (WAR packaging) | 3.3.9+ |
| Storage | CRedis (sorted sets + hash) | Cluster: `HTL_STRATEGY_HUB` |
| RPC (outbound) | gRPC (netty-shaded) | 1.58.1 |
| RPC (SOA) | CDubbo (Baiji) | via fx-spring-boot-starter |
| Logging | log4j2 + TripLog | — |
| Monitoring | CAT (Transaction/Event) + TripLog | — |
| Ops Console (frontend) | React 18 + Ant Design 5 + Vite 5 | `admin-web/` |
| Health | VI Ignite Plugin | `hotel.schedulertest.ignite` |

---

## Quick Start

### Backend

```shell
./mvnw spring-boot:run
# → http://localhost:8080/
```

Run configuration class: `AppSpringBootConfiguration`

### Frontend (Admin Console)

```shell
cd admin-web
npm install
npm run dev
```

### Local Mocks

| Mock | Endpoint | Notes |
|------|----------|-------|
| HTTP Action | `POST /mock/actions/execute` | Returns `{ success: true, result: "ok" }` |
| HTTP Policy | `POST /mock/policy/score` | Returns `{ executeline_score: 2500 }` |
| gRPC Server | `127.0.0.1:50051` | In-process; auto-starts when `scheduler.mock.grpc.enabled=true` |

---

## Project Structure

```
schedulertest/
├── src/main/java/com/ctrip/hotel/schedulertest/
│   ├── kernel/            # Core scheduling loop (TimeLineTrigger + TaskWorker)
│   ├── redis/             # Redis repositories (Timeline, Executeline, TypeConfig)
│   ├── domain/            # Domain models (TypeConfigDomain, ParsedMember)
│   ├── service/           # Business services (TaskService, TypeConfigService)
│   ├── policy/            # Scheduling policies (Cron, Interval, Score)
│   ├── invoker/           # Action & Policy invocation (HTTP, gRPC, adapters)
│   ├── event/             # Internal event system (7 event types)
│   ├── admin/             # REST admin API + DTOs
│   ├── soa/               # SOA ScheduleServiceTest implementation
│   ├── config/            # Spring configuration, Redis client, properties
│   ├── mock/              # Local mock controllers + gRPC server
│   ├── vi/                # VI Ignite health check plugin
│   ├── cdubbo/            # CDubbo component scan configuration
│   └── util/              # MemberParser, SoaResponseHelper
├── src/main/proto/        # gRPC proto definitions (action, policy, hotel)
├── src/main/resources/    # application.properties, log4j2.xml, ignite config
├── admin-web/             # React frontend (Ant Design console)
├── 内核设计.md             # Kernel design document (Chinese)
└── 接口设计.md             # Interface design document (Chinese)
```

---

## Configuration

All properties use prefix `scheduler` in `application.properties`:

| Key | Default | Description |
|-----|---------|-------------|
| `scheduler.redis.cluster` | `HTL_STRATEGY_HUB` | CRedis cluster name |
| `scheduler.redis.key-prefix` | _(empty)_ | Key prefix for isolation (set `scheduler-local:<user>:` for dev) |
| `scheduler.timeline.tick-ms` | `10000` | Max idle sleep for TimeLineTrigger |
| `scheduler.timeline.min-sleep-ms` | `50` | Min sleep between successful advances |
| `scheduler.timeline.batch-size` | `5000` | Max members per advance batch |
| `scheduler.worker.threads` | `8` | TaskWorker thread pool size |
| `scheduler.worker.poll-ms` | `60000` | Idle poll interval for TaskWorker |
| `scheduler.action.max-retries` | `3` | Action inline retry count |
| `scheduler.action.timeout-ms` | `30000` | HTTP Action timeout |
| `scheduler.policy.timeout-ms` | `3000` | HTTP/gRPC Policy timeout |
| `scheduler.mock.grpc.enabled` | `true` | Enable local gRPC mock server |
| `scheduler.mock.grpc.port` | `50051` | Mock gRPC server port |

---

## SOA API

Service: `ScheduleServiceTest` (via CDubbo/Baiji)

| Operation | Category | Description |
|-----------|----------|-------------|
| `putTypeConfig` | Config | Create/update typeconfig for an action_id |
| `getTypeConfig` | Config | Get typeconfig by action_id |
| `listTypeConfigs` | Config | List all typeconfigs |
| `registerTask` | Control | Register a timed task |
| `cancelTask` | Control | Cancel a task (idempotent) |
| `getTask` | Control | Get task detail |
| `listTasks` | Control | List tasks with filters |
| `checkHealth` | Health | Health check |

---

## Schedule Types

| Type | Advance Behavior | Callback Behavior |
|------|-----------------|-------------------|
| `once` | ZREM timeline + ZADD executeline | No callback |
| `interval` | ZADD timeline (now + intervalMs) + ZADD executeline | No callback |
| `cron` | ZADD timeline (next cron) + ZADD executeline | No callback |
| `fixed_delay` | ZADD timeline (now + timeoutMs, **backup**) + ZADD executeline | Success: ZADD timeline (finishAt + intervalMs); Failure: rely on backup |

---

## Reference

- [内核设计.md](../内核设计.md) — Kernel design (Chinese)
- [接口设计.md](../接口设计.md) — Interface design (Chinese)
- [Trip FX Framework](http://fx/)
- [IMPLEMENTATION_PLAN.md](../IMPLEMENTATION_PLAN.md) — Phased status
