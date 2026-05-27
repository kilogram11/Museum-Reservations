# 博物馆预约管理系统

本项目是软件工程实训项目，包含后端服务、管理端前端、微信小程序端、原型设计、测试材料和项目文档。

## 项目结构

```text
big-work
├── ZDYZ/                 # Spring Boot 后端服务
├── admin-front/ZDYZ/     # Vue 管理端前端
├── app-front/ZDYZ/       # 微信小程序端
├── 原型/                 # 用户端原型页面
├── 测试/                 # 测试用例、测试报告和缺陷统计
├── 故宫图片/             # 页面和原型素材
└── *.docx / *.png / *.json # 需求、设计、数据库和流程文档
```

## 技术栈

- 后端：Spring Boot 3、Java 17、MyBatis-Plus、MySQL、Redis、JWT、Maven
- 管理端：Vue 3、Vite、Element Plus、Axios、ECharts
- 小程序端：微信小程序原生开发、云函数目录结构
- 文档与测试：需求规格、数据库设计、业务流程、测试用例、缺陷统计

## 后端启动

```bash
cd ZDYZ
./mvnw spring-boot:run
```

Windows 环境可使用：

```bash
cd ZDYZ
mvnw.cmd spring-boot:run
```

默认后端端口为 `8081`。启动前请准备 MySQL 数据库，并根据本地环境检查 `ZDYZ/src/main/resources/application.yml`。

## 管理端启动

```bash
cd admin-front/ZDYZ
npm install
npm run dev
```

## 微信小程序端

使用微信开发者工具打开 `app-front/ZDYZ` 目录，确认 `project.config.json` 中的小程序配置与本地开发环境一致。

## 注意事项

- `AccessKey.csv`、`project.private.config.json`、`node_modules/`、`target/`、IDE 配置和本地上传文件已通过 `.gitignore` 排除。
- 如需公开部署，请不要提交真实密钥、数据库密码、云服务 AccessKey 或私有环境配置。
- 上传 GitHub 前建议先检查 `git status` 和暂存清单，确认没有敏感文件被加入。
