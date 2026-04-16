package com.example.satoken.demo.interceptor;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.stp.StpUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.io.IOException;
import java.util.Map;

@Component
@Slf4j
public class WebSocketAuthInterceptor implements HandshakeInterceptor {

    @Value("${sa-token.token-name:satoken}")
    private String tokenName;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        // 1. 从请求中获取 Token
        String token = getToken(request);
        if (token == null || token.isEmpty()) {
            response.getBody().write("{\"code\":401,\"msg\":\"请先登录获取 Token\"}".getBytes());
            return false;  // 拒绝握手
        }

        try {
            // 2. 用 Sa-Token 验证 Token，无效会抛出异常
            Object loginId = StpUtil.getLoginIdByToken(token);
            if (loginId == null) {
                throw new NotLoginException("Token 不存在或已过期", token, null);
            }

            // 3. 把用户ID存入attributes，后续 Handler 里直接用
            attributes.put("loginId", loginId);
            attributes.put("token", token);
            return true;  // 允许握手
        } catch (NotLoginException e) {
            response.getBody().write("{\"code\":401,\"msg\":\"Token 无效或已过期\"}".getBytes());
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {
        // 握手完成后的处理，通常不需要做什么
    }

    /**
     * 支持 3 种 Token 传递方式
     */
    private String getToken(ServerHttpRequest request) {
        // 方式一：URL 参数 ws://host/ws?satoken=xxx
        String query = request.getURI().getQuery();
        if (query != null) {
            for (String param : query.split("&")) {
                if (param.startsWith(tokenName + "=")) {
                    return param.substring(tokenName.length() + 1);
                }
                if (param.startsWith("token=")) {
                    return param.substring(6);
                }
            }
        }

        // 方式二：Header satoken: xxx
        String satoken = request.getHeaders().getFirst(tokenName);
        if (satoken != null && !satoken.isEmpty()) {
            return satoken;
        }

        // 方式三：Header Authorization: Bearer xxx
        String auth = request.getHeaders().getFirst("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            return auth.substring(7);
        }

        return null;
    }
}
