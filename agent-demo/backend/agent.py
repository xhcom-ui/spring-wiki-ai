#!/usr/bin/env python3
"""
最小 Agent Loop 实现
按照文章描述的思路实现：
- 读用户任务
- 让模型选择下一步
- 按模型要求调用工具
- 把工具结果喂回去，继续循环
"""
import json
from typing import List, Dict, Any
from tools import Tools


class AgentHarness:
    """
    Agent 运行时边界（Harness）
    实现文章里提到的边界控制：
    - 最大循环轮数
    - 最大工具调用次数
    - 工具输出裁剪
    - 步骤日志
    """
    def __init__(self, max_steps: int = 8, workspace: str = "."):
        self.max_steps = max_steps
        self.tools = Tools(workspace)
        self.logs: List[Dict[str, Any]] = []
        self.total_tool_calls = 0
    
    def log(self, step: int, message: str, data: Any = None):
        """记录步骤日志"""
        entry = {"step": step, "message": message}
        if data:
            entry["data"] = data
        self.logs.append(entry)
        print(f"[Step {step}] {message}")
    
    def before_tool_call(self, tool_name: str, arguments: Dict) -> bool:
        """工具调用前的钩子：可以校验参数、检查权限"""
        self.log(-1, f"准备调用工具: {tool_name}", arguments)
        self.total_tool_calls += 1
        return True  # 默认允许
    
    def after_tool_call(self, tool_name: str, result: Dict) -> Dict:
        """工具调用后的钩子：可以修改返回、做裁剪"""
        if result.get("success"):
            # 成功结果裁剪，只返回关键信息
            if "content" in result:
                if len(result["content"]) > 3000:
                    result["content"] = result["content"][:3000] + "... [truncated]"
        return result


class MinimalAgent:
    """最小 Agent 实现"""
    
    def __init__(self, client, model: str = "gpt-4o", harness: AgentHarness = None):
        self.client = client
        self.model = model
        self.harness = harness or AgentHarness()
        self.messages: List[Dict] = []
    
    def _get_system_prompt(self) -> str:
        return """你是一个智能助手，可以调用工具来帮助用户。
你的工作方式：
1. 理解用户任务
2. 决定使用工具获取信息
3. 整合信息回答问题
4. 如果需要继续使用工具
5. 任务完成时总结

可用工具：
- list_files: 列出文件和目录
- read_file: 读取文件内容
- run_command: 运行白名单命令

每次只能调用一个工具，等工具返回后再决定下一步。
任务完成后，给用户一个完整的总结。"""
    
    def _call_model(self, messages: List, tools: List) -> Dict:
        """调用模型"""
        response = self.client.chat.completions.create(
            model=self.model,
            messages=messages,
            tools=tools,
            temperature=0.7,
        )
        return response.choices[0].message
    
    def _run_tool(self, tool_name: str, arguments: Dict) -> str:
        """运行工具"""
        if not self.harness.before_tool_call(tool_name, arguments):
            return json.dumps({"success": False, "error": "工具调用被拦截"}, ensure_ascii=False)
        
        if tool_name == "list_files":
            result = self.harness.tools.list_files(**arguments)
        elif tool_name == "read_file":
            result = self.harness.tools.read_file(**arguments)
        elif tool_name == "run_command":
            result = self.harness.tools.run_command(**arguments)
        else:
            result = {"success": False, "error": f"未知工具: {tool_name}"}
        
        result = self.harness.after_tool_call(tool_name, result)
        return json.dumps(result, ensure_ascii=False)
    
    async def run(self, task: str) -> Dict[str, Any]:
        """
        运行 Agent
        这是文章里描述的最小 Loop：
        - 读用户任务
        - 让模型选择下一步
        - 按模型要求调用工具
        - 把工具结果喂回去，继续循环
        """
        self.messages = [
            {"role": "system", "content": self._get_system_prompt()},
            {"role": "user", "content": task}
        ]
        
        self.harness.log(0, f"开始任务", task)
        
        tools = self.harness.tools.get_tools_schema()
        
        for step in range(1, self.harness.max_steps + 1):
            self.harness.log(step, "调用模型...")
            
            assistant_msg = self._call_model(self.messages, tools)
            self.messages.append(assistant_msg)
            
            if not assistant_msg.tool_calls:
                self.harness.log(step, "任务完成（无工具调用）")
                break
            
            for tool_call in assistant_msg.tool_calls:
                tool_name = tool_call.function.name
                arguments = json.loads(tool_call.function.arguments or "{}")
                
                self.harness.log(step, f"执行工具调用", {"tool": tool_name, "args": arguments)
                
                observation = self._run_tool(tool_name, arguments)
                
                self.messages.append({
                    "role": "tool",
                    "tool_call_id": tool_call.id,
                    "content": observation,
                })
            
            if step == self.harness.max_steps:
                self.harness.log(step, "达到最大步数限制，停止")
        
        final_msg = self._call_model(self.messages, [])
        self.messages.append(final_msg)
        
        return {
            "success": True,
            "answer": final_msg.content,
            "steps": step,
            "tool_calls": self.harness.total_tool_calls,
            "logs": self.harness.logs
        }
