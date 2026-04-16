import { useState } from 'react';
import { OpenAI } from 'openai';

interface AgentState {
  isRunning: boolean;
  lastResponse: string;
  error: string | null;
}

interface UseAgentReturn {
  runAgent: (prompt: string) => Promise<string>;
  agentState: AgentState;
}

export function useAgent(): UseAgentReturn {
  const [agentState, setAgentState] = useState<AgentState>({
    isRunning: false,
    lastResponse: '',
    error: null
  });

  const runAgent = async (prompt: string): Promise<string> => {
    setAgentState(prev => ({
      ...prev,
      isRunning: true,
      error: null
    }));

    try {
      // 这里应该调用真正的AI模型
      // 为了简化，我们模拟一个AI响应
      const response = await simulateAIResponse(prompt);
      
      setAgentState(prev => ({
        ...prev,
        isRunning: false,
        lastResponse: response
      }));

      return response;
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : 'Unknown error';
      setAgentState(prev => ({
        ...prev,
        isRunning: false,
        error: errorMessage
      }));
      throw error;
    }
  };

  // 模拟AI响应
  const simulateAIResponse = (prompt: string): Promise<string> => {
    return new Promise((resolve) => {
      setTimeout(() => {
        let response = '';
        
        if (prompt.includes('hello') || prompt.includes('hi')) {
          response = 'Hello! I am your Harness AI Agent. How can I help you today?';
        } else if (prompt.includes('exit') || prompt.includes('quit')) {
          response = 'Goodbye! Have a nice day.';
        } else if (prompt.includes('help')) {
          response = 'I can help you with various tasks. Try asking me to write code, explain concepts, or run commands.';
        } else {
          response = `I'm processing your request: "${prompt}". This is a simulated response.`;
        }
        
        resolve(response);
      }, 1000);
    });
  };

  return {
    runAgent,
    agentState
  };
}
