package com.example.activiti.controller;

import com.example.activiti.entity.Menu;
import com.example.activiti.service.PermissionService;
import com.example.activiti.service.MenuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/menu")
public class MenuController {

    @Autowired
    private MenuService menuService;

    @Autowired
    private PermissionService permissionService;

    // 获取所有菜单
    @GetMapping("/all")
    public ResponseEntity<List<Menu>> getAllMenus() {
        permissionService.requireLogin();
        List<Menu> menus = menuService.getMenusForCurrentUser();
        return ResponseEntity.ok(menus);
    }
}
