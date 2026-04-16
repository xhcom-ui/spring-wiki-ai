package com.xxx.rag.common.constant;

/**
 * RAG 常量类
 */
public class RagConstant {
    // 集合名称
    public static final String COLLECTION_NAME = "airag_demo";
    
    // 相似度阈值
    public static final float SIMILARITY_THRESHOLD = 0.7f;
    
    // 默认 topK
    public static final int DEFAULT_TOP_K = 5;
    
    // 最大 topK
    public static final int MAX_TOP_K = 10;
    
    // 分块大小
    public static final int CHUNK_SIZE = 1000;
    
    // 分块重叠度
    public static final int CHUNK_OVERLAP = 200;
    
    // 批处理大小
    public static final int BATCH_SIZE = 100;
    
    // 文档类型
    public static final String DOC_TYPE_PDF = "pdf";
    public static final String DOC_TYPE_DOCX = "docx";
    public static final String DOC_TYPE_XLSX = "xlsx";
    public static final String DOC_TYPE_TXT = "txt";
    
    // 元数据键
    public static final String META_DOC_ID = "docId";
    public static final String META_SOURCE = "source";
    public static final String META_CREATE_TIME = "createTime";
    
    // 错误码
    public static final int ERROR_CODE_SUCCESS = 0;
    public static final int ERROR_CODE_BUSINESS = 1001;
    public static final int ERROR_CODE_RAG = 2001;
    public static final int ERROR_CODE_MODEL = 3001;
    
    // 错误消息
    public static final String ERROR_MSG_BUSINESS = "业务错误";
    public static final String ERROR_MSG_RAG = "RAG 流程错误";
    public static final String ERROR_MSG_MODEL = "模型调用错误";
}
