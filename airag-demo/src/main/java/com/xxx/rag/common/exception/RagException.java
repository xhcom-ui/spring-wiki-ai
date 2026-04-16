package com.xxx.rag.common.exception;

import com.xxx.rag.common.constant.RagConstant;

/**
 * RAG 流程异常
 */
public class RagException extends RuntimeException {
    private int code;

    public RagException(String message) {
        super(message);
        this.code = RagConstant.ERROR_CODE_RAG;
    }

    public RagException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
