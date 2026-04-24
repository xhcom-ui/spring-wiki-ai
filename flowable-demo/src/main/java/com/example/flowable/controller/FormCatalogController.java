package com.example.flowable.controller;

import com.example.flowable.controller.dto.FormCatalogManageRequest;
import com.example.flowable.entity.FormCatalog;
import com.example.flowable.service.FormCatalogService;
import com.example.flowable.service.PermissionService;
import com.example.flowable.util.PageResponseUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/form-catalog")
public class FormCatalogController {

    @Autowired
    private FormCatalogService formCatalogService;

    @Autowired
    private PermissionService permissionService;

    @GetMapping("/active")
    public ResponseEntity<List<FormCatalog>> getActiveCatalogs() {
        permissionService.requireLogin();
        return ResponseEntity.ok(formCatalogService.getActiveCatalogs());
    }

    @GetMapping
    public ResponseEntity<List<FormCatalog>> getAllCatalogs() {
        permissionService.requirePermission("form:manage");
        return ResponseEntity.ok(formCatalogService.getAllCatalogs());
    }

    @GetMapping("/{id}/impact")
    public ResponseEntity<Map<String, Object>> getCatalogImpact(@PathVariable Long id) {
        permissionService.requirePermission("form:manage");
        return ResponseEntity.ok(formCatalogService.getUsageImpact(id));
    }

    @GetMapping("/query")
    public ResponseEntity<Map<String, Object>> queryCatalogs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String scope
    ) {
        permissionService.requirePermission("form:manage");
        List<FormCatalog> records = formCatalogService.getAllCatalogs().stream()
                .filter(item -> matchKeyword(item, keyword))
                .filter(item -> status == null || status.equals(item.getStatus()))
                .filter(item -> scope == null || scope.isBlank() || scope.equalsIgnoreCase(item.getScope()))
                .toList();
        return ResponseEntity.ok(PageResponseUtils.paginate(records, page, size));
    }

    @PostMapping
    public ResponseEntity<FormCatalog> createCatalog(@RequestBody FormCatalogManageRequest request) {
        permissionService.requirePermission("form:manage");
        return ResponseEntity.ok(formCatalogService.create(toEntity(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<FormCatalog> updateCatalog(@PathVariable Long id, @RequestBody FormCatalogManageRequest request) {
        permissionService.requirePermission("form:manage");
        return ResponseEntity.ok(formCatalogService.update(id, toEntity(request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteCatalog(@PathVariable Long id) {
        permissionService.requirePermission("form:manage");
        formCatalogService.delete(id);
        return ResponseEntity.ok(Map.of("message", "表单目录已删除"));
    }

    private FormCatalog toEntity(FormCatalogManageRequest request) {
        FormCatalog catalog = new FormCatalog();
        catalog.setFormKey(request.getFormKey());
        catalog.setFormName(request.getFormName());
        catalog.setPageLabel(request.getPageLabel());
        catalog.setComponentKey(request.getComponentKey());
        catalog.setFieldSchemaJson(request.getFieldSchemaJson());
        catalog.setScope(request.getScope());
        catalog.setDescription(request.getDescription());
        catalog.setSort(request.getSort());
        catalog.setStatus(request.getStatus());
        return catalog;
    }

    private boolean matchKeyword(FormCatalog item, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }
        String needle = keyword.trim().toLowerCase();
        return List.of(item.getFormKey(), item.getFormName(), item.getPageLabel(), item.getComponentKey(), item.getDescription())
                .stream()
                .filter(value -> value != null && !value.isBlank())
                .anyMatch(value -> value.toLowerCase().contains(needle));
    }
}
