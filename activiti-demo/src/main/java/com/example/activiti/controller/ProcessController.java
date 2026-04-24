package com.example.activiti.controller;

import com.example.activiti.service.PermissionService;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.ProcessDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/process")
public class ProcessController {

    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    private PermissionService permissionService;

    @PostMapping("/deploy")
    public ResponseEntity<Map<String, String>> deployProcess(@RequestParam("file") MultipartFile file) {
        permissionService.requirePermission("process:design");
        Deployment deployment = repositoryService.createDeployment()
                .addInputStream(file.getOriginalFilename(), openInputStream(file))
                .name(file.getOriginalFilename())
                .deploy();
        return ResponseEntity.ok(Map.of("message", "流程部署成功", "deploymentId", deployment.getId()));
    }

    @GetMapping("/definitions")
    public ResponseEntity<List<ProcessDefinition>> getProcessDefinitions() {
        permissionService.requirePermission("process:design");
        List<ProcessDefinition> definitions = repositoryService.createProcessDefinitionQuery()
                .latestVersion()
                .list();
        return ResponseEntity.ok(definitions);
    }

    @DeleteMapping("/definition/{processDefinitionId}")
    public ResponseEntity<Map<String, String>> deleteProcessDefinition(@PathVariable String processDefinitionId) {
        permissionService.requirePermission("process:design");
        repositoryService.deleteDeployment(processDefinitionId, true);
        return ResponseEntity.ok(Map.of("message", "流程定义删除成功"));
    }

    private InputStream openInputStream(MultipartFile file) {
        try {
            return file.getInputStream();
        } catch (IOException e) {
            throw new IllegalArgumentException("读取流程文件失败: " + e.getMessage(), e);
        }
    }
}
