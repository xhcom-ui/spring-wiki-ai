package com.security.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 认证控制器
 */
@Controller
@RequestMapping("/")
public class AuthController {

    /**
     * 登录页面
     */
    @GetMapping("login")
    public String login() {
        return "login";
    }

    /**
     * 登录处理
     */
    @PostMapping("login")
    public String login(@RequestParam String username, @RequestParam String password) {
        // 登录逻辑由Spring Security处理
        return "redirect:/home";
    }

    /**
     * 注册页面
     */
    @GetMapping("register")
    public String register() {
        return "register";
    }

    /**
     * 注册处理
     */
    @PostMapping("register")
    public String register(@RequestParam String username, @RequestParam String password, @RequestParam String name) {
        // 注册逻辑
        return "redirect:/login";
    }

    /**
     * 注销处理
     */
    @GetMapping("logout")
    public String logout() {
        // 注销逻辑由Spring Security处理
        return "redirect:/login";
    }

}
