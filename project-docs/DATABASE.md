# Database (Redis) Design

## Overview

The scheduler uses **Redis only** for persistence — no relational database. All state is stored in a CRedis cluster (`HTL_STRATEGY_HUB`) using 3 keys:

| Key | Redis Type | Purpose |
|-----|-----------|---------|
| `timeline` | ZSET | Scheduled tasks waiting for trigger time |
| `executeline` | ZSET | Tasks ready for execution (priority queue) |
| `typeconfig` | HASH | Per-action configuration |

---

## Key Prefix

All keys support an optional prefix via `scheduler.redis.key-prefix`:

| Environment | Prefix | Effective Keys |
|-------------|--------|---------------|
| Production (default) | _(empty)_ | `timeline`, `executeline`, `typeconfig` |
| Local dev | `scheduler-local:alice:` | `scheduler-local:alice:timeline`, etc. |
| Integration test | `scheduler-it:` | `scheduler-it:timeline`, etc. |

Applied at startup by `RedisKeyPrefixConfiguration.applyPrefix()`.

---

## Data Model

### timeline (ZSET)

Tasks waiting for their trigger time.

| Field | Type | Description |
|-------|------|-------------|
| **member** | `string` | Task identifier: `{value1};{value2};...;{action_id}` |
| **score** | `long` | Next trigger timestamp (epoch millis) |

**Score semantics by schedule type:**

| schedule_type | Score Meaning |
|--------------|---------------|
| `once` | First (and only) trigger time; ZREM on advance |
| `interval` | `now + intervalMs` (written at advance) |
| `cron` | Next cron occurrence (written at advance) |
| `fixed_delay` | **Backup** `now + timeoutMs` (overwritten by callback on success) |

---

### executeline (ZSET)

Tasks ready for execution, ordered by priority.

| Field | Type | Description |
|-------|------|-------------|
| **member** | `string` | Same member string as timeline |
| **score** | `long` | Priority score (smaller = higher priority) |

**Score calculation:**
- If `policy_handler` configured: external Policy service returns score
- Otherwise: built-in defaults (once=1000, interval=2000, cron=3000, fixed_delay=4000)

---

### typeconfig (HASH)

Per-action configuration stored as JSON.

| Field | Type | Description |
|-------|------|-------------|
| **field** | `string` | `action_id` (e.g., `main`, `refreshPrice`) |
| **value** | `JSON` | Full configuration object |

**JSON Structure:**

```json
{
  "scheduleType": "interval",
  "intervalMs": 60000,
  "timeoutMs": null,
  "cronExpr": null,
  "handler": {
    "kind": "http",
    "baseUrl": "https://business.example.com",
    "path": "/actions/main/execute"
  },
  "policyHandler": {
    "kind": "grpc",
    "target": "127.0.0.1:50051",
    "service": "policy.v1.PolicyService",
    "method": "Score",
    "deadlineMs": 3000
  }
}
```

**Field validation by scheduleType:**

| scheduleType | Required Fields |
|-------------|----------------|
| `once` | `handler` |
| `interval` | `handler`, `intervalMs` (> 0) |
| `cron` | `handler`, `cronExpr` (valid Spring CronExpression) |
| `fixed_delay` | `handler`, `intervalMs` (> 0), `timeoutMs` (> 0) |

**Handler kinds:**

| kind | Required Fields |
|------|----------------|
| `http` | `baseUrl`, `path` |
| `grpc` | `target`, `service`, `method`; `deadlineMs` optional (> 0) |

---

## Member Encoding

Tasks are encoded as semicolon-delimited strings:

```
member = "{value1};{value2};...;{valueN};{action_id}"
```

