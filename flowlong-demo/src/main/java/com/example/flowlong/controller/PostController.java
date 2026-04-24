package com.example.flowlong.controller;

import com.example.flowlong.common.PageResult;
import com.example.flowlong.controller.dto.PostManageRequest;
import com.example.flowlong.controller.dto.PostQueryRequest;
import com.example.flowlong.entity.Department;
import com.example.flowlong.entity.Post;
import com.example.flowlong.service.DepartmentService;
import com.example.flowlong.service.PermissionService;
import com.example.flowlong.service.PostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/posts")
public class PostController {

    @Autowired
    private PostService postService;

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private PermissionService permissionService;

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getPosts() {
        try {
            permissionService.requirePermission("post:manage");
            return ResponseEntity.ok(toViews(postService.getAllPosts()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @GetMapping("/query")
    public ResponseEntity<PageResult<Map<String, Object>>> queryPosts(PostQueryRequest query) {
        try {
            permissionService.requirePermission("post:manage");
            Map<Long, Department> departmentMap = departmentService.getAllDepartments().stream()
                    .collect(Collectors.toMap(Department::getId, Function.identity(), (left, right) -> left, LinkedHashMap::new));
            return ResponseEntity.ok(postService.queryPosts(query).map(post -> toView(post, departmentMap)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @GetMapping("/lookup")
    public ResponseEntity<List<Map<String, Object>>> lookupPosts() {
        try {
            permissionService.requireLogin();
            return ResponseEntity.ok(toViews(postService.getEnabledPosts()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @PostMapping
    public ResponseEntity<?> createPost(@RequestBody PostManageRequest request) {
        try {
            permissionService.requirePermission("post:manage");
            return ResponseEntity.ok(toView(postService.createPost(request)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updatePost(@PathVariable Long id, @RequestBody PostManageRequest request) {
        try {
            permissionService.requirePermission("post:manage");
            return ResponseEntity.ok(toView(postService.updatePost(id, request)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePost(@PathVariable Long id) {
        try {
            permissionService.requirePermission("post:manage");
            postService.deletePost(id);
            return ResponseEntity.ok(Map.of("message", "岗位已删除"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private List<Map<String, Object>> toViews(List<Post> posts) {
        Map<Long, Department> departmentMap = departmentService.getAllDepartments().stream()
                .collect(Collectors.toMap(Department::getId, Function.identity(), (left, right) -> left, LinkedHashMap::new));
        return posts.stream().map(post -> toView(post, departmentMap)).toList();
    }

    private Map<String, Object> toView(Post post) {
        Map<Long, Department> departmentMap = departmentService.getAllDepartments().stream()
                .collect(Collectors.toMap(Department::getId, Function.identity(), (left, right) -> left, LinkedHashMap::new));
        return toView(post, departmentMap);
    }

    private Map<String, Object> toView(Post post, Map<Long, Department> departmentMap) {
        Map<String, Object> view = new LinkedHashMap<>();
        Department department = departmentMap.get(post.getDepartmentId());
        view.put("id", post.getId());
        view.put("name", post.getName());
        view.put("code", post.getCode());
        view.put("departmentId", post.getDepartmentId());
        view.put("departmentName", department == null ? null : department.getName());
        view.put("level", post.getLevel());
        view.put("status", post.getStatus());
        view.put("description", post.getDescription());
        view.put("createdAt", post.getCreatedAt());
        view.put("updatedAt", post.getUpdatedAt());
        return view;
    }
}
