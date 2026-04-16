# OpenDataLoader PDF 解析示例

本项目演示了如何在 Spring Boot 应用中封装调用 [OpenDataLoader PDF](https://github.com/opendataloader-project/opendataloader-pdf) 开源项目，用于解析 PDF 文档并提取结构化数据。

## 环境要求

- Java 11+
- Python 3.10+
- Maven

## 安装依赖

### 1. 安装 Python 依赖

```bash
pip install opendataloader-pdf
```

### 2. 安装 Java 依赖

```bash
mvn clean install
```

## 项目结构

- `src/main/java/com/example/opendataloader/` - Java 代码目录
  - `Application.java` - 应用入口
  - `PdfParserController.java` - REST 接口控制器
  - `PdfParserService.java` - PDF 解析服务
- `scripts/` - Python 脚本目录
  - `parse_pdf.py` - 调用 opendataloader-pdf 解析 PDF 的脚本
- `src/main/resources/` - 配置文件目录
  - `application.yml` - 应用配置

## 配置说明

在 `application.yml` 文件中，您可以配置以下参数：

```yaml
opendataloader:
  python-path: python3  # Python 可执行文件路径
  script-path: scripts/parse_pdf.py  # Python 脚本路径
  output-dir: output  # 输出目录
```

## API 接口

### 解析 PDF 文件

- **URL**: `/api/pdf/parse`
- **方法**: POST
- **参数**:
  - `file`: PDF 文件（必填）
  - `formats`: 输出格式，逗号分隔，如 `markdown,json,html`（可选，默认值：`markdown,json`）
- **返回**: 解析结果

## 使用示例

### 使用 curl 测试

```bash
curl -X POST http://localhost:8082/api/pdf/parse \
  -F "file=@example.pdf" \
  -F "formats=markdown,json,html"
```

### 使用 Postman 测试

1. 打开 Postman
2. 创建一个 POST 请求，URL 为 `http://localhost:8082/api/pdf/parse`
3. 在 "Body" 选项卡中选择 "form-data"
4. 添加一个 key 为 "file" 的参数，类型为 "File"，选择要上传的 PDF 文件
5. 可选：添加一个 key 为 "formats" 的参数，值为 "markdown,json,html"
6. 点击 "Send" 按钮发送请求

## 输出格式

OpenDataLoader PDF 支持以下输出格式：

- **markdown**: 结构化的 Markdown 格式，适合用于 RAG（检索增强生成）
- **json**: JSON 格式，包含元素的边界框信息，适合用于源引用
- **html**: HTML 格式，适合用于网页展示

## 注意事项

1. 确保 Python 环境中已安装 `opendataloader-pdf` 包
2. 确保 Java 环境版本为 11 或更高
3. 上传的 PDF 文件大小不能超过 10MB（可在 application.yml 中修改）
4. 解析结果会保存在 `output` 目录中
