package org.wiki.service;

import java.util.List;
import java.util.Map;

// 查询结果封装
public class QueryResult {
    private long totalCount;
    private List<Map<String, Object>> dataList;
    private int pageNum;
    private int pageSize;
    
    public QueryResult(long totalCount, List<Map<String, Object>> dataList, int pageNum, int pageSize) {
        this.totalCount = totalCount;
        this.dataList = dataList;
        this.pageNum = pageNum;
        this.pageSize = pageSize;
    }
    
    public long getTotalCount() { return totalCount; }
    public List<Map<String, Object>> getDataList() { return dataList; }
    public int getPageNum() { return pageNum; }
    public int getPageSize() { return pageSize; }
    public int getTotalPages() { return (int) Math.ceil((double) totalCount / pageSize); }
}
