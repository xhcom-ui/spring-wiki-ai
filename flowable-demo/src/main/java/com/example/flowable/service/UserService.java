package com.example.flowable.service;

import cn.dev33.satoken.stp.StpUtil;
import com.example.flowable.entity.Role;
import com.example.flowable.entity.User;
import com.example.flowable.exception.ConflictException;
import com.example.flowable.exception.NotFoundException;
import com.example.flowable.exception.UnauthorizedException;
import com.example.flowable.repository.RoleRepository;
import com.example.flowable.repository.UserRepository;
import com.example.flowable.repository.UserRoleRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Autowired
    @Lazy
    private RoleService roleService;

    @Autowired
    private AuthorizationRelationLoader relationLoader;

    public User login(String username, String password) {
        ensureSeedUsers();
        User user = userRepository.findByUsername(username);
        if (user == null) {
            throw new UnauthorizedException("用户不存在");
        }
        if (!password.equals(user.getPassword())) {
            throw new UnauthorizedException("密码错误");
        }
        if (user.getStatus() != 1) {
            throw new UnauthorizedException("用户已禁用");
        }
        StpUtil.login(user.getId());
        return relationLoader.attachRoles(user);
    }

    public User register(String username, String password, String nickname, String email, String phone) {
        ensureSeedUsers();
        User existingUser = userRepository.findByUsername(username);
        if (existingUser != null) {
            throw new ConflictException("用户名已存在");
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(password);
        user.setNickname(nickname);
        user.setEmail(email);
        user.setPhone(phone);
        user.setStatus(1);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        Role defaultRole = ensureDefaultEmployeeRole();
        User savedUser = userRepository.save(user);
        syncUserRoles(savedUser.getId(), defaultRole == null ? List.of() : List.of(defaultRole.getId()));
        return relationLoader.attachRoles(savedUser);
    }

    public User getCurrentUser() {
        if (!StpUtil.isLogin()) {
            return null;
        }
        Long userId = StpUtil.getLoginIdAsLong();
        return relationLoader.attachRoles(userRepository.findById(userId).orElse(null));
    }

    public void logout() {
        StpUtil.logout();
    }

    public List<User> getAllUsers() {
        ensureSeedUsers();
        return relationLoader.attachRoles(userRepository.findAll());
    }

    public User getUserById(Long id) {
        return relationLoader.attachRoles(userRepository.findById(id).orElse(null));
    }

    public User createUser(String username, String password, String nickname, String email, String phone, Integer status, List<Long> roleIds) {
        ensureSeedUsers();
        User existingUser = userRepository.findByUsername(username);
        if (existingUser != null) {
            throw new ConflictException("用户名已存在");
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(password == null || password.isBlank() ? "123456" : password);
        user.setNickname(nickname);
        user.setEmail(email);
        user.setPhone(phone);
        user.setStatus(status == null ? 1 : status);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        User savedUser = userRepository.save(user);
        syncUserRoles(savedUser.getId(), roleIds);
        return relationLoader.attachRoles(savedUser);
    }

    public User updateUser(Long id, String password, String nickname, String email, String phone, Integer status, List<Long> roleIds) {
        User user = userRepository.findById(id).orElseThrow(() -> new NotFoundException("用户不存在"));
        if (password != null && !password.isBlank()) {
            user.setPassword(password);
        }
        user.setNickname(nickname);
        user.setEmail(email);
        user.setPhone(phone);
        if (status != null) {
            user.setStatus(status);
        }
        user.setUpdatedAt(LocalDateTime.now());
        User savedUser = userRepository.save(user);
        if (roleIds != null) {
            syncUserRoles(savedUser.getId(), roleIds);
        }
        return relationLoader.attachRoles(savedUser);
    }

    public void deleteUser(Long id) {
        User user = userRepository.findById(id).orElseThrow(() -> new NotFoundException("用户不存在"));
        if ("admin".equalsIgnoreCase(user.getUsername())) {
            throw new ConflictException("默认管理员不允许删除");
        }
        User currentUser = getCurrentUser();
        if (currentUser != null && id.equals(currentUser.getId())) {
            throw new ConflictException("当前登录用户不允许删除自己");
        }
        userRoleRepository.deleteByUserId(id);
        userRepository.delete(user);
    }

    private Role ensureDefaultEmployeeRole() {
        roleService.getAllRoles();
        Role defaultRole = roleRepository.findByCode("employee");
        if (defaultRole != null) {
            return relationLoader.attachMenus(defaultRole);
        }
        Role role = new Role();
        role.setName("普通员工");
        role.setCode("employee");
        role.setDescription("默认员工角色");
        role.setStatus(1);
        role.setCreatedAt(LocalDateTime.now());
        role.setUpdatedAt(LocalDateTime.now());
        role.setMenus(new ArrayList<>());
        return relationLoader.attachMenus(roleRepository.save(role));
    }

    private void ensureSeedUsers() {
        roleService.getAllRoles();
        ensureSeedUser("admin", "123456", "系统管理员", "admin@example.com", "13800000000", "admin");
        ensureSeedUser("manager", "123456", "审批经理", "manager@example.com", "13800000001", "manager");
        ensureSeedUser("employee", "123456", "普通员工", "employee@example.com", "13800000002", "employee");
    }

    private void ensureSeedUser(String username, String password, String nickname, String email, String phone, String roleCode) {
        Role role = roleRepository.findByCode(roleCode);
        User existing = userRepository.findByUsername(username);
        if (existing == null) {
            User user = new User();
            user.setUsername(username);
            user.setPassword(password);
            user.setNickname(nickname);
            user.setEmail(email);
            user.setPhone(phone);
            user.setStatus(1);
            user.setCreatedAt(LocalDateTime.now());
            user.setUpdatedAt(LocalDateTime.now());
            User savedUser = userRepository.save(user);
            syncUserRoles(savedUser.getId(), role == null ? List.of() : List.of(role.getId()));
            return;
        }
        existing = relationLoader.attachRoles(existing);
        boolean missingRole = existing.getRoles() == null
                || existing.getRoles().stream().noneMatch(item -> roleCode.equalsIgnoreCase(item.getCode()));
        if (missingRole && role != null) {
            syncUserRoles(existing.getId(), List.of(role.getId()));
            existing.setUpdatedAt(LocalDateTime.now());
            userRepository.save(existing);
        }
    }

    private void syncUserRoles(Long userId, List<Long> roleIds) {
        userRoleRepository.deleteByUserId(userId);
        if (roleIds != null && !roleIds.isEmpty()) {
            userRoleRepository.insertBatch(userId, roleIds);
        }
    }
}
