package com.mongo.plus.service;

import com.mongo.plus.entity.User;
import java.util.List;

/**
 * 用户服务接口
 */
public interface UserService {

    /**
     * 保存用户
     */
    User save(User user);

    /**
     * 根据ID查询用户
     */
    User findById(String id);

    /**
     * 查询所有用户
     */
    List<User> findAll();

    /**
     * 根据用户名查询用户
     */
    List<User> findByName(String name);

    /**
     * 更新用户
     */
    User update(User user);

    /**
     * 删除用户
     */
    void delete(String id);

}
