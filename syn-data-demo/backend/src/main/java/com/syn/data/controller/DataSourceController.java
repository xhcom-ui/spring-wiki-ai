package com.syn.data.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.syn.data.entity.DataSourceConfig;
import com.syn.data.mapper.DataSourceConfigMapper;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据源配置控制器
 */
@RestController
@RequestMapping("/api/datasource")
public class DataSourceController {

    @Resource
    private DataSourceConfigMapper dataSourceConfigMapper;

    /**
     * 获取所有数据源
     */
    @GetMapping
    public List<DataSourceConfig> list() {
        return dataSourceConfigMapper.selectList(null);
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

        Map<String, Object> result = new HashMap<>();
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;

        try {
            // 加载驱动
            if ("mysql".equals(config.getType())) {
                Class.forName("com.mysql.cj.jdbc.Driver");
            } else if ("postgresql".equals(config.getType())) {
                Class.forName("org.postgresql.Driver");
            } else {
                throw new RuntimeException("不支持的数据源类型: " + config.getType());
            }

            // 构建连接URL
            String url;
            if ("mysql".equals(config.getType())) {
                url = String.format("jdbc:mysql://%s:%d/%s?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai",
                        config.getHost(), config.getPort(), config.getDatabaseName());
            } else {
                url = String.format("jdbc:postgresql://%s:%d/%s",
                        config.getHost(), config.getPort(), config.getDatabaseName());
            }

            // 测试连接
            long startTime = System.currentTimeMillis();
            conn = DriverManager.getConnection(url, config.getUsername(), config.getPassword());
            long endTime = System.currentTimeMillis();

            // 获取数据库版本信息
            stmt = conn.createStatement();
            rs = stmt.executeQuery("SELECT version()");
            String version = "";
            if (rs.next()) {
                version = rs.getString(1);
            }

            // 获取字符集信息
            String charset = "";
            if ("mysql".equals(config.getType())) {
                rs = stmt.executeQuery("SHOW VARIABLES LIKE 'character_set_database'");
                if (rs.next()) {
                    charset = rs.getString(2);
                }
            } else if ("postgresql".equals(config.getType())) {
                rs = stmt.executeQuery("SHOW client_encoding");
                if (rs.next()) {
                    charset = rs.getString(1);
                }
            }

            result.put("success", true);
            result.put("message", "连接测试成功");
            result.put("version", version);
            result.put("charset", charset);
            result.put("connectTime", endTime - startTime);

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "连接测试失败: " + e.getMessage());
        } finally {
            try {
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
                if (conn != null) conn.close();
            } catch (Exception e) {
                // 忽略关闭异常
            }
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

        Map<String, Object> result = new HashMap<>();
        Connection conn = null;

        try {
            // 加载驱动
            if ("mysql".equals(config.getType())) {
                Class.forName("com.mysql.cj.jdbc.Driver");
            } else if ("postgresql".equals(config.getType())) {
                Class.forName("org.postgresql.Driver");
            }

            // 构建连接URL
            String url;
            if ("mysql".equals(config.getType())) {
                url = String.format("jdbc:mysql://%s:%d/%s?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai",
                        config.getHost(), config.getPort(), config.getDatabaseName());
            } else {
                url = String.format("jdbc:postgresql://%s:%d/%s",
                        config.getHost(), config.getPort(), config.getDatabaseName());
            }

            // 测试多次连接耗时
            int testCount = 5;
            long totalTime = 0;

            for (int i = 0; i < testCount; i++) {
                long startTime = System.currentTimeMillis();
                conn = DriverManager.getConnection(url, config.getUsername(), config.getPassword());
                long endTime = System.currentTimeMillis();
                totalTime += (endTime - startTime);
                conn.close();
            }

            result.put("success", true);
            result.put("message", "性能测试成功");
            result.put("testCount", testCount);
            result.put("avgConnectTime", totalTime / testCount);
            result.put("totalTime", totalTime);

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "性能测试失败: " + e.getMessage());
        } finally {
            try {
                if (conn != null) conn.close();
            } catch (Exception e) {
                // 忽略关闭异常
            }
        }

        return result;
    }

    /**
     * 获取数据源类型列表
     */
    @GetMapping("/types")
    public List<Map<String, Object>> getTypes() {
        List<Map<String, Object>> types = new java.util.ArrayList<>();
        
        Map<String, Object> mysql = new HashMap<>();
        mysql.put("value", "mysql");
        mysql.put("label", "MySQL");
        mysql.put("defaultPort", 3306);
        types.add(mysql);

        Map<String, Object> postgresql = new HashMap<>();
        postgresql.put("value", "postgresql");
        postgresql.put("label", "PostgreSQL");
        postgresql.put("defaultPort", 5432);
        types.add(postgresql);

        return types;
    }

}
