package com.example.flowlong.controller;

import com.aizuda.bpm.engine.entity.FlwTask;
import com.example.flowlong.entity.Leave;
import com.example.flowlong.entity.User;
import com.example.flowlong.service.FlowLongService;
import com.example.flowlong.service.PermissionService;
import com.example.flowlong.service.ProcessBusinessConfigService;
import com.example.flowlong.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/flowlong")
public class FlowLongController {

    @Autowired
    private FlowLongService flowLongService;

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private UserService userService;

    @Autowired
    private ProcessBusinessConfigService processBusinessConfigService;

    @PostMapping("/deploy")
    public ResponseEntity<Map<String, Object>> deployProcess(@RequestBody Map<String, String> request) {
        try {
            permissionService.requirePermission("process:design");
            String processName = request.get("processName");
            String modelContent = request.get("modelContent");
            Map<String, Object> deployment = flowLongService.deployProcessAndRecord(processName, modelContent);
            return ResponseEntity.ok(deployment);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(error("流程部署失败: " + e.getMessage()));
        }
    }

    @PostMapping("/deploy/definition/{id}")
    public ResponseEntity<Map<String, Object>> deployDefinition(@PathVariable Long id) {
        try {
            permissionService.requirePermission("process:design");
            Map<String, Object> deployment = flowLongService.deployDefinition(id);
            return ResponseEntity.ok(deployment);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(error("流程定义部署失败: " + e.getMessage()));
        }
    }

    @PostMapping("/deploy/active/{processKey}")
    public ResponseEntity<Map<String, Object>> deployActiveDefinition(@PathVariable String processKey) {
        try {
            permissionService.requirePermission("process:design");
            Map<String, Object> deployment = flowLongService.deployActiveDefinition(processKey);
            return ResponseEntity.ok(deployment);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(error("激活版本部署失败: " + e.getMessage()));
        }
    }

    @GetMapping("/process/{processKey}/status")
    public ResponseEntity<Map<String, Object>> getProcessStatus(@PathVariable String processKey) {
        try {
            permissionService.requireLogin();
            return ResponseEntity.ok(flowLongService.getProcessStatus(processKey));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        }
    }

    @GetMapping("/process/{processKey}/business-config")
    public ResponseEntity<Map<String, Object>> getProcessBusinessConfig(@PathVariable String processKey) {
        try {
            permissionService.requireLogin();
            return ResponseEntity.ok(processBusinessConfigService.getActiveProcessBusinessConfig(processKey));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(error("获取流程业务配置失败: " + e.getMessage()));
        }
    }

    @GetMapping("/deployments/{processKey}")
    public ResponseEntity<?> getDeploymentHistory(@PathVariable String processKey) {
        try {
            permissionService.requireAnyPermission("process:design", "process:compare");
            return ResponseEntity.ok(flowLongService.getDeploymentHistory(processKey));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(error("获取部署记录失败: " + e.getMessage()));
        }
    }

