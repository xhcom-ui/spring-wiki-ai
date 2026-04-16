# Apache BVal 数据校验示例

## 项目简介

本项目基于 Spring Boot 3.5.0 和 Apache BVal 实现了数据校验功能，主要包括以下特性：

1. **基本数据校验**：使用标准的 JSR-380 注解进行数据校验
2. **扩展数据校验**：使用 Apache BVal 特有的注解进行扩展校验
3. **多种校验方式**：支持验证整个对象、验证指定属性和验证值

## 技术栈

- Spring Boot 3.5.0
- Apache BVal 3.0.2
- Jakarta Validation API
- Lombok

## 项目结构

```
apache-bval-demo/
├── src/main/java/com/example/bval/ # 项目代码
│   ├── controller/           # 控制器
│   ├── entity/               # 实体类
│   ├── service/              # 业务逻辑层
│   ├── Application.java      # 启动类
│   └── TestValidation.java   # 测试类
├── src/main/resources/        # 配置文件
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

```bash
# 启动服务
java -jar target/apache-bval-demo-1.0.0.jar
```

### 3. 测试接口

#### 验证整个对象

```bash
curl -X POST http://localhost:8080/users/validate \
  -H "Content-Type: application/json" \
  -d '{
    "email": "pack@gmail.com",
    "password": "12345",
    "name": "pack_xg",
    "age": 0
  }'
```

#### 验证指定属性

```bash
curl -X POST http://localhost:8080/users/validate-property \
  -H "Content-Type: application/json" \
  -d '{
    "propertyName": "age",
    "age": 0
  }'
```

#### 验证值

```bash
curl -X POST http://localhost:8080/users/validate-value \
  -H "Content-Type: application/json" \
  -d '{
    "propertyName": "age",
    "value": 200
  }'
```

#### 创建用户

```bash
curl -X POST http://localhost:8080/users \
  -H "Content-Type: application/json" \
  -d '{
    "email": "pack@gmail.com",
    "password": "123456",
    "name": "pack_xg",
    "age": 25,
    "cardNumber": "4111111111111111",
    "iban": "DE89370400440532013000"
  }'
```

### 4. 运行测试类

```bash
# 运行测试类
java -cp target/apache-bval-demo-1.0.0.jar com.example.bval.TestValidation
```

## 核心功能

### 1. 基本数据校验

- **@NotNull**：验证属性不为 null
- **@Size**：验证字符串长度
- **@Min**：验证数值最小值
- **@Max**：验证数值最大值

### 2. 扩展数据校验

- **@Visa**：验证 Visa 信用卡号
- **@IBAN**：验证国际银行账号
- **其他扩展注解**：Apache BVal 还提供了其他扩展注解，如 @Mastercard、@AmericanExpress、@EAN13 等

### 3. 多种校验方式

- **验证整个对象**：验证对象的所有属性
- **验证指定属性**：只验证对象的指定属性
- **验证值**：在属性赋值前验证值的有效性

## 配置说明

- **依赖配置**：在 pom.xml 中添加了 Apache BVal 和相关依赖
- **验证器配置**：使用 `Validation.byProvider(ApacheValidationProvider.class)` 获取验证器

## 注意事项

1. **依赖版本**：使用 Apache BVal 3.0.2 版本，与 Spring Boot 3.5.0 兼容
2. **验证器获取**：Apache BVal 文档建议仅获取 ValidatorFactory 的单实例，因为工厂创建是一个资源消耗较大的过程
3. **扩展注解**：使用扩展注解需要添加 bval-extras 依赖

## 参考文档

- [Apache BVal 官方文档](https://bval.apache.org/)
- [Spring Boot 官方文档](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Jakarta Bean Validation 官方文档](https://jakarta.ee/specifications/bean-validation/3.0/)
