package com.example.text2sql.service;

import com.example.text2sql.config.Text2SqlConfig;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import com.theokanning.openai.service.OpenAiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class Text2SqlService {

    @Autowired
    private OpenAiService openAiService;

    @Autowired
    private Text2SqlConfig text2SqlConfig;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Value("${openai.model}")
    private String model;

    @Value("${openai.temperature}")
    private double temperature;

    public Map<String, Object> convertTextToSql(String text) throws Exception {
        // 1. 生成数据库表结构描述
        String schemaDescription = generateSchemaDescription();

        // 2. 构建 OpenAI 提示
        String prompt = buildPrompt(text, schemaDescription);

        // 3. 调用 OpenAI API 生成 SQL
        String sql = generateSqlFromOpenAI(prompt);

        // 4. 执行 SQL 查询
        List<Map<String, Object>> results = executeSql(sql);

        // 5. 构建响应
        return Map.of(
                "originalText", text,
                "generatedSql", sql,
                "results", results
        );
    }

    private String generateSchemaDescription() {
        StringBuilder schemaBuilder = new StringBuilder();
        schemaBuilder.append("数据库类型: " + text2SqlConfig.getType() + "\n");
        schemaBuilder.append("表结构:\n");

        for (Text2SqlConfig.Table table : text2SqlConfig.getTables()) {
            schemaBuilder.append("表名: " + table.getName() + "\n");
            schemaBuilder.append("列:\n");
            for (Text2SqlConfig.Column column : table.getColumns()) {
                schemaBuilder.append("  - " + column.getName() + " (" + column.getType() + "): " + column.getDescription() + "\n");
            }
            schemaBuilder.append("\n");
        }

        return schemaBuilder.toString();
    }

    private String buildPrompt(String text, String schemaDescription) {
        return "你是一个 SQL 专家，请根据以下数据库表结构，将用户的自然语言查询转换为 SQL 语句。\n" +
                "\n" +
                "数据库表结构:\n" +
                schemaDescription +
                "\n" +
                "用户查询:\n" +
                text + "\n" +
                "\n" +
                "请生成符合 " + text2SqlConfig.getType() + " 语法的 SQL 语句，只返回 SQL 语句本身，不要包含其他任何内容。\n" +
                "确保 SQL 语句正确无误，并且能够直接执行。\n" +
                "如果查询需要关联多个表，请使用正确的 JOIN 语句。\n" +
                "如果查询涉及时间范围，请使用正确的时间函数。\n" +
                "如果查询需要聚合函数，请使用正确的聚合函数。\n";
    }

    private String generateSqlFromOpenAI(String prompt) throws Exception {
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new ChatMessage(ChatMessageRole.SYSTEM.value(), "你是一个 SQL 专家，能够将自然语言查询转换为 SQL 语句。"));
        messages.add(new ChatMessage(ChatMessageRole.USER.value(), prompt));

        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .model(model)
                .messages(messages)
                .temperature(temperature)
                .maxTokens(500)
                .build();

        try {
            String response = openAiService.createChatCompletion(request)
                    .getChoices().get(0)
                    .getMessage().getContent();
            log.info("Generated SQL: {}", response);
            return response;
        } catch (Exception e) {
            log.error("Error generating SQL: {}", e.getMessage(), e);
            throw new Exception("生成 SQL 失败: " + e.getMessage());
        }
    }

    private List<Map<String, Object>> executeSql(String sql) throws Exception {
        try {
            log.info("Executing SQL: {}", sql);
            return jdbcTemplate.queryForList(sql);
        } catch (Exception e) {
            log.error("Error executing SQL: {}", e.getMessage(), e);
            throw new Exception("执行 SQL 失败: " + e.getMessage());
        }
    }
}
