package com.example.flowlong.repository;

import com.example.flowlong.controller.dto.UserQueryRequest;
import org.apache.ibatis.annotations.Param;

import com.example.flowlong.entity.User;

import java.util.List;
import java.util.Optional;

public interface UserRepository {
    User selectById(Long id);

    User findByUsername(String username);

    List<User> findAll();

    List<User> findPageByCondition(@Param("query") UserQueryRequest query);

    long countByCondition(@Param("query") UserQueryRequest query);

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
