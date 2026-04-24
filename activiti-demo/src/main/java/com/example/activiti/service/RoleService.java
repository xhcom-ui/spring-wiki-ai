package com.example.activiti.service;

import com.example.activiti.entity.Menu;
import com.example.activiti.entity.Role;
import com.example.activiti.exception.ConflictException;
import com.example.activiti.exception.NotFoundException;
import com.example.activiti.repository.MenuRepository;
import com.example.activiti.repository.RoleMenuRepository;
import com.example.activiti.repository.RoleRepository;
import com.example.activiti.repository.UserRoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class RoleService {

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private MenuRepository menuRepository;

    @Autowired
    private RoleMenuRepository roleMenuRepository;

    @Autowired
    private MenuService menuService;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Autowired
    private AuthorizationRelationLoader relationLoader;

    public List<Role> getAllRoles() {
        ensureDefaultRoles();
        List<Role> roles = roleRepository.findAll();
        relationLoader.attachMenus(roles);
        return roles.stream()
                .sorted(Comparator.comparing(Role::getId, Comparator.nullsLast(Long::compareTo)))
                .toList();
    }

    public Role getRoleById(Long id) {
        return relationLoader.attachMenus(roleRepository.findById(id).orElse(null));
    }

    public Role createRole(Role role) {
        Role existing = roleRepository.findByCode(role.getCode());
        if (existing != null) {
            throw new ConflictException("角色编码已存在");
        }
        LocalDateTime now = LocalDateTime.now();
        role.setId(null);
        role.setCreatedAt(now);
        role.setUpdatedAt(now);
        if (role.getStatus() == null) {
            role.setStatus(1);
        }
        if (role.getMenus() == null) {
            role.setMenus(new ArrayList<>());
        }
        Role savedRole = roleRepository.save(role);
        syncRoleMenus(savedRole.getId(), role.getMenus() == null ? List.of() : role.getMenus().stream().map(Menu::getId).toList());
        return relationLoader.attachMenus(savedRole);
    }

    public Role updateRole(Long id, Role request) {
        Role role = roleRepository.findById(id).orElseThrow(() -> new NotFoundException("角色不存在"));
        Role sameCode = roleRepository.findByCode(request.getCode());
        if (sameCode != null && !sameCode.getId().equals(id)) {
            throw new ConflictException("角色编码已存在");
        }
        role.setName(request.getName());
        role.setCode(request.getCode());
        role.setDescription(request.getDescription());
        role.setStatus(request.getStatus() == null ? role.getStatus() : request.getStatus());
        role.setUpdatedAt(LocalDateTime.now());
        return relationLoader.attachMenus(roleRepository.save(role));
    }

    public Role assignMenus(Long roleId, List<Long> menuIds) {
        Role role = roleRepository.findById(roleId).orElseThrow(() -> new NotFoundException("角色不存在"));
        role.setUpdatedAt(LocalDateTime.now());
        Role savedRole = roleRepository.save(role);
        syncRoleMenus(savedRole.getId(), menuIds);
        return relationLoader.attachMenus(savedRole);
    }

    public void deleteRole(Long id) {
        Role role = roleRepository.findById(id).orElseThrow(() -> new NotFoundException("角色不存在"));
        if ("admin".equalsIgnoreCase(role.getCode()) || "employee".equalsIgnoreCase(role.getCode()) || "manager".equalsIgnoreCase(role.getCode())) {
            throw new ConflictException("默认角色不允许删除");
        }
        if (userRoleRepository.countUsersByRoleId(id) > 0) {
            throw new ConflictException("角色已分配给用户，无法删除");
        }
        roleMenuRepository.deleteByRoleId(id);
        roleRepository.delete(role);
    }

    private List<Role> buildDefaultRoles() {
        LocalDateTime now = LocalDateTime.now();
        List<Role> roles = new ArrayList<>();
        roles.add(buildRole("系统管理员", "admin", "拥有完整流程平台管理权限", now, defaultMenuIdsFor("admin")));
        roles.add(buildRole("审批经理", "manager", "负责审批与监控", now, defaultMenuIdsFor("manager")));
        roles.add(buildRole("普通员工", "employee", "负责提交申请与查看个人待办", now, defaultMenuIdsFor("employee")));
        return roles;
    }

    private Role buildRole(String name, String code, String description, LocalDateTime now, List<Long> menuIds) {
        Role role = new Role();
        role.setName(name);
        role.setCode(code);
        role.setDescription(description);
        role.setStatus(1);
        role.setCreatedAt(now);
        role.setUpdatedAt(now);
        role.setMenus(menuIds.isEmpty() ? new ArrayList<>() : new ArrayList<>(menuRepository.findAllById(menuIds)));
        return role;
    }

    private void ensureDefaultRoles() {
        menuService.getAllMenusIncludingDisabled();
        List<Role> existingRoles = roleRepository.findAll();
        List<Role> defaults = buildDefaultRoles();
        if (existingRoles.isEmpty()) {
            defaults.forEach(this::saveRoleWithMenus);
            return;
        }
        List<Role> missing = new ArrayList<>();
        for (Role item : defaults) {
            Role existing = existingRoles.stream()
                    .filter(role -> item.getCode().equalsIgnoreCase(role.getCode()))
                    .findFirst()
                    .orElse(null);
            if (existing == null) {
                item.setId(null);
                missing.add(item);
                continue;
            }
            List<Long> existingMenuIds = roleMenuRepository.findMenuIdsByRoleId(existing.getId());
            List<Long> defaultMenuIds = item.getMenus() == null ? List.of() : item.getMenus().stream().map(Menu::getId).toList();
            if ((existingMenuIds == null || existingMenuIds.isEmpty()) && !defaultMenuIds.isEmpty()) {
                syncRoleMenus(existing.getId(), defaultMenuIds);
                existing.setUpdatedAt(LocalDateTime.now());
                roleRepository.save(existing);
            }
        }
        if (!missing.isEmpty()) {
            missing.forEach(this::saveRoleWithMenus);
        }
    }

    private List<Long> defaultMenuIdsFor(String roleCode) {
        List<Menu> menus = menuRepository.findAll();
        List<String> allowedPaths = switch (roleCode) {
            case "admin" -> menus.stream().map(Menu::getPath).toList();
            case "manager" -> List.of("/dashboard", "/tasks", "/monitoring", "/versions", "/orchestration");
            case "employee" -> List.of("/dashboard", "/leave", "/tasks");
            default -> List.of();
        };
        return menus.stream()
                .filter(menu -> allowedPaths.contains(menu.getPath()))
                .map(Menu::getId)
                .toList();
    }

    private void saveRoleWithMenus(Role role) {
        Role savedRole = roleRepository.save(role);
        List<Long> menuIds = role.getMenus() == null ? List.of() : role.getMenus().stream().map(Menu::getId).toList();
        syncRoleMenus(savedRole.getId(), menuIds);
    }

    private void syncRoleMenus(Long roleId, List<Long> menuIds) {
        roleMenuRepository.deleteByRoleId(roleId);
        if (menuIds != null && !menuIds.isEmpty()) {
            roleMenuRepository.insertBatch(roleId, menuIds);
        }
    }
}
