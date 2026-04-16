package com.security.service.impl;

import com.security.entity.Menu;
import com.security.repository.MenuRepository;
import com.security.service.MenuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 菜单服务实现类
 */
@Service
public class MenuServiceImpl implements MenuService {

    @Autowired
    private MenuRepository menuRepository;

    @Override
    public Menu findById(Long id) {
        return menuRepository.findById(id).orElse(null);
    }

    @Override
    public List<Menu> findAll() {
        return menuRepository.findAll();
    }

    @Override
    public Menu save(Menu menu) {
        return menuRepository.save(menu);
    }

    @Override
    public void deleteById(Long id) {
        menuRepository.deleteById(id);
    }

    @Override
    public List<Menu> getMenusByUserPermissions() {
        // 获取当前用户
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        // 获取用户权限
        List<String> userPermissions = authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority().replace("ROLE_", ""))
                .collect(Collectors.toList());
        
        // 获取所有菜单
        List<Menu> allMenus = menuRepository.findAll();
        
        // 根据用户权限过滤菜单
        return allMenus.stream()
                .filter(menu -> userPermissions.contains(menu.getPermission()))
                .collect(Collectors.toList());
    }

}
