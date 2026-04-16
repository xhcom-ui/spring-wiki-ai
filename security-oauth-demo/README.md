# Spring Boot 3 + Spring Security 6 + OAuth2 实战项目

## 项目简介

本项目基于 Spring Boot 3、Spring Security 6、Spring Authorization Server 实现了一个完整的 OAuth2/OIDC 认证授权系统，包含以下核心组件：

1. **认证服务器（auth-server）**：实现 OAuth2/OIDC 认证授权中心，负责用户登录、同意授权、颁发 Token
2. **资源服务器（resource-server）**：实现受保护的 API 资源，验证 Token 并保护资源
3. **API 网关（gateway）**：统一处理所有请求，包括路由、鉴权等
4. **客户端应用（client-app）**：实现一个简单的 Web 应用，展示如何使用 OAuth2 进行认证和授权

## 技术栈

- Spring Boot 3.2.0
- Spring Security 6.2.0
- Spring Authorization Server 1.2.0
- Spring Cloud Gateway
- PostgreSQL
- Redis
- Thymeleaf
- Docker Compose

## 项目结构

```
security-oauth-demo/
├── auth-server/              # 认证服务器
│   ├── src/main/java/com/example/security/auth/ # 认证服务器代码
│   ├── src/main/resources/    # 配置文件
│   ├── pom.xml               # 依赖配置
│   └── Dockerfile            # Docker 构建文件
├── resource-server/          # 资源服务器
│   ├── src/main/java/com/example/security/resource/ # 资源服务器代码
│   ├── src/main/resources/    # 配置文件
│   ├── pom.xml               # 依赖配置
│   └── Dockerfile            # Docker 构建文件
├── gateway/                  # API 网关
│   ├── src/main/java/com/example/security/gateway/ # 网关代码
│   ├── src/main/resources/    # 配置文件
│   ├── pom.xml               # 依赖配置
│   └── Dockerfile            # Docker 构建文件
├── client-app/               # 客户端应用
│   ├── src/main/java/com/example/security/client/ # 客户端代码
│   ├── src/main/resources/templates/ # 前端页面
│   ├── src/main/resources/    # 配置文件
│   ├── pom.xml               # 依赖配置
│   └── Dockerfile            # Docker 构建文件
├── docker-compose.yml        # Docker Compose 配置
├── pom.xml                   # 父项目依赖配置
└── README.md                 # 项目说明
```

## 快速开始

### 1. 构建项目

```bash
# 构建所有模块
mvn clean package
```

### 2. 启动服务

使用 Docker Compose 启动所有服务：

```bash
docker-compose up -d
```

### 3. 访问地址

- **认证服务器**：http://localhost:9000
- **资源服务器**：http://localhost:9001
- **API 网关**：http://localhost:8080
- **客户端应用**：http://localhost:8081

### 4. 测试流程

1. **访问客户端应用**：http://localhost:8081
2. **点击 "Login with OAuth2"**，跳转到认证服务器登录页面
3. **输入用户名和密码**：
   - 用户名：user，密码：password
   - 或用户名：admin，密码：admin123
4. **同意授权**，跳转到客户端应用的仪表盘页面
5. **查看用户信息**，点击 "View Profile" 查看详细用户信息
6. **测试资源服务器接口**：
   - 公共接口：http://localhost:8080/api/public/hello
   - 受保护接口：http://localhost:8080/api/protected/hello

## 核心功能

### 1. 认证服务器

- 实现 OAuth2/OIDC 认证授权中心
- 支持 Authorization Code + PKCE 授权流程
- 支持 Client Credentials 授权流程
- 支持 Token 撤销、刷新等功能
- 使用 PostgreSQL 存储客户端信息
- 使用 Redis 存储 Token

### 2. 资源服务器

- 实现受保护的 API 资源
- 验证 JWT Token 的有效性
- 支持基于 scope 的权限控制
- 提供公共接口和受保护接口

### 3. API 网关

- 统一处理所有请求
- 路由请求到对应的服务
- 传递 Token 到下游服务
- 实现请求鉴权

### 4. 客户端应用

- 实现 OAuth2 登录流程
- 展示用户信息
- 提供仪表盘和个人资料页面
- 使用 Thymeleaf 模板引擎

## 安全特性

- 使用 BCrypt 加密密码
- 使用 JWT 作为 Token 格式
- 支持 Token 撤销和过期
- 基于 scope 的权限控制
- 防 CSRF 攻击
- 安全的授权码流程（PKCE）

## 部署说明

### 1. 环境要求

- Docker
- Docker Compose
- Java 17+
- Maven 3.8+

### 2. 配置说明

- **PostgreSQL**：默认数据库为 `auth_db`，用户名为 `postgres`，密码为 `postgres123`
- **Redis**：默认端口为 6379，无密码
- **认证服务器**：默认端口为 9000，issuer 为 `http://localhost:9000`
- **资源服务器**：默认端口为 9001
- **API 网关**：默认端口为 8080
- **客户端应用**：默认端口为 8081

### 3. 扩展说明

- **添加新客户端**：在 `auth-server` 的 `SecurityConfig.java` 中添加新的 `RegisteredClient`
- **添加新 scope**：在 `auth-server` 的 `SecurityConfig.java` 中为客户端添加新的 scope
- **添加新用户**：在 `auth-server` 的 `SecurityConfig.java` 中添加新的 `UserDetails`
- **添加新接口**：在 `resource-server` 的 `ResourceController.java` 中添加新的接口

## 注意事项

1. 本项目为演示项目，生产环境需要根据实际情况进行配置
2. 生产环境应使用 HTTPS 协议
3. 生产环境应使用更安全的密码存储方式
4. 生产环境应配置合适的 Token 过期时间
5. 生产环境应配置合适的限流策略

## 参考文档

- [Spring Authorization Server 官方文档](https://docs.spring.io/spring-authorization-server/docs/current/reference/html/)
- [Spring Security 官方文档](https://docs.spring.io/spring-security/docs/current/reference/html5/)
- [OAuth 2.0 官方文档](https://oauth.net/2/)
- [OpenID Connect 官方文档](https://openid.net/connect/)
