package com.security.controller;

import com.security.entity.Menu;
import com.security.service.MenuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

/**
 * 主页控制器
 */
@Controller
@RequestMapping("/")
public class HomeController {

    @Autowired
    private MenuService menuService;

    /**
     * 主页
     */
    @GetMapping("home")
    public String home() {
        return "home";
    }

    /**
     * 公共页面
     */
    @GetMapping("public")
    public String publicPage() {
        return "public";
    }

    /**
     * 用户页面
     */
    @GetMapping("user")
    public String userPage() {
        return "user";
    }

    /**
     * 管理员页面
     */
    @GetMapping("admin")
    public String adminPage() {
        return "admin";
    }

    /**
     * 获取用户菜单
     */
    @GetMapping("api/menus")
    @ResponseBody
    public List<Menu> getMenus() {
        return menuService.getMenusByUserPermissions();
    }

}
