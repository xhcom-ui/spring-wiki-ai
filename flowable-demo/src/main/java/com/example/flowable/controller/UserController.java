package com.example.flowable.controller;

import com.example.flowable.controller.dto.UserManageRequest;
import com.example.flowable.entity.Role;
import com.example.flowable.entity.User;
import com.example.flowable.service.PermissionService;
import com.example.flowable.service.UserService;
import com.example.flowable.util.PageResponseUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private PermissionService permissionService;

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getUsers() {
        permissionService.requirePermission("user:manage");
        return ResponseEntity.ok(userService.getAllUsers().stream().map(this::toUserView).toList());
    }

    @GetMapping("/query")
    public ResponseEntity<Map<String, Object>> queryUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status
    ) {
        permissionService.requirePermission("user:manage");
        List<Map<String, Object>> records = userService.getAllUsers().stream()
                .map(this::toUserView)
                .filter(item -> matchKeyword(item, keyword))
                .filter(item -> status == null || status.equals(item.get("status")))
                .toList();
        return ResponseEntity.ok(PageResponseUtils.paginate(records, page, size));
    }

    @GetMapping("/lookup")
    public ResponseEntity<List<Map<String, Object>>> lookupUsers() {
        permissionService.requireLogin();
        return ResponseEntity.ok(userService.getAllUsers().stream().map(this::toUserLiteView).toList());
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createUser(@RequestBody UserManageRequest request) {
        permissionService.requirePermission("user:manage");
        User user = userService.createUser(
                request.getUsername(),
                request.getPassword(),
                request.getNickname(),
                request.getEmail(),
                request.getPhone(),
                request.getStatus(),
                request.getRoleIds()
        );
        return ResponseEntity.ok(toUserView(user));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateUser(@PathVariable Long id, @RequestBody UserManageRequest request) {
        permissionService.requirePermission("user:manage");
        User user = userService.updateUser(
                id,
                request.getPassword(),
                request.getNickname(),
                request.getEmail(),
                request.getPhone(),
                request.getStatus(),
                request.getRoleIds()
        );
        return ResponseEntity.ok(toUserView(user));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteUser(@PathVariable Long id) {
        permissionService.requirePermission("user:manage");
        userService.deleteUser(id);
        return ResponseEntity.ok(Map.of("message", "用户已删除"));
    }

    private Map<String, Object> toUserView(User user) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", user.getId());
        view.put("username", user.getUsername());
        view.put("nickname", user.getNickname());
        view.put("email", user.getEmail());
        view.put("phone", user.getPhone());
        view.put("status", user.getStatus());
        List<Role> roles = user.getRoles() == null ? List.of() : user.getRoles();
        view.put("roleIds", roles.stream().map(Role::getId).toList());
        view.put("roleNames", roles.stream().map(Role::getName).toList());
        view.put("roles", new ArrayList<>(roles.stream().map(role -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", role.getId());
            item.put("name", role.getName());
            item.put("code", role.getCode());
            return item;
        }).toList()));
        return view;
    }

    private Map<String, Object> toUserLiteView(User user) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", user.getId());
        view.put("username", user.getUsername());
        view.put("nickname", user.getNickname());
        view.put("status", user.getStatus());
        return view;
    }

    private boolean matchKeyword(Map<String, Object> item, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }
        String needle = keyword.trim().toLowerCase();
        return String.valueOf(item.get("username")).toLowerCase().contains(needle)
                || String.valueOf(item.get("nickname")).toLowerCase().contains(needle)
                || String.valueOf(item.get("email")).toLowerCase().contains(needle)
                || String.valueOf(item.get("phone")).toLowerCase().contains(needle);
    }
}
