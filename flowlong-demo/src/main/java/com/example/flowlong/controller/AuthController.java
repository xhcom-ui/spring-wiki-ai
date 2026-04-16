package com.example.flowlong.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.example.flowlong.entity.User;
import com.example.flowlong.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    // 登录
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> request) {
        try {
            String username = request.get("username");
            String password = request.get("password");
            User user = userService.login(username, password);
            return ResponseEntity.ok(Map.of(
                    "token", StpUtil.getTokenInfo().getTokenValue(),
                    "user", user
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // 注册
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody Map<String, String> request) {
        try {
            String username = request.get("username");
            String password = request.get("password");
            String nickname = request.get("nickname");
            String email = request.get("email");
            String phone = request.get("phone");
            User user = userService.register(username, password, nickname, email, phone);
            return ResponseEntity.ok(Map.of("message", "注册成功", "user", user));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // 登出
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout() {
        try {
            userService.logout();
            return ResponseEntity.ok(Map.of("message", "登出成功"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // 获取当前用户信息
    @GetMapping("/current")
    public ResponseEntity<User> getCurrentUser() {
        try {
            User user = userService.getCurrentUser();
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    // 检查是否登录
    @GetMapping("/check")
    public ResponseEntity<Map<String, Object>> checkLogin() {
        return ResponseEntity.ok(Map.of(
                "isLogin", StpUtil.isLogin(),
                "token", StpUtil.getTokenInfo().getTokenValue()
        ));
    }
}
