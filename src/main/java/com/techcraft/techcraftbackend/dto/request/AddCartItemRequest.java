package com.techcraft.techcraftbackend.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class AddCartItemRequest {

    @NotNull(message = "ID linh kiện (product_id) không được để trống")
    @JsonAlias({"productId", "product_id"})
    private UUID productId;

    @Min(value = 1, message = "Số lượng linh kiện phải lớn hơn hoặc bằng 1")
    @JsonAlias({"quantity"})
    @Builder.Default
    private Integer quantity = 1;
}
