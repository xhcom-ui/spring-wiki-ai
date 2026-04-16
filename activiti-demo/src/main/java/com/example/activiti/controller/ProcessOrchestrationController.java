package com.example.activiti.controller;

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

    // Orchestrate multiple processes
    @PostMapping("/orchestrate")
    public ResponseEntity<Map<String, String>> orchestrateProcesses(@RequestBody Map<String, Object> orchestrationRequest) {
        try {
            Map<String, String> results = orchestrationService.orchestrateProcesses(orchestrationRequest);
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "流程编排失败: " + e.getMessage()));
        }
    }

    // Start process with callback
    @PostMapping("/start-with-callback")
    public ResponseEntity<Map<String, String>> startProcessWithCallback(@RequestBody Map<String, Object> request) {
        try {
            String processKey = (String) request.get("processKey");
            Map<String, Object> variables = (Map<String, Object>) request.get("variables");
            String callbackProcessKey = (String) request.get("callbackProcessKey");

            String processInstanceId = orchestrationService.startProcessWithCallback(processKey, variables, callbackProcessKey);
            return ResponseEntity.ok(Map.of("processInstanceId", processInstanceId, "message", "流程启动成功"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "启动流程失败: " + e.getMessage()));
        }
    }

    // Signal process
    @PostMapping("/signal")
    public ResponseEntity<Map<String, String>> signalProcess(@RequestBody Map<String, Object> request) {
        try {
            String processInstanceId = (String) request.get("processInstanceId");
            String signalName = (String) request.get("signalName");
            Map<String, Object> variables = (Map<String, Object>) request.get("variables");

            orchestrationService.signalProcess(processInstanceId, signalName, variables);
            return ResponseEntity.ok(Map.of("message", "流程信号发送成功"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "发送信号失败: " + e.getMessage()));
        }
    }
}
