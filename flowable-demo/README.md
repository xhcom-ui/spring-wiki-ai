# flowable-demo

基于 Spring Boot 3 + Flowable 7 + Vue 3 的流程工作台示例项目，覆盖登录认证、权限菜单、流程设计、表单目录、请假发起、待办审批、流程监控和版本历史对比等能力。

当前项目同时支持两套流程设计方式：

- 传统 BPMN 设计器
- SeaFlow 风格自定义编排设计器

流程定义会持久化保存到数据库，前端可以在设计页切换新旧设计模式，后端通过 `designer_type` 和 `design_schema_json` 兼容两种设计数据。

## 技术栈

### 后端

- Java 17
- Spring Boot 3.2.0
- Flowable 7.1.0
- MyBatis + XML Mapper
- PostgreSQL
- Sa-Token

### 前端

- Vue 3
- Vite 6
- Vue Router 4
- Element Plus
- Axios
- bpmn-js
- bpmn-js-properties-panel

## 核心功能

- 用户登录、注册、登出、登录态检查
- 用户管理、角色管理、菜单权限管理
- 动态菜单和前端路由权限控制
- BPMN 流程设计器
- SeaFlow 自定义编排设计器
- 表单目录管理与字段 schema 配置
- 请假流程发起与待办审批
- 流程版本保存、激活、停用、删除、对比
- 流程监控中心、实例详情、历史轨迹查看

## 项目结构

```text
flowable-demo
├── frontend                         # Vue 3 前端
│   ├── src/api                      # HTTP 请求与认证封装
│   ├── src/components               # BPMN 设计器、轨迹图、SeaFlow 组件
│   ├── src/composables              # 通用分页查询逻辑
│   ├── src/layouts                  # 页面布局
│   ├── src/utils                    # schema 表单、SeaFlow 设计工具
│   └── src/views                    # 登录、首页、管理页、监控页等
├── src/main/java                    # Spring Boot 后端代码
├── src/main/resources
│   ├── application.yml              # 后端配置
│   ├── mapper                       # MyBatis XML
│   └── schema.sql                   # 业务表建表脚本
└── pom.xml
```

## 环境要求

- JDK 17+
- Maven 3.9+
- Node.js 18+
- PostgreSQL 14+

## 默认配置

后端默认配置在 `src/main/resources/application.yml`：

- 服务端口：`8088`
- 数据库：`jdbc:postgresql://localhost:5432/flowable_db`
- 用户名：`postgres`
- 密码：`123456`
- MyBatis XML：`classpath:/mapper/*.xml`

前端默认配置在 `frontend/vite.config.js`：

- 前端端口：`3006`
- `/api` 代理到：`http://localhost:8088`

Spring Boot 首次启动时会执行 `schema.sql` 初始化业务表；Flowable 引擎表由 Flowable 自动维护并根据配置自动更新。

## 快速启动

### 1. 准备数据库

先创建数据库：

```sql
CREATE DATABASE flowable_db;
```

如果本地 PostgreSQL 账号密码不是 `postgres/123456`，请先修改 `application.yml`。

### 2. 启动后端

在项目根目录执行：

```bash
mvn spring-boot:run
```

后端默认地址：

```text
http://localhost:8088
```

### 3. 启动前端

进入前端目录并启动：

```bash
cd frontend
npm install
npm run dev
```

前端默认地址：

```text
http://localhost:3006
```

## 页面路由

前端核心页面入口如下：

- `/login`：登录页
- `/dashboard`：工作台首页
- `/designer`：流程设计页
- `/leave`：请假发起
- `/tasks`：待办任务
- `/users`：用户管理
- `/roles`：角色管理
- `/menu-permissions`：菜单权限
- `/form-catalog`：表单目录
- `/versions`：版本历史对比
- `/monitoring`：流程监控中心

这些页面都带有权限标识，前端会基于当前用户登录态和菜单权限进行路由拦截。

## 设计器模式

流程设计页当前支持两套模式：

- `BPMN`：基于 `bpmn-js` 的标准流程设计器
- `CUSTOM`：SeaFlow 风格的自定义编排设计器

数据库表 `process_definition` 中与此相关的关键字段：

- `designer_type`：当前版本使用的设计器类型
- `design_schema_json`：自定义编排的结构化设计数据
- `bpmn_xml`：BPMN XML 内容

这意味着：

