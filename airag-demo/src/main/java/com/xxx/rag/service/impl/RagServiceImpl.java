package com.xxx.rag.service.impl;

import com.xxx.rag.common.exception.BusinessException;
import com.xxx.rag.service.facades.RagServiceFacade;
import lombok.extern.slf4j.Slf4j;

import java.io.File;

/**
 * RAG 服务实现
 */
@Slf4j
public class RagServiceImpl {

    private final RagServiceFacade ragServiceFacade;

    public RagServiceImpl(RagServiceFacade ragServiceFacade) {
        this.ragServiceFacade = ragServiceFacade;
    }

    /**
     * 上传文档并索引
     */
    public void uploadDocument(File file, String docType) {
        log.info("开始处理文档: {}, 类型: {}", file.getName(), docType);
        long startTime = System.currentTimeMillis();

        try {
            ragServiceFacade.processDocument(file, docType);
            long endTime = System.currentTimeMillis();
            log.info("文档处理完成，耗时: {}ms", endTime - startTime);
        } catch (Exception e) {
            log.error("文档处理失败: {}", e.getMessage(), e);
            throw new BusinessException("文档处理失败: " + e.getMessage());
        }
    }

    /**
     * 处理用户查询
     */
    public String query(String question) {
        log.info("开始处理查询: {}", question);
        long startTime = System.currentTimeMillis();

        try {
            String answer = ragServiceFacade.processQuery(question);
            long endTime = System.currentTimeMillis();
            log.info("查询处理完成，耗时: {}ms", endTime - startTime);
            return answer;
        } catch (Exception e) {
            log.error("查询处理失败: {}", e.getMessage(), e);
            throw new BusinessException("查询处理失败: " + e.getMessage());
        }
    }

    /**
     * 清空向量库
     */
    public void clearVectorStore() {
        log.info("开始清空向量库");
        try {
            ragServiceFacade.clearVectorStore();
            log.info("向量库清空完成");
        } catch (Exception e) {
            log.error("清空向量库失败: {}", e.getMessage(), e);
            throw new BusinessException("清空向量库失败: " + e.getMessage());
        }
    }
}
