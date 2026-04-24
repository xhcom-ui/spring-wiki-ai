package com.example.flowlong.controller;

import com.example.flowlong.common.PageResult;
import com.example.flowlong.controller.dto.MenuManageRequest;
import com.example.flowlong.controller.dto.MenuQueryRequest;
import com.example.flowlong.entity.Menu;
import com.example.flowlong.service.MenuService;
import com.example.flowlong.service.PermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/menus")
public class MenuManagementController {

    @Autowired
    private MenuService menuService;

    @Autowired
    private PermissionService permissionService;

    @GetMapping
    public ResponseEntity<List<Menu>> getMenus() {
        try {
            permissionService.requirePermission("menu:manage");
            return ResponseEntity.ok(menuService.getAllMenusIncludingDisabled());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @GetMapping("/query")
    public ResponseEntity<PageResult<Menu>> queryMenus(MenuQueryRequest query) {
        try {
            permissionService.requirePermission("menu:manage");
            return ResponseEntity.ok(menuService.queryMenus(query));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @PostMapping
    public ResponseEntity<?> createMenu(@RequestBody MenuManageRequest request) {
        try {
            permissionService.requirePermission("menu:manage");
            return ResponseEntity.ok(menuService.createMenu(toEntity(request)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateMenu(@PathVariable Long id, @RequestBody MenuManageRequest request) {
        try {
            permissionService.requirePermission("menu:manage");
            return ResponseEntity.ok(menuService.updateMenu(id, toEntity(request)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteMenu(@PathVariable Long id) {
        try {
            permissionService.requirePermission("menu:manage");
            menuService.deleteMenu(id);
            return ResponseEntity.ok(Map.of("message", "菜单已删除"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private Menu toEntity(MenuManageRequest request) {
        Menu menu = new Menu();
        menu.setName(request.getName());
        menu.setPath(request.getPath());
        menu.setComponent(request.getComponent());
        menu.setIcon(request.getIcon());
        menu.setParentId(request.getParentId());
        menu.setSort(request.getSort());
        menu.setType(request.getType());
        menu.setPermission(request.getPermission());
        menu.setStatus(request.getStatus());
        return menu;
    }

}
