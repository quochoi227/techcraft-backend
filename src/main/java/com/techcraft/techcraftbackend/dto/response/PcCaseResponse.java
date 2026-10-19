package com.techcraft.techcraftbackend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PcCaseResponse {

    private UUID id;
    private String name;
    private String useCase;
    private BigDecimal totalPrice;
    private String description;

    private boolean isActive;

    @Builder.Default
    private List<PcCaseItemResponse> items = new ArrayList<>();

    private Instant createdAt;
    private Instant updatedAt;
}
