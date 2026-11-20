package com.techcraft.techcraftbackend.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.techcraft.techcraftbackend.enums.ProductCategory;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PcCaseItemRequest {

    private ProductCategory category;

    @NotNull(message = "ID linh kiện (product_id) không được để trống")
    @JsonAlias({"productId", "product_id"})
    private UUID productId;
}
