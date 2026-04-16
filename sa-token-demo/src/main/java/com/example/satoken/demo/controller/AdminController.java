package com.example.satoken.demo.controller;

import cn.dev33.satoken.apikey.SaApiKeyTemplate;
import cn.dev33.satoken.stp.StpUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/admin")
@Slf4j
public class AdminController {

    @Resource
    private SaApiKeyTemplate saApiKeyTemplate;

    // 登录接口
    @PostMapping("/login")
    public Map<String, Object> login(@RequestParam String username, @RequestParam String password) {
        // 简单的用户名密码验证
        if ("admin".equals(username) && "admin123".equals(password)) {
            // 登录并生成 token
            StpUtil.login(1L);
            String token = StpUtil.getTokenInfo().getTokenValue();
            Map<String, Object> result = new HashMap<>();
            result.put("code", 200);
            result.put("msg", "登录成功");
            result.put("data", token);
            return result;
        } else {
            Map<String, Object> result = new HashMap<>();
            result.put("code", 401);
            result.put("msg", "用户名或密码错误");
            return result;
        }
    }

    // 生成 API Key
    @PostMapping("/api-key/generate")
    public Map<String, Object> generateApiKey(@RequestParam String appId) {
        // 验证登录状态
        StpUtil.checkLogin();
        
        // 生成 API Key
        String apiKey = saApiKeyTemplate.generateApiKey(appId);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("msg", "API Key 生成成功");
        result.put("data", apiKey);
        return result;
    }

    // 撤销 API Key
    @PostMapping("/api-key/revoke")
    public Map<String, Object> revokeApiKey(@RequestParam String apiKey) {
        // 验证登录状态
        StpUtil.checkLogin();
        
        // 撤销 API Key
        boolean success = saApiKeyTemplate.revokeApiKey(apiKey);
        Map<String, Object> result = new HashMap<>();
        if (success) {
            result.put("code", 200);
            result.put("msg", "API Key 撤销成功");
        } else {
            result.put("code", 400);
            result.put("msg", "API Key 撤销失败");
        }
        return result;
    }

    // 获取 API Key 信息
    @GetMapping("/api-key/info")
    public Map<String, Object> getApiKeyInfo(@RequestParam String apiKey) {
        // 验证登录状态
        StpUtil.checkLogin();
        
        // 获取 API Key 信息
        String appId = saApiKeyTemplate.getApiKey(apiKey);
        Map<String, Object> result = new HashMap<>();
        if (appId != null) {
            result.put("code", 200);
            result.put("msg", "获取成功");
            Map<String, Object> data = new HashMap<>();
            data.put("appId", appId);
            data.put("apiKey", apiKey);
            result.put("data", data);
        } else {
            result.put("code", 400);
            result.put("msg", "API Key 不存在");
        }
        return result;
    }

    // 登出接口
    @PostMapping("/logout")
    public Map<String, Object> logout() {
        // 登出
        StpUtil.logout();
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("msg", "登出成功");
        return result;
    }
}
