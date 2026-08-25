# Business Flows

## Core Scheduling Loop

The scheduler operates as a continuous two-phase loop:

1. **TimeLineTrigger** scans for due tasks and advances them to the executeline
2. **TaskWorker** pops tasks from the executeline and executes them

```mermaid
flowchart TB
    subgraph "Phase 1: Trigger"
        A[timeline ZSET] -->|score ≤ now| B[TimeLineTrigger]
        B -->|HGET config| C[typeconfig HASH]
        B -->|Policy calcScore| D[Policy Service]
        B -->|MULTI/EXEC advance| E[executeline ZSET]
    end

    subgraph "Phase 2: Execute"
        E -->|ZPOPMIN| F[TaskWorker]
        F -->|HGET handler| C
        F -->|Invoke action| G[Action Service]
        F -->|callback if needed| A
    end

    style A fill:#e1f5fe
    style E fill:#fff3e0
    style C fill:#f3e5f5
```

---

## Task Lifecycle

Complete lifecycle from registration to execution:

```mermaid
sequenceDiagram
    participant BS as Business System
    participant TS as TaskService
    participant TL as timeline
    participant TLT as TimeLineTrigger
    participant TC as typeconfig
    participant SP as SchedulingPolicy
    participant EL as executeline
    participant TW as TaskWorker
    participant ACT as Action Service

    Note over BS,ACT: Registration
    BS->>TS: registerTask(value, actionId, firstTriggerAtMs)
    TS->>TC: HGET typeconfig actionId
    TC-->>TS: config
    TS->>TL: ZADD timeline firstTriggerAtMs member
    TS-->>BS: {member, scheduleType, timelineScoreMs}

    Note over BS,ACT: Timeline Advance
    TLT->>TL: ZRANGE timeline 0 0 WITHSCORES
    TL-->>TLT: nearest member (score ≤ now)
    TLT->>TC: HGET typeconfig actionId
    TC-->>TLT: config
    TLT->>SP: calcScore(member, config)
    SP-->>TLT: executeline_score
    TLT->>TL: MULTI: ZREM timeline
    TLT->>EL: MULTI: ZADD executeline score member

    Note over BS,ACT: Execution
    TW->>EL: ZPOPMIN executeline
    EL-->>TW: member
    TW->>TC: HGET typeconfig actionId
    TC-->>TW: handler config
    TW->>ACT: POST /actions/{actionId}/execute
    ACT-->>TW: {success: true, result: "..."}
```

---

## Schedule Type Behaviors

### once (One-time execution)

```mermaid
sequenceDiagram
    participant TL as timeline
    participant TLT as TimeLineTrigger
    participant EL as executeline
    participant TW as TaskWorker

    Note over TL: score = trigger_time
    TLT->>TL: advance (score ≤ now)
    TLT->>TL: ZREM timeline member
    TLT->>EL: ZADD executeline score member
    TW->>EL: ZPOPMIN
    TW->>TW: execute action
    Note over TW: No callback
```

**Behavior:** Task executes once and is removed from the system.

---

### interval (Fixed interval)

```mermaid
sequenceDiagram
    participant TL as timeline
    participant TLT as TimeLineTrigger
    participant EL as executeline
    participant TW as TaskWorker

    Note over TL: score = trigger_time
    TLT->>TL: advance (score ≤ now)
    TLT->>TL: ZADD timeline (now + intervalMs) member
    TLT->>EL: ZADD executeline score member
    TW->>EL: ZPOPMIN
    TW->>TW: execute action
    Note over TW: No callback

    Note over TL: Next trigger already scheduled
```

**Behavior:** Next trigger time is written at advance (`now + intervalMs`). No callback needed.

---

### cron (Cron expression)

```mermaid
sequenceDiagram
    participant TL as timeline
    participant TLT as TimeLineTrigger
    participant CP as CronPolicy
    participant EL as executeline
    participant TW as TaskWorker

    Note over TL: score = trigger_time
    TLT->>TL: advance (score ≤ now)
    TLT->>CP: next(cronExpr, now)
    CP-->>TLT: next_run_at_ms
    TLT->>TL: ZADD timeline next_run_at_ms member
    TLT->>EL: ZADD executeline score member
    TW->>EL: ZPOPMIN
    TW->>TW: execute action
    Note over TW: No callback
```

