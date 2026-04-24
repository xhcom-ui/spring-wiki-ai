package com.example.flowable.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.example.flowable.entity.Menu;
import com.example.flowable.entity.Role;
import com.example.flowable.entity.User;
import com.example.flowable.service.PermissionService;
import com.example.flowable.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private PermissionService permissionService;

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> request) {
        User user = userService.login(request.get("username"), request.get("password"));
        return ResponseEntity.ok(Map.of(
                "token", StpUtil.getTokenInfo().getTokenValue(),
                "user", user
        ));
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody Map<String, String> request) {
        User user = userService.register(
                request.get("username"),
                request.get("password"),
                request.get("nickname"),
                request.get("email"),
                request.get("phone")
        );
        return ResponseEntity.ok(Map.of("message", "注册成功", "user", user));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout() {
        userService.logout();
        return ResponseEntity.ok(Map.of("message", "登出成功"));
    }

    @GetMapping("/current")
    public ResponseEntity<Map<String, Object>> getCurrentUser() {
        permissionService.requireLogin();
        return ResponseEntity.ok(toUserView(userService.getCurrentUser()));
    }

    @GetMapping("/check")
    public ResponseEntity<Map<String, Object>> checkLogin() {
        boolean isLogin = StpUtil.isLogin();
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("isLogin", isLogin);
        response.put("token", isLogin ? StpUtil.getTokenValue() : null);
        return ResponseEntity.ok(response);
    }

    private Map<String, Object> toUserView(User user) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", user.getId());
        view.put("username", user.getUsername());
        view.put("nickname", user.getNickname());
        view.put("email", user.getEmail());
        view.put("phone", user.getPhone());
        view.put("status", user.getStatus());
        view.put("createdAt", user.getCreatedAt());
        view.put("updatedAt", user.getUpdatedAt());
        List<Role> roles = user.getRoles() == null ? List.of() : user.getRoles();
        view.put("isAdmin", permissionService.isAdmin(user));
        view.put("roleCodes", roles.stream().map(Role::getCode).toList());
        view.put("roleNames", roles.stream().map(Role::getName).toList());
        view.put("permissions", new ArrayList<>(permissionService.getPermissions(user)));
        view.put("menuPaths", new ArrayList<>(permissionService.getMenuPaths(user)));
        view.put("roles", new ArrayList<>(roles.stream().map(role -> {
            Map<String, Object> roleView = new LinkedHashMap<>();
            roleView.put("id", role.getId());
            roleView.put("name", role.getName());
            roleView.put("code", role.getCode());
            List<Menu> menus = role.getMenus() == null ? List.of() : role.getMenus();
            roleView.put("menuIds", menus.stream().map(Menu::getId).toList());
            roleView.put("menuPaths", menus.stream().map(Menu::getPath).toList());
            return roleView;
        }).toList()));
        return view;
    }
}
