package com.example.activiti.controller;

import com.example.activiti.service.PermissionService;
import com.example.activiti.service.TaskPageCatalogService;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.ProcessDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/design")
public class ProcessDesignController {

    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private TaskPageCatalogService taskPageCatalogService;

    @PostMapping("/save")
    public ResponseEntity<Map<String, String>> saveProcessDefinition(@RequestParam("file") MultipartFile file) {
        permissionService.requirePermission("process:design");
        Deployment deployment = repositoryService.createDeployment()
                .addInputStream(file.getOriginalFilename(), openInputStream(file))
                .name(file.getOriginalFilename())
                .deploy();
        return ResponseEntity.ok(Map.of("message", "流程保存成功", "deploymentId", deployment.getId()));
    }

    @GetMapping("/definition/{processDefinitionId}")
    public ResponseEntity<Map<String, Object>> getProcessDefinition(@PathVariable String processDefinitionId) {
        permissionService.requirePermission("process:design");
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
    }

    @PutMapping("/update/{deploymentId}")
    public ResponseEntity<Map<String, String>> updateProcessDefinition(@PathVariable String deploymentId, @RequestParam("file") MultipartFile file) {
        permissionService.requirePermission("process:design");
        repositoryService.deleteDeployment(deploymentId, true);
        Deployment deployment = repositoryService.createDeployment()
                .addInputStream(file.getOriginalFilename(), openInputStream(file))
                .name(file.getOriginalFilename())
                .deploy();
        return ResponseEntity.ok(Map.of("message", "流程更新成功", "deploymentId", deployment.getId()));
    }

    @DeleteMapping("/definition/{deploymentId}")
    public ResponseEntity<Map<String, String>> deleteProcessDefinition(@PathVariable String deploymentId) {
        permissionService.requirePermission("process:design");
        repositoryService.deleteDeployment(deploymentId, true);
        return ResponseEntity.ok(Map.of("message", "流程定义删除成功"));
    }

    @GetMapping("/definitions")
    public ResponseEntity<List<ProcessDefinition>> getProcessDefinitions() {
        permissionService.requirePermission("process:design");
        List<ProcessDefinition> definitions = repositoryService.createProcessDefinitionQuery()
                .latestVersion()
                .list();
        return ResponseEntity.ok(definitions);
    }

    @GetMapping("/runtime-catalog")
    public ResponseEntity<Map<String, Object>> getRuntimeCatalog() {
        permissionService.requireLogin();
        return ResponseEntity.ok(taskPageCatalogService.getCatalog());
    }

    private InputStream openInputStream(MultipartFile file) {
        try {
            return file.getInputStream();
        } catch (IOException e) {
            throw new IllegalArgumentException("读取流程文件失败: " + e.getMessage(), e);
        }
    }
}
