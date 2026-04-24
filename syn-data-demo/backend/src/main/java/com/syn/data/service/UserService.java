package com.syn.data.service;

import com.syn.data.entity.User;
import com.syn.data.mapper.UserMapper;
import cn.dev33.satoken.stp.StpUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 用户服务类
 */
@Slf4j
@Service
public class UserService {

    @Resource
    private UserMapper userMapper;

    /**
     * 用户登录
     */
    public Map<String, Object> login(String username, String password) {
        // 验证用户名和密码
        User user = userMapper.selectByUsername(username);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        if (user.getStatus() != 1) {
            throw new RuntimeException("用户已被禁用");
        }
        if (!matchesPassword(password, user.getPassword())) {
            throw new RuntimeException("密码错误");
        }

        if (!isHashedPassword(user.getPassword())) {
            user.setPassword(hashPassword(password));
            user.setUpdatedAt(LocalDateTime.now());
            userMapper.updateById(user);
        }

        // 生成SO-Token (使用Sa-Token)
        StpUtil.login(user.getId());
        String token = StpUtil.getTokenValue();

        // 构建返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("user", user);

        return result;
    }

    /**
     * 验证SO-Token
     */
    public void validateToken(String token) {
        try {
            Object loginId = StpUtil.getLoginIdByToken(token);
            if (loginId == null) {
                throw new RuntimeException("token未登录");
            }
        } catch (Exception e) {
            log.error("Token validation failed: {}", e.getMessage());
            throw new RuntimeException("无效的token");
        }
    }

    /**
     * 获取当前登录用户
     */
    public User getCurrentUser() {
        Long userId = StpUtil.getLoginIdAsLong();
        return userMapper.selectById(userId);
    }

    /**
     * 根据用户ID获取用户信息
     */
    public User getUserById(Long userId) {
        return userMapper.selectById(userId);
    }

    /**
     * 注册新用户
     */
    public User register(User user) {
        // 检查用户名是否已存在
        User existingUser = userMapper.selectByUsername(user.getUsername());
        if (existingUser != null) {
            throw new RuntimeException("用户名已存在");
        }

        // 设置默认值
        user.setStatus(1);
        user.setRole("user");
        user.setPassword(hashPassword(user.getPassword()));
        LocalDateTime now = LocalDateTime.now();
        user.setCreatedAt(now);
        user.setUpdatedAt(now);

        // 保存用户
        userMapper.insert(user);
        return user;
    }

    /**
     * 更新用户信息
     */
    public User updateUser(User user) {
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        return user;
    }

    /**
     * 重置密码
     */
    public void resetPassword(Long userId, String newPassword) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        user.setPassword(hashPassword(newPassword));
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
    }

    private boolean matchesPassword(String rawPassword, String storedPassword) {
        if (storedPassword == null || storedPassword.isBlank()) {
            return false;
        }
        return storedPassword.equals(rawPassword) || storedPassword.equals(hashPassword(rawPassword));
    }

    private boolean isHashedPassword(String password) {
        return password != null && password.matches("^[a-fA-F0-9]{64}$");
    }

    private String hashPassword(String rawPassword) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawPassword.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte item : hashed) {
                builder.append(String.format("%02x", item));
            }
            return builder.toString();
        } catch (Exception e) {
            throw new RuntimeException("密码加密失败", e);
        }
    }
}
