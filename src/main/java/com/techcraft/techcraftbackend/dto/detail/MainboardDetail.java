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
public class MainboardDetail {

    @NotBlank(message = "Socket của Mainboard không được để trống")
    private String socket;

    @NotBlank(message = "Kích thước (form_factor: ATX, mATX, ITX...) không được để trống")
    private String formFactor;

    @NotBlank(message = "Chipset không được để trống")
    private String chipset;

    @NotBlank(message = "Loại bộ nhớ RAM hỗ trợ (memory_type) không được để trống")
    private String memoryType;

    @NotNull(message = "Số khe RAM (memory_slots) không được để trống")
    @Min(value = 1, message = "Số khe RAM phải lớn hơn hoặc bằng 1")
    private Integer memorySlots;

    @NotNull(message = "Dung lượng RAM tối đa (max_memory_gb) không được để trống")
    @Min(value = 1, message = "Dung lượng RAM tối đa phải lớn hơn 0")
    private Integer maxMemoryGb;

    @NotNull(message = "Số khe M.2 (m2_slots) không được để trống")
    @Min(value = 0, message = "Số khe M.2 không được âm")
    private Integer m2Slots;

    @NotBlank(message = "Phiên bản PCIe (pcie_version) không được để trống")
    private String pcieVersion;
}
