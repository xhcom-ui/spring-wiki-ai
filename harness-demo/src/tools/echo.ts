// echo工具实现

interface EchoArgs {
  text: string;
}

export async function echo(args: EchoArgs): Promise<string> {
  // 模拟echo工具的实现
  return args.text;
}
