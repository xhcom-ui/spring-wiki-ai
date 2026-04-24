package com.example.activiti.repository;

import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface RoleMenuRepository {
    List<Long> findMenuIdsByRoleId(Long roleId);

    int deleteByRoleId(Long roleId);

    int deleteByMenuId(Long menuId);

    int insertBatch(@Param("roleId") Long roleId, @Param("menuIds") List<Long> menuIds);
}
