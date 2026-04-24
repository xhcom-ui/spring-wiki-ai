# sse-demo

基于 Spring Boot 3、SSE 和 Redis 的服务端推送示例工程，适合做订单状态变更、审批通知、监控告警、系统公告这类“后端异步产生事件，前端实时接收消息”的场景。

当前版本已经做了这些收敛：

- 修正为可直接启动的 Spring Boot 应用
- 去掉 RabbitMQ 中转，改成 Redis Pub/Sub
- 统一 Redis topic 分发，消息体自带 `userId`
- SSE 连接管理支持同一用户多连接
- API 发送链路支持可配置幂等校验
- 前端示例和后端事件名保持一致

## 技术栈

- Java 17
- Spring Boot 3.0.7
- Spring Web
- Spring Data Redis
- SSE

## 适用场景

- 订单服务向用户推送订单状态更新
- 审批流向待办人推送审批通知
- 监控平台推送实时告警
- 运营平台推送系统公告

## 项目结构

```text
sse-demo
├── frontend
│   ├── src              # Vue 3 消息接收控制台
│   ├── package.json
│   └── vite.config.js
├── src/main/java/org/wiki
│   ├── config          # Redis Pub/Sub 配置
│   ├── consumer        # Redis 消费并转发 SSE
│   ├── controller      # SSE 连接接口、开放发送接口
│   ├── entity          # 消息与统一响应模型
│   ├── manager         # SSE 连接管理
│   ├── producer        # MQ 生产者
│   ├── tools           # JSON 工具
│   └── SseDemoApplication.java
├── src/main/resources
│   ├── application.yml
│   ├── application-dev.yml
│   └── vue             # 前端接入示例
└── pom.xml
```

## 运行前提

本 demo 依赖以下基础设施：

- Redis

默认开发配置：

- 服务端口：`8089`
- Redis：`localhost:6379`

## 启动方式

### 1. 启动 Redis

确保本地服务可访问：

- Redis：默认无密码

### 2. 启动后端

```bash
cd sse-demo
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
mvn spring-boot:run
```

也可以先打包再启动：

```bash
cd sse-demo
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
mvn clean package
java -jar target/sse-demo-1.0-SNAPSHOT.jar
```

启动后访问：

```text
http://localhost:8089
```

### 3. 启动 Vue 前端

```bash
cd frontend
npm install
npm run dev
```

前端默认地址：

```text
http://localhost:3012
```

`vite.config.js` 已内置代理：

- `/api` -> `http://localhost:8089`
- `/sse` -> `http://localhost:8089`

## 核心接口

### 1. 建立 SSE 连接

```http
GET /sse/connect?userId=u1001
Accept: text/event-stream
```

连接成功后会先收到一个 `connected` 事件。

### 2. 发送单条消息

```http
POST /api/sse/send
Content-Type: application/json
```

请求体示例：

```json
{
  "userId": "u1001",
  "type": "ORDER_UPDATE",
  "content": "{\"orderId\":\"ORD-10001\",\"status\":\"PAID\"}",
  "messageId": "msg-10001"
}
```

### 3. 批量发送消息

```http
POST /api/sse/send/batch
Content-Type: application/json
```

## 消息模型

`SseMessage` 字段：

- `userId`：目标用户
- `type`：消息类型
- `content`：业务内容，建议 JSON 字符串
- `messageId`：消息唯一标识
- `timestamp`：消息时间戳

当前支持的 `type`：

- `ORDER_UPDATE`
- `APM_ALERT`
- `APPROVAL_NOTIFY`
- `SYSTEM_NOTICE`
- `CUSTOM`

后端会把消息类型映射成 SSE 事件名：

- `ORDER_UPDATE` -> `order_update`
- `APM_ALERT` -> `apm_alert`
- `APPROVAL_NOTIFY` -> `approval_notify`
- `SYSTEM_NOTICE` -> `system_notice`
- `CUSTOM` -> `custom`

## 当前消息链路

1. 业务服务调用 `/api/sse/send`
2. 服务端校验参数，必要时做 Redis 幂等检查
3. 消息发布到 Redis topic `sse:push`
4. `SseMessageConsumer` 订阅并消费消息
5. `SseEmitterManager` 按 `userId` 把消息推到当前在线连接
6. 前端通过 `EventSource` 实时接收

## 配置说明

### `application.yml`

公共配置：

- `spring.application.name=sse-demo`
- `spring.profiles.active=dev`
- `server.port=8089`
- `app.sse.timeout=30000`
- `app.idempotent.enabled=true`

### `application-dev.yml`

开发环境默认连接：

- Redis `localhost:6379`
- 幂等默认关闭，便于本地重复调试

## 前端接入

当前项目已经补了一个独立的 Vue 3 + Vite 前端：

- `frontend/src/App.vue`
- `frontend/src/style.css`

功能包括：

- 输入用户 ID 建立/断开 SSE 连接
- 实时显示事件类型、消息预览、原始 JSON
- 调用 `/api/sse/send` 发送测试消息
- 通过 Vite 代理直接连接后端 `/sse/connect`

仓库里仍保留两个轻量示例文件，适合参考最小接入方式：

- `src/main/resources/vue/SseClient.vue`
- `src/main/resources/vue/sse.js`

其中 `SseClient.vue` 已按当前后端事件名调整，可直接参考：

```js
const eventSource = new EventSource('/sse/connect?userId=u1001')
eventSource.addEventListener('order_update', (event) => {
  const message = JSON.parse(event.data)
  console.log(message)
})
```

## 这次优化了什么

- 新增 `SseDemoApplication`，替换无效的 `Main.java`
- 新增 `application.yml`，补齐主配置
- 改成 Redis Pub/Sub 中转，移除 RabbitMQ 依赖
- `SseEmitterManager` 改成支持同用户多连接
- SSE 初始连接事件会返回 `connectionId`
- `SseApiController` 改成参数归一化 + 可配置幂等
- `JsonUtils` 从 Gson 改为 Spring 默认 Jackson
- 清理重复依赖和错误的依赖声明
- 前端示例事件名改成与后端一致

## 当前限制

- 这是单体 demo，SSE 连接只保存在当前应用内存里
- 多实例部署时，Redis Pub/Sub 可以广播消息，但 SSE 连接仍只保存在各实例内存里
- `broadcastSystemNotice` 里的在线用户列表仍是占位实现
- 没有补鉴权，生产环境应在 `/sse/connect` 和 `/api/sse/send` 前加认证与权限控制
