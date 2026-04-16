# Spring Boot + Redis + MySQL 缓存一致性示例

## 项目简介

本项目基于 Spring Boot 3.2.0、Redis 和 MySQL 实现了缓存一致性的双写策略和延迟双删方案，主要包括以下特性：

1. **双写策略**：先更新数据库，再删除缓存
2. **延迟双删**：先删除缓存，再更新数据库，最后延迟一段时间后再次删除缓存
3. **缓存一致性**：确保缓存中的数据与数据库中的数据保持一致
4. **高并发支持**：通过延迟双删解决并发更新时的缓存一致性问题

## 技术栈

- Spring Boot 3.2.0
- Spring Data JPA
- MySQL 8.0
- Redis 7
- Redisson 3.16.8
- Spring Cache
- Lombok

## 项目结构

```
springboot-redis-mysql-demo/
├── db/                       # 数据库初始化脚本
│   └── init.sql              # 初始化数据库和表结构
├── src/main/java/com/example/redis/ # 项目代码
│   ├── config/               # 配置类
│   ├── controller/           # 控制器
│   ├── entity/               # 实体类
│   ├── repository/           # 数据访问层
│   ├── service/              # 业务逻辑层
│   └── Application.java      # 启动类
├── src/main/resources/        # 配置文件
│   └── application.yml       # 应用配置
├── docker-compose.yml        # Docker Compose 配置
├── Dockerfile                # Docker 构建文件
├── pom.xml                   # 依赖配置
└── README.md                 # 项目说明
```

## 核心功能

### 1. 双写策略

- **保存商品**：先更新数据库，再删除缓存
- **删除商品**：先删除数据库记录，再删除缓存

### 2. 延迟双删

- **更新商品**：先删除缓存，再更新数据库，最后延迟 500ms 再次删除缓存
- **解决并发问题**：通过延迟删除缓存，确保并发更新时的缓存一致性

### 3. 缓存操作

- **读取缓存**：使用 `@Cacheable` 注解自动从缓存读取数据
- **删除缓存**：使用 `@CacheEvict` 注解自动删除缓存
- **延迟删除**：使用 Redisson 延迟队列实现延迟删除缓存

## 快速开始

### 1. 构建项目

```bash
# 构建项目
mvn clean package
```

### 2. 启动服务

使用 Docker Compose 启动所有服务：

```bash
docker-compose up -d
```

### 3. 测试接口

#### 添加商品

```bash
curl -X POST http://localhost:8080/products \
  -H "Content-Type: application/json" \
  -d '{
    "name": "iPad Pro",
    "price": 6999.99,
    "stock": 50,
    "description": "苹果平板"
  }'
```

#### 查询商品

```bash
# 根据 ID 查询商品
curl http://localhost:8080/products/1

# 查询所有商品
curl http://localhost:8080/products
```

#### 更新商品

```bash
curl -X PUT http://localhost:8080/products \
  -H "Content-Type: application/json" \
  -d '{
    "id": 1,
    "name": "iPhone 13 Pro",
    "price": 6999.99,
    "stock": 80,
    "description": "苹果手机 Pro"
  }'
```

#### 删除商品

```bash
curl -X DELETE http://localhost:8080/products/1
```

## 配置说明

### 1. 数据库配置

- **MySQL**：localhost:3306，数据库名：test_db
- **用户名**：root
- **密码**：123456

### 2. Redis 配置

- **主机**：localhost
- **端口**：6379
- **数据库**：0

### 3. 缓存配置

- **缓存类型**：Redis
- **缓存过期时间**：10 分钟
- **延迟双删时间**：500 毫秒

## 核心实现

### 1. 缓存延迟删除监听器

- 使用 Redisson 延迟队列实现延迟删除缓存
- 监听队列中的缓存删除任务，确保在更新数据后延迟删除缓存

### 2. 双写策略实现

- **保存操作**：先更新数据库，再删除缓存
- **删除操作**：先删除数据库记录，再删除缓存
- **更新操作**：先删除缓存，再更新数据库，最后延迟删除缓存

### 3. 缓存一致性保证

- 通过延迟双删解决并发更新时的缓存一致性问题
- 确保缓存中的数据与数据库中的数据保持一致

## 注意事项

1. **依赖版本**：使用 Redisson 3.16.8 版本，与 Spring Boot 3.2.0 兼容
2. **延迟时间**：默认延迟时间为 500ms，可根据实际情况调整
3. **缓存键**：缓存键格式为 `product:{id}` 和 `product:all`
4. **数据库初始化**：启动时会自动创建数据库和表结构，并插入测试数据

## 参考文档

- [Spring Boot 官方文档](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Spring Cache 官方文档](https://docs.spring.io/spring-framework/docs/current/reference/html/integration.html#cache)
- [Redisson 官方文档](https://redisson.org/)
- [MySQL 官方文档](https://dev.mysql.com/doc/)
- [Redis 官方文档](https://redis.io/documentation/)
