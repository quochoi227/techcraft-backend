package com.techcraft.techcraftbackend.dto.response;

import com.techcraft.techcraftbackend.enums.ProductCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class CartItemResponse {

    private UUID id;
    private UUID productId;
    private String productName;
    private BigDecimal productPrice;
    private String productImage;
    private ProductCategory category;
    private Integer stockQuantity;
    private Integer quantity;
    private BigDecimal subtotal;
    private ProductResponse product;
    private Instant createdAt;
    private Instant updatedAt;
}
