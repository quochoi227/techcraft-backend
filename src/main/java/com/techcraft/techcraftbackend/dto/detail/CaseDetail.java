package com.techcraft.techcraftbackend.dto.detail;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class CaseDetail {

    @NotEmpty(message = "Danh sách form factor mainboard hỗ trợ (form_factor_support) không được để trống")
    private List<String> formFactorSupport;

    @NotNull(message = "Chiều dài GPU tối đa hỗ trợ (max_gpu_length_mm) không được để trống")
    @Min(value = 1, message = "Chiều dài GPU tối đa phải lớn hơn 0")
    private Integer maxGpuLengthMm;

    @NotNull(message = "Chiều cao tản nhiệt CPU tối đa (max_cooler_height_mm) không được để trống")
    @Min(value = 1, message = "Chiều cao tản nhiệt CPU tối đa phải lớn hơn 0")
    private Integer maxCoolerHeightMm;

    @Min(value = 0, message = "Số khay 3.5 inch không được âm")
    private Integer driveBays35;

    @Min(value = 0, message = "Số khay 2.5 inch không được âm")
    private Integer driveBays25;
}
