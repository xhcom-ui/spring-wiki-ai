package com.syn.data.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.syn.data.entity.User;

/**
 * 用户Mapper接口
 */
public interface UserMapper extends BaseMapper<User> {
    /**
     * 根据用户名查询用户
     */
    User selectByUsername(String username);
}
