package com.example.flowable.controller;

import com.example.flowable.controller.dto.ProcessDefinitionRequest;
import com.example.flowable.entity.ProcessDefinitionEntity;
import com.example.flowable.exception.NotFoundException;
import com.example.flowable.service.PermissionService;
import com.example.flowable.service.ProcessDefinitionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/process-definition")
public class ProcessDefinitionController {

    private final PermissionService permissionService;
    private final ProcessDefinitionService processDefinitionService;

    public ProcessDefinitionController(PermissionService permissionService, ProcessDefinitionService processDefinitionService) {
        this.permissionService = permissionService;
        this.processDefinitionService = processDefinitionService;
    }

    @PostMapping("/save")
    public ResponseEntity<ProcessDefinitionEntity> saveProcessDefinition(@RequestBody ProcessDefinitionRequest request) {
        permissionService.requirePermission("process:design");
        ProcessDefinitionEntity processDefinition = processDefinitionService.saveProcessDefinition(
                request.getProcessKey(),
                request.getProcessName(),
                request.getBpmnXml(),
                request.getDesignerType(),
                request.getDesignSchemaJson(),
                request.getUserId()
        );
        return ResponseEntity.ok(processDefinition);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProcessDefinitionEntity> getProcessDefinitionById(@PathVariable Long id) {
        permissionService.requireLogin();
        ProcessDefinitionEntity processDefinition = processDefinitionService.getProcessDefinitionById(id);
        if (processDefinition == null) {
            throw new NotFoundException("流程定义不存在");
        }
        return ResponseEntity.ok(processDefinition);
    }

    @GetMapping("/by-key/{processKey}/version/{version}")
    public ResponseEntity<ProcessDefinitionEntity> getProcessDefinitionByKeyAndVersion(@PathVariable String processKey, @PathVariable Integer version) {
        permissionService.requirePermission("process:compare");
        ProcessDefinitionEntity processDefinition = processDefinitionService.getProcessDefinitionByKeyAndVersion(processKey, version);
        if (processDefinition == null) {
            throw new NotFoundException("流程定义不存在");
        }
        return ResponseEntity.ok(processDefinition);
    }

    @GetMapping("/latest/{processKey}")
    public ResponseEntity<ProcessDefinitionEntity> getLatestProcessDefinitionByKey(@PathVariable String processKey) {
        permissionService.requirePermission("process:compare");
        ProcessDefinitionEntity processDefinition = processDefinitionService.getLatestProcessDefinitionByKey(processKey);
        if (processDefinition == null) {
            throw new NotFoundException("流程定义不存在");
        }
        return ResponseEntity.ok(processDefinition);
    }

    @GetMapping("/all")
    public ResponseEntity<List<ProcessDefinitionEntity>> getAllProcessDefinitions() {
        permissionService.requirePermission("process:compare");
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
                request.getDesignSchemaJson(),
                request.getUserId()
        );
        return ResponseEntity.ok(processDefinition);
    }

    @PutMapping("/deactivate/{id}")
    public ResponseEntity<Map<String, String>> deactivateProcessDefinition(@PathVariable Long id, @RequestBody(required = false) ProcessDefinitionRequest request) {
        permissionService.requirePermission("process:design");
        processDefinitionService.deactivateProcessDefinition(id, request != null ? request.getUserId() : null);
        return ResponseEntity.ok(Map.of("message", "流程定义已停用"));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Map<String, String>> deleteProcessDefinition(@PathVariable Long id) {
        permissionService.requirePermission("process:design");
        processDefinitionService.deleteProcessDefinition(id);
        return ResponseEntity.ok(Map.of("message", "流程定义已删除"));
    }

    @PutMapping("/activate/{id}")
    public ResponseEntity<ProcessDefinitionEntity> activateProcessVersion(@PathVariable Long id, @RequestBody ProcessDefinitionRequest request) {
        permissionService.requirePermission("process:design");
        return ResponseEntity.ok(processDefinitionService.activateProcessVersion(id, request.getUserId()));
    }

    @GetMapping("/versions/{processKey}")
    public ResponseEntity<List<ProcessDefinitionEntity>> getVersionsByProcessKey(@PathVariable String processKey) {
        permissionService.requirePermission("process:compare");
        return ResponseEntity.ok(processDefinitionService.getVersionsByProcessKey(processKey));
    }

    @GetMapping("/compare")
    public ResponseEntity<Map<String, Object>> compareProcessDefinitions(@RequestParam Long leftId, @RequestParam Long rightId) {
        permissionService.requirePermission("process:compare");
        return ResponseEntity.ok(processDefinitionService.compareProcessDefinitions(leftId, rightId));
    }
}
