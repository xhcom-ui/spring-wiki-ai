# 数据同步平台

## 项目概述

开发一个可视化、可配置的数据同步平台，支持从关系型数据库（MySQL/PostgreSQL）到Elasticsearch的数据同步，提供SQL编写、Mapping配置、任务调度、实时监控一站式解决方案。

## 核心价值

- 降低使用门槛：可视化操作替代命令行和代码开发
- 提高开发效率：分钟级配置替代天级开发
- 保障数据质量：完善的数据验证和监控机制
- 支持多种场景：批处理、准实时、增量/全量同步

## 技术栈

### 后端
- Spring Boot 3.2.0
- MyBatis-Plus 3.5.5
- Elasticsearch 8.11.0
- XXL-Job 2.4.0（任务调度）
- Sa-Token（认证）

### 前端
- Vue 3
- Element Plus
- ECharts（数据可视化）
- Monaco Editor（代码编辑器）

## 核心功能

### 1. 数据源管理
- 支持MySQL 5.7/8.0+、PostgreSQL 9.5+数据源
- 连接配置、测试和性能测试
- 多数据源支持和安全机制（密码加密存储）

### 2. SQL编辑器
- 智能编辑（语法高亮、代码补全、格式化）
- 变量支持（系统变量、自定义变量、上下文变量）
- SQL模板和执行计划分析
- 数据预览和历史记录

### 3. 映射配置
- 自动映射（基于字段名和数据类型）
- 手动映射（字段类型选择和属性配置）
- 高级映射（嵌套对象、父子文档、Join字段）
- ES Mapping配置（索引设置、字段设置、分析器配置）

### 4. 同步任务配置
- 基本信息（任务名称、描述、分组、负责人、优先级）
- 执行配置（数据源、ES集群、索引、同步模式）
- 增量配置（字段选择、增量策略、断点续传）
- 调度配置（定时执行、事件触发、手动执行）

### 5. 实时同步
- MySQL Binlog监听（自定义实现，不依赖第三方库）
- PostgreSQL WAL逻辑解码
- 批量处理和队列管理
- 实时监控和告警

### 6. 监控与告警
- 实时监控（执行进度、处理速度、资源使用）
- 历史监控（执行记录、趋势分析、性能对比）
- 告警配置（失败告警、超时告警、异常告警）
- 多种告警方式（邮件、企业微信、钉钉、短信、电话）

### 7. 数据质量管理
- 数据校验（数量校验、质量校验、格式校验、业务规则校验）
- 问题管理（错误分类、重试机制、数据修复）
- 质量统计（问题类型分布、状态分布、每日趋势）

## 项目启动

### 后端启动
```bash
cd syn-data-demo/backend
mvn clean package
java -jar target/syn-data-backend-1.0.0.jar
```

### 前端启动
```bash
cd syn-data-demo/frontend
npm install
npm run dev
```

### 访问地址
- 前端：http://localhost:3000
- 后端：http://localhost:8085

### 默认账号
- 管理员：admin / admin123
- 普通用户：user / user123

## 技术特性

1. **准实时同步**：通过监听数据库日志实现准实时数据同步
2. **批量处理**：使用批量队列和定时器提高同步效率
3. **容错机制**：支持连接断开重连和异常告警
4. **可视化监控**：提供实时监控页面，展示同步状态和性能指标
5. **数据质量保障**：全面的数据校验和问题处理机制
6. **安全可靠**：密码加密存储，操作日志记录，断点续传

## 注意事项

1. **环境配置**：需要确保Java 17+和Node.js 14+已安装
2. **数据库配置**：需要执行`schema.sql`文件创建数据库和表结构
3. **Elasticsearch配置**：需要确保Elasticsearch服务运行在9200端口
4. **MySQL配置**：需要开启binlog，并设置`binlog-format=ROW`
5. **PostgreSQL配置**：需要开启逻辑复制，并设置`wal_level=logical`

## 项目结构

```
syn-data-demo/
├── backend/
│   ├── src/main/java/com/syn/data/
│   │   ├── controller/    # 控制器
│   │   ├── entity/         # 实体类
│   │   ├── mapper/         # 数据访问
│   │   ├── service/        # 业务逻辑
│   │   └── SynDataApplication.java  # 应用入口
│   ├── resources/
│   │   ├── application.yml  # 配置文件
│   │   ├── schema.sql       # 数据库脚本
│   └── pom.xml              # Maven配置
├── frontend/
│   ├── src/
│   │   ├── views/          # 页面
│   │   ├── router/          # 路由
│   │   ├── utils/           # 工具
│   │   └── main.js          # 入口文件
│   ├── package.json         # 依赖配置
│   └── vite.config.js       # Vite配置
└── README.md                # 项目说明
```

## 未来规划

1. **多数据源支持**：支持更多类型的数据源，如Oracle、SQL Server等
2. **多目标支持**：支持同步到更多目标系统，如MongoDB、Redis等
3. **集群部署**：支持集群部署，提高系统可靠性和吞吐量
4. **AI智能**：引入AI技术，智能优化同步策略和性能
5. **生态集成**：与更多DevOps工具集成，如Jenkins、GitLab等
