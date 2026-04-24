package com.example.activiti.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.example.activiti.entity.Menu;
import com.example.activiti.entity.Role;
import com.example.activiti.entity.User;
import com.example.activiti.service.PermissionService;
import com.example.activiti.service.UserService;
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
        String username = request.get("username");
        String password = request.get("password");
        User user = userService.login(username, password);
        return ResponseEntity.ok(Map.of(
                "token", StpUtil.getTokenInfo().getTokenValue(),
                "user", toUserView(user)
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
        return ResponseEntity.ok(Map.of("message", "注册成功", "user", toUserView(user)));
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
        return ResponseEntity.ok(Map.of(
                "isLogin", StpUtil.isLogin(),
                "token", StpUtil.getTokenInfo().getTokenValue()
        ));
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
