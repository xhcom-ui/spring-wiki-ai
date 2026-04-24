package com.example.flowable.controller;

import com.example.flowable.controller.dto.RoleManageRequest;
import com.example.flowable.entity.Menu;
import com.example.flowable.entity.Role;
import com.example.flowable.service.PermissionService;
import com.example.flowable.service.RoleService;
import com.example.flowable.util.PageResponseUtils;
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
        permissionService.requirePermission("role:manage");
        return ResponseEntity.ok(roleService.getAllRoles().stream().map(this::toRoleView).toList());
    }

    @GetMapping("/query")
    public ResponseEntity<Map<String, Object>> queryRoles(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status
    ) {
        permissionService.requirePermission("role:manage");
        List<Map<String, Object>> records = roleService.getAllRoles().stream()
                .map(this::toRoleView)
                .filter(item -> matchKeyword(item, keyword))
                .filter(item -> status == null || status.equals(item.get("status")))
                .toList();
        return ResponseEntity.ok(PageResponseUtils.paginate(records, page, size));
    }

    @GetMapping("/options")
    public ResponseEntity<List<Map<String, Object>>> getRoleOptions() {
        permissionService.requireLogin();
        return ResponseEntity.ok(roleService.getAllRoles().stream().map(this::toRoleView).toList());
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createRole(@RequestBody RoleManageRequest request) {
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
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateRole(@PathVariable Long id, @RequestBody RoleManageRequest request) {
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
    }

    @PutMapping("/{id}/menus")
    public ResponseEntity<Map<String, Object>> assignMenus(@PathVariable Long id, @RequestBody RoleManageRequest request) {
        permissionService.requirePermission("menu:manage");
        return ResponseEntity.ok(toRoleView(roleService.assignMenus(id, request.getMenuIds())));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteRole(@PathVariable Long id) {
        permissionService.requirePermission("role:manage");
        roleService.deleteRole(id);
        return ResponseEntity.ok(Map.of("message", "角色已删除"));
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

    private boolean matchKeyword(Map<String, Object> item, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }
        String needle = keyword.trim().toLowerCase();
        return String.valueOf(item.get("name")).toLowerCase().contains(needle)
                || String.valueOf(item.get("code")).toLowerCase().contains(needle)
                || String.valueOf(item.get("description")).toLowerCase().contains(needle);
    }
}
