package com.techcraft.techcraftbackend.dto.detail;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class PsuDetail {

    @NotNull(message = "Công suất nguồn (wattage_w) không được để trống")
    @Min(value = 1, message = "Công suất nguồn phải lớn hơn 0")
    private Integer wattageW;

    @NotBlank(message = "Chứng chỉ hiệu suất (efficiency: 80+ Bronze, 80+ Gold...) không được để trống")
    private String efficiency;

    @NotBlank(message = "Chuẩn dây nguồn (modular: Full, Semi, Non-modular) không được để trống")
    private String modular;

    @NotBlank(message = "Kích thước nguồn (form_factor: ATX, SFX...) không được để trống")
    private String formFactor;
}
