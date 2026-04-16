package com.xxx.rag.common.exception;

import com.xxx.rag.common.constant.RagConstant;

/**
 * 业务异常
 */
public class BusinessException extends RuntimeException {
    private int code;

    public BusinessException(String message) {
        super(message);
        this.code = RagConstant.ERROR_CODE_BUSINESS;
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
