package com.example.flowlong.controller;

import com.example.flowlong.common.PageResult;
import com.example.flowlong.controller.dto.RoleManageRequest;
import com.example.flowlong.controller.dto.RoleQueryRequest;
import com.example.flowlong.entity.Menu;
import com.example.flowlong.entity.Role;
import com.example.flowlong.service.PermissionService;
import com.example.flowlong.service.RoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/roles")
public class RoleController {

    @Autowired
    private RoleService roleService;

    @Autowired
    private PermissionService permissionService;

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getRoles() {
        try {
            permissionService.requirePermission("role:manage");
            return ResponseEntity.ok(roleService.getAllRoles().stream().map(this::toRoleView).toList());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @GetMapping("/query")
    public ResponseEntity<PageResult<Map<String, Object>>> queryRoles(RoleQueryRequest query) {
        try {
            permissionService.requirePermission("role:manage");
            return ResponseEntity.ok(roleService.queryRoles(query).map(this::toRoleView));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @GetMapping("/options")
    public ResponseEntity<List<Map<String, Object>>> getRoleOptions() {
        try {
            permissionService.requireLogin();
            return ResponseEntity.ok(roleService.getAllRoles().stream().map(this::toRoleView).toList());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @PostMapping
    public ResponseEntity<?> createRole(@RequestBody RoleManageRequest request) {
        try {
            permissionService.requirePermission("role:manage");
            Role role = new Role();
            role.setName(request.getName());
            role.setCode(request.getCode());
            role.setDescription(request.getDescription());
            role.setStatus(request.getStatus());
            Role created = roleService.createRole(role);
            if (request.getMenuIds() != null) {
                created = roleService.assignMenus(created.getId(), request.getMenuIds());
            }
            return ResponseEntity.ok(toRoleView(created));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateRole(@PathVariable Long id, @RequestBody RoleManageRequest request) {
        try {
            permissionService.requirePermission("role:manage");
            Role role = new Role();
            role.setName(request.getName());
            role.setCode(request.getCode());
            role.setDescription(request.getDescription());
            role.setStatus(request.getStatus());
            Role updated = roleService.updateRole(id, role);
            if (request.getMenuIds() != null) {
                updated = roleService.assignMenus(id, request.getMenuIds());
            }
            return ResponseEntity.ok(toRoleView(updated));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}/menus")
    public ResponseEntity<?> assignMenus(@PathVariable Long id, @RequestBody RoleManageRequest request) {
        try {
            permissionService.requirePermission("menu:manage");
            Role updated = roleService.assignMenus(id, request.getMenuIds());
            return ResponseEntity.ok(toRoleView(updated));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteRole(@PathVariable Long id) {
        try {
            permissionService.requirePermission("role:manage");
            roleService.deleteRole(id);
            return ResponseEntity.ok(Map.of("message", "角色已删除"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private Map<String, Object> toRoleView(Role role) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", role.getId());
        view.put("name", role.getName());
        view.put("code", role.getCode());
        view.put("description", role.getDescription());
        view.put("status", role.getStatus());
        view.put("createdAt", role.getCreatedAt());
        view.put("updatedAt", role.getUpdatedAt());
        List<Menu> menus = role.getMenus() == null ? List.of() : role.getMenus();
        view.put("menuIds", menus.stream().map(Menu::getId).toList());
        view.put("menuNames", menus.stream().map(Menu::getName).toList());
        return view;
    }

}
