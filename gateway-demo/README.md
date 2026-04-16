# 网关插件热插拔示例

基于 Spring Cloud Gateway 的插件热插拔和动态启停示例，支持在不重启网关服务的情况下，灵活地开启或关闭各种功能，如限流、鉴权等。

## 技术栈

- Spring Boot 3.2.0
- Spring Cloud Gateway
- So-Token 权限框架
- Java 17

## 项目结构

```
com.gateway
├── config/             配置类
│   └── RouterConfig.java  路由配置
├── controller/         控制器
│   ├── PluginController.java  插件管理控制器
│   └── TestController.java    测试控制器
├── filter/             过滤器
│   └── PluginGatewayFilter.java  插件网关过滤器
├── manager/            插件管理器
│   └── PluginManager.java  插件管理器
├── plugin/             插件接口
│   ├── GatewayPlugin.java  网关插件接口
│   └── impl/           插件实现
│       ├── AuthPlugin.java    鉴权插件
│       ├── LogPlugin.java     日志插件
│       └── RateLimitPlugin.java  限流插件
├── GatewayApplication.java  应用主类
└── application.yml     配置文件
```

## 核心功能

1. **插件热插拔**：支持在运行时动态加载和卸载插件
2. **动态启停**：支持在运行时开启或关闭插件
3. **权限管理**：使用 So-Token 框架进行权限管理
4. **限流功能**：支持请求速率限制
5. **日志记录**：记录请求和响应日志

## 插件列表

| 插件名称 | 功能描述 | 默认状态 |
|---------|---------|---------|
| auth    | 鉴权插件，使用 So-Token 验证 token | 关闭 |
| log     | 日志插件，记录请求和响应信息 | 关闭 |
| rateLimit | 限流插件，限制请求速率 | 关闭 |
| timeout | 超时插件，实现请求超时分级和核心/非核心接口差异化 | 关闭 |
| grayscale | 灰度发布插件，实现流量按权重分配到不同版本 | 关闭 |
| trafficRecording | 流量录制插件，记录生产环境的真实流量 | 关闭 |
| trafficReplay | 流量回放插件，回放录制的流量验证新版本 | 关闭 |

## 快速开始

### 环境准备

- Java 17+
- Maven 3.8+
- Redis（用于限流功能）

### 构建和运行

1. **构建项目**
   ```bash
   cd gateway-demo
   mvn clean package
   ```

2. **运行项目**
   ```bash
   java -jar target/gateway-demo-1.0.0.jar
   ```

3. **访问网关**
   网关服务将在 `http://localhost:8080` 运行

## API 接口

### 插件管理接口

- **获取所有插件**
  - URL: `/api/plugins`
  - Method: GET
  - Response: 插件列表及其状态

- **启动插件**
  - URL: `/api/plugins/{pluginName}/start`
  - Method: POST
  - Response: 启动成功消息

- **停止插件**
  - URL: `/api/plugins/{pluginName}/stop`
  - Method: POST
  - Response: 停止成功消息

- **重新加载插件**
  - URL: `/api/plugins/reload`
  - Method: POST
  - Response: 重新加载成功消息

- **获取插件状态**
  - URL: `/api/plugins/{pluginName}`
  - Method: GET
  - Response: 插件状态信息

### 测试接口

- **测试接口1**
  - URL: `/api/test/hello`
  - Method: GET
  - Response: `{"message": "Hello, Gateway!"}`

- **测试接口2**
  - URL: `/api/test/status`
  - Method: GET
  - Response: `{"status": "ok", "message": "Gateway is working properly"}`

## 配置说明

### 插件配置

在 `application.yml` 中配置插件相关参数：

```yaml
plugin:
  # 插件目录
  directory: plugins
  # 插件扫描间隔（秒）
  scan-interval: 5
```

### So-Token 配置

在 `application.yml` 中配置 So-Token 相关参数：

```yaml
sa:
  token:
    # token名称 (同时也是cookie名称)
    token-name: so-token
    # token有效期，单位秒
    timeout: 86400
    # token风格
    style: uuid
    # 是否输出操作日志
    is-log: true
```

### 超时插件配置

超时插件的默认配置如下：

| 接口级别 | 超时时间 | 适用场景 |
|---------|---------|---------|
| core | 30000ms | 核心接口（支付、登录、订单等） |
| important | 10000ms | 重要接口（用户信息、商品详情等） |
| normal | 5000ms | 普通接口（推荐内容、评论等） |
| fast | 1000ms | 快速接口（健康检查、状态查询等） |

可以通过 API 接口动态更新超时配置，无需重启服务。

### 灰度发布插件配置

灰度发布插件的默认配置如下：

