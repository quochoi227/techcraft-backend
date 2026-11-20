package com.techcraft.techcraftbackend.service.impl;

import com.techcraft.techcraftbackend.service.TextChunkerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class TextChunkerServiceImpl implements TextChunkerService {

    private static final int DEFAULT_CHUNK_SIZE = 700;
    private static final int DEFAULT_OVERLAP = 100;

    @Override
    public List<String> chunk(String text) {
        return chunk(text, DEFAULT_CHUNK_SIZE, DEFAULT_OVERLAP);
    }

    @Override
    public List<String> chunk(String text, int chunkSize, int overlap) {
        List<String> chunks = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return chunks;
        }

        String cleanedText = text.replace("\r\n", "\n").trim();
        if (cleanedText.length() <= chunkSize) {
            chunks.add(cleanedText);
            return chunks;
        }

        int start = 0;
        int textLength = cleanedText.length();

        while (start < textLength) {
            int end = Math.min(start + chunkSize, textLength);

            if (end < textLength) {
                // Tìm điểm ngắt tự nhiên gần nhất trong phạm vi overlap
                int breakPoint = findNaturalBreakPoint(cleanedText, start, end);
                if (breakPoint > start + (chunkSize / 2)) {
                    end = breakPoint;
                }
            }

            String chunkContent = cleanedText.substring(start, end).trim();
            if (!chunkContent.isBlank()) {
                chunks.add(chunkContent);
            }

            if (end >= textLength) {
                break;
            }

            // Dịch chuyển con trỏ start có tính đến overlap
            start = Math.max(end - overlap, start + 1);
        }

        log.debug("Chunked text (length: {}) into {} chunks", textLength, chunks.size());
        return chunks;
    }

    private int findNaturalBreakPoint(String text, int start, int end) {
        // 1. Ưu tiên ngắt đoạn (\n\n)
        int doubleNewline = text.lastIndexOf("\n\n", end);
        if (doubleNewline > start + 100) {
            return doubleNewline + 2;
        }

        // 2. Ngắt dòng đơn (\n)
        int singleNewline = text.lastIndexOf("\n", end);
        if (singleNewline > start + 100) {
            return singleNewline + 1;
        }

        // 3. Ngắt câu (. , ! , ?)
        for (int i = end - 1; i >= start + (end - start) / 2; i--) {
            char c = text.charAt(i);
            if ((c == '.' || c == '!' || c == '?') && i + 1 < text.length() && Character.isWhitespace(text.charAt(i + 1))) {
                return i + 1;
            }
        }

        // 4. Ngắt từ (khoảng trắng)
        int lastSpace = text.lastIndexOf(" ", end);
        if (lastSpace > start + 100) {
            return lastSpace + 1;
        }

        return end;
    }
}
