# syn-data-demo

可视化数据同步平台示例项目，支持把 MySQL / PostgreSQL 的业务数据同步到 Elasticsearch，并提供数据源管理、同步任务编排、watcher 配置、实时监控、数据质量校验和告警管理等能力。

当前项目已经从“动态生成 Java watcher 源码”切换为“前端提交 watcher 配置 + 后端持久化 + 运行时直接消费配置模型”的方案。`SyncTask` 可以直接绑定 `watcherId`，实时监控页也直接基于数据库里的 watcher 配置运行。

当前版本额外补齐了三类基础能力：

- Sa-Token 登录态、角色鉴权、统一 401 / 403 处理
- 用户密码、数据源密码脱敏，只写不回显
- 任务执行结果、告警规则/记录、监控统计改为基于数据库真实持久化数据

## 当前实现概览

- 平台元数据库：MySQL
- 源端业务库：支持 MySQL / PostgreSQL 数据源配置
- 目标端：Elasticsearch
- 后端：Spring Boot + MyBatis-Plus + Sa-Token
- 前端：Vue 3 + Element Plus + ECharts
- watcher 模型：数据库持久化，不再依赖动态生成监听器类
- 权限模型：`admin` / `user`

## 技术栈

### 后端

- Java 17
- Spring Boot 3.2.0
- MyBatis-Plus 3.5.5
- MySQL / PostgreSQL JDBC Driver
- Elasticsearch Java Client 8.11.0
- XXL-Job Core 2.4.0
- Druid
- Sa-Token 1.38.0

### 前端

- Vue 3
- Vite 5
- Vue Router 4
- Element Plus
- Axios
- ECharts

## 核心功能

- 用户登录、注册、获取当前用户、重置密码
- 数据源管理、连接测试、性能测试、数据源类型查询
- 同步任务管理、手动执行、测试执行、回滚、暂停、恢复
- SQL 测试、执行计划预览、字段提取、SQL 模板查询
- watcher 配置管理、启动、停止、状态查询
- SyncTask 与 watcher 绑定
- 实时监控、历史监控、任务详情、告警规则和告警记录
- 数据质量校验、问题列表、修复、忽略、统计、导出
- watcher 配置辅助生成页
- 前端菜单、路由、按钮根据角色自动收紧

## 目录结构

```text
syn-data-demo
├── backend
│   ├── src/main/java/com/syn/data
│   │   ├── controller          # REST 接口
│   │   ├── entity              # MyBatis-Plus 实体
│   │   ├── mapper              # Mapper 接口
│   │   ├── service             # 业务服务
│   │   ├── binlog              # 旧版 MySQL binlog 框架参考源码
│   │   └── wal                 # 旧版 PostgreSQL WAL 框架参考源码
│   ├── src/main/resources
│   │   ├── application.yml
│   │   ├── mapper
│   │   └── schema.sql
│   └── pom.xml
├── frontend
│   ├── src/router
│   ├── src/utils
│   ├── src/views
│   └── package.json
└── README.md
```

## 页面入口

前端当前页面路由：

- `/login`：登录页
- `/home/dashboard`：首页概览，`admin` / `user`
- `/home/datasource`：数据源管理，仅 `admin`
- `/home/mapping`：映射配置，仅 `admin`
- `/home/sync-task`：同步任务，`admin` / `user`
- `/home/task-log`：任务日志，`admin` / `user`
- `/home/data-quality`：数据质量，`admin` / `user`
- `/home/realtime-monitor`：实时监控，仅 `admin`
- `/home/codegen`：watcher 配置辅助页，仅 `admin`

登录后的默认跳转：

- `admin` 默认进入 `/home/datasource`
- `user` 默认进入 `/home/dashboard`

## 配置说明

### 后端默认配置

`backend/src/main/resources/application.yml` 当前默认值：

- 服务端口：`8080`
- 平台数据库：`${SYN_DATA_DB_URL}`
- 平台数据库账号：`${SYN_DATA_DB_USERNAME}`
- 平台数据库密码：`${SYN_DATA_DB_PASSWORD}`
- Elasticsearch：`localhost:9200`
- token 名称：`so-token`

未显式传环境变量时，会回退到本地开发默认值。

### 前端默认配置

`frontend/vite.config.js` 当前默认值：

- 开发端口：`3000`
- `/api` 代理到：`http://localhost:8080`

## 环境要求

- JDK 17+
- Maven 3.9+
- Node.js 18+
- MySQL 8.x
- Elasticsearch 8.x

如果要真正验证源端同步，还需要额外准备：

- MySQL 业务库，或
- PostgreSQL 业务库

## 数据库初始化

这个 demo 当前没有在 `application.yml` 中开启自动 SQL 初始化，`schema.sql` 需要手工导入。

导入方式示例：

```bash
mysql -uroot -p < backend/src/main/resources/schema.sql
```

