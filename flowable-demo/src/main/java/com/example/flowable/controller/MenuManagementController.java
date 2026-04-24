package com.example.flowable.controller;

import com.example.flowable.controller.dto.MenuManageRequest;
import com.example.flowable.entity.Menu;
import com.example.flowable.service.MenuService;
import com.example.flowable.service.PermissionService;
import com.example.flowable.util.PageResponseUtils;
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
        permissionService.requirePermission("menu:manage");
        return ResponseEntity.ok(menuService.getAllMenusIncludingDisabled());
    }

    @GetMapping("/options")
    public ResponseEntity<List<Menu>> getMenuOptions() {
        permissionService.requirePermission("role:manage");
        return ResponseEntity.ok(menuService.getAllMenusIncludingDisabled());
    }

    @GetMapping("/query")
    public ResponseEntity<Map<String, Object>> queryMenus(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status
    ) {
        permissionService.requirePermission("menu:manage");
        List<Menu> records = menuService.getAllMenusIncludingDisabled().stream()
                .filter(menu -> matchKeyword(menu, keyword))
                .filter(menu -> status == null || status.equals(menu.getStatus()))
                .toList();
        return ResponseEntity.ok(PageResponseUtils.paginate(records, page, size));
    }

    @PostMapping
    public ResponseEntity<Menu> createMenu(@RequestBody MenuManageRequest request) {
        permissionService.requirePermission("menu:manage");
        return ResponseEntity.ok(menuService.createMenu(toEntity(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Menu> updateMenu(@PathVariable Long id, @RequestBody MenuManageRequest request) {
        permissionService.requirePermission("menu:manage");
        return ResponseEntity.ok(menuService.updateMenu(id, toEntity(request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteMenu(@PathVariable Long id) {
        permissionService.requirePermission("menu:manage");
        menuService.deleteMenu(id);
        return ResponseEntity.ok(Map.of("message", "菜单已删除"));
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

    private boolean matchKeyword(Menu menu, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }
        String needle = keyword.trim().toLowerCase();
        return String.valueOf(menu.getName()).toLowerCase().contains(needle)
                || String.valueOf(menu.getPath()).toLowerCase().contains(needle)
                || String.valueOf(menu.getPermission()).toLowerCase().contains(needle);
    }
}
