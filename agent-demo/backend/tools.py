#!/usr/bin/env python3
"""
Agent 工具模块
实现文章中提到的三个基础工具：
- list_files(path): 列出目录文件
- read_file(path): 读取文件内容
- run_command(command): 运行白名单命令
"""
import os
import subprocess
from pathlib import Path
from typing import List, Dict, Any


class Tools:
    """工具类集合"""
    
    def __init__(self, workspace: str = "."):
        self.workspace = Path(workspace).resolve()
        self.command_whitelist = {
            "ls", "pwd", "echo", "cat", "head", "tail",
            "python3 --version", "pip3 --version",
            "git status", "git log --oneline -10"
        }
    
    def list_files(self, path: str = ".") -> Dict[str, Any]:
        """
        列出目录文件
        """
        try:
            target_path = (self.workspace / path).resolve()
            
            if not str(target_path).startswith(str(self.workspace)):
                return {"success": False, "error": "路径超出工作区边界"}
            
            if not target_path.exists():
                return {"success": False, "error": "路径不存在"}
            
            if target_path.is_file():
                files = [target_path.name]
                directories = []
            else:
                items = list(target_path.iterdir())
                files = [f.name for f in items if f.is_file()]
                directories = [d.name for d in items if d.is_dir()]
            
            return {
                "success": True,
                "path": str(target_path.relative_to(self.workspace)),
                "files": sorted(files),
                "directories": sorted(directories)
            }
        except Exception as e:
            return {"success": False, "error": str(e)}
    
    def read_file(self, path: str) -> Dict[str, Any]:
        """
        读取文件内容
        限制文件大小，防止大文件污染上下文
        """
        try:
            target_path = (self.workspace / path).resolve()
            
            if not str(target_path).startswith(str(self.workspace)):
                return {"success": False, "error": "路径超出工作区边界"}
            
            if not target_path.exists():
                return {"success": False, "error": "文件不存在"}
            
            if not target_path.is_file():
                return {"success": False, "error": "不是文件"}
            
            size = target_path.stat().st_size
            if size > 100 * 1024:  # 100KB
                content = target_path.read_text(encoding="utf-8", errors="replace")[:5000]
                truncated = True
            else:
                content = target_path.read_text(encoding="utf-8", errors="replace")
                truncated = False
            
            return {
                "success": True,
                "path": str(target_path.relative_to(self.workspace)),
                "content": content,
                "size": size,
                "truncated": truncated
            }
        except Exception as e:
            return {"success": False, "error": str(e)}
    
    def run_command(self, command: str) -> Dict[str, Any]:
        """
        运行白名单命令
        """
        try:
            if not any(command.strip().startswith(cmd) for cmd in self.command_whitelist):
                return {
                    "success": False,
                    "error": f"命令不在白名单中。白名单: {list(self.command_whitelist)}"
                }
            
            result = subprocess.run(
                command,
                shell=True,
                cwd=self.workspace,
                stdout=subprocess.PIPE,
                stderr=subprocess.PIPE,
                timeout=30,
                text=True
            )
            
            stdout = result.stdout[:5000]  # 限制输出长度
            stderr = result.stderr[:2000]
            
            return {
                "success": True,
                "command": command,
                "stdout": stdout,
                "stderr": stderr,
                "returncode": result.returncode
            }
        except subprocess.TimeoutExpired:
            return {"success": False, "error": "命令执行超时（30秒）"}
        except Exception as e:
            return {"success": False, "error": str(e)}
    
    def get_tools_schema(self) -> List[Dict[str, Any]]:
        """获取工具定义（OpenAI Function Calling 格式）"""
        return [
            {
                "type": "function",
                "function": {
                    "name": "list_files",
                    "description": "列出工作区内指定路径的文件和目录",
                    "parameters": {
                        "type": "object",
                        "properties": {
                            "path": {
                                "type": "string",
                                "description": "要列出的目录路径，默认为当前目录"
                            }
                        },
                        "required": []
                    }
                }
            },
            {
                "type": "function",
                "function": {
                    "name": "read_file",
                    "description": "读取工作区内指定文件的内容（大文件会被截断）",
                    "parameters": {
                        "type": "object",
                        "properties": {
                            "path": {
                                "type": "string",
                                "description": "要读取的文件路径"
                            }
                        },
                        "required": ["path"]
                    }
                }
            },
            {
                "type": "function",
                "function": {
                    "name": "run_command",
                    "description": "运行白名单内的命令（安全限制）",
                    "parameters": {
                        "type": "object",
                        "properties": {
                            "command": {
                                "type": "string",
                                "description": "要运行的命令（必须在白名单中）"
                            }
                        },
                        "required": ["command"]
                    }
                }
            }
        ]
