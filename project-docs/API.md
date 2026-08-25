# API Documentation

## Overview

The scheduler exposes three API surfaces:

| Surface | Protocol | Base Path | Purpose |
|---------|----------|-----------|---------|
| **SOA** | CDubbo (Baiji) | `ScheduleServiceTest` | Business system integration |
| **Admin REST** | HTTP | `/admin/api/v1` | Ops console, monitoring |
| **Mock** | HTTP/gRPC | `/mock/*`, `:50051` | Local development |

---

## Admin REST API

Base path: `/admin/api/v1`

### Tasks

#### Create Task

```
POST /admin/api/v1/tasks
Content-Type: application/json
```

**Request Body:**

```json
{
  "value": ["hotel_example", "48", "22"],
  "actionId": "main",
  "firstTriggerAtMs": 1719792000000
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `value` | `string[]` | Yes | Ordered business values (opaque to scheduler) |
| `actionId` | `string` | Yes | Action identifier (must have typeconfig) |
| `firstTriggerAtMs` | `long` | Yes | First trigger epoch-millis (positive) |

**Response (200):**

```json
{
  "member": "hotel_example;48;22;main",
  "value": ["hotel_example", "48", "22"],
  "actionId": "main",
  "scheduleType": "interval",
  "timelineScoreMs": 1719792000000
}
```

**Errors:**
- `400` — `VALIDATION_EMPTY_VALUE`, `VALIDATION_INVALID_REQUEST`
- `409` — `MEMBER_ALREADY_EXISTS`
- `422` — `TYPECONFIG_NOT_FOUND`
- `503` — `STORAGE_WRITE_FAILED`

---

#### Cancel Task

```
DELETE /admin/api/v1/tasks?member={member}
```

| Param | Type | Required | Description |
|-------|------|----------|-------------|
| `member` | `string` | Yes | Full member string |

**Response (200):**

```json
{
  "member": "hotel_example;48;22;main",
  "alreadyCanceled": false
}
```

**Idempotent:** Canceling a non-existent member returns `alreadyCanceled: true`.

---

#### List Tasks

```
GET /admin/api/v1/tasks?actionId=&scheduleType=&page=&pageSize=
```

| Param | Type | Required | Description |
|-------|------|----------|-------------|
| `actionId` | `string` | No | Filter by action_id |
| `scheduleType` | `string` | No | Filter: once/interval/cron/fixed_delay |
| `page` | `int` | No | Page number (default 1) |
| `pageSize` | `int` | No | Items per page (default 20, max 100) |

**Response (200):**

```json
{
  "items": [
    {
      "member": "hotel_example;48;22;main",
      "value": ["hotel_example", "48", "22"],
      "actionId": "main",
      "scheduleType": "interval",
      "timelineScoreMs": 1719792060000
    }
  ],
  "page": 1,
  "pageSize": 20,
  "total": 1,
  "windowLimit": 100
}
```

**Note:** Filters apply within a nearest-100 task window (prevents full scans).

---

#### Get Task Detail

```
GET /admin/api/v1/tasks/detail?member={member}
```

**Response (200):**

```json
{
  "member": "hotel_example;48;22;main",
  "value": ["hotel_example", "48", "22"],
  "actionId": "main",
  "scheduleType": "interval",
  "timelineScoreMs": 1719792060000,
  "inTimeline": true,
  "inExecuteLine": false,
  "executeLineScore": null
}
```

---

### TypeConfigs

#### List TypeConfigs

```
GET /admin/api/v1/type-configs?scheduleType=&page=&pageSize=
```

| Param | Type | Required | Description |
|-------|------|----------|-------------|
| `scheduleType` | `string` | No | Filter by schedule type |
| `page` | `int` | No | Page number (default 1) |
| `pageSize` | `int` | No | Items per page (default 20) |

**Response (200):**

```json
{
  "items": [
    {
      "actionId": "main",
      "scheduleType": "interval",
      "intervalMs": 60000,
      "timeoutMs": null,
      "cronExpr": null,
      "handler": {
        "kind": "http",
        "baseUrl": "https://business.example.com",
        "path": "/actions/main/execute"
      },
      "policyHandler": null
    }
  ],
  "page": 1,
  "pageSize": 20,
  "total": 1
}
```

---

#### Get TypeConfig Detail

```
GET /admin/api/v1/type-configs/detail?actionId={actionId}
```

**Response (200):** Full typeconfig object (same shape as list items).

**Errors:** `404` if actionId not found.

---

#### Upsert TypeConfig

```
PUT /admin/api/v1/type-configs
Content-Type: application/json
```

**Request Body:**

```json
{
  "actionId": "main",
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

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `actionId` | `string` | Yes | Action identifier |
| `scheduleType` | `string` | Yes | `once` / `interval` / `cron` / `fixed_delay` |
| `intervalMs` | `long` | interval/fixed_delay | Interval in milliseconds |
| `timeoutMs` | `long` | fixed_delay | Backup timeout in milliseconds |
| `cronExpr` | `string` | cron | Spring CronExpression |
| `handler` | `HandlerConfigDto` | Yes | Action execution route |
| `policyHandler` | `HandlerConfigDto` | No | Policy scoring route (optional) |

**HandlerConfigDto (HTTP):**

```json
{
  "kind": "http",
  "baseUrl": "https://business.example.com",
  "path": "/actions/main/execute"
}
```

**HandlerConfigDto (gRPC):**

```json
{
  "kind": "grpc",
  "target": "127.0.0.1:50051",
  "service": "action.v1.ActionService",
  "method": "Execute",
  "deadlineMs": 30000
}
```

**Response (200):** Full typeconfig object.

**Errors:**
- `400` — `INVALID_SCHEDULE_TYPE`, `MISSING_REQUIRED_FIELD`, `INVALID_HANDLER_CONFIG`

---

### Monitor

#### Timeline Monitor

```
GET /admin/api/v1/monitor/timeline?start=&stop=&scoreFrom=&scoreTo=
```

| Param | Type | Description |
|-------|------|-------------|
| `start` | `long` | Rank start (default 0) |
| `stop` | `long` | Rank stop (default 99) |
| `scoreFrom` | `long` | Score range start (alternative to rank) |
| `scoreTo` | `long` | Score range end |

**Constraints:** `stop - start + 1 <= 100`

**Response (200):**

```json
{
  "totalCount": 150,
  "rangeStart": 0,
  "rangeStop": 99,
  "items": [
    { "member": "hotel;48;22;main", "scoreMs": 1719792060000 }
  ],
  "nearDueItems": [
    { "member": "hotel;49;22;main", "scoreMs": 1719792001000 }
  ]
}
```

**nearDueItems:** Tasks in `[now, now+60s)` (max 20).

---

#### Executeline Monitor

```
GET /admin/api/v1/monitor/executeline?start=&stop=
```

| Param | Type | Description |
|-------|------|-------------|
| `start` | `long` | Rank start (default 0) |
| `stop` | `long` | Rank stop (default 99) |

**Response (200):**

```json
{
  "totalCount": 5,
  "rangeStart": 0,
  "rangeStop": 4,
  "items": [
    { "member": "hotel;48;22;main", "scoreMs": 2000 }
  ]
}
```

---

### Dashboard

#### Get Dashboard

```
GET /admin/api/v1/dashboard
```

**Response (200):**

```json
{
  "timelineCount": 150,
  "executeLineCount": 5,
  "typeConfigCount": 3,
  "nearestTasks": [
    { "member": "hotel;48;22;main", "scoreMs": 1719792001000 }
  ],
  "recentTaskWindow": [
    {
      "member": "hotel;48;22;main",
      "value": ["hotel", "48", "22"],
      "actionId": "main",
      "scheduleType": "interval",
      "timelineScoreMs": 1719792060000
    }
  ],
  "workerThreads": 8,
  "tickMs": 10000,
  "pollMs": 60000
}
```

**nearestTasks:** Tasks in `(-inf, now+60s]` (includes overdue, max 10).

---

## gRPC Contracts

### Action Service

Proto: `src/main/proto/action/v1/action.proto`

```protobuf
service ActionService {
  rpc Execute (ExecuteRequest) returns (ExecuteResponse);
}

message ExecuteRequest {
  string member = 1;
  repeated string value = 2;
  string schedule_type = 3;
  int64 triggered_at_ms = 4;
  int32 retry_num = 5;
  string action_id = 6;
}

message ExecuteResponse {
  bool success = 1;
  string result = 2;
  string error_code = 3;
  string error_message = 4;
}
```

### Policy Service

Proto: `src/main/proto/policy/v1/policy.proto`

```protobuf
service PolicyService {
  rpc Score (ScoreRequest) returns (ScoreResponse);
}

message ScoreRequest {
  string member = 1;
  repeated string value = 2;
  string action_id = 3;
  string schedule_type = 4;
  int64 triggered_at_ms = 5;
}

message ScoreResponse {
  optional int64 executeline_score = 1;
}
```

### Hotel Service (Demo)

Proto: `src/main/proto/hotel/v1/hotel.proto`

```protobuf
service HotelService {
  rpc RefreshPrice (RefreshPriceRequest) returns (RefreshPriceResponse);
}
```

Demo heterogeneous route; not required for production.

---

## Mock Endpoints

### HTTP Action Mock

```
POST http://localhost:8080/mock/actions/execute
```

**Response:**

```json
{
  "success": true,
  "result": "ok",
  "error_code": null,
  "error_message": null
}
```

**Query params:** `delayMs`, `status`, `success`, `businessFail`

---

### HTTP Policy Mock

```
POST http://localhost:8080/mock/policy/score
```

**Response:**

```json
{
  "executeline_score": 2500
}
```

---

### gRPC Mock Server

Auto-starts on `127.0.0.1:50051` when `scheduler.mock.grpc.enabled=true`.

**Behavior:**
- Action `action_id=grpc_ok` → success
- Action `action_id=grpc_fail` → `MOCK_ACTION_FAIL`
- Hotel RefreshPrice `action_id=grpc_fail` → `MOCK_HOTEL_FAIL`
- Policy Score → always `executeline_score=2500`

---

## Error Model

### Admin REST Errors

All errors return a uniform JSON body:

```json
{
  "errorType": "VALIDATION_ERROR",
  "errorCode": "MISSING_REQUIRED_FIELD",
  "detail": "value is required",
  "field": "value"
}
```

| HTTP Status | errorType | Description |
|-------------|-----------|-------------|
| 400 | `VALIDATION_ERROR` | Request structure invalid |
| 404 | `NOT_FOUND` | Resource not found |
| 409 | `CONFLICT` | State conflict (duplicate) |
| 422 | `BUSINESS_RULE_VIOLATION` | Business rule failed |
| 500 | `INTERNAL_ERROR` | Unexpected error |
| 503 | `DEPENDENCY_UNAVAILABLE` | Redis unavailable |

### Error Codes

| errorCode | HTTP | retryable | Scenario |
|-----------|------|-----------|----------|
| `VALIDATION_EMPTY_VALUE` | 400 | false | value is empty |
| `VALIDATION_INVALID_REQUEST` | 400 | false | Field validation failed |
| `TYPECONFIG_NOT_FOUND` | 422/404 | false | action_id not configured |
| `MEMBER_ALREADY_EXISTS` | 409 | false | Duplicate task |
| `STORAGE_WRITE_FAILED` | 503 | true | Redis write error |
| `TASK_NOT_FOUND` | 404 | false | Member not found |
| `INVALID_SCHEDULE_TYPE` | 400 | false | Unknown schedule type |
| `MISSING_REQUIRED_FIELD` | 400 | false | Missing required field |
| `INVALID_HANDLER_CONFIG` | 400 | false | Handler config invalid |
| `INVALID_RANGE` | 400 | false | Monitor range invalid |

---

## Limits

| Constant | Value | Purpose |
|----------|-------|---------|
| `TASK_WINDOW` | 100 | Max tasks in list/detail |
| `MONITOR_RANGE_MAX` | 100 | Max monitor ZRANGE window |
| `NEAR_DUE_LIMIT` | 20 | Max near-due items |
| `NEAR_DUE_WINDOW_MS` | 60,000 | Near-due window (60s) |
| `DASHBOARD_NEAREST` | 10 | Max dashboard near-due |
