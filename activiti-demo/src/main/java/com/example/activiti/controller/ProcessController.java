package com.example.activiti.controller;

import com.example.activiti.service.ActivitiService;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.repository.Deployment; 
import org.activiti.engine.repository.ProcessDefinition; 
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/process")
public class ProcessController {

    @Autowired
    private ActivitiService activitiService;

    @Autowired
    private RepositoryService repositoryService;

    // 部署流程
    @PostMapping("/deploy")
    public ResponseEntity<Map<String, String>> deployProcess(@RequestParam("file") MultipartFile file) {
        try {
            Deployment deployment = repositoryService.createDeployment()
                    .addInputStream(file.getOriginalFilename(), file.getInputStream())
                    .name(file.getOriginalFilename())
                    .deploy();
            return ResponseEntity.ok(Map.of("message", "流程部署成功", "deploymentId", deployment.getId()));
        } catch (IOException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "部署流程失败: " + e.getMessage()));
        }
    }

    // 获取流程定义列表
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

    // 删除流程定义
    @DeleteMapping("/definition/{processDefinitionId}")
    public ResponseEntity<Map<String, String>> deleteProcessDefinition(@PathVariable String processDefinitionId) {
        try {
            repositoryService.deleteDeployment(processDefinitionId, true);
            return ResponseEntity.ok(Map.of("message", "流程定义删除成功"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "删除流程定义失败: " + e.getMessage()));
        }
    }
}
