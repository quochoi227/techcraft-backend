package com.techcraft.techcraftbackend.service;

import com.techcraft.techcraftbackend.dto.request.ChatRequest;
import com.techcraft.techcraftbackend.dto.response.ChatResponse;
import com.techcraft.techcraftbackend.dto.response.PcCaseResponse;
import com.techcraft.techcraftbackend.enums.ChatIntent;
import com.techcraft.techcraftbackend.repository.projection.ChunkSimilarityProjection;
import com.techcraft.techcraftbackend.service.impl.ChatServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private IntentClassifierService intentClassifierService;

    @Mock
    private RagService ragService;

    @Mock
    private PcConfigSuggestionService pcConfigSuggestionService;

    @Mock
    private GeminiService geminiService;

    @InjectMocks
    private ChatServiceImpl chatService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(chatService, "topK", 4);
        ReflectionTestUtils.setField(chatService, "minSimilarity", 0.55);
    }

    @Test
    void chat_GeneralChat_DoesNotQueryRagNorPcConfig() {
        ChatRequest request = ChatRequest.builder().message("Xin chào shop").build();
        when(intentClassifierService.classify("Xin chào shop")).thenReturn(ChatIntent.GENERAL_CHAT);
        when(geminiService.generateText(anyString(), anyList(), eq("Xin chào shop")))
                .thenReturn("Chào bạn! Tôi có thể giúp gì cho bạn?");

        ChatResponse response = chatService.chat(request);

        assertNotNull(response);
        assertEquals("Chào bạn! Tôi có thể giúp gì cho bạn?", response.getReply());
        assertEquals(ChatIntent.GENERAL_CHAT, response.getDetectedIntent());

        verifyNoInteractions(ragService);
        verify(pcConfigSuggestionService, never()).getAvailablePcCasesPromptText();
    }

    @Test
    void chat_PolicyInquiry_QueriesOnlyRag() {
        ChatRequest request = ChatRequest.builder().message("Chính sách bảo hành").build();
        when(intentClassifierService.classify("Chính sách bảo hành")).thenReturn(ChatIntent.POLICY_INQUIRY);

        ChunkSimilarityProjection chunk = mock(ChunkSimilarityProjection.class);
        when(chunk.getDocumentTitle()).thenReturn("Chính sách bảo hành TechCraft");
        when(chunk.getContent()).thenReturn("Bảo hành 36 tháng");

        when(ragService.searchSimilarChunks(eq("Chính sách bảo hành"), anyInt(), anyDouble()))
                .thenReturn(List.of(chunk));

        when(geminiService.generateText(anyString(), anyList(), eq("Chính sách bảo hành")))
                .thenReturn("Thời gian bảo hành là 36 tháng.");

        ChatResponse response = chatService.chat(request);

        assertNotNull(response);
        assertEquals(ChatIntent.POLICY_INQUIRY, response.getDetectedIntent());
        assertEquals(1, response.getPolicySources().size());
        assertEquals("Chính sách bảo hành TechCraft", response.getPolicySources().get(0));

        verify(ragService, times(1)).searchSimilarChunks(eq("Chính sách bảo hành"), anyInt(), anyDouble());
        verify(pcConfigSuggestionService, never()).getAvailablePcCasesPromptText();
    }

    @Test
    void chat_PcRecommendation_QueriesOnlyPcConfigAndExtractsCards() {
        ChatRequest request = ChatRequest.builder().message("Tư vấn build PC 20 triệu").build();
        when(intentClassifierService.classify("Tư vấn build PC 20 triệu")).thenReturn(ChatIntent.PC_RECOMMENDATION);

        UUID caseId = UUID.randomUUID();
        when(pcConfigSuggestionService.getAvailablePcCasesPromptText())
                .thenReturn("Danh sách PC Case khả dụng: Gaming 20M");

        when(geminiService.generateText(anyString(), anyList(), eq("Tư vấn build PC 20 triệu")))
                .thenReturn("Cấu hình phù hợp là [PC_CASE_ID: " + caseId + "] Gaming 20M.");

        when(pcConfigSuggestionService.extractPcCaseIds(anyString())).thenReturn(Set.of(caseId));
        PcCaseResponse caseResponse = PcCaseResponse.builder().id(caseId).name("Gaming 20M").build();
        when(pcConfigSuggestionService.findFullPcCasesByIds(Set.of(caseId))).thenReturn(List.of(caseResponse));

        ChatResponse response = chatService.chat(request);

        assertNotNull(response);
        assertEquals(ChatIntent.PC_RECOMMENDATION, response.getDetectedIntent());
        assertEquals(1, response.getSuggestedPcCases().size());
        assertEquals("Gaming 20M", response.getSuggestedPcCases().get(0).getName());

        verify(pcConfigSuggestionService, times(1)).getAvailablePcCasesPromptText();
        verifyNoInteractions(ragService);
    }
}
