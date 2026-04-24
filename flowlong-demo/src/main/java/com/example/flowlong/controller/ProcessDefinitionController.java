package com.example.flowlong.controller;

import com.example.flowlong.controller.dto.ProcessDefinitionRequest;
import com.example.flowlong.entity.ProcessDefinitionEntity;
import com.example.flowlong.entity.User;
import com.example.flowlong.service.PermissionService;
import com.example.flowlong.service.ProcessDefinitionService;
import com.example.flowlong.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/process-definition")
public class ProcessDefinitionController {

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private UserService userService;

    private final ProcessDefinitionService processDefinitionService;

    public ProcessDefinitionController(ProcessDefinitionService processDefinitionService) {
        this.processDefinitionService = processDefinitionService;
    }

    @PostMapping("/save")
    public ResponseEntity<?> saveProcessDefinition(@RequestBody ProcessDefinitionRequest request) {
        try {
            permissionService.requirePermission("process:design");
            ProcessDefinitionEntity processDefinition = processDefinitionService.saveProcessDefinition(
                    request.getProcessKey(),
                    request.getProcessName(),
                    request.getBpmnXml(),
                    request.getDesignerType(),
                    request.getDesignSchemaJson(),
                    currentUserId()
            );
            return ResponseEntity.ok(processDefinition);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getProcessDefinitionById(@PathVariable Long id) {
        try {
            permissionService.requireLogin();
            ProcessDefinitionEntity processDefinition = processDefinitionService.getProcessDefinitionById(id);
            if (processDefinition == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(processDefinition);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/by-key/{processKey}/version/{version}")
    public ResponseEntity<?> getProcessDefinitionByKeyAndVersion(@PathVariable String processKey, @PathVariable Integer version) {
        try {
            permissionService.requirePermission("process:compare");
            ProcessDefinitionEntity processDefinition = processDefinitionService.getProcessDefinitionByKeyAndVersion(processKey, version);
            if (processDefinition == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(processDefinition);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/latest/{processKey}")
    public ResponseEntity<?> getLatestProcessDefinitionByKey(@PathVariable String processKey) {
        try {
            permissionService.requirePermission("process:compare");
            ProcessDefinitionEntity processDefinition = processDefinitionService.getLatestProcessDefinitionByKey(processKey);
            if (processDefinition == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(processDefinition);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/active/{processKey}")
    public ResponseEntity<?> getActiveProcessDefinitionByKey(@PathVariable String processKey) {
        try {
            permissionService.requireLogin();
            ProcessDefinitionEntity processDefinition = processDefinitionService.getActiveProcessDefinitionByKey(processKey);
            if (processDefinition == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(processDefinition);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/all")
    public ResponseEntity<?> getAllProcessDefinitions() {
        try {
            permissionService.requireAnyPermission("process:design", "process:compare");
            List<ProcessDefinitionEntity> processDefinitions = processDefinitionService.getAllProcessDefinitions();
            return ResponseEntity.ok(processDefinitions);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/active")
    public ResponseEntity<?> getActiveProcessDefinitions() {
        try {
            permissionService.requireLogin();
            List<ProcessDefinitionEntity> processDefinitions = processDefinitionService.getActiveProcessDefinitions();
            return ResponseEntity.ok(processDefinitions);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateProcessDefinition(@PathVariable Long id, @RequestBody ProcessDefinitionRequest request) {
        try {
            permissionService.requirePermission("process:design");
            ProcessDefinitionEntity processDefinition = processDefinitionService.updateProcessDefinition(
                    id,
                    request.getProcessName(),
                    request.getBpmnXml(),
                    request.getDesignerType(),
                    request.getDesignSchemaJson(),
                    currentUserId()
            );
            return ResponseEntity.ok(processDefinition);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/deactivate/{id}")
    public ResponseEntity<Map<String, String>> deactivateProcessDefinition(@PathVariable Long id, @RequestBody(required = false) ProcessDefinitionRequest request) {
        try {
            permissionService.requirePermission("process:design");
            processDefinitionService.deactivateProcessDefinition(id, currentUserId());
            return ResponseEntity.ok(Map.of("message", "流程定义已停用"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "停用流程定义失败: " + e.getMessage()));
        }
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Map<String, String>> deleteProcessDefinition(@PathVariable Long id) {
        try {
            permissionService.requirePermission("process:design");
            processDefinitionService.deleteProcessDefinition(id);
            return ResponseEntity.ok(Map.of("message", "流程定义已删除"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "删除流程定义失败: " + e.getMessage()));
        }
    }

    @PutMapping("/activate/{id}")
    public ResponseEntity<?> activateProcessVersion(@PathVariable Long id, @RequestBody ProcessDefinitionRequest request) {
        try {
            permissionService.requirePermission("process:design");
            ProcessDefinitionEntity processDefinition = processDefinitionService.activateProcessVersion(id, currentUserId());
            return ResponseEntity.ok(processDefinition);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/versions/{processKey}")
    public ResponseEntity<?> getVersionsByProcessKey(@PathVariable String processKey) {
        try {
            permissionService.requirePermission("process:compare");
            return ResponseEntity.ok(processDefinitionService.getVersionsByProcessKey(processKey));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/compare")
    public ResponseEntity<?> compareProcessDefinitions(@RequestParam Long leftId, @RequestParam Long rightId) {
        try {
            permissionService.requirePermission("process:compare");
            return ResponseEntity.ok(processDefinitionService.compareProcessDefinitions(leftId, rightId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private Long currentUserId() {
        User currentUser = userService.getCurrentUser();
        return currentUser == null ? null : currentUser.getId();
    }
}
