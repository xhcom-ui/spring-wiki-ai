package com.syn.data.service;

import com.syn.data.entity.User;
import com.syn.data.mapper.UserMapper;
import cn.dev33.satoken.stp.StpUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;
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
        if (!password.equals(user.getPassword())) {
            throw new RuntimeException("密码错误");
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
            StpUtil.checkToken(token);
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
        user.setCreatedAt(new Date());
        user.setUpdatedAt(new Date());

        // 保存用户
        userMapper.insert(user);
        return user;
    }

    /**
     * 更新用户信息
     */
    public User updateUser(User user) {
        user.setUpdatedAt(new Date());
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
        user.setPassword(newPassword);
        user.setUpdatedAt(new Date());
        userMapper.updateById(user);
    }
}
