package com.techcraft.techcraftbackend.dto.chat;

import com.techcraft.techcraftbackend.enums.ProductCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PcCasePromptDto {

    private UUID id;
    private String name;
    private String useCase;
    private BigDecimal totalPrice;
    private String description;

    @Builder.Default
    private List<ComponentSummary> components = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ComponentSummary {
        private ProductCategory category;
        private String name;
        private BigDecimal price;
        private Map<String, Object> keySpecs;
    }
}
