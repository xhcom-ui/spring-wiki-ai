package com.example.flowable.controller;

import com.example.flowable.entity.Leave;
import com.example.flowable.entity.User;
import com.example.flowable.exception.ForbiddenException;
import com.example.flowable.exception.NotFoundException;
import com.example.flowable.exception.UnauthorizedException;
import com.example.flowable.service.FlowableService;
import com.example.flowable.service.PermissionService;
import com.example.flowable.service.ProcessDesignRuntimeService;
import com.example.flowable.service.UserService;
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

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private UserService userService;

    @Autowired
    private ProcessDesignRuntimeService processDesignRuntimeService;

    @PostMapping("/deploy")
    public ResponseEntity<Map<String, String>> deployProcess(@RequestBody Map<String, String> request) {
        permissionService.requirePermission("process:design");
        flowableService.deployProcess(request.get("processName"), request.get("modelContent"));
        return ResponseEntity.ok(Map.of("message", "流程部署成功"));
    }

    @PostMapping("/leave/start")
    public ResponseEntity<Leave> startLeaveProcess(@RequestBody Leave leave) {
        permissionService.requirePermission("leave:submit");
        User currentUser = userService.getCurrentUser();
        leave.setApplicant(currentUser == null ? leave.getApplicant() : currentUser.getUsername());
        return ResponseEntity.ok(flowableService.startLeaveProcess(leave));
    }

    @GetMapping("/tasks/{assignee}")
    public ResponseEntity<List<Map<String, Object>>> getTasksByAssignee(@PathVariable String assignee) {
        permissionService.requirePermission("task:approve");
        User currentUser = requireCurrentUser();
        if (!permissionService.isAdmin(currentUser)
                && !assignee.equals(currentUser.getUsername())
                && !permissionService.hasPermission("monitoring:view")) {
            throw new ForbiddenException("不允许查看其他人的待办任务");
        }
        return ResponseEntity.ok(flowableService.getTasksForUser(currentUser));
    }

    @GetMapping("/runtime-config/{processKey}")
    public ResponseEntity<Map<String, Object>> getRuntimeConfig(@PathVariable String processKey) {
        permissionService.requireLogin();
        return ResponseEntity.ok(processDesignRuntimeService.buildLaunchConfig(processKey));
    }

    @PostMapping("/task/complete")
    public ResponseEntity<Map<String, String>> completeTask(@RequestBody Map<String, Object> request) {
        permissionService.requirePermission("task:approve");
        String taskId = (String) request.get("taskId");
        String outcome = (String) request.get("outcome");
        String comment = (String) request.get("comment");
        Map<String, Object> variables = castVariables(request.get("variables"));
        String processInstanceId = (String) request.get("processInstanceId");
        Task task = flowableService.getTaskById(taskId);
        if (task == null) {
            throw new NotFoundException("任务不存在");
        }
        User currentUser = requireCurrentUser();
        if (!permissionService.isAdmin(currentUser)
                && task.getAssignee() != null
                && !task.getAssignee().equals(currentUser.getUsername())) {
            throw new ForbiddenException("不允许处理其他人的任务");
        }
        flowableService.claimTaskIfNecessary(taskId, currentUser.getUsername());
        Map<String, Object> completionVariables = processDesignRuntimeService.buildTaskCompletionVariables(task, outcome, comment, variables);
        String status = flowableService.completeTask(taskId, completionVariables, processInstanceId, outcome, currentUser.getUsername(), comment);
        return ResponseEntity.ok(Map.of("message", "任务完成成功", "status", status == null ? "" : status));
    }

    @GetMapping("/leaves")
    public ResponseEntity<List<Leave>> getAllLeaves() {
        permissionService.requirePermission("monitoring:view");
        return ResponseEntity.ok(flowableService.getAllLeaves());
    }

    @GetMapping("/leave/{id}")
    public ResponseEntity<Leave> getLeaveById(@PathVariable Long id) {
        permissionService.requireLogin();
        Leave leave = flowableService.getLeaveById(id);
        if (leave == null) {
            throw new NotFoundException("请假记录不存在");
        }
        return ResponseEntity.ok(leave);
    }

    @PostMapping("/deploy/file")
    public ResponseEntity<Map<String, String>> deployProcessFromFile(@RequestBody Map<String, String> request) {
        permissionService.requirePermission("process:design");
        flowableService.deployProcessFromFile(request.get("processName"), request.get("resourceName"));
        return ResponseEntity.ok(Map.of("message", "流程部署成功"));
    }

    @GetMapping("/leaves/applicant/{applicant}")
    public ResponseEntity<List<Leave>> getLeavesByApplicant(@PathVariable String applicant) {
        permissionService.requirePermission("leave:submit");
        User currentUser = requireCurrentUser();
        if (!permissionService.isAdmin(currentUser)
                && !applicant.equals(currentUser.getUsername())
                && !permissionService.hasPermission("monitoring:view")) {
            throw new ForbiddenException("不允许查看其他人的请假记录");
        }
        return ResponseEntity.ok(flowableService.getLeavesByApplicant(applicant));
    }

    private User requireCurrentUser() {
        User currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            throw new UnauthorizedException("未登录");
        }
        return currentUser;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castVariables(Object rawValue) {
        if (rawValue instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of();
    }
}
