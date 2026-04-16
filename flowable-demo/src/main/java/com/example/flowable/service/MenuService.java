package com.example.flowable.service;

import com.example.flowable.entity.Menu;
import com.example.flowable.repository.MenuRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MenuService {

    @Autowired
    private MenuRepository menuRepository;

    // 获取所有菜单
    public List<Menu> getAllMenus() {
        return menuRepository.findByStatus(1);
    }

    // 根据ID获取菜单
    public Menu getMenuById(Long id) {
        return menuRepository.findById(id).orElse(null);
    }
}
