from typing import Dict, List, Any, Optional
from langchain_openai import ChatOpenAI
from langchain_core.prompts import ChatPromptTemplate, MessagesPlaceholder
from langchain_core.output_parsers import StrOutputParser
from langchain_core.runnables import RunnablePassthrough
from loguru import logger

class ReActAgent:
    def __init__(self, tools: List, model_name: str = "gpt-4-turbo"):
        """初始化 ReAct Agent
        
        Args:
            tools: 工具列表
            model_name: 模型名称
        """
        self.tools = tools
        self.model = ChatOpenAI(model=model_name, temperature=0.7)
        self.prompt = self._create_prompt()
        self.chain = self._create_chain()
        self.memory = []
    
    def _create_prompt(self):
        """创建 ReAct 模式的提示模板"""
        system_prompt = """
你是一个基于 ReAct 模式的 AI Agent，需要通过推理、执行和观察来完成任务。

ReAct 模式步骤：
1. 推理（Reasoning）：分析当前任务状态，生成内部推理，决定下一步行动
2. 执行（Acting）：根据推理结果执行具体操作，如调用工具
3. 观察（Observation）：观察行动结果，将反馈用于下一轮思考

可用工具：
{tools}

工具使用格式：
Action: 工具名称
Action Input: 工具参数（JSON格式）

请按照以下格式输出：

Reasoning: 你的推理过程
Action: 工具名称
Action Input: 工具参数

当你认为已经获得足够信息或完成任务时，请使用以下格式输出最终答案：

Reasoning: 你的推理过程
Final Answer: 最终答案
        """
        
        prompt = ChatPromptTemplate.from_messages([
            ("system", system_prompt),
            MessagesPlaceholder(variable_name="chat_history"),
            ("human", "{input}")
        ])
        return prompt
    
    def _create_chain(self):
        """创建推理链"""
        tools_str = "\n".join([f"- {tool.name}: {tool.description}" for tool in self.tools])
        
        chain = (
            {
                "input": RunnablePassthrough(),
                "tools": RunnablePassthrough(lambda x: tools_str),
                "chat_history": RunnablePassthrough(lambda x: self.memory)
            }
            | self.prompt
            | self.model
            | StrOutputParser()
        )
        return chain
    
    def _parse_output(self, output: str) -> Dict[str, Any]:
        """解析模型输出"""
        lines = output.strip().split("\n")
        result = {}
        
        for line in lines:
            if line.startswith("Reasoning:"):
                result["reasoning"] = line[len("Reasoning:"):].strip()
            elif line.startswith("Action:"):
                result["action"] = line[len("Action:"):].strip()
            elif line.startswith("Action Input:"):
                result["action_input"] = line[len("Action Input:"):].strip()
            elif line.startswith("Final Answer:"):
                result["final_answer"] = line[len("Final Answer:"):].strip()
        
        return result
    
    def _execute_tool(self, tool_name: str, tool_input: str) -> str:
        """执行工具"""
        for tool in self.tools:
            if tool.name == tool_name:
                try:
                    import json
                    input_data = json.loads(tool_input)
                    result = tool.run(input_data)
                    return str(result)
                except Exception as e:
                    return f"Error executing tool: {str(e)}"
        return f"Tool not found: {tool_name}"
    
    def run(self, task: str, max_steps: int = 10) -> str:
        """运行 Agent
        
        Args:
            task: 任务描述
            max_steps: 最大步数
        
        Returns:
            任务结果
        """
        logger.info(f"Starting task: {task}")
        
        for step in range(max_steps):
            logger.info(f"Step {step + 1}/{max_steps}")
            
            # 生成推理和行动
            output = self.chain.invoke(task)
            logger.debug(f"Model output: {output}")
            
            # 解析输出
            parsed_output = self._parse_output(output)
            
            # 检查是否有最终答案
            if "final_answer" in parsed_output:
                logger.info(f"Task completed with answer: {parsed_output['final_answer']}")
                return parsed_output["final_answer"]
            
            # 执行工具
            if "action" in parsed_output and "action_input" in parsed_output:
                tool_name = parsed_output["action"]
                tool_input = parsed_output["action_input"]
                logger.info(f"Executing tool: {tool_name} with input: {tool_input}")
                
                # 执行工具并获取结果
                observation = self._execute_tool(tool_name, tool_input)
                logger.info(f"Tool result: {observation}")
                
                # 将结果添加到记忆中
                self.memory.append({"role": "assistant", "content": output})
                self.memory.append({"role": "system", "content": f"Observation: {observation}"})
                
                # 更新任务描述，包含观察结果
                task = f"{task}\n\nPrevious observation: {observation}"
            else:
                logger.warning("No action found in model output")
                break
        
        logger.warning("Max steps reached, task not completed")
        return "Task could not be completed within the maximum number of steps"
