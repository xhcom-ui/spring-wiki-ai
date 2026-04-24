package com.example.flowlong.service;

import com.example.flowlong.common.PageBuilder;
import com.example.flowlong.common.PageResult;
import com.example.flowlong.controller.dto.RoleQueryRequest;
import com.example.flowlong.entity.Menu;
import com.example.flowlong.entity.Role;
import com.example.flowlong.repository.MenuRepository;
import com.example.flowlong.repository.RoleMenuRepository;
import com.example.flowlong.repository.RoleRepository;
import com.example.flowlong.repository.UserRoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class RoleService {

    private final Object defaultRolesLock = new Object();

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

    public PageResult<Role> queryRoles(RoleQueryRequest query) {
        ensureDefaultRoles();
        List<Role> roles = roleRepository.findPageByCondition(query);
        relationLoader.attachMenus(roles);
        long total = roleRepository.countByCondition(query);
        return PageBuilder.build(query, roles, total);
    }

    public Role createRole(Role role) {
        Role existing = roleRepository.findByCode(role.getCode());
        if (existing != null) {
            throw new RuntimeException("角色编码已存在");
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
        syncRoleMenus(savedRole.getId(), role.getMenus().stream().map(Menu::getId).toList());
        return relationLoader.attachMenus(savedRole);
    }

    public Role updateRole(Long id, Role request) {
        Role role = roleRepository.findById(id).orElseThrow(() -> new RuntimeException("角色不存在"));
        Role sameCode = roleRepository.findByCode(request.getCode());
        if (sameCode != null && !sameCode.getId().equals(id)) {
            throw new RuntimeException("角色编码已存在");
        }
        role.setName(request.getName());
        role.setCode(request.getCode());
        role.setDescription(request.getDescription());
        role.setStatus(request.getStatus() == null ? role.getStatus() : request.getStatus());
        role.setUpdatedAt(LocalDateTime.now());
        return relationLoader.attachMenus(roleRepository.save(role));
    }

    public Role assignMenus(Long roleId, List<Long> menuIds) {
        Role role = roleRepository.findById(roleId).orElseThrow(() -> new RuntimeException("角色不存在"));
        role.setUpdatedAt(LocalDateTime.now());
        Role savedRole = roleRepository.save(role);
        syncRoleMenus(savedRole.getId(), menuIds);
        return relationLoader.attachMenus(savedRole);
    }

    public void deleteRole(Long id) {
        Role role = roleRepository.findById(id).orElseThrow(() -> new RuntimeException("角色不存在"));
        if ("admin".equalsIgnoreCase(role.getCode()) || "employee".equalsIgnoreCase(role.getCode()) || "manager".equalsIgnoreCase(role.getCode())) {
            throw new RuntimeException("默认角色不允许删除");
        }
        if (userRoleRepository.countUsersByRoleId(id) > 0) {
            throw new RuntimeException("角色已分配给用户，无法删除");
        }
        roleMenuRepository.deleteByRoleId(id);
        roleRepository.delete(role);
    }

    private void ensureDefaultRoles() {
        synchronized (defaultRolesLock) {
            menuService.getAllMenusIncludingDisabled();
            List<Role> existingRoles = roleRepository.findAll();
            List<Role> defaults = buildDefaultRoles();
            if (existingRoles.isEmpty()) {
                defaults.forEach(this::saveRoleWithMenus);
                return;
            }
            List<Role> missing = new ArrayList<>();
            for (Role item : defaults) {
                if (existingRoles.stream().noneMatch(role -> item.getCode().equalsIgnoreCase(role.getCode()))) {
                    item.setId(null);
                    missing.add(item);
                }
            }
            if (!missing.isEmpty()) {
                missing.forEach(this::saveRoleWithMenus);
            }
            reconcileDefaultRoleMenus(existingRoles);
        }
    }

    private List<Role> buildDefaultRoles() {
        LocalDateTime now = LocalDateTime.now();
        List<Role> roles = new ArrayList<>();
        roles.add(buildRole("系统管理员", "admin", "拥有完整平台权限", now, defaultMenuIdsFor("admin")));
        roles.add(buildRole("审批经理", "manager", "负责审批和流程监控", now, defaultMenuIdsFor("manager")));
        roles.add(buildRole("普通员工", "employee", "负责提交申请和查看待办", now, defaultMenuIdsFor("employee")));
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

    private List<Long> defaultMenuIdsFor(String roleCode) {
        List<Menu> menus = menuRepository.findAll();
        List<String> allowedPaths = switch (roleCode) {
            case "admin" -> menus.stream().map(Menu::getPath).toList();
            case "manager" -> List.of("/dashboard", "/tasks", "/versions", "/monitoring");
            case "employee" -> List.of("/dashboard", "/designer", "/designer/studio", "/leave", "/tasks");
            default -> List.of();
        };
        return menus.stream()
                .filter(menu -> allowedPaths.contains(menu.getPath()))
                .map(Menu::getId)
                .toList();
    }

    private void reconcileDefaultRoleMenus(List<Role> existingRoles) {
        for (Role role : existingRoles) {
            String roleCode = role.getCode() == null ? "" : role.getCode().toLowerCase();
            if (!List.of("admin", "manager", "employee").contains(roleCode)) {
                continue;
            }
            List<Long> requiredMenuIds = defaultMenuIdsFor(roleCode);
            List<Long> currentMenuIds = roleMenuRepository.findMenuIdsByRoleId(role.getId());
            List<Long> mergedMenuIds = new ArrayList<>(currentMenuIds);
            boolean changed = false;
            for (Long menuId : requiredMenuIds) {
                if (!mergedMenuIds.contains(menuId)) {
                    mergedMenuIds.add(menuId);
                    changed = true;
                }
            }
            if (changed) {
                syncRoleMenus(role.getId(), mergedMenuIds);
                role.setUpdatedAt(LocalDateTime.now());
                roleRepository.save(role);
            }
        }
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
