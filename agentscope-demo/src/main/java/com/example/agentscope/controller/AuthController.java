package com.example.agentscope.controller;

import com.example.agentscope.model.User;
import com.example.agentscope.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
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
                    "message", "登录成功",
                    "user", user
            ));
        } catch (Exception e) {
            log.error("Login failed", e);
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
            return ResponseEntity.ok(Map.of(
                    "message", "注册成功",
                    "user", user
            ));
        } catch (Exception e) {
            log.error("Register failed", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
