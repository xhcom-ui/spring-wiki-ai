package com.example.activiti.service;

import cn.dev33.satoken.stp.StpUtil;
import com.example.activiti.entity.User;
import com.example.activiti.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class UserService {

    @Autowired
    private UserRepository userRepository;

    // 登录
    public User login(String username, String password) {
        User user = userRepository.findByUsername(username);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        if (!password.equals(user.getPassword())) {
            throw new RuntimeException("密码错误");
        }
        if (user.getStatus() != 1) {
            throw new RuntimeException("用户已禁用");
        }
        // 登录成功，生成 token
        StpUtil.login(user.getId());
        return user;
    }

    // 注册
    public User register(String username, String password, String nickname, String email, String phone) {
        User existingUser = userRepository.findByUsername(username);
        if (existingUser != null) {
            throw new RuntimeException("用户名已存在");
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(password);
        user.setNickname(nickname);
        user.setEmail(email);
        user.setPhone(phone);
        user.setStatus(1);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    // 获取当前用户
    public User getCurrentUser() {
        Long userId = StpUtil.getLoginIdAsLong();
        return userRepository.findById(userId).orElse(null);
    }

    // 登出
    public void logout() {
        StpUtil.logout();
    }

    // 获取所有用户
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // 根据ID获取用户
    public User getUserById(Long id) {
        return userRepository.findById(id).orElse(null);
    }
}
