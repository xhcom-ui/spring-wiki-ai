package org.wiki.service;

import cn.hutool.core.text.csv.CsvData;
import cn.hutool.core.text.csv.CsvRow;
import cn.hutool.db.StatementUtil;
import cn.hutool.core.text.csv.CsvReader;
import cn.hutool.core.text.csv.CsvUtil;
import cn.hutool.core.util.StrUtil;
import org.wiki.entity.ImportConfig;
import org.wiki.entity.ImportResult;

import java.io.File;
import java.sql.*;
import java.util.*;

// 通用数据导入服务
public class DataImportService {
    
    private Connection conn;
    
    public DataImportService(Connection conn) {
        this.conn = conn;
    }
    
    // 从CSV文件导入
    public ImportResult importFromCsv(String csvFilePath, ImportConfig config) throws Exception {
        System.out.println("========== 开始导入数据 ==========");
        System.out.println("文件路径：" + csvFilePath);
        System.out.println("目标表：" + config.getTableName());
        System.out.println("批次大小：" + config.getBatchSize());
        
        long startTime = System.currentTimeMillis();
        ImportResult result = new ImportResult();

        CsvReader reader = CsvUtil.getReader();
        // 读取为字符串数组列表
        CsvData data = reader.read(new File(csvFilePath));
        // 获取表头
        List<String> headers1 = data.getHeader();
        System.out.println("表头: " + headers1);

        // 获取行数据
        List<CsvRow> rows = data.getRows();
        for (CsvRow row : rows) {
            // 获取每一行的数据，可以按列名或索引获取
            String name = row.getByName("name");
            String age = row.getByName("age");
            String city = row.getByName("city");
            System.out.println("行数据: " + row);
            System.out.println("姓名: " + name + ", 年龄: " + age + ", 城市: " + city);
        }


        if (rows.isEmpty()) {
            System.out.println("文件为空");
            return result;
        }
        
        // 第一行是列名（如果配置中没有指定列名）
        if (config.getColumns().isEmpty()) {
            String[] headers = rows.get(0).toArray(new String[0]);
            for (String header : headers) {
                config.addColumn(header.trim());
            }
            rows.remove(0);  // 移除表头行
        }
        
        result.setTotalCount(rows.size());
        System.out.println("数据行数：" + rows.size());
        System.out.println("列名：" + config.getColumns());
        
        // 构建SQL
        String sql = buildInsertSql(config);
        System.out.println("SQL语句：" + sql);
        
        // 关闭自动提交
        boolean autoCommit = conn.getAutoCommit();
        conn.setAutoCommit(false);
        
        try {
            // 分批导入
            int batchCount = (int) Math.ceil((double) rows.size() / config.getBatchSize());
            System.out.println("\n共分 " + batchCount + " 批导入");
            
            for (int i = 0; i < batchCount; i++) {
                int fromIndex = i * config.getBatchSize();
                int toIndex = Math.min((i + 1) * config.getBatchSize(), rows.size());
                List<CsvRow> batchRows = rows.subList(fromIndex, toIndex);
                
                System.out.println("\n===== 第 " + (i + 1) + " 批 =====");
                System.out.println("数据范围：" + (fromIndex + 1) + " - " + toIndex);
                
                // 执行批量导入
                int batchSuccess = importBatch(sql, batchRows, config, result);
                result.setSuccessCount(result.getSuccessCount() + batchSuccess);
                
                System.out.println("成功：" + batchSuccess + " 条");
            }
            
            // 提交事务
            conn.commit();
            System.out.println("\n事务提交成功");
            
        } catch (Exception e) {
            // 回滚事务
            conn.rollback();
            System.err.println("\n导入失败，事务已回滚：" + e.getMessage());
            throw e;
            
        } finally {
            // 恢复自动提交
            conn.setAutoCommit(autoCommit);
        }
        
        result.setFailCount(result.getTotalCount() - result.getSuccessCount());
        result.setCostTime(System.currentTimeMillis() - startTime);
        
        return result;
    }
    
