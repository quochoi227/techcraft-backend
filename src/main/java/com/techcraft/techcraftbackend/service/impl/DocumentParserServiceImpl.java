package com.techcraft.techcraftbackend.service.impl;

import com.techcraft.techcraftbackend.exception.BadRequestException;
import com.techcraft.techcraftbackend.service.DocumentParserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
public class DocumentParserServiceImpl implements DocumentParserService {

    private static final long MAX_FILE_SIZE = 15 * 1024 * 1024; // 15MB

    @Override
    public String parse(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Tập tin tải lên không được để trống.");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BadRequestException("Kích thước tập tin vượt quá giới hạn cho phép (tối đa 15MB).");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new BadRequestException("Tên tập tin không hợp lệ.");
        }

        String lowerFilename = originalFilename.toLowerCase();

        try {
            if (lowerFilename.endsWith(".pdf")) {
                return parsePdf(file);
            } else if (lowerFilename.endsWith(".docx")) {
                return parseDocx(file);
            } else if (lowerFilename.endsWith(".txt") || lowerFilename.endsWith(".md")) {
                return parseText(file);
            } else {
                throw new BadRequestException("Định dạng file không được hỗ trợ. Vui lòng tải lên file .pdf, .docx, .txt hoặc .md.");
            }
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.error("Lỗi khi đọc file '{}': {}", originalFilename, e.getMessage(), e);
            throw new BadRequestException("Không thể đọc nội dung file '" + originalFilename + "': " + e.getMessage());
        }
    }

    private String parsePdf(MultipartFile file) throws Exception {
        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            if (text == null || text.isBlank()) {
                throw new BadRequestException("Tập tin PDF không chứa nội dung văn bản (hoặc là file scan ảnh).");
            }
            return text.trim();
        }
    }

    private String parseDocx(MultipartFile file) throws Exception {
        try (InputStream is = file.getInputStream();
             XWPFDocument document = new XWPFDocument(is);
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            String text = extractor.getText();
            if (text == null || text.isBlank()) {
                throw new BadRequestException("Tập tin DOCX không chứa nội dung văn bản.");
            }
            return text.trim();
        }
    }

    private String parseText(MultipartFile file) throws Exception {
        try (InputStream is = file.getInputStream()) {
            String text = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            if (text.isBlank()) {
                throw new BadRequestException("Tập tin văn bản rỗng.");
            }
            return text.trim();
        }
    }
}
