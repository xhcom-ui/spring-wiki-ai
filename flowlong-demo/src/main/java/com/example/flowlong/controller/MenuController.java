package com.example.flowlong.controller;

import com.example.flowlong.entity.Menu;
import com.example.flowlong.service.MenuService;
import com.example.flowlong.service.PermissionService;
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
        try {
            permissionService.requireLogin();
            List<Menu> menus = menuService.getMenusForCurrentUser();
            return ResponseEntity.ok(menus);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }
}
