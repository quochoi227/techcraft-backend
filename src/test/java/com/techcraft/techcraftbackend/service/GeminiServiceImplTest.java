package com.techcraft.techcraftbackend.service;

import com.google.genai.Client;
import com.google.genai.Models;
import com.google.genai.errors.ApiException;
import com.google.genai.errors.GenAiIOException;
import com.google.genai.types.Candidate;
import com.google.genai.types.Content;
import com.google.genai.types.ContentEmbedding;
import com.google.genai.types.EmbedContentConfig;
import com.google.genai.types.EmbedContentResponse;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import com.techcraft.techcraftbackend.dto.chat.ChatMessageDto;
import com.techcraft.techcraftbackend.exception.AppException;
import com.techcraft.techcraftbackend.service.impl.GeminiServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GeminiServiceImplTest {

    @Mock
    private Client geminiClient;

    @Mock
    private Models models;

    private GeminiServiceImpl geminiService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(geminiClient, "models", models);
        geminiService = new GeminiServiceImpl(geminiClient);
        ReflectionTestUtils.setField(geminiService, "chatModel", "gemini-3.5-flash-lite");
        ReflectionTestUtils.setField(geminiService, "embeddingModel", "gemini-embedding-2");
        ReflectionTestUtils.setField(geminiService, "embeddingDimension", 3072);
        ReflectionTestUtils.setField(geminiService, "temperature", 0.3);
    }

    @Test
    void generateEmbedding_ValidText_ReturnsVectorAndPassesConfig() {
        EmbedContentResponse response = EmbedContentResponse.builder()
                .embeddings(List.of(
                        ContentEmbedding.builder().values(List.of(0.1f, 0.2f, 0.3f)).build()
                ))
                .build();

        when(models.embedContent(eq("gemini-embedding-2"), eq("Bảo hành 36 tháng"), any(EmbedContentConfig.class)))
                .thenReturn(response);

        float[] result = geminiService.generateEmbedding("Bảo hành 36 tháng");

        assertArrayEquals(new float[]{0.1f, 0.2f, 0.3f}, result, 1e-6f);

        ArgumentCaptor<EmbedContentConfig> configCaptor = ArgumentCaptor.forClass(EmbedContentConfig.class);
        verify(models).embedContent(eq("gemini-embedding-2"), eq("Bảo hành 36 tháng"), configCaptor.capture());
        assertEquals(3072, configCaptor.getValue().outputDimensionality().orElseThrow());
    }

    @Test
    void generateEmbedding_BlankText_ThrowsBadRequest() {
        AppException ex = assertThrows(AppException.class, () -> geminiService.generateEmbedding("   "));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        verifyNoInteractions(models);
    }

    @Test
    void generateEmbedding_EmptyResponse_ThrowsInternalServerError() {
        EmbedContentResponse response = EmbedContentResponse.builder()
                .embeddings(List.of())
                .build();

        when(models.embedContent(anyString(), anyString(), any(EmbedContentConfig.class))).thenReturn(response);

        AppException ex = assertThrows(AppException.class, () -> geminiService.generateEmbedding("Hello"));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getStatus());
    }

    @Test
    void generateEmbeddings_MultipleChunksOver100_SplitsIntoBatchesOf100() {
        List<String> texts = new ArrayList<>();
        for (int i = 0; i < 105; i++) {
            texts.add("Chunk " + i);
        }

        when(models.embedContent(eq("gemini-embedding-2"), anyList(), any(EmbedContentConfig.class)))
                .thenAnswer(invocation -> {
                    List<String> batch = invocation.getArgument(1);
                    List<ContentEmbedding> embeddings = batch.stream()
                            .map(item -> ContentEmbedding.builder().values(List.of(0.5f, 0.6f)).build())
                            .toList();
                    return EmbedContentResponse.builder().embeddings(embeddings).build();
                });

        List<float[]> result = geminiService.generateEmbeddings(texts);

        assertEquals(105, result.size());
        assertArrayEquals(new float[]{0.5f, 0.6f}, result.get(0), 1e-6f);
        assertArrayEquals(new float[]{0.5f, 0.6f}, result.get(104), 1e-6f);

        ArgumentCaptor<List<String>> batchCaptor = ArgumentCaptor.forClass(List.class);
        verify(models, times(2)).embedContent(eq("gemini-embedding-2"), batchCaptor.capture(), any(EmbedContentConfig.class));
        assertEquals(100, batchCaptor.getAllValues().get(0).size());
        assertEquals(5, batchCaptor.getAllValues().get(1).size());
    }

    @Test
    void generateEmbeddings_EmptyOrContainsBlank_ThrowsBadRequest() {
        AppException emptyEx = assertThrows(AppException.class, () -> geminiService.generateEmbeddings(List.of()));
        assertEquals(HttpStatus.BAD_REQUEST, emptyEx.getStatus());

        AppException blankItemEx = assertThrows(AppException.class,
                () -> geminiService.generateEmbeddings(List.of("valid", " ")));
        assertEquals(HttpStatus.BAD_REQUEST, blankItemEx.getStatus());

        verifyNoInteractions(models);
    }

    @Test
    void generateText_WithSystemInstructionAndHistory_MapsRolesAndReturnsTrimmedText() {
        GenerateContentResponse response = GenerateContentResponse.builder()
                .candidates(List.of(
                        Candidate.builder()
                                .content(Content.fromParts(Part.fromText("  Chào bạn, cấu hình phù hợp là PC Gaming.  ")))
                                .build()
                ))
                .build();

        when(models.generateContent(eq("gemini-3.5-flash-lite"), anyList(), any(GenerateContentConfig.class)))
                .thenReturn(response);

        List<ChatMessageDto> history = List.of(
                ChatMessageDto.builder().role("user").content("Xin chào").build(),
                ChatMessageDto.builder().role("assistant").content("Chào bạn!").build(),
                ChatMessageDto.builder().role("model").content("Bạn cần tư vấn gì?").build()
        );

        String reply = geminiService.generateText("Bạn là trợ lý TechCraft", history, "Tư vấn PC 20 triệu");

        assertEquals("Chào bạn, cấu hình phù hợp là PC Gaming.", reply);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Content>> contentsCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<GenerateContentConfig> configCaptor = ArgumentCaptor.forClass(GenerateContentConfig.class);
        verify(models).generateContent(eq("gemini-3.5-flash-lite"), contentsCaptor.capture(), configCaptor.capture());

        List<Content> sentContents = contentsCaptor.getValue();
        assertEquals(4, sentContents.size());
        assertEquals("user", sentContents.get(0).role().orElseThrow());
        assertEquals("model", sentContents.get(1).role().orElseThrow());
        assertEquals("model", sentContents.get(2).role().orElseThrow());
        assertEquals("user", sentContents.get(3).role().orElseThrow());
        assertEquals("Tư vấn PC 20 triệu", sentContents.get(3).text());

        GenerateContentConfig sentConfig = configCaptor.getValue();
        assertEquals(0.3f, sentConfig.temperature().orElseThrow(), 1e-6f);
        assertEquals("Bạn là trợ lý TechCraft", sentConfig.systemInstruction().orElseThrow().text());
    }

    @Test
    void generateText_EmptyResponse_ReturnsFallbackMessage() {
        GenerateContentResponse emptyResponse = GenerateContentResponse.builder()
                .candidates(List.of())
                .build();

        when(models.generateContent(anyString(), anyList(), any(GenerateContentConfig.class)))
                .thenReturn(emptyResponse);

        String reply = geminiService.generateText(null, null, "Xin chào");

        assertEquals("Xin lỗi, hiện tại tôi không thể xử lý câu trả lời. Vui lòng thử lại sau.", reply);
    }

    @Test
    void generateText_SdkThrowsApiOrIoException_ThrowsAppException() {
        when(models.generateContent(anyString(), anyList(), any(GenerateContentConfig.class)))
                .thenThrow(new ApiException(429, "RESOURCE_EXHAUSTED", "Quota exceeded"));

        AppException apiEx = assertThrows(AppException.class,
                () -> geminiService.generateText(null, null, "Xin chào"));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, apiEx.getStatus());

        when(models.generateContent(anyString(), anyList(), any(GenerateContentConfig.class)))
                .thenThrow(new GenAiIOException("Network timeout"));

        AppException ioEx = assertThrows(AppException.class,
                () -> geminiService.generateText(null, null, "Xin chào"));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ioEx.getStatus());
    }
}
