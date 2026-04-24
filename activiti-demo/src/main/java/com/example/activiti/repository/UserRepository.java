package com.example.activiti.repository;

import com.example.activiti.entity.User;

import java.util.List;
import java.util.Optional;

public interface UserRepository {
    User selectById(Long id);

    User findByUsername(String username);

    List<User> findAll();

    int insert(User user);

    int update(User user);

    int removeById(Long id);

    default Optional<User> findById(Long id) {
        return Optional.ofNullable(selectById(id));
    }

    default User save(User user) {
        if (user.getId() == null) {
            insert(user);
        } else {
            update(user);
        }
        return user;
    }

    default void delete(User user) {
        if (user != null && user.getId() != null) {
            removeById(user.getId());
        }
    }
}
