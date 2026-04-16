// grep工具实现

interface GrepArgs {
  pattern: string;
  path: string;
}

export async function grep(args: GrepArgs): Promise<string> {
  // 模拟grep工具的实现
  return `Searching for "${args.pattern}" in ${args.path}...

Found 3 matches:
1. ${args.path}:10: This line contains ${args.pattern}
2. ${args.path}:25: Another line with ${args.pattern}
3. ${args.path}:42: Final line with ${args.pattern}`;
}
