package com.techcraft.techcraftbackend.service;

import com.techcraft.techcraftbackend.dto.request.ChatRequest;
import com.techcraft.techcraftbackend.dto.response.ChatResponse;

public interface ChatService {

    ChatResponse chat(ChatRequest request);
}
