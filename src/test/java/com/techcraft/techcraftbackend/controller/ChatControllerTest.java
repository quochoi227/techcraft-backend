package com.techcraft.techcraftbackend.controller;

import com.techcraft.techcraftbackend.dto.request.ChatRequest;
import com.techcraft.techcraftbackend.dto.response.ChatResponse;
import com.techcraft.techcraftbackend.enums.ChatIntent;
import com.techcraft.techcraftbackend.service.ChatService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ChatService chatService;

    @Test
    void chat_PublicAccess_ReturnsOk() throws Exception {
        ChatRequest request = ChatRequest.builder()
                .message("Xin chào shop")
                .build();

        ChatResponse mockResponse = ChatResponse.builder()
                .reply("Chào bạn! TechCraft có thể hỗ trợ gì cho bạn?")
                .detectedIntent(ChatIntent.GENERAL_CHAT)
                .build();

        when(chatService.chat(any(ChatRequest.class))).thenReturn(mockResponse);

        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.reply").value("Chào bạn! TechCraft có thể hỗ trợ gì cho bạn?"))
                .andExpect(jsonPath("$.data.detectedIntent").value("GENERAL_CHAT"));
    }

    @Test
    void chat_BlankMessage_ReturnsBadRequest() throws Exception {
        ChatRequest request = ChatRequest.builder()
                .message("")
                .build();

        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
}
