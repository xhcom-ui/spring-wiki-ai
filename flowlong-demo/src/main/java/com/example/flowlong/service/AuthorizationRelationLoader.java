package com.example.flowlong.service;

import com.example.flowlong.entity.Menu;
import com.example.flowlong.entity.Role;
import com.example.flowlong.entity.User;
import com.example.flowlong.repository.MenuRepository;
import com.example.flowlong.repository.RoleMenuRepository;
import com.example.flowlong.repository.RoleRepository;
import com.example.flowlong.repository.UserRoleRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class AuthorizationRelationLoader {

    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final RoleMenuRepository roleMenuRepository;
    private final MenuRepository menuRepository;

    public AuthorizationRelationLoader(
            UserRoleRepository userRoleRepository,
            RoleRepository roleRepository,
            RoleMenuRepository roleMenuRepository,
            MenuRepository menuRepository
    ) {
        this.userRoleRepository = userRoleRepository;
        this.roleRepository = roleRepository;
        this.roleMenuRepository = roleMenuRepository;
        this.menuRepository = menuRepository;
    }

    public User attachRoles(User user) {
        if (user == null || user.getId() == null) {
            return user;
        }
        List<Long> roleIds = userRoleRepository.findRoleIdsByUserId(user.getId());
        List<Role> roles = roleIds.isEmpty() ? new ArrayList<>() : new ArrayList<>(roleRepository.findAllById(roleIds));
        attachMenus(roles);
        user.setRoles(roles);
        return user;
    }

    public List<User> attachRoles(List<User> users) {
        if (users == null) {
            return List.of();
        }
        users.forEach(this::attachRoles);
        return users;
    }

    public Role attachMenus(Role role) {
        if (role == null || role.getId() == null) {
            return role;
        }
        List<Long> menuIds = roleMenuRepository.findMenuIdsByRoleId(role.getId());
        List<Menu> menus = menuIds.isEmpty() ? new ArrayList<>() : new ArrayList<>(menuRepository.findAllById(menuIds));
        role.setMenus(menus);
        return role;
    }

    public List<Role> attachMenus(List<Role> roles) {
        if (roles == null) {
            return List.of();
        }
        roles.forEach(this::attachMenus);
        return roles;
    }
}
