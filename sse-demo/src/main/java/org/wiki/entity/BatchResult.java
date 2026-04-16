package org.wiki.entity;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.util.ArrayList;
import java.util.List;

/**
 * 批量操作结果封装
 * 用于批量发送消息、批量处理等场景
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class BatchResult {
    /**
     * 总记录数
     */
    private Integer total = 0;
    
    /**
     * 成功数
     */
    private Integer successCount = 0;
    
    /**
     * 失败数
     */
    private Integer failureCount = 0;
    
    /**
     * 成功记录ID列表
     */
    private List<String> successIds = new ArrayList<>();
    
    /**
     * 失败记录列表
     */
    private List<FailureRecord> failureRecords = new ArrayList<>();
    
    /**
     * 失败记录详情
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class FailureRecord {
        /**
         * 记录ID
         */
        private String recordId;
        
        /**
         * 失败原因
         */
        private String errorMsg;
        
        /**
         * 失败时间戳
         */
        private Long failureTime = System.currentTimeMillis();

        public FailureRecord(String recordId, String errorMsg) {
            this.recordId = recordId;
            this.errorMsg = errorMsg;
        }
    }
    
    /**
     * 添加成功记录
     */
    public void addSuccess(String recordId) {
        this.successIds.add(recordId);
        this.successCount++;
        this.total++;
    }
    
    /**
     * 添加失败记录
     */
    public void addFailure(String recordId, String errorMsg) {
        FailureRecord failureRecord = new FailureRecord(recordId, errorMsg);
        this.failureRecords.add(failureRecord);
        this.failureCount++;
        this.total++;
    }
    
    /**
     * 批量添加成功记录
     */
    public void addSuccesses(List<String> recordIds) {
        this.successIds.addAll(recordIds);
        this.successCount += recordIds.size();
        this.total += recordIds.size();
    }
    
    /**
     * 批量添加失败记录
     */
    public void addFailures(List<FailureRecord> failures) {
        this.failureRecords.addAll(failures);
        this.failureCount += failures.size();
        this.total += failures.size();
    }
    
    /**
     * 判断是否全部成功
     */
    public boolean isAllSuccess() {
        return failureCount == 0 && total > 0;
    }
    
    /**
     * 判断是否全部失败
     */
    public boolean isAllFailure() {
        return successCount == 0 && total > 0;
    }
    
    /**
     * 获取成功率
     */
    public double getSuccessRate() {
        if (total == 0) {
            return 0.0;
        }
        return (double) successCount / total * 100;
    }
    
    /**
     * 获取失败率
     */
    public double getFailureRate() {
        if (total == 0) {
            return 0.0;
        }
        return (double) failureCount / total * 100;
    }
    
    /**
     * 合并多个批量结果
     */
    public static BatchResult merge(List<BatchResult> results) {
        BatchResult mergedResult = new BatchResult();
        
        for (BatchResult result : results) {
            mergedResult.total += result.total;
            mergedResult.successCount += result.successCount;
            mergedResult.failureCount += result.failureCount;
            mergedResult.successIds.addAll(result.successIds);
            mergedResult.failureRecords.addAll(result.failureRecords);
        }
        
        return mergedResult;
    }

    
    /**
     * 转换为字符串表示
     */
    @Override
    public String toString() {
        return String.format("BatchResult{total=%d, success=%d, failure=%d, successRate=%.2f%%}", 
            total, successCount, failureCount, getSuccessRate());
    }
}