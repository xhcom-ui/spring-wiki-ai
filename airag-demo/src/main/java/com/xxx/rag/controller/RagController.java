package com.xxx.rag.controller;

import com.xxx.rag.common.result.Result;
import com.xxx.rag.service.impl.RagServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

/**
 * RAG 控制器
 */
@Slf4j
@RestController
@RequestMapping("/rag")
public class RagController {

    private final RagServiceImpl ragService;

    public RagController(RagServiceImpl ragService) {
        this.ragService = ragService;
    }

    /**
     * 上传文档
     */
    @PostMapping("/upload")
    public Result<?> upload(@RequestParam("file") MultipartFile file, 
                          @RequestParam("docType") String docType) {
        log.info("收到文档上传请求，文件名: {}, 类型: {}", file.getOriginalFilename(), docType);

        try {
            // 保存临时文件
            File tempFile = File.createTempFile("temp", "." + docType);
            file.transferTo(tempFile);

            // 处理文档
            ragService.uploadDocument(tempFile, docType);

            // 删除临时文件
            tempFile.delete();

            return Result.success("文档上传成功");
        } catch (IOException e) {
            log.error("文件处理失败: {}", e.getMessage(), e);
            return Result.fail(1001, "文件处理失败: " + e.getMessage());
        } catch (Exception e) {
            log.error("文档上传失败: {}", e.getMessage(), e);
            return Result.fail(1001, e.getMessage());
        }
    }

    /**
     * 查询
     */
    @PostMapping("/query")
    public Result<?> query(@RequestBody QueryRequest request) {
        log.info("收到查询请求: {}", request.getQuestion());

        try {
            String answer = ragService.query(request.getQuestion());
            return Result.success(answer);
        } catch (Exception e) {
            log.error("查询失败: {}", e.getMessage(), e);
            return Result.fail(1001, e.getMessage());
        }
    }

    /**
     * 清空向量库
     */
    @PostMapping("/clear")
    public Result<?> clear() {
        log.info("收到清空向量库请求");

        try {
            ragService.clearVectorStore();
            return Result.success("向量库清空成功");
        } catch (Exception e) {
            log.error("清空向量库失败: {}", e.getMessage(), e);
            return Result.fail(1001, e.getMessage());
        }
    }

    /**
     * 查询请求对象
     */
    public static class QueryRequest {
        private String question;

        public String getQuestion() {
            return question;
        }

        public void setQuestion(String question) {
            this.question = question;
        }
    }
}
