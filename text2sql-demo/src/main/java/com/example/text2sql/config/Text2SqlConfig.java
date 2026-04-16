package com.example.text2sql.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "text2sql.database")
public class Text2SqlConfig {
    private String type;
    private List<Table> tables;

    @Data
    public static class Table {
        private String name;
        private List<Column> columns;
    }

    @Data
    public static class Column {
        private String name;
        private String type;
        private String description;
    }
}
