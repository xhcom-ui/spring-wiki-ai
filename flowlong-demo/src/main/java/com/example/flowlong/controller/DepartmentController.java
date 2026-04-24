package com.example.flowlong.controller;

import com.example.flowlong.common.PageResult;
import com.example.flowlong.controller.dto.DepartmentManageRequest;
import com.example.flowlong.controller.dto.DepartmentQueryRequest;
import com.example.flowlong.entity.Department;
import com.example.flowlong.service.DepartmentService;
import com.example.flowlong.service.PermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/departments")
public class DepartmentController {

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private PermissionService permissionService;

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getDepartments() {
        try {
            permissionService.requirePermission("department:manage");
            return ResponseEntity.ok(departmentService.getAllDepartments().stream().map(this::toView).toList());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @GetMapping("/query")
    public ResponseEntity<PageResult<Map<String, Object>>> queryDepartments(DepartmentQueryRequest query) {
        try {
            permissionService.requirePermission("department:manage");
            return ResponseEntity.ok(departmentService.queryDepartments(query).map(this::toView));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @GetMapping("/lookup")
    public ResponseEntity<List<Map<String, Object>>> lookupDepartments() {
        try {
            permissionService.requireLogin();
            return ResponseEntity.ok(departmentService.getEnabledDepartments().stream().map(this::toView).toList());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @PostMapping
    public ResponseEntity<?> createDepartment(@RequestBody DepartmentManageRequest request) {
        try {
            permissionService.requirePermission("department:manage");
            return ResponseEntity.ok(toView(departmentService.createDepartment(request)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateDepartment(@PathVariable Long id, @RequestBody DepartmentManageRequest request) {
        try {
            permissionService.requirePermission("department:manage");
            return ResponseEntity.ok(toView(departmentService.updateDepartment(id, request)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteDepartment(@PathVariable Long id) {
        try {
            permissionService.requirePermission("department:manage");
            departmentService.deleteDepartment(id);
            return ResponseEntity.ok(Map.of("message", "部门已删除"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private Map<String, Object> toView(Department department) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", department.getId());
        view.put("name", department.getName());
        view.put("code", department.getCode());
        view.put("leader", department.getLeader());
        view.put("parentId", department.getParentId() != null && department.getParentId() > 0 ? department.getParentId() : null);
        view.put("status", department.getStatus());
        view.put("description", department.getDescription());
        view.put("createdAt", department.getCreatedAt());
        view.put("updatedAt", department.getUpdatedAt());
        return view;
    }
}
