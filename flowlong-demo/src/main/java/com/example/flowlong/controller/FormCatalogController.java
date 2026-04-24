package com.example.flowlong.controller;

import com.example.flowlong.common.PageResult;
import com.example.flowlong.controller.dto.FormCatalogManageRequest;
import com.example.flowlong.controller.dto.FormCatalogQueryRequest;
import com.example.flowlong.entity.FormCatalog;
import com.example.flowlong.service.FormCatalogService;
import com.example.flowlong.service.PermissionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/form-catalogs")
public class FormCatalogController {

    private final FormCatalogService formCatalogService;
    private final PermissionService permissionService;

    public FormCatalogController(FormCatalogService formCatalogService, PermissionService permissionService) {
        this.formCatalogService = formCatalogService;
        this.permissionService = permissionService;
    }

    @GetMapping("/runtime")
    public ResponseEntity<?> getRuntimeCatalogs() {
        try {
            permissionService.requireLogin();
            return ResponseEntity.ok(formCatalogService.getRuntimeCatalogViews());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/runtime/{formKey}")
    public ResponseEntity<?> getRuntimeCatalog(@PathVariable String formKey) {
        try {
            permissionService.requireLogin();
            FormCatalog formCatalog = formCatalogService.getCatalogByFormKey(formKey);
            if (formCatalog == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(formCatalogService.toRuntimeView(formCatalog));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/query")
    public ResponseEntity<?> queryCatalogs(FormCatalogQueryRequest query) {
        try {
            permissionService.requirePermission("form:manage");
            PageResult<FormCatalog> result = formCatalogService.queryCatalogs(query);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<?> createCatalog(@RequestBody FormCatalogManageRequest request) {
        try {
            permissionService.requirePermission("form:manage");
            return ResponseEntity.ok(formCatalogService.createCatalog(toEntity(request)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateCatalog(@PathVariable Long id, @RequestBody FormCatalogManageRequest request) {
        try {
            permissionService.requirePermission("form:manage");
            return ResponseEntity.ok(formCatalogService.updateCatalog(id, toEntity(request)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCatalog(@PathVariable Long id) {
        try {
            permissionService.requirePermission("form:manage");
            formCatalogService.deleteCatalog(id);
            return ResponseEntity.ok(Map.of("message", "表单目录已删除"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private FormCatalog toEntity(FormCatalogManageRequest request) {
        FormCatalog formCatalog = new FormCatalog();
        formCatalog.setFormKey(request.getFormKey());
        formCatalog.setTitle(request.getTitle());
        formCatalog.setDescription(request.getDescription());
        formCatalog.setSchemaJson(request.getSchemaJson());
        formCatalog.setStatus(request.getStatus());
        return formCatalog;
    }
}
