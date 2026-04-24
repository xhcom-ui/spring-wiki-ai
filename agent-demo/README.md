# Agent Demo - 手搓最小 Agent

按文章《手搓一个 Agent：从最小 Loop 到 Harness》思路实现的完整项目。

## 项目结构

```
agent-demo/
├── backend/
│   ├── main.py          # FastAPI 后端服务
│   ├── agent.py         # Agent Loop 核心实现
│   ├── tools.py         # 工具类实现
│   ├── requirements.txt # Python 依赖
│   └── .env.example     # 环境变量示例
├── frontend/
│   ├── src/
│   │   ├── App.vue      # 主组件
│   │   └── main.js      # 入口文件
│   ├── index.html       # HTML 入口
│   ├── package.json     # 前端依赖
│   └── vite.config.js   # Vite 配置
└── README.md            # 项目说明
```

## 核心概念

### 🔄 Agent Loop

最小可工作的循环：
1. 读用户任务
2. 让模型选择下一步
3. 按模型要求调用工具
4. 把工具结果喂回去，继续循环

### 🛡️ Harness

运行时边界控制：
- 最大循环步数限制
- 工具输出裁剪
- before/after 钩子
- 完整执行日志

### 🔧 Tools

安全边界：
- **list_files**: 路径边界检查
- **read_file**: 大小限制、内容裁剪
- **run_command**: 白名单命令限制

## 快速开始

### 1. 后端配置

```bash
cd backend

# 安装依赖
pip install -r requirements.txt

# 配置环境变量
cp .env.example .env
# 编辑 .env，填入 OPENAI_API_KEY

# 启动后端服务
python main.py
```

后端服务运行在 http://localhost:8000

### 2. 前端配置

```bash
cd frontend

# 安装依赖
npm install

# 启动开发服务器
npm run dev
```

前端服务运行在 http://localhost:3000

### 3. 使用

1. 打开浏览器访问 http://localhost:3000
2. 配置工作区路径（例如：./workspace）
3. 输入任务描述
4. 点击「运行 Agent」
5. 查看结果和执行日志

## 示例任务

```
- 列出当前目录下的所有文件
- 查看项目结构
- 读取 README.md 的内容
- 查看 git 状态
- 查看最近的 git 日志
```

## API 端点

### GET /health

健康检查

### GET /api/tools

获取可用工具列表

### POST /api/agent/run

运行 Agent

请求体：
```json
{
    "task": "你的任务描述",
    "workspace": "./workspace",
    "model": "gpt-4o",
    "maxSteps": 8
}
```

响应：
```json
{
    "success": true,
    "answer": "Agent 的回答",
    "steps": 3,
    "tool_calls": 2,
    "logs": [...]
}
```

## 核心代码说明

### tools.py

实现三个基础工具，每个工具都有安全边界：
- 路径检查防止越权访问
- 内容裁剪防止污染上下文
- 命令白名单防止恶意执行

### agent.py

实现最小 Agent Loop：
- `AgentHarness`: 边界控制类
- `MinimalAgent`: 主 Agent 类
- `run()`: 核心循环方法

### main.py

FastAPI 服务，提供 API 接口

## 学习要点

1. **从能跑到能用**
   - 最小 Loop 很容易写
   - 真正的工作在边界控制

2. **工具不是函数列表**
   - 模型需要知道工具描述和参数
   - 我们需要检查参数和权限

3. **记忆不是越多越好**
   - 只保留对当前任务有用的信息
   - 考虑摘要、索引、检索

4. **验证比完成重要**
   - 模型说「搞定了」不算数
   - 要有可验证的证据

## 扩展方向

- 添加更多工具（编辑、删除、git 操作等）
- 实现记忆系统
- 添加权限检查和用户确认
- 集成更多验证手段
- 实现子 Agent 支持

## 参考资料

- 原文：《手搓一个 Agent：从最小 Loop 到 Harness》
- Pi 项目：https://github.com/badlogic/pi-mono
- OpenAI Function Calling
- Claude Code 思路

## 许可证

MIT
