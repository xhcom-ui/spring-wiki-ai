from typing import Dict, Any
from langchain_core.tools import Tool
import requests
import subprocess
import os
import json
from loguru import logger

class SearchTool:
    """网络搜索工具"""
    
    name = "search"
    description = "用于搜索网络信息，输入一个搜索查询字符串"
    
    def run(self, input_data: Dict[str, Any]) -> str:
        """执行搜索
        
        Args:
            input_data: 包含查询字符串的字典，键为 "query"
        
        Returns:
            搜索结果
        """
        query = input_data.get("query", "")
        if not query:
            return "搜索查询不能为空"
        
        try:
            # 使用 DuckDuckGo 搜索 API
            url = f"https://api.duckduckgo.com/?q={query}&format=json"
            response = requests.get(url, timeout=10)
            response.raise_for_status()
            data = response.json()
            
            # 提取搜索结果
            results = []
            if "Abstract" in data and data["Abstract"]:
                results.append(f"摘要: {data['Abstract']}")
            if "RelatedTopics" in data:
                for topic in data["RelatedTopics"]:
                    if "Text" in topic:
                        results.append(f"相关信息: {topic['Text']}")
            
            if results:
                return "\n".join(results[:5])  # 限制结果数量
            else:
                return "未找到相关信息"
        except Exception as e:
            return f"搜索失败: {str(e)}"

class CodeExecutionTool:
    """代码执行工具"""
    
    name = "execute_code"
    description = "用于执行 Python 代码，输入要执行的代码字符串"
    
    def run(self, input_data: Dict[str, Any]) -> str:
        """执行代码
        
        Args:
            input_data: 包含代码的字典，键为 "code"
        
        Returns:
            代码执行结果
        """
        code = input_data.get("code", "")
        if not code:
            return "代码不能为空"
        
        try:
            # 执行代码并捕获输出
            result = subprocess.run(
                ["python3", "-c", code],
                capture_output=True,
                text=True,
                timeout=10
            )
            
            if result.returncode == 0:
                return f"执行成功:\n{result.stdout}"
            else:
                return f"执行失败:\n{result.stderr}"
        except Exception as e:
            return f"执行出错: {str(e)}"

class FileTool:
    """文件操作工具"""
    
    name = "file_operation"
    description = "用于文件操作，输入操作类型（read/write/delete）、文件路径和内容（仅写操作需要）"
    
    def run(self, input_data: Dict[str, Any]) -> str:
        """执行文件操作
        
        Args:
            input_data: 包含操作类型、文件路径和内容的字典
        
        Returns:
            操作结果
        """
        operation = input_data.get("operation", "")
        file_path = input_data.get("file_path", "")
        content = input_data.get("content", "")
        
        if not operation or not file_path:
            return "操作类型和文件路径不能为空"
        
        try:
            if operation == "read":
                if os.path.exists(file_path):
                    with open(file_path, "r", encoding="utf-8") as f:
                        return f.read()
                else:
                    return f"文件不存在: {file_path}"
            
            elif operation == "write":
                # 确保目录存在
                os.makedirs(os.path.dirname(file_path) if os.path.dirname(file_path) else ".", exist_ok=True)
                with open(file_path, "w", encoding="utf-8") as f:
                    f.write(content)
                return f"文件写入成功: {file_path}"
            
            elif operation == "delete":
                if os.path.exists(file_path):
                    os.remove(file_path)
                    return f"文件删除成功: {file_path}"
                else:
                    return f"文件不存在: {file_path}"
            
            else:
                return f"不支持的操作类型: {operation}"
        except Exception as e:
            return f"文件操作失败: {str(e)}"

class CalculatorTool:
    """计算器工具"""
    
    name = "calculate"
    description = "用于计算数学表达式，输入一个数学表达式字符串"
    
    def run(self, input_data: Dict[str, Any]) -> str:
        """执行计算
        
        Args:
            input_data: 包含数学表达式的字典，键为 "expression"
        
        Returns:
            计算结果
        """
        expression = input_data.get("expression", "")
        if not expression:
            return "表达式不能为空"
        
        try:
            # 使用 eval 计算表达式
            # 注意：这里仅用于演示，实际应用中应使用更安全的计算方法
            result = eval(expression)
            return f"计算结果: {result}"
        except Exception as e:
            return f"计算失败: {str(e)}"

# 创建工具实例
def get_tools():
    """获取工具列表"""
    search_tool = SearchTool()
    code_tool = CodeExecutionTool()
    file_tool = FileTool()
    calculator_tool = CalculatorTool()
    
    # 转换为 LangChain Tool 格式
    tools = [
        Tool(
            name=search_tool.name,
            func=search_tool.run,
            description=search_tool.description
        ),
        Tool(
            name=code_tool.name,
            func=code_tool.run,
            description=code_tool.description
        ),
        Tool(
            name=file_tool.name,
            func=file_tool.run,
            description=file_tool.description
        ),
        Tool(
            name=calculator_tool.name,
            func=calculator_tool.run,
            description=calculator_tool.description
        )
    ]
    
    return tools
