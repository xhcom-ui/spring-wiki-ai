package com.example.activiti.controller;

import com.example.activiti.entity.TaskPageCatalogItem;
import com.example.activiti.service.PermissionService;
import com.example.activiti.service.TaskPageCatalogService;
import com.example.activiti.util.PageResponseUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/runtime-catalog")
public class TaskPageCatalogController {

    @Autowired
    private TaskPageCatalogService taskPageCatalogService;

    @Autowired
    private PermissionService permissionService;

    @GetMapping("/catalog")
    public ResponseEntity<?> getCatalog() {
        permissionService.requireLogin();
        return ResponseEntity.ok(taskPageCatalogService.getCatalog());
    }

    @GetMapping("/items")
    public ResponseEntity<?> getItems() {
        permissionService.requirePermission("runtime:catalog:manage");
        return ResponseEntity.ok(taskPageCatalogService.getAllItems());
    }

    @GetMapping("/items/{id}/references")
    public ResponseEntity<?> getItemReferences(@PathVariable Long id) {
        permissionService.requirePermission("runtime:catalog:manage");
        return ResponseEntity.ok(taskPageCatalogService.getItemReferences(id));
    }

    @GetMapping("/items/query")
    public ResponseEntity<?> queryItems(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String itemType,
            @RequestParam(required = false) String pageMode,
            @RequestParam(required = false) Integer status
    ) {
        permissionService.requirePermission("runtime:catalog:manage");
        List<TaskPageCatalogItem> records = taskPageCatalogService.getAllItems().stream()
                .filter(item -> matchKeyword(item, keyword))
                .filter(item -> itemType == null || itemType.isBlank() || itemType.equalsIgnoreCase(item.getItemType()))
                .filter(item -> pageMode == null || pageMode.isBlank() || pageMode.equalsIgnoreCase(item.getPageMode()))
                .filter(item -> status == null || status.equals(item.getStatus()))
                .toList();
        return ResponseEntity.ok(PageResponseUtils.paginate(records, page, size));
    }

    @PostMapping("/items")
    public ResponseEntity<?> createItem(@RequestBody TaskPageCatalogItem request) {
        permissionService.requirePermission("runtime:catalog:manage");
        return ResponseEntity.ok(taskPageCatalogService.createItem(request));
    }

    @PutMapping("/items/{id}")
    public ResponseEntity<?> updateItem(@PathVariable Long id, @RequestBody TaskPageCatalogItem request) {
        permissionService.requirePermission("runtime:catalog:manage");
        return ResponseEntity.ok(taskPageCatalogService.updateItem(id, request));
    }

    @DeleteMapping("/items/{id}")
    public ResponseEntity<?> deleteItem(@PathVariable Long id) {
        permissionService.requirePermission("runtime:catalog:manage");
        taskPageCatalogService.deleteItem(id);
        return ResponseEntity.ok(Map.of("message", "目录项已删除"));
    }

    private boolean matchKeyword(TaskPageCatalogItem item, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }
        String needle = keyword.trim().toLowerCase();
        return List.of(
                        item.getItemKey(),
                        item.getLabel(),
                        item.getDescription(),
                        item.getBusinessKind(),
                        item.getDefaultTodoPage(),
                        item.getDefaultDonePage()
                ).stream()
                .filter(value -> value != null && !value.isBlank())
                .anyMatch(value -> value.toLowerCase().contains(needle));
    }
}