    @GetMapping("/deployment-records/{deploymentId}")
    public ResponseEntity<?> getDeploymentRecordDetail(@PathVariable Long deploymentId) {
        try {
            permissionService.requireAnyPermission("process:design", "process:compare");
            return ResponseEntity.ok(flowLongService.getDeploymentRecordDetail(deploymentId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(error("获取部署详情失败: " + e.getMessage()));
        }
    }

    @PostMapping("/deployment-records/{deploymentId}/rollback")
    public ResponseEntity<?> rollbackDeployment(@PathVariable Long deploymentId) {
        try {
            permissionService.requirePermission("process:design");
            return ResponseEntity.ok(flowLongService.rollbackDeployment(deploymentId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(error("回滚部署失败: " + e.getMessage()));
        }
    }

    @PostMapping("/leave/start")
    public ResponseEntity<?> startLeaveProcess(@RequestBody Leave leave) {
        try {
            permissionService.requirePermission("leave:submit");
            User currentUser = userService.getCurrentUser();
            leave.setApplicant(currentUser == null ? leave.getApplicant() : currentUser.getUsername());
            Leave savedLeave = flowLongService.startLeaveProcess(leave);
            return ResponseEntity.ok(savedLeave);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(error("发起请假失败: " + e.getMessage()));
        }
    }

    @GetMapping("/tasks/{assignee}")
    public ResponseEntity<List<Map<String, Object>>> getTasksByAssignee(@PathVariable String assignee) {
        try {
            permissionService.requirePermission("task:approve");
            User currentUser = userService.getCurrentUser();
            if (currentUser == null) {
                throw new RuntimeException("未登录");
            }
            if (!permissionService.isAdmin(currentUser)
                    && !assignee.equals(currentUser.getUsername())
                    && !permissionService.hasPermission("monitoring:view")) {
                throw new RuntimeException("不允许查看其他人的待办任务");
            }
            List<Map<String, Object>> tasks = flowLongService.getTasksByAssignee(assignee);
            return ResponseEntity.ok(tasks);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Collections.emptyList());
        }
    }

    @PostMapping("/task/complete")
    public ResponseEntity<Map<String, Object>> completeTask(@RequestBody Map<String, Object> request) {
        try {
            permissionService.requirePermission("task:approve");
            String taskId = (String) request.get("taskId");
            Map<String, Object> variables = extractVariables(request.get("variables"));
            String processInstanceId = (String) request.get("processInstanceId");
            String status = (String) request.get("status");

            FlwTask task = flowLongService.getTaskById(taskId);
            if (task == null) {
                throw new RuntimeException("任务不存在");
            }
            User currentUser = userService.getCurrentUser();
            if (currentUser == null) {
                throw new RuntimeException("未登录");
            }
            boolean assignedToCurrentUser = flowLongService.getTasksByAssignee(currentUser.getUsername()).stream()
                    .anyMatch(item -> String.valueOf(item.get("id")).equals(taskId));
            if (!permissionService.isAdmin(currentUser) && !assignedToCurrentUser) {
                throw new RuntimeException("不允许处理其他人的任务");
            }

            flowLongService.completeTask(taskId, variables, processInstanceId, status);
            return ResponseEntity.ok(Map.of("message", "任务完成成功"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(error("任务完成失败: " + e.getMessage()));
        }
    }

    @GetMapping("/leaves")
    public ResponseEntity<List<Leave>> getAllLeaves() {
        try {
            permissionService.requirePermission("monitoring:view");
            List<Leave> leaves = flowLongService.getAllLeaves();
            return ResponseEntity.ok(leaves);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    // 根据ID获取请假申请
    @GetMapping("/leave/{id}")
    public ResponseEntity<Leave> getLeaveById(@PathVariable Long id) {
        try {
            permissionService.requireLogin();
            Leave leave = flowLongService.getLeaveById(id);
            if (leave == null) {
                return ResponseEntity.notFound().build();
            }
            User currentUser = userService.getCurrentUser();
            if (currentUser != null
                    && !permissionService.isAdmin(currentUser)
                    && !permissionService.hasPermission("monitoring:view")
                    && !currentUser.getUsername().equals(leave.getApplicant())) {
                throw new RuntimeException("不允许查看其他人的请假记录");
            }
            return ResponseEntity.ok(leave);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @GetMapping("/leaves/applicant/{applicant}")
    public ResponseEntity<List<Leave>> getLeavesByApplicant(@PathVariable String applicant) {
        try {
            permissionService.requirePermission("leave:submit");
            User currentUser = userService.getCurrentUser();
            if (currentUser == null) {
                throw new RuntimeException("未登录");
            }
            if (!permissionService.isAdmin(currentUser)
                    && !applicant.equals(currentUser.getUsername())
                    && !permissionService.hasPermission("monitoring:view")) {
                throw new RuntimeException("不允许查看其他人的请假记录");
            }
            return ResponseEntity.ok(flowLongService.getLeavesByApplicant(applicant));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    private Map<String, Object> error(String message) {
        return Map.of("error", message);
    }

    private Map<String, Object> extractVariables(Object rawVariables) {
        if (!(rawVariables instanceof Map<?, ?> source)) {
            return Collections.emptyMap();
        }
        Map<String, Object> variables = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : source.entrySet()) {
            if (entry.getKey() instanceof String key) {
                variables.put(key, entry.getValue());
            }
        }
        return variables;
    }
}
