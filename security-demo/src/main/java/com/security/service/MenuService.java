package com.security.service;

import com.security.entity.Menu;
import java.util.List;

/**
 * 菜单服务接口
 */
public interface MenuService {

    /**
     * 根据ID查询菜单
     */
    Menu findById(Long id);

    /**
     * 查询所有菜单
     */
    List<Menu> findAll();

    /**
     * 保存菜单
     */
    Menu save(Menu menu);

    /**
     * 删除菜单
     */
    void deleteById(Long id);

    /**
     * 根据用户权限获取菜单
     */
    List<Menu> getMenusByUserPermissions();

}
