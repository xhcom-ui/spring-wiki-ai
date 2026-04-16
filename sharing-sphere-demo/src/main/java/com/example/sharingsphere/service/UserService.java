package com.example.sharingsphere.service;

import com.example.sharingsphere.entity.User;
import java.util.List;

public interface UserService {
    User save(User user);
    User findById(Long id);
    List<User> findAll();
    void deleteById(Long id);
    User update(User user);
}
