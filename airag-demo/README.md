# AI RAG 系统

基于 RAG 架构的企业级知识库检索增强生成系统，面向高并发、可维护、可观测的生产环境。

## 技术栈

- Java + Spring Boot 3.x + Spring AI + Chroma + RAG + MySQL + MyBatis-Plus + Maven

## 项目结构

```
com.xxx.rag
├── config/             配置类（Spring AI、Chroma、线程池、Redis、MyBatis等）
├── controller/         HTTP 接口层，仅接收参数、返回结果，无业务逻辑
├── service/
│   ├── impl/           业务实现
│   └── facades/        组合服务、流程编排
├── repository/         向量库 & 数据库访问层
├── mapper/             MySQL 数据访问
├── entity/             数据库实体
├── vo/                 请求/响应视图对象
├── dto/                内部数据传输
├── common/
│   ├── exception/      统一异常
│   ├── result/         统一返回体
│   └── constant/       常量
├── rag/                RAG 核心模块（强制独立包）
│   ├── loader/         文档加载器
│   ├── splitter/       文本分块
│   ├── embedding/      向量化封装
│   ├── store/          向量存储（Chroma）
│   ├── retriever/      检索器
│   ├── rerank/         重排序
│   └── prompt/         Prompt 模板管理
└── util/               工具类
```

## 功能特性

- 支持 PDF、DOCX、XLSX、TXT 等多种文档格式的加载和处理
- 完整的 RAG 流程：文档加载 → 分块 → 向量化 → 入库 → 检索 → 重排 → 生成
- 基于 Chroma 向量库的高效检索
- 基于大模型的重排序，提高检索结果的相关性
- 统一的异常处理和返回格式
- 完善的日志记录

## 快速开始

### 环境准备

1. **安装 Java 17+**
2. **安装 Maven 3.8+**
3. **安装 MySQL**，创建数据库 `airag_demo`
4. **安装 Chroma**，启动 Chroma 服务在 `http://localhost:8000`
5. **配置环境变量**：设置 `OPENAI_API_KEY` 为你的 OpenAI API 密钥

### 构建和运行

1. **构建项目**
   ```bash
   cd airag-demo
   mvn clean package
   ```

2. **运行项目**
   ```bash
   java -jar target/airag-demo-1.0.0.jar
   ```

3. **访问前端页面**
   打开浏览器，访问 `http://localhost:8080/api/index.html`

## API 接口

### 1. 上传文档

- **URL**: `/api/rag/upload`
- **Method**: POST
- **参数**:
  - `file`: 要上传的文件
  - `docType`: 文档类型（pdf, docx, xlsx, txt）
- **返回**:
  ```json
  {
    "code": 0,
    "msg": "success",
    "data": "文档上传成功"
  }
  ```

### 2. 智能查询

- **URL**: `/api/rag/query`
- **Method**: POST
- **参数**:
  ```json
  {
    "question": "你的问题"
  }
  ```
- **返回**:
  ```json
  {
    "code": 0,
    "msg": "success",
    "data": "回答内容"
  }
  ```

### 3. 清空向量库

- **URL**: `/api/rag/clear`
- **Method**: POST
- **返回**:
  ```json
  {
    "code": 0,
    "msg": "success",
    "data": "向量库清空成功"
  }
  ```

## 配置说明

### 数据库配置

在 `application.yml` 中配置 MySQL 连接信息：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/airag_demo?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: 123456
    driver-class-name: com.mysql.cj.jdbc.Driver
```

### Spring AI 配置

在 `application.yml` 中配置 Spring AI：

```yaml
spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
      base-url: https://api.openai.com/v1
    chroma:
      uri: http://localhost:8000
```

## 注意事项

1. **Chroma 服务**：确保 Chroma 服务已启动，并且可在 `http://localhost:8000` 访问
2. **OpenAI API 密钥**：确保已设置 `OPENAI_API_KEY` 环境变量
3. **文档大小**：默认支持最大 100MB 的文档上传
4. **性能优化**：对于大型文档，可能需要调整分块大小和批处理大小

## 扩展建议

1. **添加用户认证**：实现 JWT 或 OAuth2 认证
2. **支持更多文档格式**：如 Markdown、PPT 等
3. **添加缓存**：使用 Redis 缓存热点数据
4. **实现多模型支持**：支持多种大模型，如 Claude、Gemini 等
5. **添加监控**：集成 Prometheus 和 Grafana 监控系统性能

## 故障排查

1. **文档上传失败**：检查文档格式是否支持，文件大小是否超过限制
2. **查询无结果**：检查向量库是否有数据，查询语句是否清晰
3. **模型调用失败**：检查 OpenAI API 密钥是否正确，网络是否畅通
4. **Chroma 连接失败**：检查 Chroma 服务是否启动，地址是否正确

## 许可证

MIT
