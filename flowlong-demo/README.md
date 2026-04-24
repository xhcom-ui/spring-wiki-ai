# flowlong-demo

基于 Spring Boot 3 + FlowLong + Vue 3 的流程工作台示例项目，覆盖登录认证、权限菜单、流程设计、流程部署、请假发起、待办审批、流程监控、版本对比、部署记录回滚和节点审计快照等能力。

这个项目同时支持两套流程设计方式：

- 老流程 BPMN 设计器
- 新流程自定义设计器

新设计器会保存树状流程 Schema，并生成兼容 FlowLong 部署的 BPMN XML；后端通过流程定义版本、部署记录和实例审计日志把设计期、部署期和运行期串起来。

## 技术栈

### 后端

- Java 17
- Spring Boot 3.2.0
- FlowLong 1.2.2
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

## 核心功能

- 用户登录、注册、登出、登录态校验
- 用户管理、角色管理、菜单权限管理
- 动态菜单与路由权限拦截
- 老流程 BPMN 设计器
- 新流程自定义设计器
- 流程定义版本管理与版本对比
- FlowLong 流程部署、部署记录详情、部署回滚
- 表单目录与字段 schema 配置
- 请假流程发起、待办审批、审批意见/退回原因/系统备注记录
- 流程监控中心、BPMN 轨迹高亮、节点审计与表单快照查看

## 项目结构

```text
flowlong-demo
├── frontend                         # Vue 3 前端
│   ├── src/api                      # HTTP 请求与认证封装
│   ├── src/components               # BPMN 设计器、轨迹图、快照组件
│   ├── src/composables              # 通用分页查询逻辑
│   ├── src/layouts                  # 页面布局
│   ├── src/utils                    # 业务表单/自定义设计器工具
│   └── src/views                    # 登录、工作台、管理页、监控页
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

- 服务端口：`8087`
- 数据库：`jdbc:postgresql://localhost:5432/flowlong_db`
- 用户名：`postgres`
- 密码：`123456`
- MyBatis XML：`classpath:/mapper/*.xml`

FlowLong 相关配置：

- 任务提醒 Cron：`0 */5 * * * ?`
- 事件任务开关：`flowlong.eventing.task=true`

前端默认配置在 `frontend/vite.config.js`：

- 前端端口：`3005`
- `/api` 代理到：`http://localhost:8087`

Spring Boot 首次启动时会执行 `schema.sql` 初始化业务表；FlowLong 运行依赖的数据和流程能力由其 Starter 提供。

## 快速启动

### 1. 准备数据库

先创建数据库：

```sql
CREATE DATABASE flowlong_db;
```

如果本地 PostgreSQL 账号密码不是 `postgres/123456`，请先修改 `application.yml`。

### 2. 启动后端

在项目根目录执行：

```bash
mvn spring-boot:run
```

后端默认地址：

```text
http://localhost:8087
```

### 3. 启动前端

进入前端目录：

```bash
cd frontend
npm install
npm run dev
```

前端默认地址：

```text
http://localhost:3005
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
- `/form-catalogs`：表单目录
- `/versions`：版本历史对比
- `/monitoring`：流程监控中心

这些页面都受登录态和权限标识控制，前端会基于当前用户菜单和权限进行路由拦截。

## 设计器与版本模型

流程设计页当前支持两套模式：

- `BPMN`：老流程 BPMN 设计器
- `CUSTOM`：新流程自定义设计器

数据库表 `process_definition` 中关键字段：

- `designer_type`：设计器类型
- `design_schema_json`：新流程设计器的 Schema 数据
- `bpmn_xml`：FlowLong 可部署的 BPMN XML
- `version`：流程版本号
- `status`：版本状态

这意味着：

- 同一流程可以沉淀多版本定义
- 可以在版本对比页对比不同版本内容
- 新旧设计器可以共存和切换
- 新设计器版本可以转成 BPMN XML 后直接部署到 FlowLong

## 部署记录与回滚

`flowlong_deployment_record` 表用于记录每次部署的来源版本和部署结果，当前已支持：

- 按流程查询部署历史
- 查看某次部署详情
- 一键回滚到某次部署来源版本
- 基于当前激活版本再次部署

对应接口包括：

- `POST /api/flowlong/deploy`
- `POST /api/flowlong/deploy/definition/{id}`
- `POST /api/flowlong/deploy/active/{processKey}`
- `GET /api/flowlong/deployments/{processKey}`
- `GET /api/flowlong/deployment-records/{deploymentId}`
- `POST /api/flowlong/deployment-records/{deploymentId}/rollback`

## 表单目录与审批表单

`sys_form_catalog` 表维护业务表单目录，关键字段包括：

- `form_key`
- `title`
- `description`
- `schema_json`
- `status`

这套 schema 会被以下页面共同消费：

- 流程设计页中的节点业务配置
- 请假发起页
- 待办审批页
- 监控中心里的只读表单快照

当前审批日志已区分：

- 审批意见
- 退回原因
- 系统备注

## 审计与监控

`process_instance_audit_log` 用于持久化实例级审计信息，包含：

- 节点标签
- 页面标签
- 表单标识与表单标题
- 审批意见 / 退回原因 / 系统备注
- 变量快照
- 表单快照
- 操作人和操作时间

`leave_application.passed_node_labels_json` 会记录实例实际走过的审批节点标签，用于让请假记录页和监控中心展示与设计器一致的节点标签。

监控中心当前支持：

- 运行中实例 / 已完成实例查询
- 服务端分页查询接口
- 实例详情
- BPMN 轨迹高亮
- 点击节点联动右侧审计记录
- 变量快照与表单快照查看

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

- `GET /api/form-catalogs/runtime`
- `GET /api/form-catalogs/runtime/{formKey}`
- `GET /api/form-catalogs/query`
- `POST /api/form-catalogs`
- `PUT /api/form-catalogs/{id}`
- `DELETE /api/form-catalogs/{id}`

### 流程定义

- `POST /api/process-definition/save`
- `GET /api/process-definition/{id}`
- `GET /api/process-definition/by-key/{processKey}/version/{version}`
- `GET /api/process-definition/latest/{processKey}`
- `GET /api/process-definition/active/{processKey}`
- `GET /api/process-definition/all`
- `GET /api/process-definition/active`
- `GET /api/process-definition/versions/{processKey}`
- `GET /api/process-definition/compare`
- `PUT /api/process-definition/update/{id}`
- `PUT /api/process-definition/activate/{id}`
- `PUT /api/process-definition/deactivate/{id}`
- `DELETE /api/process-definition/delete/{id}`

### FlowLong 运行时

- `GET /api/flowlong/process/{processKey}/status`
- `GET /api/flowlong/process/{processKey}/business-config`
- `POST /api/flowlong/leave/start`
- `GET /api/flowlong/tasks/{assignee}`
- `POST /api/flowlong/task/complete`
- `GET /api/flowlong/leaves`
- `GET /api/flowlong/leave/{id}`
- `GET /api/flowlong/leaves/applicant/{applicant}`

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
- `sys_form_catalog`
- `leave_application`
- `process_definition`
- `flowlong_deployment_record`
- `process_instance_audit_log`

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
- 如果扩展流程设计器，建议同步维护前端设计器 Schema、`process_definition` 持久化结构和 FlowLong 部署转换逻辑。
- 如果扩展审批业务，建议同步维护表单目录 schema、任务办理页和 `process_instance_audit_log` 的快照字段。
- 当前设计器依赖已收敛到 `frontend/src`，不再依赖额外的同级前端参考项目目录。
