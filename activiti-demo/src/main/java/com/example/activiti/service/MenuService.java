package com.example.activiti.service;

import com.example.activiti.entity.Menu;
import com.example.activiti.entity.Role;
import com.example.activiti.entity.User;
import com.example.activiti.exception.ConflictException;
import com.example.activiti.exception.NotFoundException;
import com.example.activiti.repository.MenuRepository;
import com.example.activiti.repository.RoleMenuRepository;
import com.example.activiti.repository.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class MenuService {

    @Autowired
    private MenuRepository menuRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private RoleMenuRepository roleMenuRepository;

    // 获取所有菜单
    public List<Menu> getAllMenus() {
        ensureDefaultMenus();
        List<Menu> menus = menuRepository.findByStatus(1);
        return menus.stream()
                .sorted(Comparator.comparing(Menu::getSort, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(Menu::getId, Comparator.nullsLast(Long::compareTo)))
                .toList();
    }

    public List<Menu> getAllMenusIncludingDisabled() {
        ensureDefaultMenus();
        List<Menu> menus = menuRepository.findAll();
        return menus.stream()
                .sorted(Comparator.comparing(Menu::getSort, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(Menu::getId, Comparator.nullsLast(Long::compareTo)))
                .toList();
    }

    // 根据ID获取菜单
    public Menu getMenuById(Long id) {
        return menuRepository.findById(id).orElse(null);
    }

    public Menu createMenu(Menu menu) {
        LocalDateTime now = LocalDateTime.now();
        menu.setId(null);
        menu.setCreatedAt(now);
        menu.setUpdatedAt(now);
        if (menu.getStatus() == null) {
            menu.setStatus(1);
        }
        if (menu.getParentId() == null) {
            menu.setParentId(0L);
        }
        if (menu.getSort() == null) {
            menu.setSort(0);
        }
        if (menu.getType() == null) {
            menu.setType(1);
        }
        return menuRepository.save(menu);
    }

    public Menu updateMenu(Long id, Menu request) {
        Menu menu = menuRepository.findById(id).orElseThrow(() -> new NotFoundException("菜单不存在"));
        menu.setName(request.getName());
        menu.setPath(request.getPath());
        menu.setComponent(request.getComponent());
        menu.setIcon(request.getIcon());
        menu.setParentId(request.getParentId() == null ? 0L : request.getParentId());
        menu.setSort(request.getSort() == null ? 0 : request.getSort());
        menu.setType(request.getType() == null ? 1 : request.getType());
        menu.setPermission(request.getPermission());
        menu.setStatus(request.getStatus() == null ? menu.getStatus() : request.getStatus());
        menu.setUpdatedAt(LocalDateTime.now());
        return menuRepository.save(menu);
    }

    public void deleteMenu(Long id) {
        Menu menu = menuRepository.findById(id).orElseThrow(() -> new NotFoundException("菜单不存在"));
        if (menu.getPath() != null && List.of(
                "/dashboard", "/designer", "/leave", "/tasks",
                "/users", "/roles", "/menu-permissions", "/orchestration",
                "/versions", "/monitoring", "/runtime-catalog"
        ).contains(menu.getPath())) {
            throw new ConflictException("默认菜单不允许删除");
        }
        roleMenuRepository.deleteByMenuId(id);
        menuRepository.delete(menu);
    }

    private List<Menu> buildDefaultMenus() {
        LocalDateTime now = LocalDateTime.now();
        List<Menu> menus = new ArrayList<>();
        menus.add(createMenu("工作台首页", "/dashboard", "DashboardView", "dashboard", 0, now));
        menus.add(createMenu("流程设计", "/designer", "ProcessStudioView", "designer", 1, now));
        menus.add(createMenu("请假发起", "/leave", "LeaveView", "leave", 2, now));
        menus.add(createMenu("待办任务", "/tasks", "TaskCenterView", "tasks", 3, now));
        menus.add(createMenu("用户管理", "/users", "UserManagementView", "users", 4, now));
        menus.add(createMenu("角色管理", "/roles", "RoleManagementView", "roles", 5, now));
        menus.add(createMenu("菜单权限", "/menu-permissions", "MenuPermissionView", "menus", 6, now));
        menus.add(createMenu("流程编排", "/orchestration", "OrchestrationView", "flow", 7, now));
        menus.add(createMenu("版本对比", "/versions", "VersionCompareView", "versions", 8, now));
        menus.add(createMenu("流程监控", "/monitoring", "MonitoringView", "monitor", 9, now));
        menus.add(createMenu("业务页目录", "/runtime-catalog", "TaskPageCatalogView", "catalog", 10, now));
        return menus;
    }

    private Menu createMenu(String name, String path, String component, String icon, int sort, LocalDateTime now) {
        Menu menu = new Menu();
        menu.setId(null);
        menu.setName(name);
        menu.setPath(path);
        menu.setComponent(component);
        menu.setIcon(icon);
        menu.setParentId(0L);
        menu.setSort(sort);
        menu.setType(1);
        menu.setPermission(resolveDefaultPermission(path));
        menu.setStatus(1);
        menu.setCreatedAt(now);
        menu.setUpdatedAt(now);
        return menu;
    }

    public List<Menu> getMenusForCurrentUser() {
        ensureDefaultMenus();
        User currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            return List.of();
        }
        if (currentUser.getRoles() != null && currentUser.getRoles().stream().anyMatch(role -> "admin".equalsIgnoreCase(role.getCode()))) {
            return getAllMenus();
        }
        List<Role> roles = currentUser.getRoles() == null ? List.of() : currentUser.getRoles();
        return roles.stream()
                .flatMap(role -> {
                    List<Menu> menus = role.getMenus() == null ? List.of() : role.getMenus();
                    return menus.stream();
                })
                .filter(menu -> menu.getStatus() != null && menu.getStatus() == 1)
                .distinct()
                .sorted(Comparator.comparing(Menu::getSort, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(Menu::getId, Comparator.nullsLast(Long::compareTo)))
                .toList();
    }

    private void ensureDefaultMenus() {
        List<Menu> existingMenus = menuRepository.findAll();
        List<Menu> defaults = buildDefaultMenus();
        if (existingMenus.isEmpty()) {
            menuRepository.saveAll(defaults);
            return;
        }

        List<Menu> missingMenus = new ArrayList<>();
        for (Menu defaultMenu : defaults) {
            boolean exists = existingMenus.stream().anyMatch(item -> defaultMenu.getPath().equals(item.getPath()));
            if (!exists) {
                defaultMenu.setId(null);
                missingMenus.add(defaultMenu);
            }
        }
        if (!missingMenus.isEmpty()) {
            menuRepository.saveAll(missingMenus);
        }
    }

    private String resolveDefaultPermission(String path) {
        return switch (path) {
            case "/dashboard" -> "dashboard:view";
            case "/designer" -> "process:design";
            case "/leave" -> "leave:submit";
            case "/tasks" -> "task:approve";
            case "/users" -> "user:manage";
            case "/roles" -> "role:manage";
            case "/menu-permissions" -> "menu:manage";
            case "/orchestration" -> "orchestration:manage";
            case "/versions" -> "process:compare";
            case "/monitoring" -> "monitoring:view";
            case "/runtime-catalog" -> "runtime:catalog:manage";
            default -> path;
        };
    }
}
