package com.techcraft.techcraftbackend.service;

import com.techcraft.techcraftbackend.dto.response.PcCaseResponse;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface PcConfigSuggestionService {

    String getAvailablePcCasesPromptText();

    Set<UUID> extractPcCaseIds(String text);

    List<PcCaseResponse> findFullPcCasesByIds(Set<UUID> ids);
}
