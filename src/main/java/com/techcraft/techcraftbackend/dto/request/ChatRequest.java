package com.techcraft.techcraftbackend.dto.request;

import com.techcraft.techcraftbackend.dto.chat.ChatMessageDto;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {

    @NotBlank(message = "Nội dung tin nhắn không được để trống")
    private String message;

    @Builder.Default
    private List<ChatMessageDto> history = new ArrayList<>();
}
