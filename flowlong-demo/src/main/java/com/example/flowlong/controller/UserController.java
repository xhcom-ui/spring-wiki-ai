package com.example.flowlong.controller;

import com.example.flowlong.common.PageResult;
import com.example.flowlong.controller.dto.UserManageRequest;
import com.example.flowlong.controller.dto.UserQueryRequest;
import com.example.flowlong.entity.Role;
import com.example.flowlong.entity.User;
import com.example.flowlong.service.PermissionService;
import com.example.flowlong.service.UserService;
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
        try {
            permissionService.requirePermission("user:manage");
            return ResponseEntity.ok(userService.getAllUsers().stream().map(this::toUserView).toList());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @GetMapping("/query")
    public ResponseEntity<PageResult<Map<String, Object>>> queryUsers(UserQueryRequest query) {
        try {
            permissionService.requirePermission("user:manage");
            return ResponseEntity.ok(userService.queryUsers(query).map(this::toUserView));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @GetMapping("/lookup")
    public ResponseEntity<List<Map<String, Object>>> lookupUsers() {
        try {
            permissionService.requireLogin();
            return ResponseEntity.ok(userService.getAllUsers().stream().map(this::toUserLiteView).toList());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody UserManageRequest request) {
        try {
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
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody UserManageRequest request) {
        try {
            permissionService.requirePermission("user:manage");
            User user = userService.updateUser(
                    id,
                    request.getNickname(),
                    request.getEmail(),
                    request.getPhone(),
                    request.getStatus(),
                    request.getRoleIds()
            );
            return ResponseEntity.ok(toUserView(user));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        try {
            permissionService.requirePermission("user:manage");
            userService.deleteUser(id);
            return ResponseEntity.ok(Map.of("message", "用户已删除"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
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

}
