package com.techcraft.techcraftbackend.dto.detail;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class CpuDetail {

    @NotBlank(message = "Socket của CPU không được để trống")
    private String socket;

    @NotNull(message = "Số nhân (cores) không được để trống")
    @Min(value = 1, message = "Số nhân CPU phải lớn hơn hoặc bằng 1")
    private Integer cores;

    @NotNull(message = "Số luồng (threads) không được để trống")
    @Min(value = 1, message = "Số luồng CPU phải lớn hơn hoặc bằng 1")
    private Integer threads;

    @NotNull(message = "Xung nhịp cơ bản (base_clock_ghz) không được để trống")
    @Positive(message = "Xung nhịp cơ bản phải lớn hơn 0")
    private Double baseClockGhz;

    @NotNull(message = "Xung nhịp tối đa (boost_clock_ghz) không được để trống")
    @Positive(message = "Xung nhịp tối đa phải lớn hơn 0")
    private Double boostClockGhz;

    @NotNull(message = "Công suất tiêu thụ nhiệt (tdp_w) không được để trống")
    @Min(value = 1, message = "TDP CPU phải lớn hơn hoặc bằng 1W")
    private Integer tdpW;

    @NotBlank(message = "Loại bộ nhớ RAM hỗ trợ (memory_type) không được để trống")
    private String memoryType;

    @NotNull(message = "Trường GPU tích hợp (integrated_gpu) không được để trống")
    private Boolean integratedGpu;
}
