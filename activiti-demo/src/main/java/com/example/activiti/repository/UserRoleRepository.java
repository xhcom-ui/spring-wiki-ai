package com.example.activiti.repository;

import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface UserRoleRepository {
    List<Long> findRoleIdsByUserId(Long userId);

    long countUsersByRoleId(Long roleId);

    int deleteByUserId(Long userId);

    int insertBatch(@Param("userId") Long userId, @Param("roleIds") List<Long> roleIds);
}
