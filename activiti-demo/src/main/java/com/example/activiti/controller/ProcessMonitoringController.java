package com.example.activiti.controller;

import com.example.activiti.service.ProcessMonitoringService;
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

    // Get running process instances
    @GetMapping("/processes/running")
    public ResponseEntity<List<Map<String, Object>>> getRunningProcessInstances() {
        try {
            List<Map<String, Object>> processInstances = monitoringService.getRunningProcessInstances();
            return ResponseEntity.ok(processInstances);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    // Get completed process instances
    @GetMapping("/processes/completed")
    public ResponseEntity<List<Map<String, Object>>> getCompletedProcessInstances() {
        try {
            List<Map<String, Object>> processInstances = monitoringService.getCompletedProcessInstances();
            return ResponseEntity.ok(processInstances);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    // Get process instance history
    @GetMapping("/processes/{processInstanceId}/history")
    public ResponseEntity<List<Map<String, Object>>> getProcessInstanceHistory(@PathVariable String processInstanceId) {
        try {
            List<Map<String, Object>> history = monitoringService.getProcessInstanceHistory(processInstanceId);
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    // Get all tasks
    @GetMapping("/tasks")
    public ResponseEntity<List<Map<String, Object>>> getAllTasks() {
        try {
            List<Map<String, Object>> tasks = monitoringService.getAllTasks();
            return ResponseEntity.ok(tasks);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    // Get tasks by assignee
    @GetMapping("/tasks/assignee/{assignee}")
    public ResponseEntity<List<Map<String, Object>>> getTasksByAssignee(@PathVariable String assignee) {
        try {
            List<Map<String, Object>> tasks = monitoringService.getTasksByAssignee(assignee);
            return ResponseEntity.ok(tasks);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    // Get process statistics
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getProcessStatistics() {
        try {
            Map<String, Object> statistics = monitoringService.getProcessStatistics();
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }
}
