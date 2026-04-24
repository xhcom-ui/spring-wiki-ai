package com.example.flowable.controller;

import com.example.flowable.entity.Menu;
import com.example.flowable.service.MenuService;
import com.example.flowable.service.PermissionService;
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

    @GetMapping("/all")
    public ResponseEntity<List<Menu>> getAllMenus() {
        permissionService.requireLogin();
        return ResponseEntity.ok(menuService.getMenusForCurrentUser());
    }
}
