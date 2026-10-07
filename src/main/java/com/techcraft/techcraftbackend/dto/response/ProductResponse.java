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

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {

    private UUID id;
    private String name;
    private BigDecimal price;
    private Integer stockQuantity;
    private ProductCategory category;
    private String description;
    private Map<String, Object> detail;
    private boolean isActive;
    @Builder.Default
    private List<ProductImageResponse> images = new ArrayList<>();
    private Instant createdAt;
    private Instant updatedAt;
}