**Behavior:** Next trigger calculated from cron expression at advance time.

---

### fixed_delay (Fixed delay after completion)

```mermaid
sequenceDiagram
    participant TL as timeline
    participant TLT as TimeLineTrigger
    participant EL as executeline
    participant TW as TaskWorker

    Note over TL: score = trigger_time
    TLT->>TL: advance (score ≤ now)
    TLT->>TL: ZADD timeline (now + timeoutMs) member [BACKUP]
    TLT->>EL: ZADD executeline score member

    alt Success path
        TW->>EL: ZPOPMIN
        TW->>TW: execute action [SUCCESS]
        TW->>TL: ZADD timeline (finishAt + intervalMs) [REAL]
        Note over TL: Backup overwritten by real next time
    else Failure path
        TW->>EL: ZPOPMIN
        TW->>TW: execute action [FAILURE]
        Note over TW: No callback
        Note over TL: Backup score remains
        TLT->>TL: advance again when backup expires
        Note over TLT: Retry cycle
    end
```

**Behavior:**
- Advance writes a **backup** score (`now + timeoutMs`)
- On success, callback overwrites with **real** next time (`finishAt + intervalMs`)
- On failure, backup expires and TimeLineTrigger retries

---

## Policy Score Calculation

```mermaid
flowchart TD
    A[TimeLineTrigger] -->|advance member| B{policy_handler configured?}
    B -->|Yes| C[PolicyInvokerFactory]
    C -->|kind=http| D[HttpPolicyInvoker]
    C -->|kind=grpc| E[GrpcPolicyInvoker]
    D -->|POST| F[External Policy Service]
    E -->|gRPC| F
    F -->|success| G[executeline_score from response]
    F -->|timeout/error| H[Degrade to built-in score]
    B -->|No| H
    H --> I[Built-in: once=1000, interval=2000, cron=3000, fixed_delay=4000]
    G --> J[Use score for executeline ZADD]
    I --> J

    style H fill:#fff3e0
    style I fill:#ffebee
```

**Degradation:** Policy failures never block advance. Built-in defaults ensure progress.

---

## Action Invocation

### HTTP Action

```mermaid
flowchart LR
    A[TaskWorker] -->|create request| B[HttpActionInvoker]
    B -->|POST baseUrl+path| C[Action Service]
    C -->|response| D{Parse ActionResponseBody}
    D -->|success=true| E[ActionInvokeResult success]
    D -->|success=false| F{HTTP status?}
    F -->|4xx| G[Non-retryable failure]
    F -->|5xx| H[Retryable failure]
    E --> I[Return to TaskWorker]
    G --> I
    H --> I
```

**Retry classification:**
- `success=true` → success
- `success=false` + HTTP 4xx → non-retryable
- `success=false` + HTTP 5xx → retryable
- Invalid response → retryable

### gRPC Action

```mermaid
flowchart LR
    A[TaskWorker] -->|create request| B[GrpcActionInvoker]
    B -->|lookup adapter| C[GrpcActionRouteRegistry]
    C -->|RouteKey service,method| D[ActionExecuteAdapter]
    D -->|convert to protobuf| E[gRPC Channel]
    E -->|ActionService.Execute| F[External gRPC Service]
    F -->|response| G{Map gRPC status}
    G -->|OK| H[ActionInvokeResult success]
    G -->|DEADLINE_EXCEEDED| I[Retryable timeout]
    G -->|UNAVAILABLE| J[Retryable unavailable]
    G -->|Other| K[Non-retryable failure]
    H --> L[Return to TaskWorker]
    I --> L
    J --> L
    K --> L
```

**Route Registry:** Only pre-registered `service/method` combinations are accepted.

---

## Event Flow

```mermaid
flowchart LR
    subgraph "Producers"
        TS[TaskService]
        TLT[TimeLineTrigger]
        TW[TaskWorker]
    end

    subgraph "Event System"
        EP[DefaultScheduleEventPublisher]
    end

    subgraph "Consumers"
        L1[LoggingScheduleEventListener]
        L2[Custom Listeners...]
    end

    subgraph "Observability"
        CAT[CAT Transaction/Event]
        LOG[TripLog]
    end

    TS -->|TaskRegistered| EP
    TLT -->|TimelineAdvanced| EP
    TLT -->|ExecutelineEnqueued| EP
    TW -->|ExecutionStarted| EP
    TW -->|ExecutionSucceeded| EP
    TW -->|ExecutionFailed| EP
    TW -->|TimelineCallbackWritten| EP
    TS -->|TaskCanceled| EP

    EP -->|dispatch| L1
    EP -->|dispatch| L2

    L1 --> CAT
    L1 --> LOG
```

