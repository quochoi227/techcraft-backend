package com.techcraft.techcraftbackend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class CartResponse {

    @Builder.Default
    private List<CartItemResponse> items = new ArrayList<>();

    private Integer totalItems;

    private Integer totalQuantity;

    private BigDecimal totalAmount;
}
