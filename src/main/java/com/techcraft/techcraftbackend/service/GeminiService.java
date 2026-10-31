package com.techcraft.techcraftbackend.service;

import com.techcraft.techcraftbackend.dto.chat.ChatMessageDto;

import java.util.List;

public interface GeminiService {

    float[] generateEmbedding(String text);

    String generateText(String systemInstruction, List<ChatMessageDto> history, String userMessage);
}
