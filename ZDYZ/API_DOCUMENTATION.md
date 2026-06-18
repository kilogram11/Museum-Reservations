# 博物馆预约系统 API 接口文档 (详细版)

本文档按照实际测试验证标准编写，包含详细的接口说明、请求示例、参数说明及返回结构。

---

## 目录

- [管理端 API (Admin)](#管理端-api-admin)
  - [1. 管理员认证 (Admin Auth)](#1-管理员认证-admin-auth)
  - [2. 活动管理 (Activity)](#2-活动管理-activity)
  - [3. 场馆管理 (Museum)](#3-场馆管理-museum)
  - [4. 公告管理 (News)](#4-公告管理-news)
  - [5. 预约核销管理 (Join)](#5-预约核销管理-join)
  - [6. 黑名单管理 (Blacklist)](#6-黑名单管理-blacklist)
- [小程序端 API (App)](#小程序端-api-app)
  - [1. 用户管理 (User)](#1-用户管理-user)
  - [2. 常用游客管理 (Identity)](#2-常用游客管理-identity)
  - [3. 预约模块 (Booking)](#3-预约模块-booking)
  - [4. 公共信息模块 (Public)](#4-公共信息模块-public)

---

## 管理端 API (Admin)

### 1. 管理员认证 (Admin Auth)

**Controller**: `/admin/auth`

#### (1) 管理员注册

1. **URL**: `/admin/auth/register`
2. **Method**: `POST`
3. **Body (JSON)**:
   ```json
   {
     "username": "admin_new",
     "password": "password123"
   }
   ```
4. **预期结果**:
   ```json
   {
     "code": 200,
     "msg": "注册成功",
     "data": { ... } // 返回管理员信息 (脱敏)
   }
   ```

#### (2) 管理员登录

1. **URL**: `/admin/auth/login`
2. **Method**: `POST`
3. **Body (JSON)**:
   ```json
   {
     "username": "admin_new",
     "password": "password123"
   }
   ```
4. **预期结果**:
   ```json
   {
     "code": 200,
     "msg": "登录成功",
     "data": {
       "token": "eyJhb...",
       "adminId": "admin_..."
     }
   }
   ```

---

### 2. 活动管理 (Activity)

**Controller**: `/activity`

#### (1) 获取活动列表

1. **URL**: `/activity/list`
2. **Method**: `POST`
3. **Body (JSON)**:
   ```json
   {
     "keyword": "瓷器", // 关键词 (可选)
     "page": 1, // 页码 (默认1)
     "limit": 10 // 每页条数 (默认10)
   }
   ```
4. **预期结果**:
   ```json
   {
     "code": 200,
     "msg": "获取成功",
     "data": {
       "records": [
         {
           "id": "...", // 数据库主键
           "activityId": "act_...", // 业务ID
           "activityTitle": "中国古代瓷器鉴赏",
           "activityStatus": 1, // 1:启用, 0:禁用
           "activityObj": "{\"startDate\":\"2025-05-20\",\"endDate\":\"2025-05-22\"...}", // JSON字符串
           "activityPic": "[\"http://...\"]" // 图片列表JSON字符串
         }
       ],
       "total": 10,
       "size": 10,
       "current": 1
     }
   }
   ```

#### (2) 添加活动

1. **URL**: `/activity/add`
2. **Method**: `POST`
3. **Body (JSON)**:

   ```json
   {
     "activityTitle": "中国古代瓷器鉴赏",
     "adminId": "admin_test_001",
     "startDate": "2025-05-20",
     "endDate": "2025-05-22",
     "status": 1,
     "content": [
       { "type": "text", "val": "展览介绍..." },
       { "type": "img", "val": "https://example.com/img1.jpg" }
     ]
   }
   ```

   > 注意：此接口会自动生成对应日期的排期数据。
   >
4. **预期结果**:

   ```json
   { "code": 200, "msg": "添加成功", "data": null }
   ```

#### (3) 获取详情

1. **URL**: `/activity/detail`
2. **Method**: `GET`
3. **Params**: `id=<_id>` (使用数据库主键 `_id`，如 `176...`)
4. **预期结果**:
   ```json
   {
     "code": 200,
     "msg": "获取成功",
     "data": {
       "_id": "...",
       "activityTitle": "...",
       "activityObj": "..." // 原始 JSON 字符串
     }
   }
   ```

#### (4) 编辑活动

1. **URL**: `/activity/edit`
2. **Method**: `POST`
3. **Body (JSON)**:
   ```json
   {
     "id": "<_id>",
     "activityTitle": "中国古代瓷器鉴赏（修改版）",
     "adminId": "admin_test_001",
     "status": 1,
     "startDate": "2025-06-01",
     "endDate": "2025-06-05",
     "content": [
       { "type": "text", "val": "新的介绍..." },
       { "type": "img", "val": "https://example.com/new.jpg" }
     ]
   }
   ```
4. **预期结果**:
   ```json
   { "code": 200, "msg": "修改成功", "data": null }
   ```

#### (5) 修改状态

1. **URL**: `/activity/status`
2. **Method**: `POST`
3. **Body (JSON)**:
   ```json
   {
     "id": "<_id>", // 或 activityId
     "status": 1 // 1:启用, 0:禁用
   }
   ```
4. **预期结果**:
   ```json
   { "code": 200, "msg": "操作成功" }
   ```

#### (6) 删除活动

1. **URL**: `/activity/del`
2. **Method**: `POST`
3. **Body (JSON)**:
   ```json
   {
     "id": "<_id>" // 数据库主键
   }
   ```
4. **预期结果**:
   ```json
   { "code": 200, "msg": "删除成功", "data": null }
   ```

---

### 3. 场馆管理 (Museum)

**Controller**: `/museum`

#### (1) 获取场馆列表

1. **URL**: `/museum/list`
2. **Method**: `POST`
3. **Body (JSON)**:
   ```json
   {
     "keyword": "",
     "page": 1,
     "limit": 10
   }
   ```
4. **预期结果**: 返回 `Result<Page<Museum>>`

#### (2) 添加场馆及排期

1. **URL**: `/museum/add`
2. **Method**: `POST`
3. **Body (JSON)**:

   ```json
   {
     "museumTitle": "上海博物馆",
     "adminId": "admin_test_001",
     "museumDesc": "大型中国古代艺术博物馆",
     "museumCover": "https://example.com/cover.jpg",
     "museumImgs": ["https://example.com/1.jpg"],
     "museumContent": "<p>HTML内容</p>",
     "museumAddress": "上海市黄浦区...",
     "museumStatus": 1,
     "museumMaxJoinCnt": 5000,
     "museumBookSet": 7,

     // 排期配置
     "startDate": "2024-05-01",
     "endDate": "2024-05-07",
     "times": [
       { "start": "09:00", "end": "12:00", "limit": 1000 },
       { "start": "13:00", "end": "17:00", "limit": 1000 }
     ]
   }
   ```
4. **预期结果**:

   ```json
   { "code": 200, "msg": "添加成功", "data": null }
   ```

#### (3) 编辑场馆

1. **URL**: `/museum/edit`
2. **Method**: `POST`
3. **Body (JSON)**:

   ```json
   {
     "id": "<_id>",
     "museumTitle": "上海博物馆（修改版）",
     "adminId": "admin_test_001",
     "museumDesc": "...",
     "museumStatus": 1,
     // 若修改排期参数，将触发重置
     "startDate": "2024-06-01",
     "endDate": "2024-06-07",
     "times": [{ "start": "09:00", "end": "12:00", "limit": 2000 }]
     // ... 其他字段
   }
   ```

   > **警告**:
   >
   > 1. 修改 `museumStatus` 会同步更新排期状态。
   > 2. 修改 `startDate`、`endDate` 或 `times` 会导致 **旧排期被删除并重新生成**。
   >

   **预期结果**:

   ```json
   { "code": 200, "msg": "修改成功", "data": null }
   ```

#### (4) 修改状态 (上/下架)

1. **URL**: `/museum/status`
2. **Method**: `POST`
3. **Body (JSON)**:

   ```json
   {
     "id": "<_id>", // 数据库主键
     "status": 1 // 1:启用 (生成排期), 0:禁用
   }
   ```
4. **预期结果**:

   ```json
   { "code": 200, "msg": "操作成功", "data": null }
   ```

   > 警告：启用操作会根据系统逻辑生成相关的排期数据。
   >

#### (4) 获取所有场馆 (下拉选)

1. **URL**: `/museum/all`
2. **Method**: `GET`
3. **预期结果**:
   ```json
   {
     "code": 200,
     "msg": "获取成功",
     "data": [
       { "_id": "...", "museumTitle": "上海博物馆" },
       { "_id": "...", "museumTitle": "自然博物馆" }
     ]
   }
   ```

---

### 4. 公告管理 (News)

**Controller**: `/news`

#### (1) 发布公告

1. **URL**: `/news/add`
2. **Method**: `POST`
3. **Body (JSON)**:

   ```json
   {
     "newsTitle": "2025年春节闭馆通知",
     "newsDesc": "闭馆时间：2月10日-17日",
     "newsStatus": 1
   }
   ```

   > 提示：系统会自动填充 `NEWS_ADD_IP` 为最新添加的场馆名称。
   >
4. **预期结果**:

   ```json
   { "code": 200, "msg": "发布成功", "data": null }
   ```

#### (2) 公告列表

1. **URL**: `/news/list`
2. **Method**: `POST`
3. **Params (Query Params)**:

   > 注意：此接口使用 RequestParam，非 JSON body
   >

   - `keyword`: (可选)
   - `page`: 1
   - `limit`: 10
   - **Example**: `POST /news/list?page=1&limit=10`
4. **预期结果**: 返回 `Result<Page<News>>`

#### (3) 查看详情

1. **URL**: `/news/view`
2. **Method**: `POST`
3. **Params**: `id=<_id>` (form-data 或 query param)
4. **预期结果**:
   ```json
   {
     "code": 200,
     "msg": "获取成功",
     "data": { ... } // 浏览量(viewCnt)会自动+1
   }
   ```

#### (4) 修改公告

1. **URL**: `/news/edit`
2. **Method**: `POST`
3. **Body (JSON)**:

   ```json
   {
     "id": "<_id>",
     "newsTitle": "修改后的标题",
     "newsDesc": "修改后的内容",
     "newsStatus": 1
   }
   ```
4. **预期结果**:

   ```json
   { "code": 200, "msg": "修改成功", "data": null }
   ```

---

### 5. 预约核销管理 (Join)

**Controller**: `/admin/join`

#### (1) 搜索预约记录

1. **URL**: `/admin/join/list`
2. **Method**: `POST`
3. **Body (JSON)**:
   ```json
   {
     "keyword": "张三",
     "page": 1,
     "limit": 10
   }
   ```
4. **预期结果**:
   ```json
   {
     "code": 200,
     "msg": "获取成功",
     "data": {
       "records": [ ... ],
       "total": ...
     }
   }
   ```

#### (2) 预约核销

1. **URL**: `/admin/join/checkin`
2. **Method**: `POST`
3. **Body (JSON)**:

   ```json
   {
     "id": "<目标预约ID>" // join_xxx 或 _id
   }
   ```
4. **预期结果**:

   ```json
   { "code": 200, "msg": "核销成功", "data": null }
   ```

   > 验证点：检查数据库中该条 `join` 记录，`JOIN_IS_CHECKIN` 应变为 1。再次调用核销接口，应报错 “该记录已核销...”。
   >

---

### 6. 黑名单管理 (Blacklist)

**Controller**: `/admin/blacklist`

#### (1) 黑名单列表

1. **URL**: `/admin/blacklist/list`
2. **Method**: `POST`
3. **Body (JSON)**:
   ```json
   {
     "keyword": "张三", // 姓名/手机号/身份证 (可选)
     "page": 1,
     "limit": 10
   }
   ```
4. **预期结果**:
   ```json
   {
     "code": 200,
     "msg": "获取成功",
     "data": {
       "records": [
         {
           "_id": "677123...", // 数据库系统ID
           "identityId": "bd6...", // 游客业务ID (操作主要使用此ID)
           "identityName": "张三",
           "identityCard": "310101199001011234",
           "identityMobile": "13800138000",
           "identityStatus": 0, // 0: 黑名单, 1: 正常
           "blackStartTime": 1716187688000, // 拉黑开始时间 (毫秒)
           "blackEndTime": 1718779688000, // 拉黑结束时间 (毫秒)
           "userBanNum": 6, // 违约次数
           "userCheckType": 0, // 0: 手动拉黑, 1: 自动拉黑
           "identityObj": "{\"blackReason\":\"恶意刷票\"}", // 扩展信息 (JSON字符串，包含原因)
           "userId": "[\"user_001\"]" // 关联的用户ID数组
         }
       ],
       "total": 1,
       "size": 10,
       "current": 1,
       "pages": 1
     }
   }
   ```

#### (2) 手动拉黑

1. **URL**: `/admin/blacklist/add`
2. **Method**: `POST`
3. **Body (JSON)**:
   ```json
   {
     "identityId": "<identityId>", // 游客业务ID (通过列表获取)
     "reason": "恶意刷票", // 拉黑原因 (选填，存入 identityObj)
     "endTime": 1798732800000 // 结束时间戳 (Long 或 String) (必填)
   }
   ```

   > **时间戳计算公式 (毫秒)**:
   > `endTime = 当前时间戳 + (天数 * 24 * 60 * 60 * 1000)`
   > 例如封禁 30 天: `System.currentTimeMillis() + 2592000000L`
   >
4. **预期结果**:
   ```json
   { "code": 200, "msg": "操作成功", "data": null }
   ```

#### (3) 更新结束时间 (延期/解封)

1. **URL**: `/admin/blacklist/updateTime`
2. **Method**: `POST`
3. **Body (JSON)**:
   ```json
   {
     "identityId": "<identityId>", // 游客业务ID (必填)
     "endTime": 1898732800000 // 新的结束时间戳 (Long 或 String) (必填)
   }
   ```
4. **预期结果**:
   ```json
   { "code": 200, "msg": "操作成功", "data": null }
   ```

#### (4) 移除黑名单 (立即)

1. **URL**: `/admin/blacklist/remove`
2. **Method**: `POST`
3. **Body (JSON)**:
   ```json
   { "identityId": "<identityId>" }
   ```
4. **预期结果**:
   ```json
   { "code": 200, "msg": "操作成功", "data": null }
   ```

---

### 7. 系统自动任务 (System Scheduled Tasks)

> 以下功能由系统后台定时任务自动触发，无需前端调用。

#### (1) 自动逾期检查 (Auto Overdue Check)

- **触发频率**: 每 5 分钟执行一次 (及系统启动时)。
- **逻辑**:
  - 扫描所有状态为 `预约成功 (JOIN_STATUS=1)` 且 `未核销 (JOIN_IS_CHECKIN=0)` 的记录。
  - 若预约日期 `JOIN_MEET_DAY` 早于今天 (小于当前日期)，自动将其更新为 `逾期/爽约 (JOIN_IS_CHECKIN=3)`。

#### (2) 自动黑名单管理 (Auto Blacklist Management)

- **触发频率**: 每 5 分钟执行一次 (及系统启动时)。
- **逻辑**:
  1. **违约统计**: 统计每位游客 (`IDENTITY_ID`) 最近 7 天内的逾期次数 (`JOIN_IS_CHECKIN=3`)，并更新 `USER_BAN_NUM`。
  2. **触发拉黑**: 若 `USER_BAN_NUM > 5` 且当前状态正常 (`IDENTITY_STATUS=1`)，自动将其：
     - 状态置为 `黑名单 (IDENTITY_STATUS=0)`。
     - 记录 `BLACK_START_TIME` 为当前时间，`BLACK_END_TIME` 为 30 天后。
     - 标记 `USER_CHECK_TYPE=1` (系统自动拉黑)。
     - **公式**: `BLACK_END_TIME = System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000`
  3. **自动解封**: 若处于黑名单状态 (`IDENTITY_STATUS=0`) 且当前时间已超过 `BLACK_END_TIME`，自动将其恢复为 `正常 (IDENTITY_STATUS=1)` 并重置 `USER_BAN_NUM=0`。
