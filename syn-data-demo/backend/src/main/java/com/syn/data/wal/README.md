# PostgreSQL WAL监听框架

## 项目概述

这是一个通用的PostgreSQL WAL监听框架，基于原生Socket实现，不依赖第三方库，适用于所有PostgreSQL数据库。

## 核心功能

- **不依赖第三方库**：使用原生Socket实现，不依赖于任何第三方WAL监听库
- **通用适用**：适用于所有PostgreSQL数据库，只要开启了逻辑复制
- **易于使用**：通过注解方式定义监听器，使用简单
- **可扩展性**：可以根据需要扩展框架的功能
- **实时监控**：实时监听PostgreSQL WAL事件，及时处理数据变化

## 技术架构

### 核心组件

1. **PgWalListenerFramework**：框架核心服务，负责管理所有PostgreSQL WAL监听任务
2. **PgWalListener**：基于原生Socket实现的PostgreSQL WAL监听器
3. **PgWalEventListener**：PostgreSQL WAL事件监听器接口
4. **PgWalDataDispatcher**：WAL数据分发器，负责将事件分发给对应的监听器
5. **PgWalThreadStarter**：WAL监听线程启动器，为每个PostgreSQL主机启动监听线程
6. **PgDataListenerContainer**：数据监听器容器，管理监听器和调用监听器方法

### 配置管理

1. **PgHost**：PostgreSQL主机配置类，存储主机的相关信息
2. **PgHostProfile**：PostgreSQL主机配置管理类，负责加载和管理PostgreSQL主机配置

### 监听器接口

1. **IPgDataListener**：PostgreSQL数据监听器接口，定义了处理数据变化的方法
2. **AbstractPgDataListener**：PostgreSQL数据监听器抽象类，提供了默认实现

### 注解

1. **PgWatcher**：PostgreSQL监听器注解，用于指定要监听的主机、数据库和表

## 配置方法

### 1. 配置PostgreSQL主机

在`application.yml`中配置PostgreSQL主机信息：

```yaml
postgres:
  hosts: host1:port1:username1:password1:database1,host2:port2:username2:password2:database2
```

### 2. 定义监听器

创建一个实现`IPgDataListener`接口的类，并使用`@PgWatcher`注解指定要监听的主机、数据库和表：

```java
@PgWatcher(hostName = "postgres-1", database = "test", table = "user")
public class UserTableWatcher implements IPgDataListener<User> {
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

框架会在应用启动时自动加载所有监听器，并为每个主机启动WAL监听线程。

## 注意事项

1. **PostgreSQL配置**：需要开启逻辑复制，并设置`wal_level=logical`
2. **权限配置**：PostgreSQL用户需要有`REPLICATION`权限
3. **网络配置**：确保PostgreSQL服务器的5432端口可以被访问
4. **性能考虑**：对于高并发场景，可能需要调整线程池大小和队列容量

## 扩展方法

1. **自定义事件解析**：可以修改`PgWalListener`类，实现更复杂的事件解析逻辑
2. **自定义数据处理**：可以实现`IPgDataListener`接口，自定义数据处理逻辑
3. **添加监控指标**：可以添加监控指标，监控WAL监听的状态和性能
4. **支持更多事件类型**：可以扩展框架，支持更多类型的WAL事件

## 示例代码

### 示例1：监听用户表变化

```java
@PgWatcher(hostName = "postgres-1", database = "test", table = "user")
public class UserTableWatcher extends AbstractPgDataListener<User> {
    @Override
    protected void onData(User data) {
        System.out.println("用户数据变化: " + data);
    }
}
```

### 示例2：监听订单表变化

```java
@PgWatcher(hostName = "postgres-1", database = "test", table = "order")
public class OrderTableWatcher implements IPgDataListener<Order> {
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