    // 构建INSERT SQL
    private String buildInsertSql(ImportConfig config) {
        StringBuilder sql = new StringBuilder("INSERT INTO ");
        sql.append(config.getTableName()).append(" (");
        sql.append(String.join(", ", config.getColumns()));
        sql.append(") VALUES (");
        
        for (int i = 0; i < config.getColumns().size(); i++) {
            if (i > 0) {
                sql.append(", ");
            }
            sql.append("?");
        }
        sql.append(")");
        
        return sql.toString();
    }
    
    // 批量导入
    private int importBatch(String sql, List<CsvRow> rows, ImportConfig config, ImportResult result) {
        int successCount = 0;
        
        try {
            // 准备批量参数
            List<Object[]> batchParams = new ArrayList<>();
            
            for (int i = 0; i < rows.size(); i++) {
                String[] row = rows.get(i).toArray(new String[0]);
                
                if (row.length != config.getColumns().size()) {
                    String error = "第 " + (i + 1) + " 行列数不匹配，期望 " + 
                                  config.getColumns().size() + " 列，实际 " + row.length + " 列";
                    result.addError(error);
                    
                    if (!config.isSkipError()) {
                        throw new IllegalArgumentException(error);
                    }
                    continue;
                }
                
                // 转换数据类型
                Object[] params = convertRowData(row, config);
                batchParams.add(params);
            }
            
            if (batchParams.isEmpty()) {
                return 0;
            }
            
            // 使用StatementUtil创建批量PreparedStatement
            PreparedStatement ps = StatementUtil.prepareStatementForBatch(
                conn, 
                sql, 
                batchParams.toArray(new Object[0][])
            );
            
            try {
                // 执行批量操作
                int[] results = ps.executeBatch();
                
                // 统计成功数量
                for (int r : results) {
                    if (r > 0 || r == Statement.SUCCESS_NO_INFO) {
                        successCount++;
                    }
                }
                
                // 获取自增主键
                List<Object> keys = StatementUtil.getGeneratedKeys(ps);
                for (Object key : keys) {
                    if (key instanceof Number) {
                        result.getGeneratedIds().add(((Number) key).longValue());
                    }
                }
                
            } finally {
                ps.close();
            }
            
        } catch (Exception e) {
            String error = "批量导入失败：" + e.getMessage();
            result.addError(error);
            System.err.println(error);
            
            if (!config.isSkipError()) {
                throw new RuntimeException(error, e);
            }
        }
        
        return successCount;
    }
    
    // 转换行数据
    private Object[] convertRowData(String[] row, ImportConfig config) {
        Object[] params = new Object[row.length];
        
        for (int i = 0; i < row.length; i++) {
            String value = row[i].trim();
            
            // 空字符串转为NULL
            if (StrUtil.isBlank(value) || "NULL".equalsIgnoreCase(value)) {
                params[i] = null;
            } else {
                // 尝试转换数字类型
                try {
                    if (value.contains(".")) {
                        params[i] = Double.parseDouble(value);
                    } else {
                        params[i] = Integer.parseInt(value);
                    }
                } catch (NumberFormatException e) {
                    // 保持字符串类型
                    params[i] = value;
                }
            }
        }
        
        return params;
    }
    
    // 打印导入结果
    public void printResult(ImportResult result) {
        System.out.println("\n========== 导入结果 ==========");
        System.out.println("总记录数：" + result.getTotalCount());
        System.out.println("成功记录：" + result.getSuccessCount());
        System.out.println("失败记录：" + result.getFailCount());
        System.out.println("成功率：" + String.format("%.2f", 
            result.getSuccessCount() * 100.0 / result.getTotalCount()) + "%");
        System.out.println("耗时：" + result.getCostTime() + " ms");
        
        if (!result.getGeneratedIds().isEmpty()) {
            System.out.println("\n生成的主键ID（前10个）：");
            int showCount = Math.min(10, result.getGeneratedIds().size());
            for (int i = 0; i < showCount; i++) {
                System.out.println("  " + (i + 1) + ". " + result.getGeneratedIds().get(i));
            }
            if (result.getGeneratedIds().size() > 10) {
                System.out.println("  ... 共 " + result.getGeneratedIds().size() + " 个");
            }
        }
        
        if (!result.getErrorMessages().isEmpty()) {
            System.out.println("\n错误信息：");
            for (String error : result.getErrorMessages()) {
                System.out.println("  - " + error);
            }
        }
    }
}

