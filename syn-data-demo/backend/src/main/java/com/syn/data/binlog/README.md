# MySQL Binlog监听框架

## 项目概述

这是一个通用的MySQL Binlog监听框架，基于原生Socket实现，不依赖第三方库，适用于所有MySQL数据库。

## 核心功能

- **不依赖第三方库**：使用原生Socket实现，不依赖于`mysql-binlog-connector-java`库
- **通用适用**：适用于所有MySQL数据库，只要开启了Binlog
- **易于使用**：通过注解方式定义监听器，使用简单
- **可扩展性**：可以根据需要扩展框架的功能
- **实时监控**：实时监听MySQL Binlog事件，及时处理数据变化

## 技术架构

### 核心组件

1. **MySQLBinlogListenerFramework**：框架核心服务，负责管理所有MySQL Binlog监听任务
2. **MySQLBinlogListener**：基于原生Socket实现的MySQL Binlog监听器
3. **MySQLBinlogEventListener**：MySQL Binlog事件监听器接口
4. **BinlogDataDispatcher**：Binlog数据分发器，负责将事件分发给对应的监听器
5. **BinlogThreadStarter**：Binlog监听线程启动器，为每个MySQL主机启动监听线程
6. **DataListenerContainer**：数据监听器容器，管理监听器和调用监听器方法

### 配置管理

1. **MySqlHost**：MySQL主机配置类，存储主机的相关信息
2. **MySqlHostProfile**：MySQL主机配置管理类，负责加载和管理MySQL主机配置

### 监听器接口

1. **IMysqlDataListener**：MySQL数据监听器接口，定义了处理数据变化的方法
2. **AbstractMysqlDataListener**：MySQL数据监听器抽象类，提供了默认实现

### 注解

1. **MysqlWatcher**：MySQL监听器注解，用于指定要监听的主机、数据库和表

## 配置方法

### 1. 配置MySQL主机

在`application.yml`中配置MySQL主机信息：

```yaml
mysql:
  hosts: host1:port1:username1:password1,host2:port2:username2:password2
```

### 2. 定义监听器

创建一个实现`IMysqlDataListener`接口的类，并使用`@MysqlWatcher`注解指定要监听的主机、数据库和表：

```java
@MysqlWatcher(hostName = "mysql-1", database = "test", table = "user")
public class UserTableWatcher implements IMysqlDataListener<User> {
    @Override
    public void onUpdate(User from, User to) {
        System.out.println("用户更新: " + from + " -> " + to);
    }

    @Override
    public void onInsert(User data) {
        System.out.println("用户新增: " + data);
    }

    @Override
    public void onDelete(User data) {
        System.out.println("用户删除: " + data);
    }
}
```

### 3. 启动框架

框架会在应用启动时自动加载所有监听器，并为每个主机启动Binlog监听线程。

## 注意事项

1. **MySQL配置**：需要开启Binlog，并设置`binlog-format=ROW`
2. **权限配置**：MySQL用户需要有`REPLICATION SLAVE`和`REPLICATION CLIENT`权限
3. **网络配置**：确保MySQL服务器的3306端口可以被访问
4. **性能考虑**：对于高并发场景，可能需要调整线程池大小和队列容量

## 扩展方法

1. **自定义事件解析**：可以修改`MySQLBinlogListener`类，实现更复杂的事件解析逻辑
2. **自定义数据处理**：可以实现`IMysqlDataListener`接口，自定义数据处理逻辑
3. **添加监控指标**：可以添加监控指标，监控Binlog监听的状态和性能
4. **支持更多事件类型**：可以扩展框架，支持更多类型的Binlog事件

## 示例代码

### 示例1：监听用户表变化

```java
@MysqlWatcher(hostName = "mysql-1", database = "test", table = "user")
public class UserTableWatcher extends AbstractMysqlDataListener<User> {
    @Override
    protected void onData(User data) {
        System.out.println("用户数据变化: " + data);
    }
}
```

### 示例2：监听订单表变化

```java
@MysqlWatcher(hostName = "mysql-1", database = "test", table = "order")
public class OrderTableWatcher implements IMysqlDataListener<Order> {
    @Override
    public void onUpdate(Order from, Order to) {
        System.out.println("订单更新: " + from + " -> " + to);
    }

    @Override
    public void onInsert(Order data) {
        System.out.println("订单新增: " + data);
    }

    @Override
    public void onDelete(Order data) {
        System.out.println("订单删除: " + data);
    }
}
```
