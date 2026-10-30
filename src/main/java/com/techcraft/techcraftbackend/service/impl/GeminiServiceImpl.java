package com.techcraft.techcraftbackend.service.impl;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.techcraft.techcraftbackend.dto.chat.ChatMessageDto;
import com.techcraft.techcraftbackend.exception.AppException;
import com.techcraft.techcraftbackend.service.GeminiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.*;

@Slf4j
@Service
public class GeminiServiceImpl implements GeminiService {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Value("${gemini.api-key}")
    private String apiKey;

    @Value("${gemini.base-url:https://generativelanguage.googleapis.com/v1beta}")
    private String baseUrl;

    @Value("${gemini.chat-model:gemini-3.5-flash-lite}")
    private String chatModel;

    @Value("${gemini.embedding-model:gemini-embedding-2}")
    private String embeddingModel;

    @Value("${gemini.embedding-dimension:3072}")
    private int embeddingDimension;

    @Value("${gemini.temperature:0.3}")
    private double temperature;

    public GeminiServiceImpl(ObjectMapper objectMapper) {
        this.restClient = RestClient.builder().build();
        this.objectMapper = objectMapper;
    }

    @Override
    public float[] generateEmbedding(String text) {
        if (text == null || text.isBlank()) {
            throw new AppException("Nội dung văn bản để tạo embedding không được để trống.", HttpStatus.BAD_REQUEST);
        }

        String url = String.format("%s/models/%s:embedContent?key=%s", baseUrl, embeddingModel, apiKey);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "models/" + embeddingModel);
        requestBody.put("content", Map.of(
                "parts", List.of(Map.of("text", text))
        ));
        requestBody.put("outputDimensionality", embeddingDimension);

        try {
            String responseStr = restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            JsonNode rootNode = objectMapper.readTree(responseStr);
            JsonNode valuesNode = rootNode.path("embedding").path("values");

            if (!valuesNode.isArray() || valuesNode.isEmpty()) {
                throw new AppException("Không nhận được vector embedding từ Google Gemini API.", HttpStatus.INTERNAL_SERVER_ERROR);
            }

            float[] embedding = new float[valuesNode.size()];
            for (int i = 0; i < valuesNode.size(); i++) {
                embedding[i] = (float) valuesNode.get(i).asDouble();
            }

            return embedding;
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("Lỗi khi gọi Gemini Embedding API: {}", e.getMessage(), e);
            throw new AppException("Lỗi kết nối dịch vụ Gemini Embedding: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public String generateText(String systemInstruction, List<ChatMessageDto> history, String userMessage) {
        if (userMessage == null || userMessage.isBlank()) {
            throw new AppException("Nội dung tin nhắn người dùng không được để trống.", HttpStatus.BAD_REQUEST);
        }

        String url = String.format("%s/models/%s:generateContent?key=%s", baseUrl, chatModel, apiKey);

        Map<String, Object> requestBody = new HashMap<>();

        if (systemInstruction != null && !systemInstruction.isBlank()) {
            requestBody.put("system_instruction", Map.of(
                    "parts", List.of(Map.of("text", systemInstruction))
            ));
        }

        List<Map<String, Object>> contents = new ArrayList<>();

        if (history != null && !history.isEmpty()) {
            for (ChatMessageDto msg : history) {
                if (msg.getContent() == null || msg.getContent().isBlank()) continue;
                String role = "assistant".equalsIgnoreCase(msg.getRole()) || "model".equalsIgnoreCase(msg.getRole())
                        ? "model" : "user";
                contents.add(Map.of(
                        "role", role,
                        "parts", List.of(Map.of("text", msg.getContent()))
                ));
            }
        }

        // Thêm câu hỏi hiện tại của user
        contents.add(Map.of(
                "role", "user",
                "parts", List.of(Map.of("text", userMessage))
        ));

        requestBody.put("contents", contents);
        requestBody.put("generationConfig", Map.of(
                "temperature", temperature
        ));

        try {
            String responseStr = restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            JsonNode rootNode = objectMapper.readTree(responseStr);
            JsonNode candidateNode = rootNode.path("candidates").path(0);
            JsonNode partsNode = candidateNode.path("content").path("parts");

            if (partsNode.isArray() && !partsNode.isEmpty()) {
                StringBuilder reply = new StringBuilder();
                for (JsonNode part : partsNode) {
                    if (part.has("text")) {
                        reply.append(part.get("text").asText());
                    }
                }
                return reply.toString().trim();
            }

            log.warn("Gemini không trả về nội dung hợp lệ: {}", responseStr);
            return "Xin lỗi, hiện tại tôi không thể xử lý câu trả lời. Vui lòng thử lại sau.";
        } catch (Exception e) {
            log.error("Lỗi khi gọi Gemini Generate Content API: {}", e.getMessage(), e);
            throw new AppException("Lỗi kết nối dịch vụ Gemini Chat: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
