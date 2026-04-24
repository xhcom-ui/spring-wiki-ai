package com.syn.data.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.syn.data.entity.DataSourceConfig;
import com.syn.data.mapper.DataSourceConfigMapper;
import com.syn.data.service.DatabaseConnectionService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 数据源配置控制器
 */
@RestController
@RequestMapping("/api/datasource")
@SaCheckRole("admin")
public class DataSourceController {

    @Resource
    private DataSourceConfigMapper dataSourceConfigMapper;

    @Resource
    private DatabaseConnectionService databaseConnectionService;

    /**
     * 获取所有数据源
     */
    @GetMapping
    public List<DataSourceConfig> list() {
        return dataSourceConfigMapper.selectList(
                new QueryWrapper<DataSourceConfig>()
                        .orderByDesc("update_time")
                        .orderByDesc("id")
        );
    }

    /**
     * 根据ID获取数据源
     */
    @GetMapping("/{id}")
    public DataSourceConfig getById(@PathVariable Long id) {
        return dataSourceConfigMapper.selectById(id);
    }

    /**
     * 创建数据源
     */
    @PostMapping
    public DataSourceConfig save(@RequestBody DataSourceConfig config) {
        applyDefaults(config);
        config.setCreateTime(LocalDateTime.now());
        config.setUpdateTime(LocalDateTime.now());
        config.setStatus(1); // 默认启用
        dataSourceConfigMapper.insert(config);
        return config;
    }

    /**
     * 更新数据源
     */
    @PutMapping
    public DataSourceConfig update(@RequestBody DataSourceConfig config) {
        DataSourceConfig existing = dataSourceConfigMapper.selectById(config.getId());
        if (existing == null) {
            throw new RuntimeException("数据源不存在");
        }
        applyDefaults(config);
        if (config.getPassword() == null || config.getPassword().isBlank()) {
            config.setPassword(existing.getPassword());
        }
        config.setUpdateTime(LocalDateTime.now());
        dataSourceConfigMapper.updateById(config);
        return config;
    }

    /**
     * 删除数据源
     */
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        dataSourceConfigMapper.deleteById(id);
    }

    /**
     * 测试数据源连接
     */
    @PostMapping("/{id}/test")
    public Map<String, Object> testConnection(@PathVariable Long id) {
        DataSourceConfig config = dataSourceConfigMapper.selectById(id);
        if (config == null) {
            throw new RuntimeException("数据源不存在");
        }

        Map<String, Object> result = new java.util.LinkedHashMap<>();
        try {
            // 测试连接
            long startTime = System.currentTimeMillis();
            try (Connection conn = databaseConnectionService.openConnection(config);
                 Statement stmt = conn.createStatement()) {
                long endTime = System.currentTimeMillis();

                // 获取数据库版本信息
                String version = querySingleValue(stmt, "SELECT version()", 1);

                // 获取字符集信息
                String charset = databaseConnectionService.resolveCharset(config, stmt);

                result.put("success", true);
                result.put("message", "连接测试成功");
                result.put("version", version);
                result.put("charset", charset);
                result.put("connectTime", endTime - startTime);
            }

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "连接测试失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 测试数据源性能
     */
    @PostMapping("/{id}/performance")
    public Map<String, Object> testPerformance(@PathVariable Long id) {
        DataSourceConfig config = dataSourceConfigMapper.selectById(id);
        if (config == null) {
            throw new RuntimeException("数据源不存在");
        }

        Map<String, Object> result = new java.util.LinkedHashMap<>();

        try {
            // 测试多次连接耗时
            int testCount = 5;
            long totalTime = 0;

            for (int i = 0; i < testCount; i++) {
                long startTime = System.currentTimeMillis();
                try (Connection ignored = databaseConnectionService.openConnection(config)) {
                    totalTime += (System.currentTimeMillis() - startTime);
                }
            }

            result.put("success", true);
            result.put("message", "性能测试成功");
            result.put("testCount", testCount);
            result.put("avgConnectTime", totalTime / testCount);
            result.put("totalTime", totalTime);

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "性能测试失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 获取数据源类型列表
     */
    @GetMapping("/types")
    public List<Map<String, Object>> getTypes() {
        return List.of(
                Map.of("value", "mysql", "label", "MySQL", "defaultPort", 3306),
                Map.of("value", "postgresql", "label", "PostgreSQL", "defaultPort", 5432)
        );
    }

    private void applyDefaults(DataSourceConfig config) {
        if (config.getPassword() == null || config.getPassword().isBlank()) {
            if (config.getId() == null) {
                throw new IllegalArgumentException("数据源密码不能为空");
            }
        }
        if (config.getPort() == null) {
            config.setPort("postgresql".equalsIgnoreCase(config.getType()) ? 5432 : 3306);
        }
        if (config.getMaxConnections() == null || config.getMaxConnections() <= 0) {
            config.setMaxConnections(10);
        }
        if (config.getMinConnections() == null || config.getMinConnections() < 0) {
            config.setMinConnections(5);
        }
        if (config.getConnectionTimeout() == null || config.getConnectionTimeout() <= 0) {
            config.setConnectionTimeout(30000);
        }
    }

    private String querySingleValue(Statement stmt, String sql, int columnIndex) throws Exception {
        try (java.sql.ResultSet rs = stmt.executeQuery(sql)) {
            return rs.next() ? rs.getString(columnIndex) : "";
        }
    }

}
