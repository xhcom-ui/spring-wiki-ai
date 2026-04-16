package org.wiki.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.hslf.usermodel.HSLFSlideShow;
import org.apache.poi.hslf.usermodel.HSLFSlide;
import org.apache.poi.hslf.usermodel.HSLFTextShape;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DocumentParserService {

    public String parseDocument(MultipartFile file) throws Exception {
        String fileName = file.getOriginalFilename();
        if (fileName == null) {
            throw new IllegalArgumentException("文件名不能为空");
        }
        
        String extension = getFileExtension(fileName).toLowerCase();
        
        switch (extension) {
            case "txt":
            case "md":
            case "log":
            case "json":
            case "html":
            case "htm":
                return parseTextFile(file);
            case "pdf":
                return parsePdfFile(file);
            case "doc":
                return parseDocFile(file);
            case "docx":
                return parseDocxFile(file);
            case "ppt":
                return parsePptFile(file);
            case "pptx":
                return parsePptxFile(file);
            default:
                throw new IllegalArgumentException("不支持的文件格式: " + extension);
        }
    }

    private String parseTextFile(MultipartFile file) throws Exception {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }

    private String parsePdfFile(MultipartFile file) throws Exception {
        try (PDDocument document = PDDocument.load(file.getInputStream())) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    private String parseDocFile(MultipartFile file) throws Exception {
        try (InputStream is = file.getInputStream();
             HWPFDocument document = new HWPFDocument(is)) {
            WordExtractor extractor = new WordExtractor(document);
            return extractor.getText();
        }
    }

    private String parseDocxFile(MultipartFile file) throws Exception {
        try (InputStream is = file.getInputStream();
             XWPFDocument document = new XWPFDocument(is)) {
            StringBuilder text = new StringBuilder();
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                text.append(paragraph.getText()).append("\n");
            }
            return text.toString();
        }
    }

    private String parsePptFile(MultipartFile file) throws Exception {
        try (InputStream is = file.getInputStream();
             HSLFSlideShow slideshow = new HSLFSlideShow(is)) {
            StringBuilder text = new StringBuilder();
            for (HSLFSlide slide : slideshow.getSlides()) {
                for (org.apache.poi.hslf.usermodel.HSLFShape shape : slide.getShapes()) {
                    if (shape instanceof HSLFTextShape) {
                        HSLFTextShape textShape = (HSLFTextShape) shape;
                        text.append(textShape.getText()).append("\n");
                    }
                }
            }
            return text.toString();
        }
    }

    private String parsePptxFile(MultipartFile file) throws Exception {
        try (InputStream is = file.getInputStream();
             XMLSlideShow slideshow = new XMLSlideShow(is)) {
            StringBuilder text = new StringBuilder();
            for (XSLFSlide slide : slideshow.getSlides()) {
                for (org.apache.poi.xslf.usermodel.XSLFShape shape : slide.getShapes()) {
                    if (shape instanceof XSLFTextShape) {
                        XSLFTextShape textShape = (XSLFTextShape) shape;
                        text.append(textShape.getText()).append("\n");
                    }
                }
            }
            return text.toString();
        }
    }

    private String getFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return "";
        }
        return fileName.substring(lastDotIndex + 1);
    }
}
