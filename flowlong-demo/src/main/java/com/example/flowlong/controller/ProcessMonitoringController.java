package com.example.flowlong.controller;

import com.example.flowlong.common.PageResult;
import com.example.flowlong.service.PermissionService;
import com.example.flowlong.service.ProcessMonitoringService;
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
        try {
            permissionService.requirePermission("monitoring:view");
            return ResponseEntity.ok(monitoringService.getRunningProcessInstances());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @GetMapping("/processes/running/query")
    public ResponseEntity<PageResult<Map<String, Object>>> queryRunningProcessInstances(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword
    ) {
        permissionService.requirePermission("monitoring:view");
        return ResponseEntity.ok(monitoringService.queryRunningProcessInstances(page, size, keyword));
    }

    @GetMapping("/processes/completed")
    public ResponseEntity<List<Map<String, Object>>> getCompletedProcessInstances() {
        try {
            permissionService.requirePermission("monitoring:view");
            return ResponseEntity.ok(monitoringService.getCompletedProcessInstances());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @GetMapping("/processes/completed/query")
    public ResponseEntity<PageResult<Map<String, Object>>> queryCompletedProcessInstances(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword
    ) {
        permissionService.requirePermission("monitoring:view");
        return ResponseEntity.ok(monitoringService.queryCompletedProcessInstances(page, size, keyword));
    }

    @GetMapping("/processes/{processInstanceId}/history")
    public ResponseEntity<List<Map<String, Object>>> getProcessInstanceHistory(@PathVariable String processInstanceId) {
        try {
            permissionService.requirePermission("monitoring:view");
            return ResponseEntity.ok(monitoringService.getProcessInstanceHistory(processInstanceId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @GetMapping("/tasks")
    public ResponseEntity<List<Map<String, Object>>> getAllTasks() {
        try {
            permissionService.requirePermission("monitoring:view");
            return ResponseEntity.ok(monitoringService.getAllTasks());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @GetMapping("/tasks/query")
    public ResponseEntity<PageResult<Map<String, Object>>> queryTasks(
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
        try {
            permissionService.requirePermission("monitoring:view");
            return ResponseEntity.ok(monitoringService.getTasksByAssignee(assignee));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getProcessStatistics() {
        try {
            permissionService.requirePermission("monitoring:view");
            return ResponseEntity.ok(monitoringService.getProcessStatistics());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @GetMapping("/processes/{processInstanceId}/detail")
    public ResponseEntity<Map<String, Object>> getProcessDetail(@PathVariable String processInstanceId) {
        try {
            permissionService.requirePermission("monitoring:view");
            return ResponseEntity.ok(monitoringService.getProcessInstanceDetail(processInstanceId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
