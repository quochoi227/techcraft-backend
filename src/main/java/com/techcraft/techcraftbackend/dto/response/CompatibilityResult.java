package com.techcraft.techcraftbackend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class CompatibilityResult {

    private boolean compatible;

    @Builder.Default
    private List<String> violations = new ArrayList<>();

    public static CompatibilityResult ok() {
        return CompatibilityResult.builder()
                .compatible(true)
                .violations(List.of())
                .build();
    }

    public static CompatibilityResult fail(List<String> violations) {
        return CompatibilityResult.builder()
                .compatible(false)
                .violations(violations != null ? List.copyOf(violations) : List.of())
                .build();
    }
}