**Event types:**
1. `TaskRegistered` — task added to timeline
2. `TaskCanceled` — task removed from timeline/executeline
3. `TimelineAdvanced` — task advanced from timeline to executeline
4. `ExecutelineEnqueued` — task entered executeline
5. `ExecutionStarted` — TaskWorker began execution
6. `ExecutionSucceeded` — action completed successfully
7. `ExecutionFailed` — action failed after retries
8. `TimelineCallbackWritten` — fixed_delay callback wrote next timeline score

---

## Admin REST Request Flow

```mermaid
sequenceDiagram
    participant UI as Admin UI
    participant REST as AdminController
    participant SOA as ScheduleServiceTestImpl
    participant SVC as TaskService / TypeConfigService
    participant REDIS as Redis

    UI->>REST: HTTP request
    REST->>SOA: assemble SOA request
    SOA->>SVC: delegate
    SVC->>REDIS: Redis operation

    alt Success
        REDIS-->>SVC: result
        SVC-->>SOA: domain object
        SOA-->>REST: SOA response (code=200)
        REST-->>UI: 200 DTO
    else Error
        REDIS-->>SVC: error
        SVC-->>SOA: SOA response (code≠200)
        SOA-->>REST: AdminServiceException
        REST-->>UI: 4xx/5xx error JSON
    end
```

**Layer mapping:**
- Admin REST → SOA → Service → Repository
- Admin REST is a thin adapter; all logic in SOA/Service layer

---

## Startup Sequence

```mermaid
sequenceDiagram
    participant APP as Spring Boot
    participant CFG as Configuration
    participant REDIS as SchedulerRedisClient
    participant HC as HealthCheck
    participant VI as IgnitePlugin
    participant LC as SchedulerLifecycle
    participant TLT as TimeLineTrigger
    participant TW as TaskWorker

    APP->>CFG: Load application.properties
    CFG->>REDIS: Initialize CRedis connection
    CFG->>HC: Run health check
    HC->>REDIS: set/get probe key
    REDIS-->>HC: OK
    HC-->>CFG: healthy

    APP->>VI: warmUP()
    VI->>VI: Validate config
    VI->>VI: selfCheck() Redis + typeconfig
    VI-->>APP: ready

    APP->>LC: @PostConstruct start()
    LC->>TLT: Start 1 daemon thread
    LC->>TW: Start M daemon threads (default 8)

    Note over TLT: Begin timeline scan loop
    Note over TW: Begin executeline poll loop
```

---

## Error Handling Flow

### Task Registration Errors

```mermaid
flowchart TD
    A[registerTask request] --> B{value empty?}
    B -->|Yes| C[400 VALIDATION_EMPTY_VALUE]
    B -->|No| D{typeconfig exists?}
    D -->|No| E[422 TYPECONFIG_NOT_FOUND]
    D -->|Yes| F{member already in timeline?}
    F -->|Yes| G[409 MEMBER_ALREADY_EXISTS]
    F -->|No| H{Redis write success?}
    H -->|No| I[503 STORAGE_WRITE_FAILED]
    H -->|Yes| J[200 Success]

    style C fill:#ffebee
    style E fill:#ffebee
    style G fill:#ffebee
    style I fill:#ffebee
    style J fill:#e8f5e9
```

### Action Invocation Errors

```mermaid
flowchart TD
    A[TaskWorker invoke] --> B{retry_num < max?}
    B -->|Yes| C[Invoke action]
    C --> D{success?}
    D -->|Yes| E[Return success]
    D -->|No| F{retryable?}
    F -->|Yes| B
    F -->|No| G[Return failure]
    B -->|No| G

    E --> H{schedule_type = fixed_delay?}
    H -->|Yes| I[Callback: ZADD timeline]
    H -->|No| J[No callback]

    G --> K{schedule_type = fixed_delay?}
    K -->|Yes| L[No callback, rely on backup]
    K -->|No| M[No callback]

    style E fill:#e8f5e9
    style G fill:#ffebee
```

