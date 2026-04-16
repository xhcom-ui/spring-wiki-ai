import { useState, useEffect } from 'react';

interface Tool {
  name: string;
  description: string;
  run: (args: any) => Promise<string>;
}

interface UseToolsReturn {
  tools: Tool[];
  runTool: (name: string, args: any) => Promise<string>;
}

export function useTools(): UseToolsReturn {
  const [tools, setTools] = useState<Tool[]>([]);

  useEffect(() => {
    // 初始化工具
    const initializedTools: Tool[] = [
      {
        name: 'grep',
        description: 'Search for patterns in files',
        run: async (args: { pattern: string; path: string }) => {
          return `Searching for "${args.pattern}" in ${args.path}... This is a simulated response.`;
        }
      },
      {
        name: 'echo',
        description: 'Print text to the console',
        run: async (args: { text: string }) => {
          return args.text;
        }
      },
      {
        name: 'ls',
        description: 'List files in a directory',
        run: async (args: { path: string }) => {
          return `Files in ${args.path}: file1.txt, file2.js, file3.ts`;
        }
      }
    ];

    setTools(initializedTools);
  }, []);

  const runTool = async (name: string, args: any): Promise<string> => {
    const tool = tools.find(t => t.name === name);
    if (!tool) {
      throw new Error(`Tool ${name} not found`);
    }

    try {
      return await tool.run(args);
    } catch (error) {
      throw new Error(`Error running tool ${name}: ${error.message}`);
    }
  };

  return {
    tools,
    runTool
  };
}
