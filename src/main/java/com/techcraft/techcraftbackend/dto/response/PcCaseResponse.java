package com.techcraft.techcraftbackend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class PcCaseResponse {

    private UUID id;
    private String name;
    private String useCase;
    private BigDecimal totalPrice;
    private String description;

    @JsonProperty("is_active")
    private boolean isActive;

    @Builder.Default
    private List<PcCaseItemResponse> items = new ArrayList<>();

    private Instant createdAt;
    private Instant updatedAt;
}