- 同一流程可以做版本化存档
- 版本对比页可以对比 BPMN 模式和自定义编排模式
- 后端运行时可以基于流程定义和表单目录生成任务办理配置

## 表单目录

`form_catalog` 表用于管理流程节点关联的业务表单目录，当前支持：

- `form_key`
- `form_name`
- `page_label`
- `component_key`
- `field_schema_json`
- `scope`
- `description`
- `sort`
- `status`

这个目录会被流程设计页、请假发起页、待办审批页和监控中心共同使用，用来驱动业务表单组件和字段布局。

## 后端接口分组

### 认证

- `POST /api/auth/login`
- `POST /api/auth/register`
- `POST /api/auth/logout`
- `GET /api/auth/current`
- `GET /api/auth/check`

### 用户/角色/菜单

- `GET/POST/PUT/DELETE /api/users`
- `GET /api/users/query`
- `GET /api/users/lookup`
- `GET/POST/PUT/DELETE /api/roles`
- `GET /api/roles/query`
- `GET /api/roles/options`
- `PUT /api/roles/{id}/menus`
- `GET/POST/PUT/DELETE /api/menus`
- `GET /api/menus/query`
- `GET /api/menu/all`

### 表单目录

- `GET /api/form-catalog/active`
- `GET /api/form-catalog`
- `GET /api/form-catalog/query`
- `GET /api/form-catalog/{id}/impact`
- `POST /api/form-catalog`
- `PUT /api/form-catalog/{id}`
- `DELETE /api/form-catalog/{id}`

### 流程定义版本管理

- `POST /api/process-definition/save`
- `GET /api/process-definition/{id}`
- `GET /api/process-definition/by-key/{processKey}/version/{version}`
- `GET /api/process-definition/latest/{processKey}`
- `GET /api/process-definition/all`
- `GET /api/process-definition/active`
- `GET /api/process-definition/versions/{processKey}`
- `GET /api/process-definition/compare`
- `PUT /api/process-definition/update/{id}`
- `PUT /api/process-definition/activate/{id}`
- `PUT /api/process-definition/deactivate/{id}`
- `DELETE /api/process-definition/delete/{id}`

### Flowable 运行时

- `POST /api/flowable/deploy`
- `POST /api/flowable/deploy/file`
- `POST /api/flowable/leave/start`
- `GET /api/flowable/tasks/{assignee}`
- `POST /api/flowable/task/complete`
- `GET /api/flowable/runtime-config/{processKey}`
- `GET /api/flowable/leaves`
- `GET /api/flowable/leave/{id}`
- `GET /api/flowable/leaves/applicant/{applicant}`

### 流程监控

- `GET /api/monitoring/processes/running`
- `GET /api/monitoring/processes/running/query`
- `GET /api/monitoring/processes/completed`
- `GET /api/monitoring/processes/completed/query`
- `GET /api/monitoring/processes/{processInstanceId}/history`
- `GET /api/monitoring/processes/{processInstanceId}/detail`
- `GET /api/monitoring/tasks`
- `GET /api/monitoring/tasks/query`
- `GET /api/monitoring/tasks/assignee/{assignee}`
- `GET /api/monitoring/statistics`

## 数据表概览

`schema.sql` 当前维护的主要业务表包括：

- `sys_user`
- `sys_role`
- `sys_menu`
- `sys_user_role`
- `sys_role_menu`
- `leave_application`
- `process_definition`
- `form_catalog`

其中 Flowable 自带的引擎表不在 `schema.sql` 中维护，而是由 Flowable 自动创建和升级。

## 常用命令

```bash
# 后端启动
mvn spring-boot:run

# 后端打包
mvn clean package

# 后端测试
mvn test

# 前端安装依赖
cd frontend && npm install

# 前端开发模式
cd frontend && npm run dev

# 前端构建
cd frontend && npm run build
```

## 开发建议

- 本地联调时先启动 PostgreSQL，再启动后端，最后启动前端。
- 如果修改了 Mapper 接口，记得同步检查 `src/main/resources/mapper` 下对应 XML。
- 如果扩展流程设计能力，建议同步维护前端设计器、`process_definition` 持久化结构和运行时配置生成逻辑。
- SeaFlow 风格设计器的现有实现位于 `frontend/src/components/seaflow` 和 `frontend/src/utils/seaflowDesigner.js`。
- 如果要继续完善文档，下一步适合补默认账号初始化、示例流程演示和接口调用示例。
