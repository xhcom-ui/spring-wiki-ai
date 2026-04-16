package com.example.opendataloader;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/pdf")
public class PdfParserController {

    @Autowired
    private PdfParserService pdfParserService;

    @PostMapping("/parse")
    public ResponseEntity<String> parsePdf(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "formats", defaultValue = "markdown,json") String formats) {
        try {
            // 检查文件是否为空
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body("文件不能为空");
            }

            // 检查文件类型
            if (!file.getContentType().equals("application/pdf")) {
                return ResponseEntity.badRequest().body("文件类型必须是 PDF");
            }

            // 解析 PDF 文件
            String result = pdfParserService.parsePdf(file, formats);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("PDF 解析失败: " + e.getMessage());
        }
    }
}