`schema.sql` 会创建并初始化以下核心表：

- `data_source_config`
- `watcher_config`
- `sync_task_config`
- `sync_task_log`
- `data_quality_issue`
- `alert_config`
- `alert_log`
- `system_config`
- `user`

脚本还会插入测试数据和默认账号，并补齐以下增量列：

- `data_source_config.connection_params`
- `sync_task_config.last_sync_status`

## 默认账号

在成功导入 `schema.sql` 后可使用：

- `admin / admin123`
- `user / user123`

说明：

- 数据库里保存的是哈希后的密码
- 登录接口和用户信息接口不会返回密码字段
- 数据源接口不会返回数据库密码

## 快速启动

### 1. 启动 Elasticsearch

确保本地 `9200` 可访问。

### 2. 初始化 MySQL 元数据库

执行：

```bash
mysql -uroot -p < backend/src/main/resources/schema.sql
```

如果你的 MySQL 账号密码或端口不是默认值，建议通过环境变量覆盖：

```bash
export SYN_DATA_DB_URL='jdbc:mysql://localhost:3306/syn_data?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai'
export SYN_DATA_DB_USERNAME='root'
export SYN_DATA_DB_PASSWORD='123456'
```

### 3. 启动后端

```bash
cd backend
mvn clean package
java -jar target/syn-data-backend-1.0.0.jar
```

开发期也可以直接：

```bash
cd backend
mvn spring-boot:run
```

后端默认地址：

```text
http://localhost:8080
```

### 4. 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端默认地址：

```text
http://localhost:3000
```

## watcher 模型说明

这是当前项目最关键的变化点。

### 新方案

- watcher 配置由前端直接提交
- watcher 配置持久化到 `watcher_config`
- `SyncTask` 通过 `watcherId` 绑定 watcher
- `MySQLBinlogListenerService` / `PostgresWalListenerService` 直接消费数据库配置
- 运行状态、队列大小、同步计数、最后同步时间会回写到 `watcher_config`
- 任务执行后的 `lastSyncTime / lastSyncStatus` 会回写到 `sync_task_config`
- 告警规则持久化到 `alert_config`
- 告警记录持久化到 `alert_log`
- 监控页聚合 `sync_task_log / sync_task_config / watcher_config / alert_log`

### 当前推荐接入链路

1. 在“数据源管理”里创建源端数据源
2. 在“Watcher 配置辅助页”生成 watcher 配置草稿
3. 调用 `/api/watchers/{sourceType}` 保存 watcher
4. 在“同步任务”里为任务绑定 `watcherId`
5. 在“实时监控”页启动 / 停止 watcher

### 兼容说明

- `/api/codegen/pg-listener`
- `/api/codegen/mysql-listener`

这两个接口现在只返回“前端 watcher 配置草稿”，不再生成 Java 监听器源码。

另外还保留了兼容入口：

- `/api/realtime/mysql/*`
- `/api/realtime/postgres/*`

新接入优先使用 `/api/watchers/*`，旧 `/api/realtime/*` 更适合兼容已有前端调用。

## 后端接口分组

## 权限说明

### 仅管理员可访问

- 数据源管理：`/api/datasource/**`
- watcher 配置：`/api/watchers/**`
- 实时兼容接口：`/api/realtime/**`
- watcher 配置辅助：`/api/codegen/**`
- 同步任务的新增、编辑、删除、启动、停止、执行、测试、回滚、暂停、恢复
- 监控里的导出、告警规则维护、手动触发告警、解决告警、测试通知

### 登录后可访问

- `GET /api/auth/user`
- `POST /api/auth/reset-password`
- `GET /api/task`
- `GET /api/task/{id}`
- `GET /api/monitoring/realtime`
- `GET /api/monitoring/history`
- `GET /api/monitoring/task/detail`
- `GET /api/monitoring/alerts/rules`
- `GET /api/monitoring/alerts/records`
- `GET /api/monitoring/alerts/stats`
- `GET /api/data-quality/**`
- `POST /api/data-quality/**`

### 认证

- `POST /api/auth/login`
- `GET /api/auth/user`
- `POST /api/auth/register`
- `POST /api/auth/reset-password`

### 数据源

- `GET /api/datasource`
- `GET /api/datasource/{id}`
- `POST /api/datasource`
- `PUT /api/datasource`
- `DELETE /api/datasource/{id}`
- `POST /api/datasource/{id}/test`
- `POST /api/datasource/{id}/performance`
- `GET /api/datasource/types`

### 同步任务

