# 博物馆预约系统 API 接口文档

本文档记录了小程序前端与后端交互的接口规范。

## 1. 用户模块 (User)

### 1.1 手机号登录/注册
后端接收手机号和验证码，如果用户不存在则自动注册。
- **URL**: `/app/user/login`
- **Method**: `POST`
- **Request Body**:
  ```json
  {
    "mobile": "13812345678",
    "code": "1234"
  }
  ```
- **Response**:
  ```json
  {
    "code": 200,
    "msg": "登录成功",
    "data": {
      "token": "eyJhbGciOiJIUzI1NiJ9..."
    }
  }
  ```
- **测试方法**:
  使用 Postman 发送 POST 请求。验证码目前固定为 `1234` (模拟)。

### 1.2 获取个人信息
需要携带 Token。
- **URL**: `/app/user/info`
- **Method**: `GET`
- **Header**: `Token: <your_token>`
- **Response**:
  ```json
  {
    "code": 200,
    "msg": "获取成功",
    "data": {
      "_id": "...",
      "userId": "...",
      "userName": "用户8888",
      "userMobile": "13800008888",
      ...
    }
  }
  ```

### 1.3 修改个人信息
- **URL**: `/app/user/update`
- **Method**: `POST`
- **Header**: `Token: <your_token>`
- **Request Body**:
  ```json
  {
    "userName": "新昵称",
    "userPic": 123
  }
  ```
- **Response**:
  ```json
  {
    "code": 200,
    "msg": "修改成功",
    "data": null
  }
  ```

## 2. 常用游客模块 (Identity)

### 2.1 获取常用游客列表
- **URL**: `/app/identity/list`
- **Method**: `GET`
- **Header**: `Token: <your_token>`
- **Response**:
  ```json
  {
    "code": 200,
    "data": [
      {
        "identityId": "...",
        "identityName": "张三",
        "identityCard": "110101199001011234",
        "identityMobile": "13800000000"
      }
    ]
  }
  ```

### 2.2 添加/编辑游客
- **URL**: `/app/identity/save`
- **Method**: `POST`
- **Header**: `Token: <your_token>`
- **Request Body**:
  ```json
  {
    "identityName": "张三",
    "identityCard": "110101199001011234",
    "identityMobile": "13800000000"
  }
  ```
- **Response**:
  ```json
  {
    "code": 200,
    "msg": "保存成功"
  }
  ```

### 2.3 删除游客
- **URL**: `/app/identity/del`
- **Method**: `POST`
- **Header**: `Token: <your_token>`
- **Request Body**:
  ```json
  {
    "identityId": "..."
  }
  ```

## 3. 预约模块 (Booking)

### 3.1 获取可预约日期
- **URL**: `/app/booking/days`
- **Method**: `GET`
- **Response**:
  ```json
  {
    "code": 200,
    "data": [
      { "day": "2025-01-01", "week": "周三", "status": 1 }
    ]
  }
  ```

### 3.2 获取时段
- **URL**: `/app/booking/times?day=2025-01-01`
- **Method**: `GET`
- **Response**:
  ```json
  {
    "code": 200,
    "data": [
      {
        "timeMark": "...",
        "startTime": "09:00",
        "endTime": "11:00",
        "surplus": 50
      }
    ]
  }
  ```

### 3.3 提交预约
- **URL**: `/app/booking/submit`
- **Method**: `POST`
- **Header**: `Token: <your_token>`
- **Request Body**:
  ```json
  {
    "timeMark": "museum_2025-01-01_09:00",
    "identityIds": ["identity_123", "identity_456"]
  }
  ```

### 3.4 我的预约记录
- **URL**: `/app/record/list`
- **Method**: `GET`
- **Header**: `Token: <your_token>`

### 3.5 取消预约
- **URL**: `/app/record/cancel`
- **Method**: `POST`
- **Header**: `Token: <your_token>`
- **Request Body**:
  ```json
  {
    "joinId": "join_..."
  }
  ```

### 3.6 预约详情（二维码）
- **URL**: `/app/record/detail?joinId=...`
- **Method**: `GET`
- **Header**: `Token: <your_token>`
- **Response**: 包含 `joinQr` 字段（Base64图片）。

---

## 4. 公共信息模块 (Public)

### (1) 首页聚合信息

- **URL**: `/app/home/index`
- **Method**: `GET`
- **Response**:
    ```json
    {
        "code": 200,
        "msg": "获取成功",
        "data": {
            "banners": ["img_url_1...", "img_url_2..."],
            "today": {
                "date": "12月26日",
                "status": 1,         // 1: 开放, 0: 闭馆
                "statusText": "今日开放", 
                "hours": "08:30 - 17:00"
            },
            "museumInfo": {
                "title": "故宫博物院",
                "address": "北京市东城区...", 
                "phone": "010-8500...",
                "desc": "...",
                "openTimeStr": "08:30 - 17:00 (周二至周日)"
            },
            "activities": [{...}], // 最新活动
            "notices": [{...}]     // 最新公告
        }
    }
    ```

### (2) 活动列表

- **URL**: `/app/home/activity/list`
- **Method**: `POST`
- **Body**:
    ```json
    { "page": 1, "limit": 10 }
    ```
- **Response**: `Result<Page<Activity>>`

### (3) 活动详情

- **URL**: `/app/home/activity/detail?id=xxx`
- **Method**: `GET`
- **Response**: `Result<Activity>`

### (4) 公告列表

- **URL**: `/app/home/notice/list`
- **Method**: `POST`
- **Body**:
    ```json
    { "page": 1, "limit": 10 }
    ```
- **Response**: `Result<Page<News>>`

### (5) 公告详情

- **URL**: `/app/home/notice/detail?id=xxx`
- **Method**: `GET`
- **Response**: `Result<News>` (浏览量自动+1)

---

## 5. 黑名单管理模块 (Admin - Blacklist)

### (1) 黑名单列表

- **URL**: `/admin/blacklist/list`
- **Method**: `POST`
- **Body**:
  ```json
  {
    "keyword": "张三", // 可选
    "page": 1,
    "limit": 10
  }
  ```
- **Response**: `Result<Page<Identity>>`

### (2) 加入黑名单

- **URL**: `/admin/blacklist/add`
- **Method**: `POST`
- **Body**:
  ```json
  {
    "identityId": "identity_...", // 游客ID
    "reason": "多次爽约"
  }
  ```
- **Response**: `Result<null>`
- **Effect**: 该游客无法再提交新的预约。

### (3) 移除黑名单

- **URL**: `/admin/blacklist/remove`
- **Method**: `POST`
- **Body**:
  ```json
  {
    "identityId": "identity_..."
  }
  ```
- **Response**: `Result<null>`
