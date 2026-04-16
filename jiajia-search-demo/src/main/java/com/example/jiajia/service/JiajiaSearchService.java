package com.example.jiajia.service;

import com.example.jiajia.entity.Document;
import com.example.jiajia.repository.DocumentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class JiajiaSearchService {

    @Autowired
    private DocumentRepository documentRepository;

    @Value("${jiajia.python-path}")
    private String pythonPath;

    @Value("${jiajia.script-path}")
    private String scriptPath;

    @Value("${jiajia.vector-model-path}")
    private String vectorModelPath;

    @Value("${jiajia.rerank-model-path}")
    private String rerankModelPath;

    @Value("${jiajia.vector-dim}")
    private int vectorDim;

    @Value("${jiajia.rrf-k}")
    private int rrfK;

    @Value("${jiajia.top-k-recall}")
    private int topKRecall;

    @Value("${jiajia.default-query-weight}")
    private double defaultQueryWeight;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public Map<String, Object> generateEmbedding(String text) throws Exception {
        String command = pythonPath + " " + scriptPath + " --action generate_embedding " +
                "--text \"" + text + "\" " +
                "--vector-model-path \"" + vectorModelPath + "\" " +
                "--rerank-model-path \"" + rerankModelPath + "\" " +
                "--vector-dim " + vectorDim + " " +
                "--rrf-k " + rrfK + " " +
                "--top-k-recall " + topKRecall + " " +
                "--default-query-weight " + defaultQueryWeight;

        log.info("执行命令: {}", command);
        Process process = Runtime.getRuntime().exec(command);
        int exitCode = process.waitFor();

        String output = readStream(process.getInputStream());
        String errorOutput = readStream(process.getErrorStream());

        if (exitCode != 0) {
            log.error("生成嵌入失败: {}", errorOutput);
            throw new Exception("生成嵌入失败: " + errorOutput);
        }

        return objectMapper.readValue(output, Map.class);
    }

    public Map<String, Object> generateEmbeddingsBatch(List<String> texts) throws Exception {
        String textsJson = objectMapper.writeValueAsString(texts);
        String command = pythonPath + " " + scriptPath + " --action generate_embeddings_batch " +
                "--texts \"" + textsJson + "\" " +
                "--vector-model-path \"" + vectorModelPath + "\" " +
                "--rerank-model-path \"" + rerankModelPath + "\" " +
                "--vector-dim " + vectorDim + " " +
                "--rrf-k " + rrfK + " " +
                "--top-k-recall " + topKRecall + " " +
                "--default-query-weight " + defaultQueryWeight;

        log.info("执行命令: {}", command);
        Process process = Runtime.getRuntime().exec(command);
        int exitCode = process.waitFor();

        String output = readStream(process.getInputStream());
        String errorOutput = readStream(process.getErrorStream());

        if (exitCode != 0) {
            log.error("批量生成嵌入失败: {}", errorOutput);
            throw new Exception("批量生成嵌入失败: " + errorOutput);
        }

        return objectMapper.readValue(output, Map.class);
    }

    public Map<String, Object> searchPipeline(String query, List<String> documents) throws Exception {
        String documentsJson = objectMapper.writeValueAsString(documents);
        String command = pythonPath + " " + scriptPath + " --action search_pipeline " +
                "--query \"" + query + "\" " +
                "--documents \"" + documentsJson + "\" " +
                "--vector-model-path \"" + vectorModelPath + "\" " +
                "--rerank-model-path \"" + rerankModelPath + "\" " +
                "--vector-dim " + vectorDim + " " +
                "--rrf-k " + rrfK + " " +
                "--top-k-recall " + topKRecall + " " +
                "--default-query-weight " + defaultQueryWeight;

        log.info("执行命令: {}", command);
        Process process = Runtime.getRuntime().exec(command);
        int exitCode = process.waitFor();

        String output = readStream(process.getInputStream());
        String errorOutput = readStream(process.getErrorStream());

        if (exitCode != 0) {
            log.error("搜索流水线失败: {}", errorOutput);
            throw new Exception("搜索流水线失败: " + errorOutput);
        }

        return objectMapper.readValue(output, Map.class);
    }

    public Document saveDocument(String title, String content, String type) {
        Document document = new Document();
        document.setTitle(title);
        document.setContent(content);
        document.setType(type);
        document.setCreatedAt(LocalDateTime.now());
        document.setUpdatedAt(LocalDateTime.now());
        return documentRepository.save(document);
    }

    public List<Document> getAllDocuments() {
        return documentRepository.findAll();
    }

    public Map<String, Object> searchDocuments(String query) throws Exception {
        List<Document> documents = documentRepository.findAll();
        List<String> documentContents = new ArrayList<>();
        for (Document doc : documents) {
            documentContents.add(doc.getContent());
        }
        return searchPipeline(query, documentContents);
    }

    private String readStream(InputStream inputStream) throws Exception {
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }
        return output.toString();
    }
}
