package com.example.agentscope.controller;

import com.example.agentscope.service.AgentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/agent")
public class AgentController {

    @Autowired
    private AgentService agentService;

    // 处理用户请求
    @PostMapping("/process")
    public ResponseEntity<Map<String, Object>> processRequest(@RequestBody Map<String, Object> request) {
        try {
            String query = request.get("query").toString();
            List<String> history = (List<String>) request.getOrDefault("history", List.of());
            String response = agentService.processRequest(query, history);
            return ResponseEntity.ok(Map.of(
                    "response", response
            ));
        } catch (Exception e) {
            log.error("Process request failed", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // 初始化 Agent
    @PostMapping("/init")
    public ResponseEntity<Map<String, String>> initAgent() {
        try {
            agentService.initAgentScope();
            return ResponseEntity.ok(Map.of("message", "Agent 初始化成功"));
        } catch (Exception e) {
            log.error("Init agent failed", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
