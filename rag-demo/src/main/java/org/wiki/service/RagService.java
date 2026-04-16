package org.wiki.service;

public interface RagService {
    /**
     * 上传文档并向量化入库
     */
    String uploadDocument(String filePath);

    /**
     * RAG 问答（检索 + 生成）
     */
    String askQuestion(String question);
}