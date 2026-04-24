package com.example.flowlong.service;

import cn.dev33.satoken.stp.StpUtil;
import com.example.flowlong.entity.Menu;
import com.example.flowlong.entity.Role;
import com.example.flowlong.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PermissionService {

    @Autowired
    private UserService userService;

    public void requireLogin() {
        StpUtil.checkLogin();
    }

    public void requirePermission(String permission) {
        requireLogin();
        if (hasPermission(permission)) {
            return;
        }
        throw new RuntimeException("无权限访问: " + permission);
    }

    public void requireAnyPermission(String... permissions) {
        requireLogin();
        if (permissions == null || permissions.length == 0) {
            return;
        }
        for (String permission : permissions) {
            if (hasPermission(permission)) {
                return;
            }
        }
        throw new RuntimeException("无权限访问: " + String.join(" / ", permissions));
    }

    public boolean hasPermission(String permission) {
        User currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            return false;
        }
        if (isAdmin(currentUser)) {
            return true;
        }
        return getPermissions(currentUser).contains(permission);
    }

    public Set<String> getPermissions(User user) {
        if (user == null) {
            return Set.of();
        }
        List<Role> roles = user.getRoles() == null ? List.of() : user.getRoles();
        return roles.stream()
                .filter(role -> role.getStatus() == null || role.getStatus() == 1)
                .flatMap(role -> {
                    List<Menu> menus = role.getMenus() == null ? List.of() : role.getMenus();
                    return menus.stream();
                })
                .filter(menu -> menu.getStatus() == null || menu.getStatus() == 1)
                .map(Menu::getPermission)
                .filter(item -> item != null && !item.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public Set<String> getMenuPaths(User user) {
        if (user == null) {
            return Set.of();
        }
        List<Role> roles = user.getRoles() == null ? List.of() : user.getRoles();
        return roles.stream()
                .filter(role -> role.getStatus() == null || role.getStatus() == 1)
                .flatMap(role -> {
                    List<Menu> menus = role.getMenus() == null ? List.of() : role.getMenus();
                    return menus.stream();
                })
                .filter(menu -> menu.getStatus() == null || menu.getStatus() == 1)
                .map(Menu::getPath)
                .filter(item -> item != null && !item.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public boolean isAdmin(User user) {
        if (user == null || user.getRoles() == null) {
            return false;
        }
        return user.getRoles().stream().anyMatch(role -> "admin".equalsIgnoreCase(role.getCode()));
    }
}
