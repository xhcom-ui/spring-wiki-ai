package org.wiki.entity;

import java.util.ArrayList;
import java.util.List;

// 导入结果
public class ImportResult {
    private int totalCount;
    private int successCount;
    private int failCount;
    private List<String> errorMessages;
    private List<Long> generatedIds;
    private long costTime;
    
    public ImportResult() {
        this.errorMessages = new ArrayList<>();
        this.generatedIds = new ArrayList<>();
    }
    
    public int getTotalCount() { return totalCount; }
    public void setTotalCount(int totalCount) { this.totalCount = totalCount; }
    public int getSuccessCount() { return successCount; }
    public void setSuccessCount(int successCount) { this.successCount = successCount; }
    public int getFailCount() { return failCount; }
    public void setFailCount(int failCount) { this.failCount = failCount; }
    public List<String> getErrorMessages() { return errorMessages; }
    public List<Long> getGeneratedIds() { return generatedIds; }
    public long getCostTime() { return costTime; }
    public void setCostTime(long costTime) { this.costTime = costTime; }
    
    public void addError(String error) {
        this.errorMessages.add(error);
    }
}