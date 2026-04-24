package com.example.activiti.controller;

import com.example.activiti.controller.dto.ProcessDefinitionRequest;
import com.example.activiti.entity.ProcessDefinitionEntity;
import com.example.activiti.service.PermissionService;
import com.example.activiti.service.ProcessDefinitionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/process-definition")
public class ProcessDefinitionController {

    @Autowired
    private ProcessDefinitionService processDefinitionService;

    @Autowired
    private PermissionService permissionService;

    @PostMapping("/save")
    public ResponseEntity<ProcessDefinitionEntity> saveProcessDefinition(@RequestBody ProcessDefinitionRequest request) {
        permissionService.requirePermission("process:design");
        ProcessDefinitionEntity processDefinition = processDefinitionService.saveProcessDefinition(
                request.getProcessKey(),
                request.getProcessName(),
                request.getBpmnXml(),
                request.getDesignerType(),
                request.getDesignerJson(),
                request.getUserId()
        );
        return ResponseEntity.ok(processDefinition);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProcessDefinitionEntity> getProcessDefinitionById(@PathVariable Long id) {
        permissionService.requirePermission("process:design");
        ProcessDefinitionEntity processDefinition = processDefinitionService.getProcessDefinitionById(id);
        if (processDefinition == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(processDefinition);
    }

    @GetMapping("/by-key/{processKey}/version/{version}")
    public ResponseEntity<ProcessDefinitionEntity> getProcessDefinitionByKeyAndVersion(@PathVariable String processKey, @PathVariable Integer version) {
        ProcessDefinitionEntity processDefinition = processDefinitionService.getProcessDefinitionByKeyAndVersion(processKey, version);
        if (processDefinition == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(processDefinition);
    }

    @GetMapping("/latest/{processKey}")
    public ResponseEntity<ProcessDefinitionEntity> getLatestProcessDefinitionByKey(@PathVariable String processKey) {
        ProcessDefinitionEntity processDefinition = processDefinitionService.getLatestProcessDefinitionByKey(processKey);
        if (processDefinition == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(processDefinition);
    }

    @GetMapping("/versions/{processKey}")
    public ResponseEntity<?> getProcessVersions(@PathVariable String processKey) {
        permissionService.requirePermission("process:compare");
        return ResponseEntity.ok(processDefinitionService.getVersionsByProcessKey(processKey));
    }

    @GetMapping("/all")
    public ResponseEntity<List<ProcessDefinitionEntity>> getAllProcessDefinitions() {
        permissionService.requirePermission("process:design");
        return ResponseEntity.ok(processDefinitionService.getAllProcessDefinitions());
    }

    @GetMapping("/active")
    public ResponseEntity<List<ProcessDefinitionEntity>> getActiveProcessDefinitions() {
        permissionService.requireLogin();
        return ResponseEntity.ok(processDefinitionService.getActiveProcessDefinitions());
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<ProcessDefinitionEntity> updateProcessDefinition(@PathVariable Long id, @RequestBody ProcessDefinitionRequest request) {
        permissionService.requirePermission("process:design");
        ProcessDefinitionEntity processDefinition = processDefinitionService.updateProcessDefinition(
                id,
                request.getProcessName(),
                request.getBpmnXml(),
                request.getDesignerType(),
                request.getDesignerJson(),
                request.getUserId()
        );
        return ResponseEntity.ok(processDefinition);
    }

    @PutMapping("/deactivate/{id}")
    public ResponseEntity<?> deactivateProcessDefinition(@PathVariable Long id) {
        permissionService.requirePermission("process:design");
        processDefinitionService.deactivateProcessDefinition(id);
        return ResponseEntity.ok(java.util.Map.of("message", "流程定义已停用"));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteProcessDefinition(@PathVariable Long id) {
        permissionService.requirePermission("process:design");
        processDefinitionService.deleteProcessDefinition(id);
        return ResponseEntity.ok(java.util.Map.of("message", "流程定义已删除"));
    }

    @PutMapping("/activate/{id}")
    public ResponseEntity<ProcessDefinitionEntity> activateProcessVersion(@PathVariable Long id, @RequestBody ProcessDefinitionRequest request) {
        permissionService.requirePermission("process:design");
        ProcessDefinitionEntity processDefinition = processDefinitionService.activateProcessVersion(id, request.getUserId());
        return ResponseEntity.ok(processDefinition);
    }

    @GetMapping("/compare")
    public ResponseEntity<?> compareProcessVersions(@RequestParam Long leftId, @RequestParam Long rightId) {
        permissionService.requirePermission("process:compare");
        return ResponseEntity.ok(processDefinitionService.compareProcessDefinitions(leftId, rightId));
    }
}
