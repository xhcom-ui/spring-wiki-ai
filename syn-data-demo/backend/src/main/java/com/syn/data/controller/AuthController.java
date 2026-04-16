package com.syn.data.controller;

import com.syn.data.entity.User;
import com.syn.data.service.UserService;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Map;

/**
 * 认证控制器
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Resource
    private UserService userService;

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody LoginRequest loginRequest) {
        return userService.login(loginRequest.getUsername(), loginRequest.getPassword());
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/user")
    public User getUserInfo() {
        // 验证token
        StpUtil.checkLogin();

        // 获取当前用户信息
        return userService.getCurrentUser();
    }

    /**
     * 注册新用户
     */
    @PostMapping("/register")
    public User register(@RequestBody User user) {
        return userService.register(user);
    }

    /**
     * 重置密码
     */
    @PostMapping("/reset-password")
    public void resetPassword(@RequestBody ResetPasswordRequest request) {
        userService.resetPassword(request.getUserId(), request.getNewPassword());
    }

    /**
     * 登录请求参数
     */
    static class LoginRequest {
        private String username;
        private String password;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    /**
     * 重置密码请求参数
     */
    static class ResetPasswordRequest {
        private Long userId;
        private String newPassword;

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public String getNewPassword() {
            return newPassword;
        }

        public void setNewPassword(String newPassword) {
            this.newPassword = newPassword;
        }
    }
}
