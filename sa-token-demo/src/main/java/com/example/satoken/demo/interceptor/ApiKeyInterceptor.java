package com.example.satoken.demo.interceptor;

import cn.dev33.satoken.apikey.SaApiKeyTemplate;
import cn.dev33.satoken.exception.ApiKeyException;
import cn.dev33.satoken.stp.StpUtil;
import com.example.satoken.demo.limiter.ApiKeyRateLimiter;
import com.example.satoken.demo.limiter.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
@Slf4j
public class ApiKeyInterceptor implements HandlerInterceptor {

    @Resource
    private SaApiKeyTemplate saApiKeyTemplate;

    @Resource
    private RateLimiter rateLimiter;

    @Resource
    private ApiKeyRateLimiter apiKeyRateLimiter;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 1. IP 限流
        String clientIp = getClientIp(request);
        if (!rateLimiter.tryAcquire(clientIp)) {
            response.setStatus(429);
            response.getWriter().write("{\"code\":429,\"msg\":\"请求过于频繁，请稍后再试\"}");
            return false;
        }

        // 2. 获取 API Key
        String apiKey = getApiKey(request);
        if (apiKey == null || apiKey.isEmpty()) {
            response.setStatus(401);
            response.getWriter().write("{\"code\":401,\"msg\":\"请提供 API Key\"}");
            return false;
        }

        try {
            // 3. 验证 API Key 有效性
            String appId = saApiKeyTemplate.getApiKey(apiKey);
            if (appId == null) {
                throw new ApiKeyException("API Key 无效");
            }

            // 4. API Key 调用频率限制
            if (!apiKeyRateLimiter.tryAcquire(apiKey)) {
                response.setStatus(429);
                response.getWriter().write("{\"code\":429,\"msg\":\"API Key 调用过于频繁，请稍后再试\"}");
                return false;
            }

            // 5. 将 appId 存入请求属性，后续可以使用
            request.setAttribute("appId", appId);
            return true;
        } catch (ApiKeyException e) {
            response.setStatus(401);
            response.getWriter().write("{\"code\":401,\"msg\":\"API Key 无效或已过期\"}");
            return false;
        }
    }

    private String getApiKey(HttpServletRequest request) {
        // 方式一：Header Authorization: sk-xxx
        String auth = request.getHeader("Authorization");
        if (auth != null && !auth.isEmpty()) {
            return auth;
        }

        // 方式二：URL 参数 apiKey=xxx
        String apiKey = request.getParameter("apiKey");
        if (apiKey != null && !apiKey.isEmpty()) {
            return apiKey;
        }

        return null;
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
