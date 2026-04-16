package com.mongo.plus.service.impl;

import com.gitee.hengboy.mongo.builder.query.QueryBuilder;
import com.mongo.plus.entity.User;
import com.mongo.plus.mapper.UserMapper;
import com.mongo.plus.service.UserService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

/**
 * 用户服务实现类
 */
@Service
public class UserServiceImpl implements UserService {

    @Resource
    private UserMapper userMapper;

    @Override
    public User save(User user) {
        // 设置创建时间和更新时间
        Date now = new Date();
        user.setCreateTime(now);
        user.setUpdateTime(now);
        // 保存用户
        userMapper.insert(user);
        return user;
    }

    @Override
    public User findById(String id) {
        return userMapper.findById(id);
    }

    @Override
    public List<User> findAll() {
        return userMapper.selectList(QueryBuilder.query());
    }

    @Override
    public List<User> findByName(String name) {
        return userMapper.selectList(
                QueryBuilder.query()
                        .eq("name", name)
        );
    }

    @Override
    public User update(User user) {
        // 设置更新时间
        user.setUpdateTime(new Date());
        // 更新用户
        userMapper.updateById(user);
        return user;
    }

    @Override
    public void delete(String id) {
        userMapper.deleteById(id);
    }

}
