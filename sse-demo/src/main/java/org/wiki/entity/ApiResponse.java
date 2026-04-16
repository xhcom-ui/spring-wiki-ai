package org.wiki.entity;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.io.Serializable;

/**
 * 通用API响应封装
 * 用于统一所有接口的返回格式
 *
 * @param <T> 数据类型
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApiResponse<T> implements Serializable {
    private static final long serialVersionUID = 1L;
    
    /**
     * 响应状态码
     */
    private Integer code;
    
    /**
     * 响应消息
     */
    private String msg;
    
    /**
     * 响应数据
     */
    private T data;
    
    /**
     * 响应时间戳
     */
    private Long timestamp = System.currentTimeMillis();
    
    /**
     * 预定义状态码
     */
    public static class Code {
        public static final int SUCCESS = 200;
        public static final int CREATED = 201;
        public static final int ACCEPTED = 202;
        
        public static final int BAD_REQUEST = 400;
        public static final int UNAUTHORIZED = 401;
        public static final int FORBIDDEN = 403;
        public static final int NOT_FOUND = 404;
        public static final int METHOD_NOT_ALLOWED = 405;
        public static final int CONFLICT = 409;
        
        public static final int INTERNAL_ERROR = 500;
        public static final int SERVICE_UNAVAILABLE = 503;
        public static final int GATEWAY_TIMEOUT = 504;
    }
    
    /**
     * 预定义消息
     */
    public static class Message {
        public static final String SUCCESS = "操作成功";
        public static final String FAILED = "操作失败";
        public static final String UNAUTHORIZED = "未授权访问";
        public static final String FORBIDDEN = "权限不足";
        public static final String NOT_FOUND = "资源不存在";
        public static final String PARAM_ERROR = "参数错误";
        public static final String SYSTEM_ERROR = "系统异常";
    }
    
    /**
     * 私有构造器
     */
    private ApiResponse(Integer code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }
    
    /**
     * 成功响应（默认消息）
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(Code.SUCCESS, Message.SUCCESS, data);
    }
    
    /**
     * 成功响应（带自定义消息）
     */
    public static <T> ApiResponse<T> success(T data, String msg) {
        return new ApiResponse<>(Code.SUCCESS, msg, data);
    }
    
    /**
     * 成功响应（无数据）
     */
    public static <T> ApiResponse<T> success() {
        return new ApiResponse<>(Code.SUCCESS, Message.SUCCESS, null);
    }
    
    /**
     * 失败响应（带状态码）
     */
    public static <T> ApiResponse<T> error(Integer code, String msg) {
        return new ApiResponse<>(code, msg, null);
    }
    
    /**
     * 失败响应（默认内部错误）
     */
    public static <T> ApiResponse<T> error(String msg) {
        return new ApiResponse<>(Code.INTERNAL_ERROR, msg, null);
    }
    
    /**
     * 参数错误响应
     */
    public static <T> ApiResponse<T> paramError(String msg) {
        return new ApiResponse<>(Code.BAD_REQUEST, msg, null);
    }
    
    /**
     * 未授权响应
     */
    public static <T> ApiResponse<T> unauthorized(String msg) {
        return new ApiResponse<>(Code.UNAUTHORIZED, msg, null);
    }
    
    /**
     * 权限不足响应
     */
    public static <T> ApiResponse<T> forbidden(String msg) {
        return new ApiResponse<>(Code.FORBIDDEN, msg, null);
    }
    
    /**
     * 资源不存在响应
     */
    public static <T> ApiResponse<T> notFound(String msg) {
        return new ApiResponse<>(Code.NOT_FOUND, msg, null);
    }
    
    /**
     * 判断是否成功
     */
    public boolean isSuccess() {
        return Code.SUCCESS == this.code;
    }
    
    /**
     * 获取数据（带默认值）
     */
    public T getDataOrDefault(T defaultValue) {
        return this.data != null ? this.data : defaultValue;
    }
}