# Spring Wiki AI 项目总览

这是一个以 `Spring Boot + AI + 工作流 + 多种中间件/工程能力` 为主题的示例仓库，包含大量独立 demo。仓库既有后端项目，也有前后端联动项目，还包含 Python、Django、Flask、WinForm 等不同技术栈的实验与样例。

## 仓库说明

- 仓库根目录下每个 `*-demo` 或独立目录通常都是一个可单独运行的示例。
- 大多数 Java 项目使用 `Maven`。
- 部分项目带有 `frontend` 前端目录，通常基于 `Vue 3 + Vite`。
- 各子项目的启动方式、依赖和业务说明，以子目录内自己的 `README.md` 为准。

## 目录分类

### 1. AI / 大模型 / RAG 相关

- `ai-agent-demo`：AI Agent 示例
- `agent-demo`：智能体相关实验
- `agentscope-demo`：AgentScope 示例
- `airag-demo`：RAG 示例
- `deep-research-demo`：深度研究类示例
- `deepseek-rag-demo`：DeepSeek + RAG 示例
- `dflash-demo`：推理加速 / DFlash 相关示例
- `hugging-face-embeddings-demo`：Hugging Face 向量/Embedding 示例
- `langchain4j-demo`：LangChain4j 示例
- `mcp-demo`：MCP（Model Context Protocol）相关示例
- `memory-demo`：记忆能力相关示例
- `ollama-demo`：Ollama 本地模型集成
- `pageIndex-python`：页面索引与检索
- `prompt-demo`：Prompt 管理/调试示例
- `rag-demo`：基础 RAG 示例
- `structured-demo`：结构化输出/结构化处理示例
- `text2sql-demo`：Text-to-SQL 示例
- `vectorstore-demo`：向量库示例
- `yuque-ai`：语雀/知识库类 AI 集成示例

### 2. 工作流 / BPM / 审批流相关

- `activiti-demo`：Activiti 工作流示例，含前端流程设计与运行时页面
- `flowable-demo`：Flowable 工作流示例，含前端流程设计与运行时页面
- `flowlong-demo`：FlowLong/自定义流程设计示例

### 3. 安全 / 认证 / 权限相关

- `sa-token-demo`：Sa-Token 示例
- `security-demo`：Spring Security 示例
- `security-oauth-demo`：OAuth2 / 认证授权示例
- `spring-token`：Token 处理相关示例

### 4. 微服务 / 网关 / 分布式相关

- `gateway-demo`：网关示例
- `seata-demo`：分布式事务示例
- `sharing-sphere-demo`：分库分表/数据分片示例
- `sentinel-demo`：限流/熔断相关示例

### 5. 数据 / 存储 / 检索相关

- `mongo-plus-demo`：MongoDB 相关示例
- `opendataloader-demo`：数据加载相关示例
- `springboot-redis-mysql-demo`：Redis + MySQL 集成示例
- `syn-data-demo`：数据同步示例
- `jiajia-search-demo`：搜索相关示例

### 6. Web / 前后端 / 全栈相关

- `django-demo`：Django 示例
- `flask-demo`：Flask 示例
- `spring-vue`：Spring Boot + Vue 示例
- `spring-wiki`：Wiki/内容管理相关示例
- `spring-python`：Spring 与 Python 协作相关示例

### 7. 基础能力 / 框架 / 工具类示例

- `apache-bval-demo`：参数校验/校验框架示例
- `hashmap-demo`：HashMap / 缓存 / 内存问题示例
- `health-demo`：健康检查示例
- `hutool-demo`：Hutool 工具库示例
- `harness-demo`：Harness 相关示例
- `netty-demo`：Netty 示例
- `parent-demo`：父工程/聚合类示例
- `park-demo`：停车场/业务管理示例
- `sk-api`：API 对接相关示例
- `springboot3-cache-demo`：Spring Boot 3 缓存示例
- `springboot3-demo`：Spring Boot 3 功能示例
- `sse-demo`：SSE 实时推送示例
- `winform-sunnyui-demo`：WinForm / SunnyUI 示例

## 根目录主要文件

- `README.md`：当前总览文档
- `pom.xml`：根 Maven 配置
- `build.sh`：批量构建脚本
- `docker-compose.yml`：统一编排入口
- `docker-compose/`：各类容器与中间件配置
- `data/`：共享数据或初始化资源

## 推荐使用方式

### 查看单个项目

先进入具体子项目，再查看其说明文档：

```bash
cd activiti-demo
cat README.md
```

### Java 项目常用命令

```bash
cd 某个-demo
mvn clean package
```

开发运行：

```bash
cd 某个-demo
mvn spring-boot:run
```

### 前端项目常用命令

```bash
cd 某个-demo/frontend
npm install
npm run dev
```

生产构建：

```bash
cd 某个-demo/frontend
npm run build
```

### Python 项目常用命令

```bash
cd 某个目录
python -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
python main.py
```

## 运行环境建议

- `JDK 17+`：多数 Spring Boot 示例建议使用
- `Maven 3.9+`
- `Node.js 18+`：前端项目建议
- `Python 3.10+`：Python 类项目建议
- `Docker / Docker Compose`：涉及中间件时推荐使用

> 注意：不同子项目的版本要求可能不同，最终以对应子项目 `README.md`、`pom.xml`、`package.json` 为准。

## 当前仓库特点

- 项目数量多，主题跨度大，适合作为内部实验仓、样例仓或功能验证仓使用。
- 工作流相关项目（`activiti-demo`、`flowable-demo`、`flowlong-demo`）已经带有较完整的前后端联动示例。
- AI 与 RAG 相关项目覆盖面较广，适合做选型、联调和 PoC。

## 阅读建议

如果你是第一次进入这个仓库，建议按下面顺序看：

1. 先看你关心的大类目录，比如 `activiti-demo`、`flowable-demo`、`rag-demo`。
2. 再看子项目中的 `README.md`。
3. 最后根据 `pom.xml`、`package.json`、`application.yml` 或 `frontend/vite.config.*` 启动项目。

## 补充说明

- 根目录 README 只做总览，不展开每个项目的详细启动说明。
- 如果某个子项目没有 README，优先看其目录结构、`pom.xml`、`src/main/resources/application*.yml` 和 `frontend/package.json`。
- 仓库会持续演进，目录和功能可能变化，建议以实际代码为准。

## License

本仓库代码和示例请按实际项目需要自行评估使用方式；如子项目内有单独 License 或说明，以子项目为准。
