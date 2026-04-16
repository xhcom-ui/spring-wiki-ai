package org.wiki.service;

import cn.hutool.db.Entity;
import cn.hutool.db.sql.Condition;
import cn.hutool.db.sql.SqlUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import java.sql.*;
import java.util.*;

// 查询条件封装
public class QueryCriteria {
    private String tableName;
    private Map<String, Object> equalConditions = new HashMap<>();
    private Map<String, String> likeConditions = new HashMap<>();
    private String orderByField;
    private boolean orderDesc;
    private Integer pageNum;
    private Integer pageSize;
    
    public QueryCriteria(String tableName) {
        this.tableName = tableName;
    }
    
    public QueryCriteria addEqual(String field, Object value) {
        equalConditions.put(field, value);
        return this;
    }
    
    public QueryCriteria addLike(String field, String value) {
        likeConditions.put(field, value);
        return this;
    }
    
    public QueryCriteria orderBy(String field, boolean desc) {
        this.orderByField = field;
        this.orderDesc = desc;
        return this;
    }
    
    public QueryCriteria page(int pageNum, int pageSize) {
        this.pageNum = pageNum;
        this.pageSize = pageSize;
        return this;
    }
    
    // Getters
    public String getTableName() { return tableName; }
    public Map<String, Object> getEqualConditions() { return equalConditions; }
    public Map<String, String> getLikeConditions() { return likeConditions; }
    public String getOrderByField() { return orderByField; }
    public boolean isOrderDesc() { return orderDesc; }
    public Integer getPageNum() { return pageNum; }
    public Integer getPageSize() { return pageSize; }
}

