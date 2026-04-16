import React, { useState, useEffect, useRef } from 'react';
import { useAgent } from '../hooks/useAgent';
import { useTools } from '../hooks/useTools';

interface REPLProps {
  initialTask?: string;
}

const REPL: React.FC<REPLProps> = ({ initialTask }) => {
  const [input, setInput] = useState('');
  const [history, setHistory] = useState<string[]>([]);
  const [isProcessing, setIsProcessing] = useState(false);
  const inputRef = useRef<HTMLInputElement>(null);
  
  const { runAgent, agentState } = useAgent();
  const { tools } = useTools();

  useEffect(() => {
    // 处理初始任务
    if (initialTask) {
      handleSubmit(initialTask);
    }
  }, [initialTask]);

  useEffect(() => {
    // 自动聚焦输入框
    if (inputRef.current) {
      inputRef.current.focus();
    }
  }, []);

  const handleSubmit = async (command: string) => {
    if (!command.trim()) return;

    // 添加到历史记录
    setHistory(prev => [...prev, `> ${command}`]);
    setInput('');
    setIsProcessing(true);

    try {
      // 运行Agent
      const result = await runAgent(command);
      // 添加Agent的响应到历史记录
      setHistory(prev => [...prev, result]);
    } catch (error) {
      // 添加错误信息到历史记录
      setHistory(prev => [...prev, `Error: ${error.message}`]);
    } finally {
      setIsProcessing(false);
    }
  };

  const handleKeyPress = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter') {
      handleSubmit(input);
    }
  };

  return (
    <div>
      <div style={{ marginBottom: '10px' }}>
        <h1 style={{ fontSize: '16px', margin: '0' }}>Harness AI Agent</h1>
        <p style={{ fontSize: '12px', margin: '5px 0' }}>Type a command or ask a question. Type "exit" to quit.</p>
      </div>
      
      <div style={{ marginBottom: '10px', maxHeight: '300px', overflowY: 'auto' }}>
        {history.map((line, index) => (
          <div key={index} style={{ fontSize: '14px', margin: '2px 0' }}>
            {line}
          </div>
        ))}
        {isProcessing && (
          <div style={{ fontSize: '14px', margin: '2px 0', color: '#666' }}>
            Processing...
          </div>
        )}
      </div>
      
      <div style={{ display: 'flex' }}>
        <span style={{ marginRight: '5px', fontSize: '14px' }}>$</span>
        <input
          ref={inputRef}
          type="text"
          value={input}
          onChange={(e) => setInput(e.target.value)}
          onKeyPress={handleKeyPress}
          disabled={isProcessing}
          style={{
            flex: 1,
            border: '1px solid #ccc',
            padding: '5px',
            fontSize: '14px',
            outline: 'none'
          }}
        />
      </div>
    </div>
  );
};

export default REPL;
