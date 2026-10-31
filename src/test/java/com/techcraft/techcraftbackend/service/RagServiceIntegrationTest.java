package com.techcraft.techcraftbackend.service;

import com.techcraft.techcraftbackend.dto.response.RagDocumentResponse;
import com.techcraft.techcraftbackend.repository.projection.ChunkSimilarityProjection;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest
class RagServiceIntegrationTest {

    @Autowired
    private RagService ragService;

    @MockitoBean
    private GeminiService geminiService;

    @Test
    void uploadAndIndexDocument_RealDatabase_InsertsChunksSuccessfully() {
        // Mock embedding to return a valid 3072-dimensional vector
        float[] dummyVector = new float[3072];
        for (int i = 0; i < 3072; i++) {
            dummyVector[i] = 0.01f;
        }
        when(geminiService.generateEmbedding(anyString())).thenReturn(dummyVector);

        String title = "Chính sách kiểm thử tích hợp " + System.currentTimeMillis();
        String content = "TechCraft cam kết đổi trả linh kiện trong 15 ngày đầu tiên nếu phát hiện lỗi phần cứng từ nhà sản xuất.";
        MockMultipartFile file = new MockMultipartFile(
                "file", "integration_test.txt", "text/plain", content.getBytes()
        );

        // 1. Upload & Index
        RagDocumentResponse response = ragService.uploadAndIndexDocument(file, title);

        assertNotNull(response);
        assertNotNull(response.getId());
        assertEquals(title, response.getTitle());
        assertTrue(response.getChunkCount() > 0);

        // 2. Search similar chunks via native pgvector cosine search
        List<ChunkSimilarityProjection> chunks = ragService.searchSimilarChunks(
                "Đổi trả trong bao nhiêu ngày?", 4, 0.0
        );

        assertNotNull(chunks);
        assertFalse(chunks.isEmpty());

        // 3. Clean up
        ragService.deleteDocument(response.getId());
    }
}
