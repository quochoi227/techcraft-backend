package com.techcraft.techcraftbackend.dto.response;

import com.techcraft.techcraftbackend.enums.ChatIntent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {

    private String reply;

    @Builder.Default
    private List<PcCaseResponse> suggestedPcCases = new ArrayList<>();

    @Builder.Default
    private List<String> policySources = new ArrayList<>();

    private ChatIntent detectedIntent;

    @Builder.Default
    private Instant timestamp = Instant.now();
}
