package com.example.activiti.controller;

import com.example.activiti.entity.ProcessDefinitionEntity;
import com.example.activiti.service.ProcessDefinitionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/process-definition")
public class ProcessDefinitionController {

    @Autowired
    private ProcessDefinitionService processDefinitionService;

    // Save process definition
    @PostMapping("/save")
    public ResponseEntity<ProcessDefinitionEntity> saveProcessDefinition(@RequestBody Map<String, Object> request) {
        try {
            String processKey = (String) request.get("processKey");
            String processName = (String) request.get("processName");
            String bpmnXml = (String) request.get("bpmnXml");
            Long userId = (Long) request.get("userId");

            ProcessDefinitionEntity processDefinition = processDefinitionService.saveProcessDefinition(processKey, processName, bpmnXml, userId);
            return ResponseEntity.ok(processDefinition);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    // Get process definition by ID
    @GetMapping("/{id}")
    public ResponseEntity<ProcessDefinitionEntity> getProcessDefinitionById(@PathVariable Long id) {
        try {
            ProcessDefinitionEntity processDefinition = processDefinitionService.getProcessDefinitionById(id);
            if (processDefinition == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(processDefinition);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    // Get process definition by key and version
    @GetMapping("/by-key/{processKey}/version/{version}")
    public ResponseEntity<ProcessDefinitionEntity> getProcessDefinitionByKeyAndVersion(@PathVariable String processKey, @PathVariable Integer version) {
        try {
            ProcessDefinitionEntity processDefinition = processDefinitionService.getProcessDefinitionByKeyAndVersion(processKey, version);
            if (processDefinition == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(processDefinition);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    // Get latest process definition by key
    @GetMapping("/latest/{processKey}")
    public ResponseEntity<ProcessDefinitionEntity> getLatestProcessDefinitionByKey(@PathVariable String processKey) {
        try {
            ProcessDefinitionEntity processDefinition = processDefinitionService.getLatestProcessDefinitionByKey(processKey);
            if (processDefinition == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(processDefinition);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    // Get all process definitions
    @GetMapping("/all")
    public ResponseEntity<List<ProcessDefinitionEntity>> getAllProcessDefinitions() {
        try {
            List<ProcessDefinitionEntity> processDefinitions = processDefinitionService.getAllProcessDefinitions();
            return ResponseEntity.ok(processDefinitions);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    // Get active process definitions
    @GetMapping("/active")
    public ResponseEntity<List<ProcessDefinitionEntity>> getActiveProcessDefinitions() {
        try {
            List<ProcessDefinitionEntity> processDefinitions = processDefinitionService.getActiveProcessDefinitions();
            return ResponseEntity.ok(processDefinitions);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    // Update process definition
    @PutMapping("/update/{id}")
    public ResponseEntity<ProcessDefinitionEntity> updateProcessDefinition(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        try {
            String processName = (String) request.get("processName");
            String bpmnXml = (String) request.get("bpmnXml");
            Long userId = (Long) request.get("userId");

            ProcessDefinitionEntity processDefinition = processDefinitionService.updateProcessDefinition(id, processName, bpmnXml, userId);
            return ResponseEntity.ok(processDefinition);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    // Deactivate process definition
    @PutMapping("/deactivate/{id}")
    public ResponseEntity<Map<String, String>> deactivateProcessDefinition(@PathVariable Long id) {
        try {
            processDefinitionService.deactivateProcessDefinition(id);
            return ResponseEntity.ok(Map.of("message", "流程定义已停用"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "停用流程定义失败: " + e.getMessage()));
        }
    }

    // Delete process definition
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Map<String, String>> deleteProcessDefinition(@PathVariable Long id) {
        try {
            processDefinitionService.deleteProcessDefinition(id);
            return ResponseEntity.ok(Map.of("message", "流程定义已删除"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "删除流程定义失败: " + e.getMessage()));
        }
    }

    // Activate process version
    @PutMapping("/activate/{id}")
    public ResponseEntity<ProcessDefinitionEntity> activateProcessVersion(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        try {
            Long userId = (Long) request.get("userId");
            ProcessDefinitionEntity processDefinition = processDefinitionService.activateProcessVersion(id, userId);
            return ResponseEntity.ok(processDefinition);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }
}
