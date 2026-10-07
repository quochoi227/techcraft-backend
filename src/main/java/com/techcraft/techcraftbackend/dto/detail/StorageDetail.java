package com.techcraft.techcraftbackend.dto.detail;

import com.fasterxml.jackson.annotation.JsonProperty;
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
public class StorageDetail {

    @NotBlank(message = "Loại ổ cứng (type: NVMe SSD, SATA SSD, HDD...) không được để trống")
    private String type;

    @NotNull(message = "Dung lượng ổ cứng (capacity_gb) không được để trống")
    @Min(value = 1, message = "Dung lượng ổ cứng phải lớn hơn 0")
    private Integer capacityGb;

    @NotBlank(message = "Giao tiếp ổ cứng (interface) không được để trống")
    @JsonProperty("interface")
    private String interfaceType;

    @Min(value = 0, message = "Tốc độ đọc không được âm")
    private Integer readMbps;

    @Min(value = 0, message = "Tốc độ ghi không được âm")
    private Integer writeMbps;
}
