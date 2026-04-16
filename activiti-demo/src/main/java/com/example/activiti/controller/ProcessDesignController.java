package com.example.activiti.controller;

import org.activiti.engine.RepositoryService;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.ProcessDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/design")
public class ProcessDesignController {

    @Autowired
    private RepositoryService repositoryService;

    // Save process definition
    @PostMapping("/save")
    public ResponseEntity<Map<String, String>> saveProcessDefinition(@RequestParam("file") MultipartFile file) {
        try {
            Deployment deployment = repositoryService.createDeployment()
                    .addInputStream(file.getOriginalFilename(), file.getInputStream())
                    .name(file.getOriginalFilename())
                    .deploy();
            return ResponseEntity.ok(Map.of("message", "流程保存成功", "deploymentId", deployment.getId()));
        } catch (IOException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "保存流程失败: " + e.getMessage()));
        }
    }

    // Get process definition by ID
    @GetMapping("/definition/{processDefinitionId}")
    public ResponseEntity<Map<String, Object>> getProcessDefinition(@PathVariable String processDefinitionId) {
        try {
            ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                    .processDefinitionId(processDefinitionId)
                    .singleResult();
            if (processDefinition == null) {
                return ResponseEntity.notFound().build();
            }
            Map<String, Object> result = new HashMap<>();
            result.put("id", processDefinition.getId());
            result.put("key", processDefinition.getKey());
            result.put("name", processDefinition.getName());
            result.put("version", processDefinition.getVersion());
            result.put("deploymentId", processDefinition.getDeploymentId());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "获取流程定义失败: " + e.getMessage()));
        }
    }

    // Update process definition
    @PutMapping("/update/{deploymentId}")
    public ResponseEntity<Map<String, String>> updateProcessDefinition(@PathVariable String deploymentId, @RequestParam("file") MultipartFile file) {
        try {
            // First delete the existing deployment
            repositoryService.deleteDeployment(deploymentId, true);
            // Then deploy the new version
            Deployment deployment = repositoryService.createDeployment()
                    .addInputStream(file.getOriginalFilename(), file.getInputStream())
                    .name(file.getOriginalFilename())
                    .deploy();
            return ResponseEntity.ok(Map.of("message", "流程更新成功", "deploymentId", deployment.getId()));
        } catch (IOException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "更新流程失败: " + e.getMessage()));
        }
    }

    // Delete process definition
    @DeleteMapping("/definition/{deploymentId}")
    public ResponseEntity<Map<String, String>> deleteProcessDefinition(@PathVariable String deploymentId) {
        try {
            repositoryService.deleteDeployment(deploymentId, true);
            return ResponseEntity.ok(Map.of("message", "流程定义删除成功"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "删除流程定义失败: " + e.getMessage()));
        }
    }

    // Get all process definitions
    @GetMapping("/definitions")
    public ResponseEntity<List<ProcessDefinition>> getProcessDefinitions() {
        try {
            List<ProcessDefinition> definitions = repositoryService.createProcessDefinitionQuery()
                    .latestVersion()
                    .list();
            return ResponseEntity.ok(definitions);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }
}
