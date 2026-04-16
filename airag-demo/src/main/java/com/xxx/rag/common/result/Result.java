package com.xxx.rag.common.result;

import com.xxx.rag.common.constant.RagConstant;

/**
 * 统一返回体
 */
public class Result<T> {
    private int code;
    private String msg;
    private T data;

    private Result(int code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    /**
     * 成功返回
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(RagConstant.ERROR_CODE_SUCCESS, "success", data);
    }

    /**
     * 失败返回
     */
    public static <T> Result<T> fail(int code, String msg) {
        return new Result<>(code, msg, null);
    }

    public int getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }

    public T getData() {
        return data;
    }
}
