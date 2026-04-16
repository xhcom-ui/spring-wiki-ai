# Docker Compose 配置目录

## 目录结构

本目录包含了各种服务的 Docker Compose 配置文件，用于快速部署和管理各种服务。

### 子目录说明

- **chatgpt-web**: ChatGPT Web 界面
- **cloudreve**: 云盘系统
- **datagear**: 数据可视化平台
- **elk-skywalking**: ELK 日志系统 + SkyWalking 链路追踪
- **eureka**: Netflix Eureka 服务注册与发现
- **frp**: 内网穿透工具
- **git**: Git 服务器
- **grafana**: 数据可视化监控平台
- **ip-sec**: IP 安全相关配置
- **java**: Java 应用容器
- **java+mysql+redis**: Java 应用 + MySQL + Redis 组合
- **jira**: Jira 项目管理工具
- **kafka**: Kafka 消息队列
- **mayfly-go**: Mayfly 数据库管理工具
- **mysql**: MySQL 数据库
- **nacos**: Nacos 服务注册与配置中心
- **nexus**: Nexus 私有仓库
- **nginx**: Nginx 服务器
- **nginx+certbot**: Nginx + Certbot 自动 HTTPS 配置
- **openresty**: OpenResty 服务器
- **rabbitmq**: RabbitMQ 消息队列
- **redis**: Redis 缓存
- **rocketmq**: RocketMQ 消息队列
- **wordpress**: WordPress 博客系统
- **xxljob**: XXL-Job 任务调度平台
- **zentao**: 禅道项目管理系统

## 使用方法

1. 进入对应服务的目录
2. 执行 `docker-compose up -d` 启动服务
3. 执行 `docker-compose down` 停止服务

## 配置说明

- 每个服务目录下都包含了对应的 `docker-compose.yml` 文件
- 可以根据需要修改配置文件中的参数
- 部分服务可能需要提前准备环境变量或配置文件

## 注意事项

- 确保 Docker 和 Docker Compose 已经安装
- 确保端口没有被占用
- 部分服务可能需要较大的内存和磁盘空间
- 生产环境使用时请根据实际情况调整配置
