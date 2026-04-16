// ls工具实现

interface LsArgs {
  path: string;
}

export async function ls(args: LsArgs): Promise<string> {
  // 模拟ls工具的实现
  return `Files in ${args.path}:

1. file1.txt
2. file2.js
3. file3.ts
4. directory1/
5. directory2/`;
}
