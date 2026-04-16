#!/usr/bin/env bun

import { Command } from 'commander';
import React from 'react';
import { render } from './ink';
import REPL from './screens/REPL';

// 命令行解析
const program = new Command();

program
  .name('harness')
  .description('A CLI-based AI Coding Agent')
  .version('1.0.0');

// 定义命令
program
  .command('start')
  .description('Start the AI agent REPL')
  .action(() => {
    // 启动REPL
    render(<REPL />);
  });

program
  .command('run <task>')
  .description('Run a specific task')
  .action((task) => {
    console.log(`Running task: ${task}`);
    // 启动REPL并执行任务
    render(<REPL initialTask={task} />);
  });

// 解析命令行参数
program.parse(process.argv);

// 如果没有指定命令，默认启动REPL
if (!process.argv.slice(2).length) {
  render(<REPL />);
}
