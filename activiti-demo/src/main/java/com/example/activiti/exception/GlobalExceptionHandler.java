package com.example.activiti.exception;

import cn.dev33.satoken.exception.NotLoginException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotLoginException.class)
    public ResponseEntity<Map<String, Object>> handleNotLogin(NotLoginException exception, HttpServletRequest request) {
        return buildResponse(HttpStatus.UNAUTHORIZED, resolveMessage(exception.getMessage(), "登录状态已失效，请重新登录"), request);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<Map<String, Object>> handleForbidden(ForbiddenException exception, HttpServletRequest request) {
        return buildResponse(HttpStatus.FORBIDDEN, resolveMessage(exception.getMessage(), "当前账号没有访问权限"), request);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Map<String, Object>> handleUnauthorized(UnauthorizedException exception, HttpServletRequest request) {
        return buildResponse(HttpStatus.UNAUTHORIZED, resolveMessage(exception.getMessage(), "当前请求未授权"), request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException exception, HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, resolveMessage(exception.getMessage(), "请求参数不合法"), request);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(BadRequestException exception, HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, resolveMessage(exception.getMessage(), "请求参数不合法"), request);
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(NotFoundException exception, HttpServletRequest request) {
        return buildResponse(HttpStatus.NOT_FOUND, resolveMessage(exception.getMessage(), "请求资源不存在"), request);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<Map<String, Object>> handleConflict(ConflictException exception, HttpServletRequest request) {
        return buildResponse(HttpStatus.CONFLICT, resolveMessage(exception.getMessage(), "请求状态冲突"), request);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntime(RuntimeException exception, HttpServletRequest request) {
        HttpStatus status = resolveRuntimeStatus(exception.getMessage());
        return buildResponse(status, resolveMessage(exception.getMessage(), "请求处理失败"), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception exception, HttpServletRequest request) {
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, resolveMessage(exception.getMessage(), "系统开小差了，请稍后再试"), request);
    }

    private HttpStatus resolveRuntimeStatus(String message) {
        String normalized = message == null ? "" : message.toLowerCase(Locale.ROOT);
        if (normalized.contains("无权限")
                || normalized.contains("无权")
                || normalized.contains("候选范围")
                || normalized.contains("已被其他处理人签收")) {
            return HttpStatus.FORBIDDEN;
        }
        if (normalized.contains("未登录")
                || normalized.contains("登录已失效")
                || normalized.contains("token")
                || normalized.contains("login")) {
            return HttpStatus.UNAUTHORIZED;
        }
        if (normalized.contains("不存在") || normalized.contains("not found")) {
            return HttpStatus.NOT_FOUND;
        }
        if (normalized.contains("不能为空")
                || normalized.contains("已存在")
                || normalized.contains("错误")
                || normalized.contains("失败")
                || normalized.contains("不允许")
                || normalized.contains("无效")
                || normalized.contains("已禁用")) {
            return HttpStatus.BAD_REQUEST;
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    private ResponseEntity<Map<String, Object>> buildResponse(HttpStatus status, String message, HttpServletRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status.value());
        body.put("error", message);
        body.put("path", request.getRequestURI());
        return ResponseEntity.status(status).body(body);
    }

    private String resolveMessage(String rawMessage, String fallback) {
        return rawMessage == null || rawMessage.isBlank() ? fallback : rawMessage;
    }
}
