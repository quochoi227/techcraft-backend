package com.techcraft.techcraftbackend.service;

import com.techcraft.techcraftbackend.service.impl.TextChunkerServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TextChunkerServiceTest {

    private TextChunkerService textChunkerService;

    @BeforeEach
    void setUp() {
        textChunkerService = new TextChunkerServiceImpl();
    }

    @Test
    void chunk_ShortText_ShouldReturnSingleChunk() {
        String text = "Chính sách bảo hành của TechCraft: Đổi trả trong 15 ngày đầu.";
        List<String> chunks = textChunkerService.chunk(text);

        assertEquals(1, chunks.size());
        assertEquals(text, chunks.get(0));
    }

    @Test
    void chunk_EmptyOrNullText_ShouldReturnEmptyList() {
        assertTrue(textChunkerService.chunk(null).isEmpty());
        assertTrue(textChunkerService.chunk("   ").isEmpty());
    }

    @Test
    void chunk_LongText_ShouldSplitWithOverlapAndNaturalBreak() {
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= 30; i++) {
            sb.append("Điều ").append(i).append(": Khách hàng được đổi mới linh kiện lỗi trong vòng 30 ngày nếu phát hiện lỗi do nhà sản xuất. ");
        }

        List<String> chunks = textChunkerService.chunk(sb.toString(), 200, 40);

        assertTrue(chunks.size() > 1);
        for (String chunk : chunks) {
            assertFalse(chunk.isBlank());
            assertTrue(chunk.length() <= 250);
        }
    }
}
