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
public class RamDetail {

    @NotBlank(message = "Loại RAM (type: DDR4, DDR5...) không được để trống")
    private String type;

    @NotNull(message = "Tốc độ Bus (speed_mhz) không được để trống")
    @Min(value = 1, message = "Tốc độ Bus phải lớn hơn 0")
    private Integer speedMhz;

    @NotNull(message = "Dung lượng mỗi thanh (capacity_gb) không được để trống")
    @Min(value = 1, message = "Dung lượng mỗi thanh RAM phải lớn hơn 0")
    private Integer capacityGb;

    @NotNull(message = "Số thanh RAM trong kit (sticks) không được để trống")
    @Min(value = 1, message = "Số thanh RAM phải lớn hơn hoặc bằng 1")
    private Integer sticks;

    @NotNull(message = "Tổng dung lượng kit RAM (total_gb) không được để trống")
    @Min(value = 1, message = "Tổng dung lượng kit RAM phải lớn hơn 0")
    private Integer totalGb;
}
