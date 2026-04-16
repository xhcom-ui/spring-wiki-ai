import os
from dotenv import load_dotenv
from agent import ReActAgent
from tools import get_tools
from loguru import logger

# 加载环境变量
load_dotenv()

# 确保设置了 OpenAI API 密钥
if not os.getenv("OPENAI_API_KEY"):
    logger.error("请设置 OPENAI_API_KEY 环境变量")
    exit(1)

def main():
    """主函数"""
    # 获取工具列表
    tools = get_tools()
    logger.info(f"加载了 {len(tools)} 个工具")
    
    # 创建 ReAct Agent
    agent = ReActAgent(tools)
    
    # 示例任务
    tasks = [
        "搜索 2026 年最新的 AI 发展趋势",
        "计算 123456789 * 987654321 的结果",
        "创建一个 Python 脚本，打印 'Hello, AI Agent!'",
        "读取当前目录下的 README.md 文件内容"
    ]
    
    # 执行任务
    for i, task in enumerate(tasks):
        logger.info(f"\n=== 任务 {i + 1}: {task} ===")
        result = agent.run(task)
        logger.info(f"结果: {result}")
        logger.info("=" * 50)

if __name__ == "__main__":
    main()
