package com.example.flowable.controller;

import com.example.flowable.service.PermissionService;
import com.example.flowable.service.ProcessMonitoringService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/monitoring")
public class ProcessMonitoringController {

    @Autowired
    private ProcessMonitoringService monitoringService;

    @Autowired
    private PermissionService permissionService;

    @GetMapping("/processes/running")
    public ResponseEntity<List<Map<String, Object>>> getRunningProcessInstances() {
        permissionService.requirePermission("monitoring:view");
        return ResponseEntity.ok(monitoringService.getRunningProcessInstances());
    }

    @GetMapping("/processes/running/query")
    public ResponseEntity<Map<String, Object>> queryRunningProcessInstances(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword
    ) {
        permissionService.requirePermission("monitoring:view");
        return ResponseEntity.ok(monitoringService.queryRunningProcessInstances(page, size, keyword));
    }

    @GetMapping("/processes/completed")
    public ResponseEntity<List<Map<String, Object>>> getCompletedProcessInstances() {
        permissionService.requirePermission("monitoring:view");
        return ResponseEntity.ok(monitoringService.getCompletedProcessInstances());
    }

    @GetMapping("/processes/completed/query")
    public ResponseEntity<Map<String, Object>> queryCompletedProcessInstances(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword
    ) {
        permissionService.requirePermission("monitoring:view");
        return ResponseEntity.ok(monitoringService.queryCompletedProcessInstances(page, size, keyword));
    }

    @GetMapping("/processes/{processInstanceId}/history")
    public ResponseEntity<List<Map<String, Object>>> getProcessInstanceHistory(@PathVariable String processInstanceId) {
        permissionService.requirePermission("monitoring:view");
        return ResponseEntity.ok(monitoringService.getProcessInstanceHistory(processInstanceId));
    }

    @GetMapping("/tasks")
    public ResponseEntity<List<Map<String, Object>>> getAllTasks() {
        permissionService.requirePermission("monitoring:view");
        return ResponseEntity.ok(monitoringService.getAllTasks());
    }

    @GetMapping("/tasks/query")
    public ResponseEntity<Map<String, Object>> queryTasks(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String assigneeFilter
    ) {
        permissionService.requirePermission("monitoring:view");
        return ResponseEntity.ok(monitoringService.queryTasks(page, size, keyword, assigneeFilter));
    }

    @GetMapping("/tasks/assignee/{assignee}")
    public ResponseEntity<List<Map<String, Object>>> getTasksByAssignee(@PathVariable String assignee) {
        permissionService.requirePermission("monitoring:view");
        return ResponseEntity.ok(monitoringService.getTasksByAssignee(assignee));
    }

    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getProcessStatistics() {
        permissionService.requirePermission("monitoring:view");
        return ResponseEntity.ok(monitoringService.getProcessStatistics());
    }

    @GetMapping("/processes/{processInstanceId}/detail")
    public ResponseEntity<Map<String, Object>> getProcessDetail(@PathVariable String processInstanceId) {
        permissionService.requirePermission("monitoring:view");
        return ResponseEntity.ok(monitoringService.getProcessInstanceDetail(processInstanceId));
    }
}
