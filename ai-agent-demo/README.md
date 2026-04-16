# AI Agent 演示项目

基于 ReAct 模式的 AI Agent 框架实现，使用 Python 和 LangChain 构建。

## 项目结构

```
ai-agent-demo/
├── agent.py          # ReAct Agent 核心实现
├── tools.py          # 工具类实现
├── main.py           # 应用入口
├── requirements.txt  # 依赖配置
└── README.md         # 项目说明
```

## 功能特点

- **ReAct 模式**：实现了推理（Reasoning）、执行（Acting）和观察（Observation）的循环过程
- **多工具支持**：集成了网络搜索、代码执行、文件操作和计算器工具
- **记忆能力**：能够记录对话历史和工具执行结果
- **灵活配置**：支持自定义模型和工具

## 依赖安装

```bash
pip install -r requirements.txt
```

## 环境配置

创建 `.env` 文件，设置 OpenAI API 密钥：

```
OPENAI_API_KEY=your_api_key_here
```

## 使用方法

运行主脚本：

```bash
python main.py
```

## 示例任务

1. 搜索 2026 年最新的 AI 发展趋势
2. 计算 123456789 * 987654321 的结果
3. 创建一个 Python 脚本，打印 'Hello, AI Agent!'
4. 读取当前目录下的 README.md 文件内容

## 扩展指南

### 添加新工具

1. 在 `tools.py` 中创建新的工具类，继承 `Tool` 基类
2. 实现 `run` 方法，处理工具逻辑
3. 在 `get_tools()` 函数中添加新工具实例

### 自定义 Agent

1. 可以修改 `agent.py` 中的 `ReActAgent` 类
2. 调整提示模板、推理逻辑或工具执行方式

## 技术栈

- Python 3.8+
- LangChain
- OpenAI API
- Requests
- Loguru

## 注意事项

- 本项目使用 OpenAI API，需要有效的 API 密钥
- 代码执行工具可能存在安全风险，仅用于演示
- 网络搜索使用 DuckDuckGo API，可能有访问限制

## 参考资料

- [ReAct: Synergizing Reasoning and Acting in Language Models](https://arxiv.org/abs/2210.03629)
- [LangChain 文档](https://docs.langchain.com/)
- [OpenAI API 文档](https://platform.openai.com/docs/)
