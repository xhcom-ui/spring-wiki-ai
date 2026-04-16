# Sa-Token Demo 项目

## 项目简介

本项目基于 Spring Boot、Sa-Token 实现了 WebSocket 身份认证和 API Key 认证与限流功能，包含以下核心功能：

1. **WebSocket 身份认证**：使用 Sa-Token 实现了 WebSocket 握手时的 Token 认证
2. **API Key 认证**：使用 Sa-Token 官方 API Key 插件实现了 API Key 的生成、验证和管理
3. **限流功能**：实现了 IP 限流和 API Key 调用频率限制
4. **后台管理**：提供了 API Key 的生成、撤销和查询功能
5. **开放 API**：提供了需要 API Key 认证的开放接口

## 技术栈

- Spring Boot 3.3.0
- Sa-Token 1.45.0
- Sa-Token API Key 插件 1.45.0
- Spring WebSocket
- Redis
- Java 17

## 项目结构

```
sa-token-demo/
├── src/main/java/com/example/satoken/demo/
│   ├── SaTokenDemoApplication.java    # 启动类
│   ├── config/                         # 配置类
│   │   ├── SaTokenApikeyConfig.java    # API Key 插件配置
│   │   ├── WebMvcConfig.java           # 拦截器注册 + CORS
│   │   └── WebSocketConfig.java        # WebSocket 配置
│   ├── controller/                     # 控制器
│   │   ├── AdminController.java        # 后台管理接口
│   │   └── ApiController.java          # 开放 API 接口
│   ├── interceptor/                    # 拦截器
│   │   ├── ApiKeyInterceptor.java      # API Key 认证 + 限流拦截器
│   │   └── WebSocketAuthInterceptor.java # WebSocket 认证拦截器
│   ├── limiter/                        # 限流相关
│   │   ├── RateLimiter.java            # IP 限流
│   │   └── ApiKeyRateLimiter.java      # API Key 限流
│   └── websocket/                      # WebSocket 相关
│       └── MyWebSocketHandler.java     # WebSocket 处理器
├── src/main/resources/
│   └── application.yml                 # 配置文件
├── pom.xml                             # 依赖配置
└── README.md                           # 项目说明
```

## 快速开始

### 1. 启动 Redis

本项目使用 Redis 存储 API Key 和限流计数，需要先启动 Redis 服务。

### 2. 构建项目

```bash
# 构建项目
mvn clean package
```

### 3. 启动项目

```bash
# 启动项目
java -jar target/sa-token-demo-1.0.0.jar
```

### 4. 测试接口

#### 后台管理接口

1. **登录**：
   ```bash
   curl -X POST http://localhost:8080/admin/login \
     -d 'username=admin&password=admin123'
   ```

2. **生成 API Key**：
   ```bash
   curl -X POST http://localhost:8080/admin/api-key/generate \
     -H 'satoken: {token}' \
     -d 'appId=testApp'
   ```

3. **撤销 API Key**：
   ```bash
   curl -X POST http://localhost:8080/admin/api-key/revoke \
     -H 'satoken: {token}' \
     -d 'apiKey={apiKey}'
   ```

4. **获取 API Key 信息**：
   ```bash
   curl -X GET http://localhost:8080/admin/api-key/info \
     -H 'satoken: {token}' \
     -d 'apiKey={apiKey}'
   ```

5. **登出**：
   ```bash
   curl -X POST http://localhost:8080/admin/logout \
     -H 'satoken: {token}'
   ```

#### 开放 API 接口

1. **测试接口**：
   ```bash
   curl -X GET http://localhost:8080/api/v1/test \
     -H 'Authorization: {apiKey}'
   ```

2. **Hello 接口**：
   ```bash
   curl -X GET http://localhost:8080/api/v1/hello \
     -H 'Authorization: {apiKey}'
   ```

#### WebSocket 连接

```javascript
// 连接 WebSocket，携带 token
const socket = new WebSocket('ws://localhost:8080/ws?satoken={token}');

// 连接成功
socket.onopen = function() {
    console.log('WebSocket 连接成功');
};

// 接收消息
socket.onmessage = function(event) {
    console.log('收到消息:', event.data);
};

// 发送消息
socket.send('Hello WebSocket!');

// 连接关闭
socket.onclose = function() {
    console.log('WebSocket 连接关闭');
};
```

## 核心功能

### 1. WebSocket 身份认证

- 使用 `WebSocketAuthInterceptor` 拦截 WebSocket 握手请求
- 支持 3 种 Token 传递方式：URL 参数、Header、Authorization
- 验证 Token 有效性，通过后升级为 WebSocket 连接
- 将用户 ID 存入会话属性，后续消息处理时使用

### 2. API Key 认证与限流

- 使用 `ApiKeyInterceptor` 拦截 API 请求
- 实现 IP 限流，防止恶意刷接口
- 验证 API Key 有效性，无效则拒绝访问
- 实现 API Key 调用频率限制，防止过度调用

### 3. 后台管理

- 提供登录接口，生成管理 Token
- 提供 API Key 的生成、撤销和查询功能
- 所有管理接口都需要验证登录状态

### 4. 开放 API

- 提供需要 API Key 认证的开放接口
- 接口调用时自动验证 API Key 有效性和限流

## 配置说明

在 `application.yml` 文件中配置以下内容：

```yaml
# API Key 配置
api-key:
  prefix: sk-        # API Key 前缀
  length: 32         # API Key 长度
  expire: 31536000   # API Key 过期时间（秒）
```

## 注意事项

1. 确保 Redis 已安装并运行
2. 确保端口 8080 未被占用
3. 首次启动时，Redis 会自动存储 API Key 相关数据
4. 后台管理接口需要先登录获取 Token 才能访问
5. 开放 API 接口需要在请求头中携带 API Key
6. WebSocket 连接需要在 URL 参数中携带 Token
