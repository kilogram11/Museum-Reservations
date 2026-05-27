# 项目架构概览

项目类型：Maven + Spring Boot（主入口：`com.museum.MuseumBookingApplication`）

热敏位置：
- 配置与入口
  - [pom.xml](pom.xml)：Maven 构建文件。
  - [src/main/java/com/museum/MuseumBookingApplication.java](src/main/java/com/museum/MuseumBookingApplication.java)：Spring Boot 启动类。
  - [src/main/resources/application.yml](src/main/resources/application.yml) / [src/main/resources/application.properties](src/main/resources/application.properties)：环境配置。

代码组织（按包）：
- `annotation`：自定义注解（如 `RequireLogin`, `RequirePermission`）。
- `common`：公共工具与异常处理：
  - `common.exception`：自定义异常与全局异常处理器。
  - `common.result`：统一响应模型（`Result`）。
  - `common.utils`：工具类（日期、ID、JWT、IP 等）。
- `config`：Spring 与第三方配置（跨域、MyBatis-Plus、Swagger、WebMvc 等）。
- `controller`：接口层，分为 `admin` 与 `app` 子包，处理 HTTP 请求与入参校验。
- `entity`：数据库映射实体（`Activity`, `Admin`, `User`, `Museum`, `Time` 等）。
- `mapper`：MyBatis 的 Mapper 接口（与 `src/main/resources/mapper/*.xml` 对应）。
- `service`：业务逻辑层接口与实现（`impl` 子包放实现类）。
- `security`：认证/拦截器（`AdminAuthInterceptor`, `AppAuthInterceptor`）。

资源与映射：
- [src/main/resources/mapper](src/main/resources/mapper/)：MyBatis XML 映射文件，对应各 `Mapper`。
- [src/main/resources/static](src/main/resources/static/)、`templates`：静态资源与模板（如存在）。

测试：
- `test/java/com/museum/booking/MuseumBookingApplicationTests.java`：基础单元/集成测试入口。

构建产物：
- `target/`：Maven 构建输出。

关键流程（简述）：
1. 启动类 `MuseumBookingApplication` 启动 Spring 容器并加载配置。
2. 请求进入 `controller` 层，经过 `security` 中的拦截器鉴权/鉴权注解校验。
3. `controller` 调用 `service` 执行业务，`service` 使用 `mapper` 与数据库交互（SQL 在 `mapper/*.xml`）。
4. 公共异常由 `common.exception.GlobalExceptionHandler` 统一处理，返回 `common.result.Result` 格式。

建议的阅读顺序（新人上手）：
- 阅读启动类： [src/main/java/com/museum/MuseumBookingApplication.java](src/main/java/com/museum/MuseumBookingApplication.java)
- 阅读配置： [src/main/resources/application.yml](src/main/resources/application.yml) 与 `config` 包
- 阅读 `controller` -> `service` -> `mapper` 的一个完整请求流

维护提示：
- Mapper XML 与 Mapper 接口需保持命名一致并在 `mapper` 目录同步更新。
- 公共工具（JWT/ID 生成）集中在 `common.utils`，更改影响面较大请谨慎。

---
文档由助手自动生成。如需更详细的类依赖图、ER 图或调用链，我可以继续生成并加入到此文件中。