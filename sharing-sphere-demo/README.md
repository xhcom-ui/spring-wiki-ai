# ShardingSphere 读写分离与分库分表 demo

## 项目简介

本项目基于 Spring Boot 3 和 ShardingSphere 实现了数据库的读写分离和分库分表功能，主要包括以下特性：

1. **读写分离**：主库负责写操作，从库负责读操作
2. **分库分表**：基于用户 ID 对用户表进行分表，分为 t_user_0 和 t_user_1 两个表
3. **自动路由**：ShardingSphere 自动根据配置的规则将请求路由到对应的数据库和表

## 技术栈

- Spring Boot 3.2.0
- ShardingSphere 5.4.0
- Spring Data JPA
- MySQL 8.0
- Docker Compose

## 项目结构

```
sharing-sphere-demo/
├── db/                       # 数据库初始化脚本
│   ├── init-master.sql       # 主库初始化脚本
│   └── init-slave.sql        # 从库初始化脚本
├── src/main/java/com/example/sharingsphere/ # 项目代码
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

#### 添加用户

```bash
curl -X POST http://localhost:8080/users \
  -H "Content-Type: application/json" \
  -d '{
    "name": "张三",
    "age": 25,
    "email": "zhangsan@example.com"
  }'
```

#### 查询用户

```bash
# 根据 ID 查询用户
curl http://localhost:8080/users/1

# 查询所有用户
curl http://localhost:8080/users
```

#### 更新用户

```bash
curl -X PUT http://localhost:8080/users \
  -H "Content-Type: application/json" \
  -d '{
    "id": 1,
    "name": "张三(更新)",
    "age": 26,
    "email": "zhangsan_updated@example.com"
  }'
```

#### 删除用户

```bash
curl -X DELETE http://localhost:8080/users/1
```

## 核心功能

### 1. 读写分离

- **主库**：负责所有写操作（插入、更新、删除）
- **从库**：负责所有读操作（查询）
- **自动切换**：ShardingSphere 自动根据操作类型选择对应的数据库

### 2. 分库分表

- **分表规则**：基于用户 ID 取模，`id % 2` 决定数据存储到哪个表
- **分表数量**：2 个表（t_user_0 和 t_user_1）
- **自动路由**：ShardingSphere 自动根据分表规则将数据路由到对应的表

### 3. 分布式主键

- 使用 Snowflake 算法生成分布式主键，确保 ID 的唯一性

## 配置说明

### 1. 数据库配置

- **主库**：localhost:3306，数据库名：db_master
- **从库**：localhost:3307，数据库名：db_slave
- **用户名**：root
- **密码**：root123

### 2. 分表配置

- **分表规则**：`t_user_${id % 2}`
- **分表列**：id
- **分表算法**：INLINE

### 3. 读写分离配置

- **写数据源**：master
- **读数据源**：slave

## 注意事项

1. **数据库准备**：启动前确保 MySQL 服务未占用 3306 和 3307 端口
2. **构建顺序**：先构建项目，再启动 Docker Compose
3. **数据同步**：本示例未配置主从复制，实际生产环境需要配置主从复制确保数据一致性
4. **性能测试**：可以通过压测工具测试读写分离和分库分表的性能提升

## 参考文档

- [ShardingSphere 官方文档](https://shardingsphere.apache.org/document/current/en/overview/)
- [Spring Boot 官方文档](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Spring Data JPA 官方文档](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/)
