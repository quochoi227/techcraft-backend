package com.techcraft.techcraftbackend.controller;

import com.techcraft.techcraftbackend.dto.response.RagDocumentResponse;
import com.techcraft.techcraftbackend.service.RagService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AdminRagControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RagService ragService;

    @Test
    void uploadDocument_Unauthenticated_ReturnsUnauthorized() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "policy.txt", "text/plain", "Chính sách bảo hành".getBytes()
        );

        mockMvc.perform(multipart("/api/admin/rag/documents").file(file))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    void uploadDocument_UserRole_ReturnsForbidden() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "policy.txt", "text/plain", "Chính sách bảo hành".getBytes()
        );

        mockMvc.perform(multipart("/api/admin/rag/documents").file(file))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void uploadDocument_AdminRole_ReturnsCreated() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "policy.txt", "text/plain", "Chính sách bảo hành 36 tháng".getBytes()
        );

        UUID docId = UUID.randomUUID();
        RagDocumentResponse response = RagDocumentResponse.builder()
                .id(docId)
                .title("Chính sách bảo hành")
                .sourceFile("policy.txt")
                .chunkCount(1)
                .fileSize(100L)
                .createdAt(Instant.now())
                .build();

        when(ragService.uploadAndIndexDocument(any(), eq("Chính sách bảo hành"))).thenReturn(response);

        mockMvc.perform(multipart("/api/admin/rag/documents")
                        .file(file)
                        .param("title", "Chính sách bảo hành"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(docId.toString()))
                .andExpect(jsonPath("$.data.title").value("Chính sách bảo hành"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAllDocuments_AdminRole_ReturnsList() throws Exception {
        UUID docId = UUID.randomUUID();
        RagDocumentResponse response = RagDocumentResponse.builder()
                .id(docId)
                .title("Chính sách đổi trả")
                .sourceFile("return_policy.pdf")
                .chunkCount(3)
                .createdAt(Instant.now())
                .build();

        when(ragService.getAllDocuments()).thenReturn(List.of(response));

        mockMvc.perform(get("/api/admin/rag/documents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].title").value("Chính sách đổi trả"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteDocument_AdminRole_ReturnsOk() throws Exception {
        UUID docId = UUID.randomUUID();
        doNothing().when(ragService).deleteDocument(docId);

        mockMvc.perform(delete("/api/admin/rag/documents/{id}", docId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
