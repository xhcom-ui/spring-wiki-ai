package org.wiki.entity;

import java.util.ArrayList;
import java.util.List;

// 导入配置
public class ImportConfig {
    private String tableName;
    private List<String> columns;
    private int batchSize;
    private boolean skipError;
    
    public ImportConfig(String tableName) {
        this.tableName = tableName;
        this.columns = new ArrayList<>();
        this.batchSize = 1000;
        this.skipError = false;
    }
    
    public String getTableName() { return tableName; }
    public List<String> getColumns() { return columns; }
    public int getBatchSize() { return batchSize; }
    public void setBatchSize(int batchSize) { this.batchSize = batchSize; }
    public boolean isSkipError() { return skipError; }
    public void setSkipError(boolean skipError) { this.skipError = skipError; }
    
    public ImportConfig addColumn(String column) {
        this.columns.add(column);
        return this;
    }
}