package com.security.config;

import com.security.entity.Menu;
import com.security.entity.Permission;
import com.security.entity.Role;
import com.security.entity.User;
import com.security.repository.MenuRepository;
import com.security.repository.PermissionRepository;
import com.security.repository.RoleRepository;
import com.security.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

/**
 * 数据初始化类
 */
@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private MenuRepository menuRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // 初始化权限
        Permission userPermission = new Permission();
        userPermission.setName("USER");
        userPermission.setDescription("用户权限");
        userPermission.setUrl("/user/**");
        permissionRepository.save(userPermission);

        Permission adminPermission = new Permission();
        adminPermission.setName("ADMIN");
        adminPermission.setDescription("管理员权限");
        adminPermission.setUrl("/admin/**");
        permissionRepository.save(adminPermission);

        // 初始化角色
        Role userRole = new Role();
        userRole.setName("USER");
        userRole.setDescription("普通用户角色");
        Set<Permission> userPermissions = new HashSet<>();
        userPermissions.add(userPermission);
        userRole.setPermissions(userPermissions);
        roleRepository.save(userRole);

        Role adminRole = new Role();
        adminRole.setName("ADMIN");
        adminRole.setDescription("管理员角色");
        Set<Permission> adminPermissions = new HashSet<>();
        adminPermissions.add(userPermission);
        adminPermissions.add(adminPermission);
        adminRole.setPermissions(adminPermissions);
        roleRepository.save(adminRole);

        // 初始化菜单
        Menu homeMenu = new Menu();
        homeMenu.setName("首页");
        homeMenu.setPath("/home");
        homeMenu.setComponent("Home");
        homeMenu.setIcon("home");
        homeMenu.setParentId(0L);
        homeMenu.setOrderNum(1);
        homeMenu.setPermission("USER");
        menuRepository.save(homeMenu);

        Menu userMenu = new Menu();
        userMenu.setName("用户页面");
        userMenu.setPath("/user");
        userMenu.setComponent("User");
        userMenu.setIcon("user");
        userMenu.setParentId(0L);
        userMenu.setOrderNum(2);
        userMenu.setPermission("USER");
        menuRepository.save(userMenu);

        Menu adminMenu = new Menu();
        adminMenu.setName("管理员页面");
        adminMenu.setPath("/admin");
        adminMenu.setComponent("Admin");
        adminMenu.setIcon("setting");
        adminMenu.setParentId(0L);
        adminMenu.setOrderNum(3);
        adminMenu.setPermission("ADMIN");
        menuRepository.save(adminMenu);

        // 初始化用户
        User user = new User();
        user.setUsername("user");
        user.setPassword(passwordEncoder.encode("123456"));
        user.setName("普通用户");
        Set<Role> userRoles = new HashSet<>();
        userRoles.add(userRole);
        user.setRoles(userRoles);
        userRepository.save(user);

        User admin = new User();
        admin.setUsername("admin");
        admin.setPassword(passwordEncoder.encode("123456"));
        admin.setName("管理员");
        Set<Role> adminRoles = new HashSet<>();
        adminRoles.add(adminRole);
        admin.setRoles(adminRoles);
        userRepository.save(admin);

        System.out.println("数据初始化完成！");
    }

}
