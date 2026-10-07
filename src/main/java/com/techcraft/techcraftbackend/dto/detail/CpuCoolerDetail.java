package com.techcraft.techcraftbackend.dto.detail;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
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
public class CpuCoolerDetail {

    @NotBlank(message = "Loại tản nhiệt (type: Air, AIO Liquid...) không được để trống")
    private String type;

    @NotEmpty(message = "Danh sách socket hỗ trợ (socket_support) không được để trống")
    private List<String> socketSupport;

    @Min(value = 1, message = "Chiều cao tản nhiệt (height_mm) phải lớn hơn 0")
    private Integer heightMm;

    @Min(value = 1, message = "Kích thước radiator (radiator_size_mm) phải lớn hơn 0")
    private Integer radiatorSizeMm;

    @NotNull(message = "TDP CPU tối đa hỗ trợ (tdp_support_w) không được để trống")
    @Min(value = 1, message = "TDP hỗ trợ phải lớn hơn 0")
    private Integer tdpSupportW;
}
