package com.techcraft.techcraftbackend.service.impl;

import com.google.genai.Client;
import com.google.genai.errors.ApiException;
import com.google.genai.errors.GenAiIOException;
import com.google.genai.types.Content;
import com.google.genai.types.ContentEmbedding;
import com.google.genai.types.EmbedContentConfig;
import com.google.genai.types.EmbedContentResponse;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import com.techcraft.techcraftbackend.dto.chat.ChatMessageDto;
import com.techcraft.techcraftbackend.exception.AppException;
import com.techcraft.techcraftbackend.service.GeminiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiServiceImpl implements GeminiService {

    private static final int MAX_EMBED_BATCH_SIZE = 100;
    private static final String FALLBACK_CHAT_REPLY =
            "Xin lỗi, hiện tại tôi không thể xử lý câu trả lời. Vui lòng thử lại sau.";

    private final Client geminiClient;

    @Value("${gemini.chat-model:gemini-3.5-flash-lite}")
    private String chatModel;

    @Value("${gemini.embedding-model:gemini-embedding-2}")
    private String embeddingModel;

    @Value("${gemini.embedding-dimension:3072}")
    private int embeddingDimension;

    @Value("${gemini.temperature:0.3}")
    private double temperature;

    @Override
    public float[] generateEmbedding(String text) {
        if (text == null || text.isBlank()) {
            throw new AppException("Nội dung văn bản để tạo embedding không được để trống.", HttpStatus.BAD_REQUEST);
        }

        try {
            EmbedContentResponse response = geminiClient.models.embedContent(
                    embeddingModel,
                    text,
                    buildEmbedConfig()
            );
            return extractEmbeddings(response, 1).get(0);
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            throw handleSdkException("Gemini Embedding", e);
        }
    }

    @Override
    public List<float[]> generateEmbeddings(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            throw new AppException("Danh sách văn bản để tạo embedding không được để trống.", HttpStatus.BAD_REQUEST);
        }
        for (String text : texts) {
            if (text == null || text.isBlank()) {
                throw new AppException("Nội dung văn bản để tạo embedding không được để trống.", HttpStatus.BAD_REQUEST);
            }
        }

        EmbedContentConfig config = buildEmbedConfig();
        List<float[]> allEmbeddings = new ArrayList<>(texts.size());

        try {
            for (int i = 0; i < texts.size(); i += MAX_EMBED_BATCH_SIZE) {
                List<String> batch = texts.subList(i, Math.min(i + MAX_EMBED_BATCH_SIZE, texts.size()));
                EmbedContentResponse response = geminiClient.models.embedContent(embeddingModel, batch, config);
                allEmbeddings.addAll(extractEmbeddings(response, batch.size()));
            }
            return allEmbeddings;
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            throw handleSdkException("Gemini Batch Embedding", e);
        }
    }

    @Override
    public String generateText(String systemInstruction, List<ChatMessageDto> history, String userMessage) {
        if (userMessage == null || userMessage.isBlank()) {
            throw new AppException("Nội dung tin nhắn người dùng không được để trống.", HttpStatus.BAD_REQUEST);
        }

        GenerateContentConfig.Builder configBuilder = GenerateContentConfig.builder()
                .temperature((float) temperature);

        if (systemInstruction != null && !systemInstruction.isBlank()) {
            configBuilder.systemInstruction(Content.fromParts(Part.fromText(systemInstruction)));
        }

        List<Content> contents = buildConversationContents(history, userMessage);

        try {
            GenerateContentResponse response = geminiClient.models.generateContent(
                    chatModel,
                    contents,
                    configBuilder.build()
            );

            String text = extractReplyText(response);
            if (text != null && !text.isBlank()) {
                return text.trim();
            }

            log.warn("Gemini không trả về nội dung hợp lệ: {}", response);
            return FALLBACK_CHAT_REPLY;
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            throw handleSdkException("Gemini Chat", e);
        }
    }

    private String extractReplyText(GenerateContentResponse response) {
        if (response == null) {
            return null;
        }
        var candidates = response.candidates().orElse(List.of());
        if (candidates.isEmpty()) {
            return null;
        }
        var contentOpt = candidates.get(0).content();
        if (contentOpt.isEmpty() || contentOpt.get().parts().orElse(List.of()).isEmpty()) {
            return null;
        }
        return response.text();
    }

    private EmbedContentConfig buildEmbedConfig() {
        return EmbedContentConfig.builder()
                .outputDimensionality(embeddingDimension)
                .build();
    }

    private List<float[]> extractEmbeddings(EmbedContentResponse response, int expectedCount) {
        List<ContentEmbedding> contentEmbeddings = response != null
                ? response.embeddings().orElse(List.of())
                : List.of();

        if (contentEmbeddings.size() != expectedCount) {
            throw new AppException(
                    "Không nhận đủ vector embedding từ Google Gemini API.",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }

        List<float[]> vectors = new ArrayList<>(contentEmbeddings.size());
        for (ContentEmbedding contentEmbedding : contentEmbeddings) {
            List<Float> values = contentEmbedding.values().orElse(List.of());
            if (values.isEmpty()) {
                throw new AppException(
                        "Không nhận được vector embedding từ Google Gemini API.",
                        HttpStatus.INTERNAL_SERVER_ERROR
                );
            }
            float[] vector = new float[values.size()];
            for (int i = 0; i < values.size(); i++) {
                vector[i] = values.get(i);
            }
            vectors.add(vector);
        }
        return vectors;
    }

    private List<Content> buildConversationContents(List<ChatMessageDto> history, String userMessage) {
        List<Content> contents = new ArrayList<>();

        if (history != null && !history.isEmpty()) {
            for (ChatMessageDto msg : history) {
                if (msg == null || msg.getContent() == null || msg.getContent().isBlank()) {
                    continue;
                }
                String role = "assistant".equalsIgnoreCase(msg.getRole()) || "model".equalsIgnoreCase(msg.getRole())
                        ? "model"
                        : "user";
                contents.add(Content.builder()
                        .role(role)
                        .parts(List.of(Part.fromText(msg.getContent())))
                        .build());
            }
        }

        contents.add(Content.builder()
                .role("user")
                .parts(List.of(Part.fromText(userMessage)))
                .build());

        return contents;
    }

    private AppException handleSdkException(String operation, Exception e) {
        if (e instanceof ApiException apiException) {
            log.error("Lỗi từ {} API (code={}, status={}): {}",
                    operation, apiException.code(), apiException.status(), apiException.message(), apiException);
            return new AppException(
                    "Lỗi kết nối dịch vụ " + operation + ": " + apiException.message(),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
        if (e instanceof GenAiIOException ioException) {
            log.error("Lỗi I/O khi gọi {} SDK: {}", operation, ioException.getMessage(), ioException);
            return new AppException(
                    "Lỗi kết nối dịch vụ " + operation + ": " + ioException.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
        log.error("Lỗi không xác định khi gọi {}: {}", operation, e.getMessage(), e);
        return new AppException(
                "Lỗi kết nối dịch vụ " + operation + ": " + e.getMessage(),
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }
}
