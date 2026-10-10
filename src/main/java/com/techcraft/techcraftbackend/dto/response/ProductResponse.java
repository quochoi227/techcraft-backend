package com.techcraft.techcraftbackend.dto.response;

import com.techcraft.techcraftbackend.enums.ProductCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ProductResponse {

    private UUID id;
    private String name;
    private BigDecimal price;
    private Integer stockQuantity;
    private ProductCategory category;
    private String description;
    private Map<String, Object> detail;
    @JsonProperty("is_active")
    private boolean isActive;
    @Builder.Default
    private List<ProductImageResponse> images = new ArrayList<>();
    private Instant createdAt;
    private Instant updatedAt;
}
