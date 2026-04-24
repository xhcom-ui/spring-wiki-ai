package com.syn.data.config;

import cn.dev33.satoken.stp.StpInterface;
import com.syn.data.entity.User;
import com.syn.data.service.UserService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class SaTokenPermissionService implements StpInterface {

    @Resource
    private UserService userService;

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        User user = userService.getUserById(Long.valueOf(String.valueOf(loginId)));
        if (user == null || user.getRole() == null) {
            return List.of();
        }
        if ("admin".equalsIgnoreCase(user.getRole())) {
            return List.of("*");
        }
        List<String> permissions = new ArrayList<>();
        permissions.add("monitoring:view");
        permissions.add("quality:view");
        return permissions;
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        User user = userService.getUserById(Long.valueOf(String.valueOf(loginId)));
        if (user == null || user.getRole() == null || user.getRole().isBlank()) {
            return List.of();
        }
        return List.of(user.getRole());
    }
}
