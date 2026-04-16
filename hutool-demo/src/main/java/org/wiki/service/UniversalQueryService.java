package org.wiki.service;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.db.sql.Condition;
import cn.hutool.db.sql.SqlUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// 通用查询服务
public class UniversalQueryService {
    
    // 执行查询
    public QueryResult query(Connection conn, QueryCriteria criteria) throws Exception {
        // 1. 构建查询SQL
        StringBuilder sql = new StringBuilder("SELECT * FROM ").append(criteria.getTableName());
        List<Object> params = new ArrayList<>();
        
        // 2. 添加WHERE条件
        String whereClause = buildWhereClause(criteria, params);
        if (StrUtil.isNotBlank(whereClause)) {
            sql.append(" ").append(whereClause);
        }
        
        // 3. 添加ORDER BY
        if (StrUtil.isNotBlank(criteria.getOrderByField())) {
            sql.append(" ORDER BY ").append(criteria.getOrderByField());
            sql.append(criteria.isOrderDesc() ? " DESC" : " ASC");
        }
        
        System.out.println("========== 查询SQL构建 ==========");
        System.out.println("原始SQL：");
        System.out.println(sql.toString());
        
        // 格式化SQL用于日志
        String formattedSql = SqlUtil.formatSql(sql.toString());
        System.out.println("\n格式化SQL：");
        System.out.println(formattedSql);
        
        // 4. 查询总数
        long totalCount = queryCount(conn, sql.toString(), params);
        System.out.println("\n查询结果总数：" + totalCount);
        
        // 5. 分页查询
        List<Map<String, Object>> dataList = queryData(conn, sql.toString(), params, criteria);
        
        return new QueryResult(totalCount, dataList, 
            criteria.getPageNum() != null ? criteria.getPageNum() : 1,
            criteria.getPageSize() != null ? criteria.getPageSize() : 10);
    }
    
    // 构建WHERE子句
    private String buildWhereClause(QueryCriteria criteria, List<Object> params) {
        List<String> conditions = new ArrayList<>();
        
        // 等值条件
        criteria.getEqualConditions().forEach((field, value) -> {
            conditions.add(field + " = ?");
            params.add(value);
        });
        
        // 模糊查询条件
        criteria.getLikeConditions().forEach((field, value) -> {
            String likeValue = SqlUtil.buildLikeValue(value, Condition.LikeType.Contains, false);
            conditions.add(field + " LIKE ?");
            params.add(likeValue);
        });
        
        if (conditions.isEmpty()) {
            return "";
        }
        
        return "WHERE " + String.join(" AND ", conditions);
    }
    
    // 查询总数
    private long queryCount(Connection conn, String sql, List<Object> params) throws Exception {
        // 移除ORDER BY提高查询效率
        String countSql = SqlUtil.removeOuterOrderBy(sql);
        countSql = "SELECT COUNT(*) FROM (" + countSql + ") t";
        
        try (PreparedStatement ps = conn.prepareStatement(countSql)) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        return 0;
    }
    
    // 查询数据
    private List<Map<String, Object>> queryData(Connection conn, String sql, 
                                                  List<Object> params, QueryCriteria criteria) throws Exception {
        // 添加分页
        if (criteria.getPageNum() != null && criteria.getPageSize() != null) {
            int offset = (criteria.getPageNum() - 1) * criteria.getPageSize();
            sql += " LIMIT ? OFFSET ?";
        }
        
        List<Map<String, Object>> resultList = new ArrayList<>();
        
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            // 设置参数
            int paramIndex = 1;
            for (Object param : params) {
                ps.setObject(paramIndex++, param);
            }
            
            // 设置分页参数
            if (criteria.getPageNum() != null && criteria.getPageSize() != null) {
                ps.setInt(paramIndex++, criteria.getPageSize());
                ps.setInt(paramIndex, (criteria.getPageNum() - 1) * criteria.getPageSize());
            }
            
            try (ResultSet rs = ps.executeQuery()) {
                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();
                
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    
                    for (int i = 1; i <= columnCount; i++) {
                        String columnName = metaData.getColumnName(i);
                        Object value = rs.getObject(i);
                        
                        // 处理特殊类型
                        if (value instanceof Blob) {
                            value = SqlUtil.blobToStr((Blob) value, java.nio.charset.StandardCharsets.UTF_8);
                        } else if (value instanceof Clob) {
                            value = SqlUtil.clobToStr((Clob) value);
                        } else if (value instanceof Timestamp) {
                            value = DateUtil.format((Timestamp) value, "yyyy-MM-dd HH:mm:ss");
                        } else if (value instanceof java.sql.Date) {
                            value = DateUtil.format((java.sql.Date) value, "yyyy-MM-dd");
                        }
                        
                        row.put(columnName, value);
                    }
                    
                    resultList.add(row);
                }
            }
        }
        
        return resultList;
    }
}

