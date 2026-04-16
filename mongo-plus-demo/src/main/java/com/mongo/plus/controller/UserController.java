package com.mongo.plus.controller;

import com.mongo.plus.entity.User;
import com.mongo.plus.service.UserService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 用户控制器
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    @Resource
    private UserService userService;

    /**
     * 保存用户
     */
    @PostMapping
    public User save(@RequestBody User user) {
        return userService.save(user);
    }

    /**
     * 根据ID查询用户
     */
    @GetMapping("/{id}")
    public User findById(@PathVariable String id) {
        return userService.findById(id);
    }

    /**
     * 查询所有用户
     */
    @GetMapping
    public List<User> findAll() {
        return userService.findAll();
    }

    /**
     * 根据用户名查询用户
     */
    @GetMapping("/name/{name}")
    public List<User> findByName(@PathVariable String name) {
        return userService.findByName(name);
    }

    /**
     * 更新用户
     */
    @PutMapping
    public User update(@RequestBody User user) {
        return userService.update(user);
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/{id}")
    public void delete(@PathVariable String id) {
        userService.delete(id);
    }

}
