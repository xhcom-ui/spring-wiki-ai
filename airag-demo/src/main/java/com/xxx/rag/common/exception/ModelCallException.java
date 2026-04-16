package com.xxx.rag.common.exception;

import com.xxx.rag.common.constant.RagConstant;

/**
 * 模型调用异常
 */
public class ModelCallException extends RuntimeException {
    private int code;

    public ModelCallException(String message) {
        super(message);
        this.code = RagConstant.ERROR_CODE_MODEL;
    }

    public ModelCallException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
