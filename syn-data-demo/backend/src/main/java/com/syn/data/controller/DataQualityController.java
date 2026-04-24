package com.syn.data.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.syn.data.service.DataQualityService;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.Map;
import java.util.List;

/**
 * 数据质量管理控制器
 */
@RestController
@RequestMapping("/api/data-quality")
@SaCheckLogin
public class DataQualityController {

    @Resource
    private DataQualityService dataQualityService;

    /**
     * 执行数量校验
     */
    @PostMapping("/validate/count")
    public DataQualityService.ValidationResult validateCount(
            @RequestParam long sourceCount, 
            @RequestParam long targetCount) {
        return dataQualityService.validateCount(sourceCount, targetCount);
    }

    /**
     * 执行质量校验
     */
    @PostMapping("/validate/quality")
    public DataQualityService.ValidationResult validateQuality(
            @RequestBody Map<String, Object> data) {
        Map<String, Object> sourceData = (Map<String, Object>) data.get("sourceData");
        Map<String, Object> targetData = (Map<String, Object>) data.get("targetData");
        return dataQualityService.validateQuality(sourceData, targetData);
    }

    /**
     * 执行格式校验
     */
    @PostMapping("/validate/format")
    public DataQualityService.ValidationResult validateFormat(
            @RequestBody Map<String, Object> data) {
        return dataQualityService.validateFormat(data);
    }

    /**
     * 执行业务规则校验
     */
    @PostMapping("/validate/business")
    public DataQualityService.ValidationResult validateBusinessRules(
            @RequestBody Map<String, Object> data) {
        return dataQualityService.validateBusinessRules(data);
    }

    /**
     * 执行完整的数据校验
     */
    @PostMapping("/validate/full")
    public DataQualityService.ValidationResult validateData(
            @RequestBody Map<String, Object> data) {
        long sourceCount = (long) data.get("sourceCount");
        long targetCount = (long) data.get("targetCount");
        Map<String, Object> sourceData = (Map<String, Object>) data.get("sourceData");
        Map<String, Object> targetData = (Map<String, Object>) data.get("targetData");
        return dataQualityService.validateData(sourceCount, targetCount, sourceData, targetData);
    }

    /**
     * 获取所有问题
     */
    @GetMapping("/issues")
    public List<DataQualityService.ValidationIssue> getIssues(
            @RequestParam(required = false) String status) {
        return dataQualityService.getIssues(status);
    }

    /**
     * 根据ID获取问题
     */
    @GetMapping("/issues/{id}")
    public DataQualityService.ValidationIssue getIssueById(@PathVariable String id) {
        return dataQualityService.getIssueById(id);
    }

    /**
     * 修复问题
     */
    @PostMapping("/issues/{id}/fix")
    public DataQualityService.FixResult fixIssue(
            @PathVariable String id, 
            @RequestParam String fixStrategy) {
        return dataQualityService.fixIssue(id, fixStrategy);
    }

    /**
     * 批量修复问题
     */
    @PostMapping("/issues/batch-fix")
    public DataQualityService.FixResult fixIssues(
            @RequestBody List<String> issueIds, 
            @RequestParam String fixStrategy) {
        return dataQualityService.fixIssues(issueIds, fixStrategy);
    }

    /**
     * 忽略问题
     */
    @PostMapping("/issues/{id}/ignore")
    public boolean ignoreIssue(@PathVariable String id) {
        return dataQualityService.ignoreIssue(id);
    }

    /**
     * 获取问题统计数据
     */
    @GetMapping("/issues/stats")
    public Map<String, Object> getIssueStats(@RequestParam String timeRange) {
        return dataQualityService.getIssueStats(timeRange);
    }

    /**
     * 导出问题报告
     */
    @PostMapping("/export")
    public Map<String, Object> exportIssueReport(
            @RequestParam String timeRange, 
            @RequestParam String format) {
        return dataQualityService.exportIssueReport(timeRange, format);
    }

}
