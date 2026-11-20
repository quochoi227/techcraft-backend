package com.techcraft.techcraftbackend.dto.chat;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageDto {

    @NotBlank
    private String role; // "user" hoặc "assistant" / "model"

    @NotBlank
    private String content;
}
