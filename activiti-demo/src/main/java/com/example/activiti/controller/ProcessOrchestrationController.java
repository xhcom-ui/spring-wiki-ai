package com.example.activiti.controller;

import com.example.activiti.service.PermissionService;
import com.example.activiti.service.ProcessOrchestrationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/orchestration")
public class ProcessOrchestrationController {

    @Autowired
    private ProcessOrchestrationService orchestrationService;

    @Autowired
    private PermissionService permissionService;

    @PostMapping("/orchestrate")
    public ResponseEntity<Map<String, String>> orchestrateProcesses(@RequestBody Map<String, Object> orchestrationRequest) {
        permissionService.requirePermission("orchestration:manage");
        return ResponseEntity.ok(orchestrationService.orchestrateProcesses(orchestrationRequest));
    }

    @PostMapping("/start-with-callback")
    @SuppressWarnings("unchecked")
    public ResponseEntity<Map<String, String>> startProcessWithCallback(@RequestBody Map<String, Object> request) {
        permissionService.requirePermission("orchestration:manage");
        String processKey = (String) request.get("processKey");
        Map<String, Object> variables = (Map<String, Object>) request.get("variables");
        String callbackProcessKey = (String) request.get("callbackProcessKey");

        String processInstanceId = orchestrationService.startProcessWithCallback(processKey, variables, callbackProcessKey);
        return ResponseEntity.ok(Map.of("processInstanceId", processInstanceId, "message", "流程启动成功"));
    }

    @PostMapping("/signal")
    @SuppressWarnings("unchecked")
    public ResponseEntity<Map<String, String>> signalProcess(@RequestBody Map<String, Object> request) {
        permissionService.requirePermission("orchestration:manage");
        String processInstanceId = (String) request.get("processInstanceId");
        String signalName = (String) request.get("signalName");
        Map<String, Object> variables = (Map<String, Object>) request.get("variables");

        orchestrationService.signalProcess(processInstanceId, signalName, variables);
        return ResponseEntity.ok(Map.of("message", "流程信号发送成功"));
    }
}