- `GET /api/task`
- `GET /api/task/{id}`
- `POST /api/task`
- `PUT /api/task`
- `DELETE /api/task/{id}`
- `POST /api/task/{id}/start`
- `POST /api/task/{id}/stop`
- `POST /api/task/{id}/execute`
- `POST /api/task/{id}/test-execute`
- `POST /api/task/{id}/rollback`
- `POST /api/task/{id}/pause`
- `POST /api/task/{id}/resume`
- `POST /api/task/{id}/test-sql`
- `POST /api/task/{id}/execution-plan`
- `POST /api/task/{id}/test-mapping`
- `GET /api/task/sql-templates`
- `POST /api/task/{id}/fields`

### watcher 配置

- `GET /api/watchers`
- `GET /api/watchers/{id}`
- `POST /api/watchers/{sourceType}`
- `PUT /api/watchers/{id}`
- `DELETE /api/watchers/{id}`
- `POST /api/watchers/{id}/start`
- `POST /api/watchers/{id}/stop`
- `GET /api/watchers/{id}/status`

### 实时兼容接口

- `POST /api/realtime/mysql/start/{watcherId}`
- `POST /api/realtime/mysql/stop/{watcherId}`
- `GET /api/realtime/mysql/status/{watcherId}`
- `POST /api/realtime/mysql/register`
- `POST /api/realtime/mysql/unregister`
- `POST /api/realtime/postgres/start/{watcherId}`
- `POST /api/realtime/postgres/stop/{watcherId}`
- `GET /api/realtime/postgres/status/{watcherId}`
- `POST /api/realtime/postgres/register`
- `POST /api/realtime/postgres/unregister`

### 监控与告警

- `GET /api/monitoring/realtime`
- `GET /api/monitoring/history`
- `GET /api/monitoring/task/detail`
- `POST /api/monitoring/export`
- `GET /api/monitoring/alerts/rules`
- `GET /api/monitoring/alerts/rules/{id}`
- `POST /api/monitoring/alerts/rules`
- `PUT /api/monitoring/alerts/rules`
- `DELETE /api/monitoring/alerts/rules/{id}`
- `POST /api/monitoring/alerts/rules/{id}/toggle`
- `GET /api/monitoring/alerts/records`
- `POST /api/monitoring/alerts/trigger`
- `POST /api/monitoring/alerts/resolve/{id}`
- `GET /api/monitoring/alerts/stats`
- `POST /api/monitoring/alerts/test-notification`

### 数据质量

- `POST /api/data-quality/validate/count`
- `POST /api/data-quality/validate/quality`
- `POST /api/data-quality/validate/format`
- `POST /api/data-quality/validate/business`
- `POST /api/data-quality/validate/full`
- `GET /api/data-quality/issues`
- `GET /api/data-quality/issues/{id}`
- `POST /api/data-quality/issues/{id}/fix`
- `POST /api/data-quality/issues/batch-fix`
- `POST /api/data-quality/issues/{id}/ignore`
- `GET /api/data-quality/issues/stats`
- `POST /api/data-quality/export`

### watcher 配置辅助生成

- `POST /api/codegen/pg-listener`
- `POST /api/codegen/mysql-listener`

## 与旧版 watcher 框架的关系

`backend/src/main/java/com/syn/data/binlog` 和 `backend/src/main/java/com/syn/data/wal` 下仍保留了旧版监听框架源码和说明，但当前 demo 的主流程已经不依赖它们。

`backend/pom.xml` 里已经通过 `maven-compiler-plugin` 排除了部分旧源码的编译：

- `com/syn/data/binlog/**`
- `com/syn/data/wal/**`
- `BinlogListenerService.java`
- `MySQLBinlogListener.java`

所以当前应把这些目录视为“历史参考代码”，而不是新增功能的主扩展点。

## 常用命令

```bash
# 后端运行
cd backend && mvn spring-boot:run

# 后端打包
cd backend && mvn clean package

# 前端运行
cd frontend && npm install && npm run dev

# 前端构建
cd frontend && npm run build
```

## 现阶段注意事项

- 旧 README 中的后端端口 `8085` 已过时，当前实际端口是 `8080`。
- 旧 README 中提到的 Monaco Editor 并不在当前前端依赖里，当前前端主要使用 Element Plus 和 ECharts。
- `schema.sql` 需要手工导入，未配置自动初始化。
- 平台元数据库当前固定为 MySQL；MySQL / PostgreSQL 的“多数据源支持”指业务源端数据源。
- XXL-Job 依赖和配置已接入，但这个 demo 当前更偏向手动执行和本地联调，若要真正启用调度，需要额外准备 XXL-Job Admin。
- `MySQLBinlogListenerService` / `PostgresWalListenerService` 当前仍是 demo 级 watcher 运行时模拟器，适合前后端联调和配置演示，不是生产级 binlog / WAL 消费实现。
- `Dashboard.vue`、`TaskLog.vue` 里仍有部分演示数据和占位交互，后续可继续替换为真实后端接口。
- 旧版 `binlog` / `wal` 目录当前仅作为历史参考代码，`pom.xml` 已排除其主流程编译。
