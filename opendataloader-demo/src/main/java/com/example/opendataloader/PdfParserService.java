package com.example.opendataloader;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
public class PdfParserService {

    @Value("${opendataloader.python-path}")
    private String pythonPath;

    @Value("${opendataloader.script-path}")
    private String scriptPath;

    @Value("${opendataloader.output-dir}")
    private String outputDir;

    private final ExecutorService executorService = Executors.newFixedThreadPool(5);
    private final Map<String, ParseResult> resultCache = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> progressMap = new ConcurrentHashMap<>();

    public CompletableFuture<String> parsePdfAsync(MultipartFile file, String formats) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return parsePdf(file, formats);
            } catch (Exception e) {
                log.error("PDF 解析失败: {}", e.getMessage(), e);
                throw new CompletionException(e);
            }
        }, executorService);
    }

    public String parsePdf(MultipartFile file, String formats) throws IOException, InterruptedException {
        // 生成文件哈希作为缓存键
        String fileHash = generateFileHash(file);
        String cacheKey = fileHash + "_" + formats;

        // 检查缓存
        if (resultCache.containsKey(cacheKey)) {
            log.info("使用缓存的解析结果");
            return resultCache.get(cacheKey).getMessage();
        }

        // 生成唯一的文件名，避免冲突
        String fileName = UUID.randomUUID().toString() + ".pdf";
        Path tempPath = Paths.get(System.getProperty("java.io.tmpdir"), fileName);
        String taskId = UUID.randomUUID().toString();

        try {
            // 初始化进度
            progressMap.put(taskId, new AtomicInteger(0));

            // 保存上传的文件到临时目录
            Files.write(tempPath, file.getBytes());
            progressMap.get(taskId).set(20);

            // 确保输出目录存在
            Path outputPath = Paths.get(outputDir);
            Files.createDirectories(outputPath);
            progressMap.get(taskId).set(40);

            // 构建 Python 命令
            String command = pythonPath + " " + scriptPath + " --input " + tempPath.toString() + " --output " + outputPath.toString() + " --formats " + formats;
            log.info("执行命令: {}", command);
            progressMap.get(taskId).set(60);

            // 执行命令，设置超时
            Process process = Runtime.getRuntime().exec(command);
            boolean completed = process.waitFor(5, TimeUnit.MINUTES);

            if (!completed) {
                process.destroy();
                throw new RuntimeException("PDF 解析超时");
            }

            int exitCode = process.exitValue();
            progressMap.get(taskId).set(80);

            // 读取命令输出
            StringBuilder output = new StringBuilder();
            try (InputStream inputStream = process.getInputStream();
                 java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(inputStream))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            // 读取错误输出
            StringBuilder errorOutput = new StringBuilder();
            try (InputStream errorStream = process.getErrorStream();
                 java.io.BufferedReader errorReader = new java.io.BufferedReader(new java.io.InputStreamReader(errorStream))) {
                String line;
                while ((line = errorReader.readLine()) != null) {
                    errorOutput.append(line).append("\n");
                }
            }

            progressMap.get(taskId).set(100);

            // 检查执行结果
            if (exitCode == 0) {
                log.info("PDF 解析成功: {}", output.toString());
                String resultMessage = "PDF 解析成功，输出目录: " + outputPath.toString();
                resultCache.put(cacheKey, new ParseResult(resultMessage, outputPath.toString()));
                return resultMessage;
            } else {
                log.error("PDF 解析失败: {}", errorOutput.toString());
                throw new RuntimeException("PDF 解析失败: " + errorOutput.toString());
            }
        } finally {
            // 清理临时文件
            Files.deleteIfExists(tempPath);
            // 移除进度
            progressMap.remove(taskId);
        }
    }

    public List<String> parsePdfBatch(List<MultipartFile> files, String formats) throws IOException, InterruptedException {
        List<CompletableFuture<String>> futures = new ArrayList<>();
        for (MultipartFile file : files) {
            futures.add(parsePdfAsync(file, formats));
        }

        // 等待所有任务完成
        CompletableFuture<Void> allOf = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        allOf.join();

        // 收集结果
        List<String> results = new ArrayList<>();
        for (CompletableFuture<String> future : futures) {
            try {
                results.add(future.get());
            } catch (Exception e) {
                results.add("解析失败: " + e.getMessage());
            }
        }
        return results;
    }

    public int getProgress(String taskId) {
        AtomicInteger progress = progressMap.get(taskId);
        return progress != null ? progress.get() : -1;
    }

    public ParseResult getParseResult(String fileHash, String formats) {
        String cacheKey = fileHash + "_" + formats;
        return resultCache.get(cacheKey);
    }

    private String generateFileHash(MultipartFile file) throws IOException {
        try (InputStream inputStream = file.getInputStream()) {
            byte[] bytes = inputStream.readAllBytes();
            return Base64.getEncoder().encodeToString(java.security.MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (Exception e) {
            log.error("生成文件哈希失败: {}", e.getMessage(), e);
            return UUID.randomUUID().toString();
        }
    }

    public static class ParseResult {
        private final String message;
        private final String outputPath;

        public ParseResult(String message, String outputPath) {
            this.message = message;
            this.outputPath = outputPath;
        }

        public String getMessage() {
            return message;
        }

        public String getOutputPath() {
            return outputPath;
        }
    }

    // 关闭线程池
    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
