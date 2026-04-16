# PageIndex Python 教程用例

本项目展示了如何使用PageIndex（VectifyAI开发的无向量检索工具）构建一个完整的RAG（检索增强生成）系统，包括后端FastAPI和前端Vue。

## 项目结构

```
pageIndex-python/
├── backend/          # 后端FastAPI代码
│   ├── main.py       # 主应用文件
│   └── requirements.txt  # 依赖文件
└── frontend/         # 前端Vue代码
    ├── index.html    # 入口HTML文件
    ├── package.json  # 项目配置和依赖
    ├── src/
    │   ├── main.js   # Vue应用入口
    │   └── App.vue   # 主组件
    └── vite.config.js  # Vite配置
```

## 功能特点

- **无向量检索**：使用PageIndex的推理式检索，无需向量数据库
- **多文档支持**：支持PDF、Word、TXT等多种文档格式
- **完整的RAG流程**：包括文档上传、索引构建、查询和结果展示
- **前后端分离**：后端使用FastAPI，前端使用Vue

## 环境要求

- Python 3.8+
- Node.js 16+

## 安装和运行

### 后端安装和运行

1. 进入后端目录：
   ```bash
   cd backend
   ```

2. 安装依赖：
   ```bash
   pip install -r requirements.txt
   ```

3. 运行后端服务：
   ```bash
   python main.py
   ```

   后端服务将在 http://localhost:8000 运行。

### 前端安装和运行

1. 进入前端目录：
   ```bash
   cd frontend
   ```

2. 安装依赖：
   ```bash
   npm install
   ```

3. 运行前端开发服务器：
   ```bash
   npm run dev
   ```

   前端应用将在 http://localhost:3000 运行。

## 使用方法

1. **上传文档**：在前端页面点击「选择文件」按钮，选择要上传的文档（支持PDF、Word、TXT格式），然后点击「上传并创建索引」按钮。

2. **查看索引列表**：上传完成后，索引列表会自动刷新，显示新创建的索引。

3. **选择索引**：在索引列表中点击「选择」按钮，选择要查询的索引。

4. **查询索引**：在查询输入框中输入查询内容，然后点击「查询」按钮。

5. **查看结果**：查询结果会显示在页面下方，包括标题、内容和相关性得分。

6. **删除索引**：在索引列表中点击「删除」按钮，删除不需要的索引。

## API接口

### 后端API接口

- **POST /api/index/create**：上传文档并创建索引
  - 请求体：multipart/form-data，包含file字段
  - 响应：{"index_id": "index_1", "message": "索引创建成功"}

- **POST /api/index/query**：查询索引
  - 请求体：{"index_id": "index_1", "query": "查询内容"}
  - 响应：{"results": [{"title": "标题", "content": "内容", "score": 0.9}]}

- **GET /api/index/list**：列出所有索引
  - 响应：{"indexes": ["index_1", "index_2"]}

- **DELETE /api/index/{index_id}**：删除索引
  - 响应：{"message": "索引删除成功"}

## 技术说明

### PageIndex 简介

PageIndex是VectifyAI开发的无向量检索工具，它使用文档结构和LLM推理进行检索，而不是传统的向量相似性搜索。相比传统的向量RAG，PageIndex具有以下特点：

- **无向量需求**：不需要向量数据库和嵌入模型
- **保持文档结构**：不会破坏文档的自然结构
- **推理式检索**：像人类专家一样进行多步推理和树搜索
- **透明的检索过程**：基于推理的检索，告别近似语义搜索（"感觉检索"）

### 实现原理

1. **文档索引**：PageIndex会分析文档结构，构建一个层次化的树结构，每个节点包含文档的一个部分及其摘要。

2. **推理式检索**：当用户查询时，PageIndex会使用LLM进行多步推理，在树结构中搜索最相关的节点。

3. **结果排序**：根据推理结果对文档部分进行排序，返回最相关的内容。

## 注意事项

- 本项目使用的是PageIndex的本地模式，所有处理都在本地进行。
- 对于大型文档，索引构建可能需要较长时间。
- 首次运行时，PageIndex可能会下载必要的模型文件。
- 请确保您的系统有足够的内存和计算资源来运行LLM。

## 扩展建议

- 添加用户认证和授权功能
- 支持更多文档格式
- 实现文档管理功能，如重命名、分类等
- 添加查询历史记录
- 集成更多LLM模型选项
- 优化前端界面，添加更多交互功能

## 参考资料

- [PageIndex GitHub仓库](https://github.com/VectifyAI/PageIndex)
- [FastAPI文档](https://fastapi.tiangolo.com/)
- [Vue文档](https://vuejs.org/)
