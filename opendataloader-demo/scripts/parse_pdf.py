#!/usr/bin/env python3
import os
import sys
import argparse
import opendataloader_pdf


def parse_pdf(input_path, output_dir, formats):
    """
    解析 PDF 文件并输出指定格式
    """
    # 确保输出目录存在
    os.makedirs(output_dir, exist_ok=True)
    
    # 调用 opendataloader-pdf 解析 PDF
    try:
        opendataloader_pdf.convert(
            input_path=[input_path],
            output_dir=output_dir,
            format=formats
        )
        print(f"PDF 解析成功，输出目录: {output_dir}")
        return True
    except Exception as e:
        print(f"PDF 解析失败: {str(e)}")
        return False


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="解析 PDF 文件")
    parser.add_argument("--input", required=True, help="PDF 文件路径")
    parser.add_argument("--output", required=True, help="输出目录")
    parser.add_argument("--formats", default="markdown,json", help="输出格式，逗号分隔，如: markdown,json,html")
    
    args = parser.parse_args()
    
    success = parse_pdf(args.input, args.output, args.formats)
    sys.exit(0 if success else 1)
