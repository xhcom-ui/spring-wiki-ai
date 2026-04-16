package org.wiki.converter;

import org.springframework.ai.converter.StructuredOutputConverter;
import org.springframework.core.convert.converter.Converter;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CustomStructuredOutputConverter<T> implements StructuredOutputConverter<T> {
    
    private final ObjectMapper objectMapper;
    private final Class<T> targetClass;
    
    @Override
    public String getFormat() {
        try {
            // 生成JSON Schema作为格式描述
            return objectMapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(objectMapper.constructType(targetClass));
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate format", e);
        }
    }
    
    @Override
    public T convert(String text) {
        try {
            // 清理JSON字符串（移除可能的markdown代码块）
            String json = text.replaceAll("```json\\s*", "")
                             .replaceAll("```\\s*", "")
                             .trim();
            return objectMapper.readValue(json, targetClass);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert text to " + targetClass.getSimpleName(), e);
        }
    }
}