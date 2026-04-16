# 深度研究系统（Deep Research）

本项目实现了一个基于 AI 的深度研究系统，能够自动搜索网络信息、分析数据，并生成详细的研究报告。

## 环境要求

- Java 17+
- PostgreSQL 12+
- Maven

## 安装依赖

### 1. 安装 Java 依赖

```bash
mvn clean install
```

### 2. 配置数据库

创建 PostgreSQL 数据库：

```sql
CREATE DATABASE deep_research_db;
```

## 项目结构

- `src/main/java/com/example/deepresearch/` - Java 代码目录
  - `entity/` - 实体类
    - `ResearchTopic.java` - 研究主题
    - `ResearchSource.java` - 研究来源
    - `ResearchResult.java` - 研究结果
  - `repository/` - 数据访问接口
    - `ResearchTopicRepository.java` - 研究主题仓库
    - `ResearchSourceRepository.java` - 研究来源仓库
    - `ResearchResultRepository.java` - 研究结果仓库
  - `service/` - 业务逻辑服务
    - `EmbeddingService.java` - 文本嵌入服务
    - `LLMService.java` - 大语言模型服务
    - `WebSearchService.java` - 网络搜索服务
    - `DeepResearchService.java` - 深度研究服务
  - `controller/` - 控制器
    - `DeepResearchController.java` - 深度研究控制器
  - `Application.java` - 应用入口
- `src/main/resources/` - 配置文件目录
  - `application.yml` - 应用配置

## 配置说明

在 `application.yml` 文件中，您需要配置以下参数：

```yaml
# Hugging Face 配置
huggingface:
  api-key: your_huggingface_api_key
  embedding-model: sentence-transformers/all-MiniLM-L6-v2

# OpenAI 配置
openai:
  api-key: your_openai_api_key
  model: gpt-4

# 深度研究配置
deep-research:
  max-iterations: 5
  max-sources: 10
  chunk-size: 1000
  chunk-overlap: 200
```

## API 接口

### 1. 启动深度研究

- **URL**: `/api/research/conduct`
- **方法**: POST
- **参数**:
  - `topic`: 研究主题（必填）
  - `description`: 研究描述（可选）
- **返回**: 研究结果

### 2. 获取研究结果

- **URL**: `/api/research/result/{topicId}`
- **方法**: GET
- **参数**:
  - `topicId`: 研究主题 ID
- **返回**: 研究结果

### 3. 获取所有研究主题

- **URL**: `/api/research/topics`
- **方法**: GET
- **返回**: 研究主题列表

## 使用示例

### 使用 curl 测试

#### 1. 启动深度研究

```bash
curl -X POST "http://localhost:8083/api/research/conduct?topic=人工智能的未来发展&description=研究人工智能在未来 10 年的发展趋势和影响"
```

#### 2. 获取研究结果

```bash
curl http://localhost:8083/api/research/result/1
```

#### 3. 获取所有研究主题

```bash
curl http://localhost:8083/api/research/topics
```

### 使用 Postman 测试

1. 打开 Postman
2. 创建相应的请求，URL 分别为：
   - POST `http://localhost:8083/api/research/conduct`（带 topic 和 description 参数）
   - GET `http://localhost:8083/api/research/result/{topicId}`（替换 topicId 为实际 ID）
   - GET `http://localhost:8083/api/research/topics`
3. 点击 "Send" 按钮发送请求

## 功能说明

1. **网络搜索**: 系统会自动搜索与研究主题相关的网络信息
2. **信息处理**: 对搜索结果进行处理和分析
3. **AI 分析**: 使用大语言模型生成研究摘要和结论
4. **结果存储**: 将研究结果存储到数据库中
5. **结果查询**: 提供 API 接口查询研究结果

## 注意事项

1. 确保 PostgreSQL 数据库已启动并创建了 `deep_research_db` 数据库
2. 确保在 `application.yml` 中配置了正确的 Hugging Face 和 OpenAI API 密钥
3. 由于网络搜索和 AI 分析需要时间，深度研究过程可能会比较耗时
4. 本项目使用了模拟的 API 调用，实际应用中需要替换为真实的 API 调用