---

## gRPC Channel Management

```mermaid
flowchart LR
    A[GrpcActionInvoker] -->|getChannel target| B[GrpcChannelRegistry]
    B --> C{channel cached?}
    C -->|Yes| D[Return cached channel]
    C -->|No| E[ManagedChannelBuilder.forTarget]
    E --> F{target prefix in-process:?}
    F -->|Yes| G[InProcessChannelBuilder]
    F -->|No| H[NettyChannelBuilder plaintext]
    G --> I[Cache and return]
    H --> I

    style D fill:#e8f5e9
    style I fill:#e8f5e9
```

**Channel pooling:** Channels are cached by target string and reused across invocations.

---

## Configuration Validation

```mermaid
flowchart TD
    A[putTypeConfig request] --> B{scheduleType valid?}
    B -->|No| C[400 INVALID_SCHEDULE_TYPE]
    B -->|Yes| D{handler present?}
    D -->|No| E[400 MISSING_REQUIRED_FIELD]
    D -->|Yes| F{handler.kind?}

    F -->|http| G{baseUrl + path present?}
    F -->|grpc| H{target + service + method present?}
    F -->|other| I[400 INVALID_HANDLER_CONFIG]

    G -->|No| E
    G -->|Yes| J{scheduleType = interval/fixed_delay?}
    H -->|No| E
    H -->|Yes| J

    J -->|Yes| K{intervalMs > 0?}
    J -->|No| L{scheduleType = cron?}

    K -->|No| E
    K -->|Yes| M{scheduleType = fixed_delay?}

    L -->|Yes| N{cronExpr valid?}
    L -->|No| O{policyHandler present?}

    N -->|No| E
    N -->|Yes| O

    M -->|Yes| P{timeoutMs > 0?}
    M -->|No| O

    P -->|No| E
    P -->|Yes| O

    O -->|Yes| Q{policyHandler.kind = grpc?}
    O -->|No| R[HSET typeconfig]

    Q -->|Yes| S{service + method = policy.v1.PolicyService/Score?}
    Q -->|No| E

    S -->|No| E
    S -->|Yes| R

    R --> T[200 Success]

    style C fill:#ffebee
    style E fill:#ffebee
    style I fill:#ffebee
    style T fill:#e8f5e9
```

---

## Monitoring Query Patterns

### Dashboard Aggregation

```mermaid
flowchart LR
    A[GET /dashboard] --> B[ZCARD timeline]
    A --> C[ZCARD executeline]
    A --> D[HLEN typeconfig]
    A --> E[ZRANGEBYSCORE timeline -inf now+60s LIMIT 0 10]
    A --> F[rangeNearestWithScores 10]

    B --> G[timelineCount]
    C --> H[executeLineCount]
    D --> I[typeConfigCount]
    E --> J[nearestTasks]
    F --> K[recentTaskWindow]

    G --> L[DashboardResponse]
    H --> L
    I --> L
    J --> L
    K --> L
```

### Timeline Monitor

```mermaid
flowchart TD
    A[GET /monitor/timeline] --> B{scoreFrom/scoreTo provided?}
    B -->|Yes| C[ZRANGEBYSCORE timeline scoreFrom scoreTo LIMIT 0 100]
    B -->|No| D{start/stop provided?}
    D -->|Yes| E[ZRANGE timeline start stop WITHSCORES]
    D -->|No| F[ZRANGE timeline 0 99 WITHSCORES]

    C --> G[items]
    E --> G
    F --> G

    A --> H[ZRANGEBYSCORE timeline now now+60s LIMIT 0 20]
    H --> I[nearDueItems]

    G --> J[TimelineMonitorResponse]
    I --> J
```

---

## Summary

The scheduler implements a **two-queue model** with Redis as the sole persistence layer:

1. **timeline** — when to trigger (score = timestamp)
2. **executeline** — what to execute now (score = priority)

The **TimeLineTrigger** advances due tasks atomically via MULTI/EXEC, while **TaskWorker** executes them with retry logic. **Policy services** optionally determine execution priority, with graceful degradation to built-in defaults.

The system supports four schedule types (once, interval, cron, fixed_delay), each with distinct advance and callback behaviors. All operations are observable via a synchronous event system with 7 event types covering the full task lifecycle.
