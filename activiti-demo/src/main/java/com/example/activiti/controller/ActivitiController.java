package com.example.activiti.controller;

import com.example.activiti.entity.Leave;
import com.example.activiti.service.ActivitiService;
import org.activiti.engine.task.Task;
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

    // 部署流程
    @PostMapping("/deploy")
    public ResponseEntity<Map<String, String>> deployProcess() {
        try {
            activitiService.deployProcess();
            return ResponseEntity.ok(Map.of("message", "流程部署成功"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "流程部署失败: " + e.getMessage()));
        }
    }

    // 启动请假流程
    @PostMapping("/leave/start")
    public ResponseEntity<Leave> startLeaveProcess(@RequestBody Leave leave) {
        try {
            Leave savedLeave = activitiService.startLeaveProcess(leave);
            return ResponseEntity.ok(savedLeave);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    // 查询用户的待办任务
    @GetMapping("/tasks/{assignee}")
    public ResponseEntity<List<Task>> getTasksByAssignee(@PathVariable String assignee) {
        try {
            List<Task> tasks = activitiService.getTasksByAssignee(assignee);
            return ResponseEntity.ok(tasks);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    // 完成任务
    @PostMapping("/task/complete")
    public ResponseEntity<Map<String, String>> completeTask(@RequestBody Map<String, Object> request) {
        try {
            String taskId = (String) request.get("taskId");
            Map<String, Object> variables = (Map<String, Object>) request.get("variables");
            String processInstanceId = (String) request.get("processInstanceId");
            String status = (String) request.get("status");

            activitiService.completeTask(taskId, variables);
            if (processInstanceId != null && status != null) {
                activitiService.updateLeaveStatus(processInstanceId, status);
            }

            return ResponseEntity.ok(Map.of("message", "任务完成成功"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "任务完成失败: " + e.getMessage()));
        }
    }

    // 查询所有请假申请
    @GetMapping("/leaves")
    public ResponseEntity<List<Leave>> getAllLeaves() {
        try {
            List<Leave> leaves = activitiService.getAllLeaves();
            return ResponseEntity.ok(leaves);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    // 根据ID查询请假申请
    @GetMapping("/leave/{id}")
    public ResponseEntity<Leave> getLeaveById(@PathVariable Long id) {
        try {
            Leave leave = activitiService.getLeaveById(id);
            if (leave == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(leave);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    // 查询流程实例状态
    @GetMapping("/process/status/{processInstanceId}")
    public ResponseEntity<Map<String, String>> getProcessInstanceStatus(@PathVariable String processInstanceId) {
        try {
            String status = activitiService.getProcessInstanceStatus(processInstanceId);
            return ResponseEntity.ok(Map.of("status", status));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "查询流程状态失败: " + e.getMessage()));
        }
    }
}
