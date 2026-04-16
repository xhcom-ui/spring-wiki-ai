# Harness AI Agent

A CLI-based AI Coding Agent inspired by the top-tier development team's Harness project.

## Overview

Harness is a powerful AI Coding Agent designed to assist developers with various tasks, including:

- Writing code in multiple programming languages
- Explaining technical concepts
- Running commands and tools
- Providing guidance on software development
- Helping with debugging and troubleshooting

## Project Structure

```
src/
├── commands/         # Slash commands
├── hooks/            # React hooks
├── components/       # UI components
├── utils/            # Utility functions
├── tools/            # Tool implementations
├── constants/        # Constants and prompts
├── ink/              # Terminal UI rendering
├── screens/          # Screen components
└── main.tsx          # Main entry point
```

## Features

- **AI-Powered Assistance**: Get help with coding tasks from an AI assistant
- **Tool Integration**: Use built-in tools like grep, echo, and ls
- **Interactive REPL**: A command-line interface for interacting with the AI
- **Extensible Architecture**: Easy to add new tools and commands
- **TypeScript Support**: Full TypeScript support for type safety

## Getting Started

### Prerequisites

- Bun (https://bun.sh/)
- TypeScript

### Installation

1. Clone the repository
2. Install dependencies:
   ```bash
   bun install
   ```
3. Build the project:
   ```bash
   bun run build
   ```

### Usage

#### Start the REPL

```bash
bun run start
```

#### Run a specific task

```bash
bun run start run "Write a function to calculate the factorial of a number"
```

### Commands

- `start`: Start the AI agent REPL
- `run <task>`: Run a specific task

### Tools

- `grep`: Search for patterns in files
- `echo`: Print text to the console
- `ls`: List files in a directory

### Examples

#### Example 1: Asking for help

```
$ bun run start
Harness AI Agent started!
Type a command or ask a question.
Type "exit" to quit.
$ help
I can help you with various tasks. Try asking me to write code, explain concepts, or run commands.
```

#### Example 2: Using a tool

```
$ bun run start
Harness AI Agent started!
Type a command or ask a question.
Type "exit" to quit.
$ ls("./")
Files in ./:  
1. file1.txt
2. file2.js
3. file3.ts
4. directory1/
5. directory2/
```

## Technical Details

- **Runtime**: Bun
- **Language**: TypeScript (strict mode)
- **Terminal UI**: React + Ink
- **CLI Parsing**: Commander.js
- **Schema Validation**: Zod
- **AI Integration**: OpenAI API

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

MIT
