package com.xxx.rag.rag.loader;

import com.xxx.rag.common.exception.RagException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 文档加载器
 */
public class DocumentLoader {

    /**
     * 加载文档内容
     */
    public static String loadDocument(File file, String docType) {
        try {
            switch (docType.toLowerCase()) {
                case "pdf":
                    return loadPdf(file);
                case "docx":
                    return loadDocx(file);
                case "xlsx":
                    return loadXlsx(file);
                case "txt":
                    return loadTxt(file);
                default:
                    throw new RagException("不支持的文档类型: " + docType);
            }
        } catch (Exception e) {
            throw new RagException("文档加载失败: " + e.getMessage());
        }
    }

    /**
     * 加载 PDF 文档
     */
    private static String loadPdf(File file) throws Exception {
        try (PDDocument document = PDDocument.load(file)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    /**
     * 加载 DOCX 文档
     */
    private static String loadDocx(File file) throws Exception {
        try (XWPFDocument document = new XWPFDocument(new FileInputStream(file))) {
            StringBuilder content = new StringBuilder();
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                content.append(paragraph.getText()).append("\n");
            }
            return content.toString();
        }
    }

    /**
     * 加载 XLSX 文档
     */
    private static String loadXlsx(File file) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook(new FileInputStream(file))) {
            StringBuilder content = new StringBuilder();
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                var sheet = workbook.getSheetAt(i);
                content.append("Sheet: " + sheet.getSheetName()).append("\n");
                for (int j = 0; j <= sheet.getLastRowNum(); j++) {
                    var row = sheet.getRow(j);
                    if (row != null) {
                        for (int k = 0; k < row.getLastCellNum(); k++) {
                            var cell = row.getCell(k);
                            if (cell != null) {
                                content.append(cell.toString()).append("\t");
                            }
                        }
                        content.append("\n");
                    }
                }
            }
            return content.toString();
        }
    }

    /**
     * 加载 TXT 文档
     */
    private static String loadTxt(File file) throws Exception {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            return content.toString();
        }
    }
}
