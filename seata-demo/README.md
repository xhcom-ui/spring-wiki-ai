# Seata Demo 项目

## 项目简介

本项目基于 Spring Boot、Spring Cloud、Seata 实现了分布式事务的演示，包括订单服务、库存服务、支付服务三个微服务，以及一个 API 网关。同时集成了 Spring Cloud Sleuth 和 Zipkin 实现了事务链路可视化，集成了 Chaos Monkey 实现了故障注入演练。

## 技术栈

- Spring Boot 3.2.0
- Spring Cloud 2023.0.0
- Seata 1.5.2
- Spring Cloud Sleuth 3.1.5
- Zipkin 2.24.3
- Chaos Monkey 3.1.0
- MyBatis-Plus 3.5.5
- MySQL 8.0
- Nacos 2.2.0

## 项目结构

```
seata-demo/
├── common/                # 公共模块
│   ├── src/main/java/com/seata/demo/common/
│   │   ├── entity/        # 公共实体类
│   │   ├── feign/         # Feign 客户端接口
│   │   ├── request/       # 请求类
│   │   └── response/      # 响应类
├── order-service/         # 订单服务
│   ├── src/main/java/com/seata/demo/order/
│   │   ├── controller/    # 控制器
│   │   ├── entity/        # 实体类
│   │   ├── mapper/        # Mapper 接口
│   │   ├── service/       # 服务层
│   │   └── OrderServiceApplication.java # 启动类
├── inventory-service/     # 库存服务
│   ├── src/main/java/com/seata/demo/inventory/
│   │   ├── controller/    # 控制器
│   │   ├── entity/        # 实体类
│   │   ├── mapper/        # Mapper 接口
│   │   ├── service/       # 服务层
│   │   └── InventoryServiceApplication.java # 启动类
├── payment-service/       # 支付服务
│   ├── src/main/java/com/seata/demo/payment/
│   │   ├── controller/    # 控制器
│   │   ├── entity/        # 实体类
│   │   ├── mapper/        # Mapper 接口
│   │   ├── service/       # 服务层
│   │   └── PaymentServiceApplication.java # 启动类
├── gateway/               # API 网关
│   ├── src/main/java/com/seata/demo/gateway/
│   │   └── GatewayApplication.java # 启动类
├── db/                    # 数据库脚本
│   └── init.sql           # 初始化脚本
├── docker-compose.yml     # Docker 配置
└── README.md              # 项目说明
```

## 快速开始

### 1. 启动基础服务

```bash
# 启动 MySQL、Nacos、Seata Server、Zipkin
docker-compose up -d
```

### 2. 构建项目

```bash
# 构建所有模块
mvn clean package
```

### 3. 启动服务

```bash
# 启动订单服务
java -jar order-service/target/order-service-1.0.0.jar

# 启动库存服务
java -jar inventory-service/target/inventory-service-1.0.0.jar

# 启动支付服务
java -jar payment-service/target/payment-service-1.0.0.jar

# 启动网关
java -jar gateway/target/gateway-1.0.0.jar
```

### 4. 测试接口

#### 创建订单

```bash
curl -X POST http://localhost:8080/order/create \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "productId": 1,
    "count": 10
  }'
```

### 5. 查看事务链路

访问 Zipkin 控制台：http://localhost:9411

### 6. 故障注入演练

Chaos Monkey 已经配置为在运行时随机注入故障，包括延迟和异常。

## 核心功能

1. **分布式事务**：使用 Seata 实现了跨服务的分布式事务管理，确保订单、库存、支付的原子性操作。

2. **事务链路可视化**：集成 Spring Cloud Sleuth 和 Zipkin，实现了请求链路的跟踪和可视化。

3. **故障注入**：集成 Chaos Monkey，实现了故障注入演练，测试系统在故障情况下的韧性。

4. **微服务架构**：采用微服务架构，将订单、库存、支付服务分离，提高系统的可扩展性和可维护性。

5. **API 网关**：使用 Spring Cloud Gateway 作为 API 网关，统一处理所有请求。

## 注意事项

1. 确保 Docker 已安装并运行
2. 确保端口 3306、8848、8091、9411、8080-8083 未被占用
3. 首次启动时，MySQL 会自动执行初始化脚本，创建必要的数据库和表结构
4. Seata Server 需要在 Nacos 中注册，确保 Nacos 启动正常
