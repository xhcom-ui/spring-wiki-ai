package com.security.service;

import com.security.entity.User;
import java.util.List;

/**
 * 用户服务接口
 */
public interface UserService {

    /**
     * 根据ID查询用户
     */
    User findById(Long id);

    /**
     * 根据用户名查询用户
     */
    User findByUsername(String username);

    /**
     * 查询所有用户
     */
    List<User> findAll();

    /**
     * 保存用户
     */
    User save(User user);

    /**
     * 删除用户
     */
    void deleteById(Long id);

}
