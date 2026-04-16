package org.wiki;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// Spring AI的结构化输出功能可以将大模型的自由文本响应自动转换为Java对象，
// 避免手动解析JSON，提供类型安全。
// 核心接口是StructuredOutputConverter，它结合了Converter和FormatProvider接口。
@SpringBootApplication
public class StructuredOutputApplication {

    public static void main(String[] args) {
        SpringApplication.run(StructuredOutputApplication.class, args);
    }


}