**Rules:**
- Segments separated by `;`
- Last segment is always `action_id`
- Preceding segments are opaque business values (scheduler doesn't parse)
- No segment may contain the delimiter `;`

**Example:**

```
member = "hotel_example;48;22;main"
         ↓             ↓  ↓  ↓
       value[0]     [1] [2] action_id
```

**Parsing:**

```java
ParsedMember parsed = MemberParser.parse(member);
// parsed.values() = ["hotel_example", "48", "22"]
// parsed.actionId() = "main"
```

**Building:**

```java
String member = MemberParser.build(
    List.of("hotel_example", "48", "22"),
    "main"
);
// member = "hotel_example;48;22;main"
```

---

## Domain Objects

### TypeConfigDomain

Immutable configuration object:

```java
record TypeConfigDomain(
    String scheduleType,      // once | interval | cron | fixed_delay
    Long intervalMs,          // nullable
    Long timeoutMs,           // nullable
    String cronExpr,          // nullable
    HandlerConfigDomain handler,        // required
    HandlerConfigDomain policyHandler   // nullable
)
```

### HandlerConfigDomain

Immutable handler configuration:

```java
record HandlerConfigDomain(
    String kind,        // http | grpc
    String baseUrl,     // HTTP only
    String path,        // HTTP only
    String target,      // gRPC only
    String service,     // gRPC only
    String method,      // gRPC only
    Integer deadlineMs  // gRPC only, nullable
)
```

### ParsedMember

Parsed member string:

```java
record ParsedMember(
    List<String> values,  // opaque business values
    String actionId       // trailing segment
)
```

### ScoredMember

ZSET member with score:

```java
record ScoredMember(
    String member,   // full member string
    long scoreMs     // score in epoch millis
)
```

---

## Repository Operations

### TimelineRepository

| Method | Redis Command | Description |
|--------|--------------|-------------|
| `add(member, scoreMs)` | `ZADD timeline scoreMs member` | Insert/update task |
| `remove(member)` | `ZREM timeline member` | Delete task |
| `getScore(member)` | `ZSCORE timeline member` | Get scheduled time |
| `count()` | `ZCARD timeline` | Total task count |
| `rangeWithScores(start, stop)` | `ZRANGE timeline start stop WITHSCORES` | Rank-bounded query |
| `rangeByScoreWithScores(min, max, offset, count)` | `ZRANGEBYSCORE timeline min max WITHSCORES LIMIT offset count` | Score-bounded query |
| `rangeNearestWithScores(limit)` | `ZRANGEBYSCORE timeline -inf +inf WITHSCORES LIMIT 0 limit` | Nearest due tasks |
| `getNearest()` | `ZRANGE timeline 0 0 WITHSCORES` | Single nearest task |
| `advanceRemove(member, execScore)` | `MULTI: ZREM timeline, ZADD executeline / EXEC` | Atomic once advance |
| `advanceUpdate(member, tlScore, execScore)` | `MULTI: ZADD timeline, ZADD executeline / EXEC` | Atomic recurring advance |
| `batchAdvance(advances)` | Pipeline of ZADD/ZREM | Batch advance (non-atomic per member) |

### ExecutelineRepository

| Method | Redis Command | Description |
|--------|--------------|-------------|
| `add(member, score)` | `ZADD executeline score member` | Queue for execution |
| `remove(member)` | `ZREM executeline member` | Complete execution |
| `popMin()` | WATCH + ZRANGE[0,0] + MULTI/ZREM/EXEC | Atomically pop highest-priority task |
| `getScore(member)` | `ZSCORE executeline member` | Get priority score |
| `count()` | `ZCARD executeline` | Total count |
| `rangeWithScores(start, stop)` | `ZRANGE executeline start stop WITHSCORES` | Rank-bounded query |

**popMin algorithm (no ZPOPMIN API):**
1. WATCH executeline
2. ZRANGE [0,0] WITHSCORES to peek minimum
3. MULTI → ZREM candidate → EXEC
4. Retry up to 16 times on conflict (EXEC returns null)

### TypeConfigRepository

| Method | Redis Command | Description |
|--------|--------------|-------------|
| `put(actionId, config)` | `HSET typeconfig actionId JSON` | Serialize and store |
| `get(actionId)` | `HGET typeconfig actionId` | Retrieve and deserialize |
| `getByActionIds(actionIds)` | Multiple `HGET` | Batch fetch |
| `count()` | `HLEN typeconfig` | Total count |
| `getAll()` | `HGETALL typeconfig` | Full dump |

**Error handling:**
- Serialization failure → `IllegalStateException`
- Deserialization failure → log warning, skip entry (fail-open)

---

## Atomicity Strategies

### 1. Single-Key Atomicity

Most operations (add, remove, getScore) rely on Redis single-command atomicity.

### 2. MULTI/EXEC (Advance)

联合写入 timeline + executeline:

```redis
MULTI
  ZREM timeline member          # or ZADD timeline nextScore member
  ZADD executeline execScore member
EXEC
```

- WATCH detects conflicts
- Retry on EXEC null (conflict)
- Used by `advanceRemove`, `advanceUpdate`

### 3. Pipeline Batch (High Throughput)

`batchAdvance` pipelines all commands:

```redis
# Pipeline (no MULTI)
ZREM timeline member1
ZADD executeline score1 member1
ZADD timeline nextScore2 member2
ZADD executeline score2 member2
...
```

- Reduces RTT from O(n) to O(1)
- Per-member non-atomic (commands interleaved by Redis)
- Acceptable under single-leader design (only TimeLineTrigger writes)
- Failed members retry next cycle

### 4. popMin (Optimistic Locking)

Simulates ZPOPMIN using WATCH + optimistic locking:

```redis
WATCH executeline
ZRANGE executeline 0 0 WITHSCORES    # peek minimum
MULTI
  ZREM executeline candidate
EXEC
# If EXEC returns null → conflict → retry
```

---

## Score Conversion

Redis ZSET scores are `double`, but scheduler uses `long` (epoch millis):

```java
// Write
double redisScore = (double) scoreMs;

// Read
long scoreMs = Math.round(redisScore);
```

No transformation — direct cast with rounding on read.

---

## Data Flow Examples

### Task Registration

```
1. Validate request
2. HGET typeconfig {actionId} → fail if missing
3. Build member = values.join(";") + ";" + actionId
4. ZSCORE timeline member → fail if exists (duplicate)
5. ZADD timeline firstTriggerAtMs member
6. Publish TaskRegistered event
```

### Task Cancellation

```
1. ZREM timeline member
2. ZREM executeline member (idempotent)
3. Publish TaskCanceled event with alreadyCanceled flag
```

### Timeline Advance (once)

```
1. ZRANGE timeline 0 0 WITHSCORES → check if due
2. HGET typeconfig {actionId}
3. SchedulingPolicy.calcScore → executeline_score
4. MULTI
     ZREM timeline member
     ZADD executeline execScore member
   EXEC
5. Publish TimelineAdvanced, ExecutelineEnqueued events
```

### Timeline Advance (interval)

```
1-3. Same as once
4. nextTimelineScore = now + intervalMs
   MULTI
     ZADD timeline nextTimelineScore member
     ZADD executeline execScore member
   EXEC
5. Publish events
```

### Task Execution (fixed_delay success)

```
1. ZPOPMIN executeline → member
2. HGET typeconfig {actionId}
3. Invoke action (HTTP/gRPC) → success
4. nextScore = finishAtMs + intervalMs
5. ZADD timeline nextScore member
6. Publish ExecutionSucceeded, TimelineCallbackWritten events
```

---

## Monitoring Queries

### Dashboard Counts

```redis
ZCARD timeline          # timelineCount
ZCARD executeline       # executeLineCount
HLEN typeconfig         # typeConfigCount
```

### Near-Due Tasks

```redis
ZRANGEBYSCORE timeline -inf (now+60s WITHSCORES LIMIT 0 10
```

### Timeline Monitor (rank-bounded)

```redis
ZRANGE timeline start stop WITHSCORES
```

### Timeline Monitor (score-bounded)

```redis
ZRANGEBYSCORE timeline scoreFrom scoreTo WITHSCORES LIMIT 0 100
```

---

## No Relational Database

The scheduler intentionally uses **no SQL database**:

- **Simplicity:** No ORM, no migrations, no connection pools
- **Performance:** Redis atomic operations are faster than SQL transactions
- **Trade-off:** No persistent history, no complex queries
- **Mitigation:** Events provide observability; admin API provides bounded queries

For audit trails or historical data, consumers should subscribe to `ScheduleEvent` and persist externally.
