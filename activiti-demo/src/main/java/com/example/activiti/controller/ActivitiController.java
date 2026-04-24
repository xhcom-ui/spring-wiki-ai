package com.example.activiti.controller;

import com.example.activiti.controller.dto.TaskInboxItem;
import com.example.activiti.entity.Leave;
import com.example.activiti.service.ActivitiService;
import com.example.activiti.service.PermissionService;
import com.example.activiti.service.ProcessMonitoringService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/activiti")
public class ActivitiController {

    @Autowired
    private ActivitiService activitiService;

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private ProcessMonitoringService processMonitoringService;

    @PostMapping("/deploy")
    public ResponseEntity<Map<String, String>> deployProcess() {
        permissionService.requirePermission("process:design");
        activitiService.deployProcess();
        return ResponseEntity.ok(Map.of("message", "流程部署成功"));
    }

    @PostMapping("/leave/start")
    public ResponseEntity<Leave> startLeaveProcess(@RequestBody Leave leave) {
        permissionService.requirePermission("leave:submit");
        return ResponseEntity.ok(activitiService.startLeaveProcess(leave));
    }

    @GetMapping("/tasks/{assignee}")
    public ResponseEntity<List<Map<String, Object>>> getTasksByAssignee(@PathVariable String assignee) {
        permissionService.requirePermission("task:approve");
        return ResponseEntity.ok(processMonitoringService.getTasksByAssignee(assignee));
    }

    @GetMapping("/tasks/inbox")
    public ResponseEntity<List<TaskInboxItem>> getCurrentUserInbox() {
        permissionService.requirePermission("task:approve");
        return ResponseEntity.ok(activitiService.getCurrentUserInbox());
    }

    @GetMapping("/task/{taskId}")
    public ResponseEntity<?> getCurrentUserTask(@PathVariable String taskId) {
        permissionService.requirePermission("task:approve");
        return ResponseEntity.ok(activitiService.getCurrentUserTask(taskId));
    }

    @PostMapping("/task/claim")
    public ResponseEntity<Map<String, String>> claimTask(@RequestBody Map<String, Object> request) {
        permissionService.requirePermission("task:approve");
        activitiService.claimTask((String) request.get("taskId"));
        return ResponseEntity.ok(Map.of("message", "任务签收成功"));
    }

    @PostMapping("/task/complete")
    @SuppressWarnings("unchecked")
    public ResponseEntity<Map<String, String>> completeTask(@RequestBody Map<String, Object> request) {
        permissionService.requirePermission("task:approve");
        String taskId = (String) request.get("taskId");
        Map<String, Object> variables = (Map<String, Object>) request.get("variables");
        String processInstanceId = (String) request.get("processInstanceId");
        String status = (String) request.get("status");

        activitiService.completeTask(taskId, variables);
        if (processInstanceId != null && status != null) {
            activitiService.updateLeaveStatus(processInstanceId, status);
        }

        return ResponseEntity.ok(Map.of("message", "任务完成成功"));
    }

    @GetMapping("/leaves")
    public ResponseEntity<List<Leave>> getAllLeaves() {
        permissionService.requirePermission("monitoring:view");
        return ResponseEntity.ok(activitiService.getAllLeaves());
    }

    @GetMapping("/leaves/applicant/{applicant}")
    public ResponseEntity<List<Leave>> getLeavesByApplicant(@PathVariable String applicant) {
        permissionService.requirePermission("leave:submit");
        return ResponseEntity.ok(activitiService.getLeavesByApplicant(applicant));
    }

    @GetMapping("/leave/{id}")
    public ResponseEntity<Leave> getLeaveById(@PathVariable Long id) {
        permissionService.requirePermission("leave:submit");
        Leave leave = activitiService.getLeaveById(id);
        if (leave == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(leave);
    }

    @GetMapping("/process/status/{processInstanceId}")
    public ResponseEntity<Map<String, String>> getProcessInstanceStatus(@PathVariable String processInstanceId) {
        permissionService.requirePermission("monitoring:view");
        return ResponseEntity.ok(Map.of("status", activitiService.getProcessInstanceStatus(processInstanceId)));
    }
}
