package com.example.flowlong.repository;

import com.example.flowlong.controller.dto.MenuQueryRequest;
import com.example.flowlong.entity.Menu;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

public interface MenuRepository {
    Menu selectById(Long id);

    List<Menu> findByStatus(Integer status);

    List<Menu> findAll();

    List<Menu> findAllById(@Param("ids") List<Long> ids);

    List<Menu> findPageByCondition(@Param("query") MenuQueryRequest query);

    long countByCondition(@Param("query") MenuQueryRequest query);

    int insert(Menu menu);

    int update(Menu menu);

    int removeById(Long id);

    default Optional<Menu> findById(Long id) {
        return Optional.ofNullable(selectById(id));
    }

    default Menu save(Menu menu) {
        if (menu.getId() == null) {
            insert(menu);
        } else {
            update(menu);
        }
        return menu;
    }

    default List<Menu> saveAll(List<Menu> menus) {
        for (Menu menu : menus) {
            save(menu);
        }
        return menus;
    }

    default void delete(Menu menu) {
        if (menu != null && menu.getId() != null) {
            removeById(menu.getId());
        }
    }
}
