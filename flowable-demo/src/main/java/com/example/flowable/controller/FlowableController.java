package com.example.flowable.controller;

import com.example.flowable.entity.Leave;
import com.example.flowable.service.FlowableService;
import org.flowable.task.api.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/flowable")
public class FlowableController {

    @Autowired
    private FlowableService flowableService;

    // 部署流程
    @PostMapping("/deploy")
    public ResponseEntity<Map<String, String>> deployProcess(@RequestBody Map<String, String> request) {
        try {
            String processName = request.get("processName");
            String modelContent = request.get("modelContent");
            flowableService.deployProcess(processName, modelContent);
            return ResponseEntity.ok(Map.of("message", "流程部署成功"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "流程部署失败: " + e.getMessage()));
        }
    }

    // 启动请假流程
    @PostMapping("/leave/start")
    public ResponseEntity<Leave> startLeaveProcess(@RequestBody Leave leave) {
        try {
            Leave savedLeave = flowableService.startLeaveProcess(leave);
            return ResponseEntity.ok(savedLeave);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    // 查询用户的待办任务
    @GetMapping("/tasks/{assignee}")
    public ResponseEntity<List<Task>> getTasksByAssignee(@PathVariable String assignee) {
        try {
            List<Task> tasks = flowableService.getTasksByAssignee(assignee);
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

            flowableService.completeTask(taskId, variables, processInstanceId, status);
            return ResponseEntity.ok(Map.of("message", "任务完成成功"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "任务完成失败: " + e.getMessage()));
        }
    }

    // 获取所有请假申请
    @GetMapping("/leaves")
    public ResponseEntity<List<Leave>> getAllLeaves() {
        try {
            List<Leave> leaves = flowableService.getAllLeaves();
            return ResponseEntity.ok(leaves);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    // 根据ID获取请假申请
    @GetMapping("/leave/{id}")
    public ResponseEntity<Leave> getLeaveById(@PathVariable Long id) {
        try {
            Leave leave = flowableService.getLeaveById(id);
            if (leave == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(leave);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    // 从文件部署流程
    @PostMapping("/deploy/file")
    public ResponseEntity<Map<String, String>> deployProcessFromFile(@RequestBody Map<String, String> request) {
        try {
            String processName = request.get("processName");
            String resourceName = request.get("resourceName");
            flowableService.deployProcessFromFile(processName, resourceName);
            return ResponseEntity.ok(Map.of("message", "流程部署成功"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "流程部署失败: " + e.getMessage()));
        }
    }
}
