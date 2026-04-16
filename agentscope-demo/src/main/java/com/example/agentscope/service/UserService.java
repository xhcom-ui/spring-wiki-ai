package com.example.agentscope.service;

import com.example.agentscope.model.User;
import com.example.agentscope.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
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

    // 根据ID获取用户
    public User getUserById(Long id) {
        return userRepository.findById(id).orElse(null);
    }

    // 根据用户名获取用户
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }
}