| 版本 | 权重 | 说明 |
|------|------|------|
| v1 | 90% | 旧版本 |
| v2 | 10% | 新版本 |

可以通过 API 接口动态更新灰度配置，无需重启服务。

### 流量录制插件配置

流量录制插件的默认配置如下：

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| recordingPath | records | 录制文件存储路径 |
| includePaths | ["/api/**"] | 需要录制的路径 |
| excludePaths | ["/api/health", "/api/metrics"] | 排除录制的路径 |

可以通过 API 接口动态更新录制配置，无需重启服务。

### 流量回放插件配置

流量回放插件的默认配置如下：

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| recordingPath | records | 录制文件存储路径 |
| targetUrl | http://localhost:8081 | 回放目标服务 URL |

可以通过 API 接口动态更新回放配置，无需重启服务。

## 使用示例

### 1. 启动鉴权插件

```bash
curl -X POST http://localhost:8080/api/plugins/auth/start
```

### 2. 测试鉴权功能

```bash
# 未携带 token，应该返回 401
curl http://localhost:8080/api/test/hello

# 携带无效 token，应该返回 401
curl -H "so-token: invalid-token" http://localhost:8080/api/test/hello

# 生成有效的 token（需要先实现登录接口）
# 然后携带有效 token 访问
curl -H "so-token: valid-token" http://localhost:8080/api/test/hello
```

### 3. 启动限流插件

```bash
curl -X POST http://localhost:8080/api/plugins/rateLimit/start
```

### 4. 测试限流功能

```bash
# 快速发送多个请求，应该会触发限流
for i in {1..20}; do curl http://localhost:8080/api/test/hello & done
```

### 5. 启动日志插件

```bash
curl -X POST http://localhost:8080/api/plugins/log/start
```

### 6. 启动超时插件

```bash
curl -X POST http://localhost:8080/api/plugins/timeout/start
```

### 7. 测试超时功能

```bash
# 测试核心接口（支付接口）
curl http://localhost:8080/api/pay/create

# 测试重要接口（用户接口）
curl http://localhost:8080/api/user/info

# 测试普通接口（推荐接口）
curl http://localhost:8080/api/recommend

# 测试快速接口（健康检查）
curl http://localhost:8080/api/health
```

### 8. 更新超时配置

```bash
# 更新超时配置
curl -X POST http://localhost:8080/api/plugins/timeout/config \
  -H "Content-Type: application/json" \
  -d '{
    "core": 45000,
    "important": 15000,
    "normal": 8000,
    "fast": 2000
  }'
```

### 9. 启动灰度发布插件

```bash
curl -X POST http://localhost:8080/api/plugins/grayscale/start
```

### 10. 测试灰度发布功能

```bash
# 发送多个请求，查看日志中的版本分配
for i in {1..10}; do curl http://localhost:8080/api/test/hello & done
```

### 11. 更新灰度配置

```bash
# 更新灰度配置，设置为 v1: 50%, v2: 50%
curl -X POST http://localhost:8080/api/plugins/grayscale/config \
  -H "Content-Type: application/json" \
  -d '{
    "v1": 50,
    "v2": 50
  }'
```

### 12. 启动流量录制插件

```bash
curl -X POST http://localhost:8080/api/plugins/trafficRecording/start
```

### 13. 测试流量录制功能

```bash
# 发送请求，触发流量录制
curl http://localhost:8080/api/test/hello
curl http://localhost:8080/api/test/status
```

### 14. 启动流量回放插件

```bash
curl -X POST http://localhost:8080/api/plugins/trafficReplay/start
```

### 15. 开始流量回放

```bash
# 开始回放录制的流量
curl -X POST http://localhost:8080/api/plugins/trafficReplay/start
```

### 16. 查看日志

```bash
# 查看网关日志，应该能看到请求和响应信息，以及灰度发布、流量录制和回放的相关信息
```

## 扩展开发

### 开发新插件

1. **实现 GatewayPlugin 接口**

```java
public class CustomPlugin implements GatewayPlugin {
    @Override
    public String getName() {
        return "custom";
    }
    
    // 实现其他方法...
}
```

2. **注册插件**

在插件类上添加 `@Component` 注解，使其被 Spring 容器扫描到。

3. **重启网关服务**

新插件会被自动加载到插件管理器中。

## 注意事项

1. **限流功能依赖 Redis**，需要确保 Redis 服务已启动
2. **鉴权功能需要实现登录接口**，生成有效的 token
3. **外部插件加载功能** 目前只是框架，需要根据实际需求实现
4. **生产环境部署** 时，建议关闭插件扫描间隔，避免频繁扫描插件目录

## 许可证

MIT
