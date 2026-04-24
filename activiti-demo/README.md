# activiti-demo

基于 Spring Boot 3 + Activiti 7 + Vue 3 的流程工作台示例项目，覆盖登录注册、权限菜单、流程设计、请假发起、待办审批、流程监控、版本对比和业务页目录管理等能力。

项目当前已经完成前后端分离联调，前端通过 `bpmn-js` 提供流程设计画布，后端基于 Activiti 负责流程定义发布、流程实例运行和任务审批流转。

## 技术栈

### 后端

- Java 17
- Spring Boot 3.2.0
- Activiti 7.1.0.M6
- MyBatis + XML Mapper
- PostgreSQL
- Sa-Token

### 前端

- Vue 3
- Vite 6
- Vue Router 4
- Axios
- bpmn-js
- bpmn-js-properties-panel

## 主要功能

- 用户登录、注册、登出、登录态校验
- 用户管理、角色管理、菜单权限管理
- 动态菜单和前端路由权限拦截
- BPMN 流程设计器和流程定义保存
- 请假流程发起、待办领取、审批办理
- 业务办理页目录配置
- 流程编排中心
- 版本历史对比
- 流程监控中心
- 运行中/已完成实例查询、历史轨迹和实例详情查看

## 项目结构

```text
activiti-demo
├── frontend                     # Vue 3 前端
│   ├── src/api                  # HTTP 请求封装
│   ├── src/components           # 流程设计器等组件
│   ├── src/composables          # 通用分页查询逻辑
│   ├── src/layouts              # 页面布局
│   ├── src/runtime              # 业务页目录/运行时注册表
│   └── src/views                # 各业务页面
├── src/main/java                # Spring Boot 后端代码
├── src/main/resources
│   ├── application.yml          # 后端配置
│   ├── mapper                   # MyBatis XML
│   ├── processes                # BPMN 示例流程
│   └── schema.sql               # 启动建表脚本
└── pom.xml
```

## 环境要求

- JDK 17+
- Maven 3.9+
- Node.js 18+
- PostgreSQL 14+

## 默认配置

后端默认配置位于 `src/main/resources/application.yml`：

- 服务端口：`8086`
- 数据库：`jdbc:postgresql://localhost:5432/activiti_db`
- 数据库用户名：`postgres`
- 数据库密码：`123456`
- MyBatis XML 路径：`classpath:/mapper/*.xml`

前端默认配置位于 `frontend/vite.config.js`：

- 前端开发端口：`3004`
- `/api` 请求代理到：`http://localhost:8086`

首次启动时，Spring Boot 会执行 `schema.sql` 自动建表，并从 `src/main/resources/processes` 加载 BPMN 流程定义资源。

## 快速启动

### 1. 准备数据库

先创建数据库：

```sql
CREATE DATABASE activiti_db;
```

如果本地 PostgreSQL 账号密码不是 `postgres/123456`，请先修改 `application.yml`。

### 2. 启动后端

在项目根目录执行：

```bash
mvn spring-boot:run
```

后端启动成功后默认监听：

```text
http://localhost:8086
```

### 3. 启动前端

进入前端目录：

```bash
cd frontend
npm install
npm run dev
```

前端默认访问地址：

```text
http://localhost:3004
```

## 页面入口

前端核心页面路由如下：

- `/login`：登录页
- `/dashboard`：工作台首页
- `/designer`：流程设计页
- `/leave`：请假发起
- `/tasks`：待办任务
- `/task-runtime/:pageKey?`：业务办理页
- `/users`：用户管理
- `/roles`：角色管理
- `/menu-permissions`：菜单权限
- `/orchestration`：流程编排中心
- `/versions`：版本历史对比
- `/monitoring`：流程监控中心
- `/runtime-catalog`：业务页目录

> 页面访问会受到登录态和权限标识控制，前端会基于当前用户菜单和权限做路由拦截。

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

### 流程设计

- `POST /api/design/save`
- `GET /api/design/definition/{processDefinitionId}`
- `GET /api/design/definitions`
- `GET /api/design/runtime-catalog`

### 流程编排

- `POST /api/orchestration/orchestrate`
- `POST /api/orchestration/start-with-callback`
- `POST /api/orchestration/signal`

### 监控中心

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

### Activiti 业务运行时

- `POST /api/activiti/deploy`
- `POST /api/activiti/leave/start`
- `GET /api/activiti/leaves`
- `GET /api/activiti/leaves/applicant/{username}`
- `GET /api/activiti/leave/{id}`
- `GET /api/activiti/tasks/{username}`
- `GET /api/activiti/tasks/inbox`
- `GET /api/activiti/task/{taskId}`
- `POST /api/activiti/task/claim`
- `POST /api/activiti/task/complete`
- `GET /api/activiti/process/status/{processInstanceId}`

### 业务页目录

- `GET /api/runtime-catalog/catalog`
- `GET /api/runtime-catalog/items`
- `GET /api/runtime-catalog/items/query`
- `GET /api/runtime-catalog/items/{id}/references`
- `POST /api/runtime-catalog/items`
- `PUT /api/runtime-catalog/items/{id}`
- `DELETE /api/runtime-catalog/items/{id}`

### 流程定义版本管理

- `POST /api/process-definition/save`
- `GET /api/process-definition/{id}`
- `GET /api/process-definition/by-key/{processKey}/version/{version}`
- `GET /api/process-definition/latest/{processKey}`
- `GET /api/process-definition/versions/{processKey}`
- `GET /api/process-definition/all`
- `GET /api/process-definition/active`
- `PUT /api/process-definition/update/{id}`
- `PUT /api/process-definition/activate/{id}`
- `PUT /api/process-definition/deactivate/{id}`
- `DELETE /api/process-definition/delete/{id}`
- `GET /api/process-definition/compare`

## 流程资源说明

项目内置两个 BPMN 示例：

- `src/main/resources/processes/leave.bpmn`
- `src/main/resources/processes/advanced-leave.bpmn`

流程设计器保存后的定义会写入 `process_definition` 表，运行时业务页目录配置会写入 `task_page_catalog` 表。

## 数据表概览

`schema.sql` 当前主要维护以下业务表：

- `sys_user`
- `sys_role`
- `sys_menu`
- `sys_user_role`
- `sys_role_menu`
- `leave_application`
- `process_definition`
- `task_page_catalog`

## 开发建议

- 如果要联调前端，请优先保证 PostgreSQL 和后端服务先启动。
- 如果调整了 MyBatis 查询，记得同时检查 `src/main/resources/mapper` 下 XML 是否同步。
- 如果要扩展流程节点业务能力，建议同步维护流程设计器元数据、运行时任务页注册表和后端任务办理接口。

## 常用命令

```bash
# 后端打包
mvn clean package

# 后端运行测试
mvn test

# 前端安装依赖
cd frontend && npm install

# 前端开发模式
cd frontend && npm run dev

# 前端构建
cd frontend && npm run build
```

## 当前适合继续扩展的方向

- 完整补齐角色管理接口说明和示例数据初始化
- 为流程设计器补更细的节点业务元数据文档
- 为请假、审批和监控场景补接口时序图
- 增加 Docker Compose 本地一键启动方案
