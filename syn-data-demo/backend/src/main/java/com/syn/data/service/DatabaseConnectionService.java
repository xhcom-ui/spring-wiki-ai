package com.syn.data.service;

import com.syn.data.entity.DataSourceConfig;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * 数据库连接辅助服务，统一管理 JDBC URL、驱动和连接测试逻辑。
 */
@Service
public class DatabaseConnectionService {

    public Connection openConnection(DataSourceConfig config) throws Exception {
        loadDriver(config.getType());
        return DriverManager.getConnection(buildJdbcUrl(config), config.getUsername(), config.getPassword());
    }

    public String resolveDriverClassName(String type) {
        if ("mysql".equalsIgnoreCase(type)) {
            return "com.mysql.cj.jdbc.Driver";
        }
        if ("postgresql".equalsIgnoreCase(type)) {
            return "org.postgresql.Driver";
        }
        throw new IllegalArgumentException("不支持的数据源类型: " + type);
    }

    public String buildJdbcUrl(DataSourceConfig config) {
        String extraParams = normalizeConnectionParams(config.getConnectionParams());
        if ("mysql".equalsIgnoreCase(config.getType())) {
            return String.format(
                    "jdbc:mysql://%s:%d/%s?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai%s",
                    config.getHost(),
                    config.getPort(),
                    config.getDatabaseName(),
                    extraParams
            );
        }
        if ("postgresql".equalsIgnoreCase(config.getType())) {
            return String.format(
                    "jdbc:postgresql://%s:%d/%s?ssl=false%s",
                    config.getHost(),
                    config.getPort(),
                    config.getDatabaseName(),
                    extraParams
            );
        }
        throw new IllegalArgumentException("不支持的数据源类型: " + config.getType());
    }

    public String resolveCharset(DataSourceConfig config, Statement stmt) throws Exception {
        if ("mysql".equalsIgnoreCase(config.getType())) {
            return querySingleValue(stmt, "SHOW VARIABLES LIKE 'character_set_database'", 2);
        }
        if ("postgresql".equalsIgnoreCase(config.getType())) {
            return querySingleValue(stmt, "SHOW client_encoding", 1);
        }
        return "";
    }

    private void loadDriver(String type) throws ClassNotFoundException {
        Class.forName(resolveDriverClassName(type));
    }

    private String normalizeConnectionParams(String connectionParams) {
        if (connectionParams == null || connectionParams.isBlank()) {
            return "";
        }
        String normalized = connectionParams.trim();
        if (normalized.startsWith("?")) {
            normalized = normalized.substring(1);
        }
        if (normalized.startsWith("&")) {
            normalized = normalized.substring(1);
        }
        return normalized.isBlank() ? "" : "&" + normalized;
    }

    private String querySingleValue(Statement stmt, String sql, int columnIndex) throws Exception {
        try (ResultSet rs = stmt.executeQuery(sql)) {
            return rs.next() ? rs.getString(columnIndex) : "";
        }
    }
}
