# MongoPlus 使用教程

## 项目简介

本项目是一个使用 MongoPlus + Spring Boot 框架的示例项目，展示了如何使用 MongoPlus 操作 MongoDB 数据库。

## 技术栈

- Spring Boot 3.2.0
- MongoDB
- MongoPlus
- Lombok

## 项目结构

```
src/
├── main/
│   ├── java/com/mongo/plus/
│   │   ├── config/         # 配置类
│   │   ├── controller/     # 控制器
│   │   ├── entity/         # 实体类
│   │   ├── mapper/         # Mapper接口
│   │   ├── service/        # 服务层
│   │   │   └── impl/       # 服务实现
│   │   └── MongoPlusApplication.java  # 应用主类
│   └── resources/
│       └── application.yml  # 配置文件
└── pom.xml                 # Maven配置文件
```

## 核心功能

1. **用户管理**：实现了用户的增删改查功能
2. **MongoPlus使用**：展示了如何使用MongoPlus操作MongoDB数据库
3. **RESTful API**：提供了RESTful风格的API接口

## 快速开始

### 1. 环境准备

- JDK 17+
- Maven 3.8+
- MongoDB 4.0+

### 2. 配置MongoDB

确保MongoDB服务已经启动，并且创建了名为`mongo_plus_demo`的数据库。

### 3. 构建和运行项目

```bash
# 构建项目
mvn clean package

# 运行项目
java -jar target/mongo-plus-demo-1.0.0.jar
```

### 4. 测试API

项目启动后，可以通过以下API进行测试：

#### 4.1 创建用户

```bash
POST /api/users
Content-Type: application/json

{
  "name": "张三",
  "age": 25,
  "email": "zhangsan@example.com"
}
```

#### 4.2 查询所有用户

```bash
GET /api/users
```

#### 4.3 根据ID查询用户

```bash
GET /api/users/{id}
```

#### 4.4 根据用户名查询用户

```bash
GET /api/users/name/{name}
```

#### 4.5 更新用户

```bash
PUT /api/users
Content-Type: application/json

{
  "id": "{id}",
  "name": "李四",
  "age": 30,
  "email": "lisi@example.com"
}
```

#### 4.6 删除用户

```bash
DELETE /api/users/{id}
```

## MongoPlus 核心使用

### 1. 实体类定义

使用 `@MongoTable` 注解标记实体类，使用 `@Id` 注解标记主键，使用 `@Field` 注解标记字段。

```java
@Data
@MongoTable(value = "user")
public class User implements Serializable {

    @Id(type = IdType.AUTO)
    private String id;

    @Field(value = "name")
    private String name;

    @Field(value = "age")
    private Integer age;

    @Field(value = "email")
    private String email;

    @Field(value = "create_time")
    private Date createTime;

    @Field(value = "update_time")
    private Date updateTime;

}
```

### 2. Mapper接口定义

继承 `BaseMapper` 接口，即可获得基本的CRUD方法。

```java
public interface UserMapper extends BaseMapper<User> {

}
```

### 3. 服务实现

使用 `UserMapper` 操作数据库，实现业务逻辑。

```java
@Service
public class UserServiceImpl implements UserService {

    @Resource
    private UserMapper userMapper;

    @Override
    public User save(User user) {
        // 设置创建时间和更新时间
        Date now = new Date();
        user.setCreateTime(now);
        user.setUpdateTime(now);
        // 保存用户
        userMapper.insert(user);
        return user;
    }

    @Override
    public User findById(String id) {
        return userMapper.findById(id);
    }

    @Override
    public List<User> findAll() {
        return userMapper.selectList(QueryBuilder.query());
    }

    @Override
    public List<User> findByName(String name) {
        return userMapper.selectList(
                QueryBuilder.query()
                        .eq("name", name)
        );
    }

    @Override
    public User update(User user) {
        // 设置更新时间
        user.setUpdateTime(new Date());
        // 更新用户
        userMapper.updateById(user);
        return user;
    }

    @Override
    public void delete(String id) {
        userMapper.deleteById(id);
    }

}
```

### 4. 控制器实现

提供RESTful API接口，处理HTTP请求。

```java
@RestController
@RequestMapping("/api/users")
public class UserController {

    @Resource
    private UserService userService;

    @PostMapping
    public User save(@RequestBody User user) {
        return userService.save(user);
    }

    @GetMapping("/{id}")
    public User findById(@PathVariable String id) {
        return userService.findById(id);
    }

    @GetMapping
    public List<User> findAll() {
        return userService.findAll();
    }

    @GetMapping("/name/{name}")
    public List<User> findByName(@PathVariable String name) {
        return userService.findByName(name);
    }

    @PutMapping
    public User update(@RequestBody User user) {
        return userService.update(user);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable String id) {
        userService.delete(id);
    }

}
```

### 5. 配置文件

在 `application.yml` 中配置MongoDB连接信息。

```yaml
spring:
  data:
    mongodb:
      uri: mongodb://localhost:27017/mongo_plus_demo

server:
  port: 8080

logging:
  level:
    com.mongo.plus: info
```

## 总结

本项目展示了如何使用 MongoPlus + Spring Boot 框架操作 MongoDB 数据库，实现了用户的增删改查功能。通过本项目的学习，您可以了解 MongoPlus 的基本使用方法，包括实体类定义、Mapper接口定义、服务实现和控制器实现等。

MongoPlus 是一个功能强大的 MongoDB ORM 框架，它提供了丰富的 API 来操作 MongoDB 数据库，简化了开发过程，提高了开发效率。
