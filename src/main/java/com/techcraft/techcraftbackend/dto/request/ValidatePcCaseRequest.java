package com.techcraft.techcraftbackend.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.techcraft.techcraftbackend.enums.ProductCategory;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ValidatePcCaseRequest {

    @Builder.Default
    private List<@Valid PcCaseItemRequest> items = new ArrayList<>();

    @JsonAlias({"productIds", "product_ids"})
    private List<UUID> productIds;

    private Map<ProductCategory, UUID> components;

    public List<PcCaseItemRequest> resolveItems() {
        if (items != null && !items.isEmpty()) {
            return items;
        }
        if (components != null && !components.isEmpty()) {
            List<PcCaseItemRequest> resolved = new ArrayList<>();
            components.forEach((category, productId) -> resolved.add(
                    PcCaseItemRequest.builder()
                            .category(category)
                            .productId(productId)
                            .build()
            ));
            return resolved;
        }
        if (productIds != null && !productIds.isEmpty()) {
            return productIds.stream()
                    .map(id -> PcCaseItemRequest.builder().productId(id).build())
                    .toList();
        }
        return List.of();
    }
}
