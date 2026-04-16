import React from 'react';
import { render as renderReact } from 'react-dom';

/**
 * 渲染React组件到终端
 */
export function render(component: React.ReactNode) {
  // 简化实现，实际项目中需要使用终端渲染库
  console.log('Rendering component:', component);
  // 这里应该使用真正的终端渲染库，如Ink
  // 为了简化，我们直接打印组件信息
  console.log('Harness AI Agent started!');
  console.log('Type a command or ask a question.');
  console.log('Type "exit" to quit.');
}
