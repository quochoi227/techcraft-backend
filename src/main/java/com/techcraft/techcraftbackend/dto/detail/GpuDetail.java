package com.techcraft.techcraftbackend.dto.detail;

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
public class GpuDetail {

    @NotNull(message = "Dung lượng VRAM (vram_gb) không được để trống")
    @Min(value = 1, message = "VRAM phải lớn hơn 0")
    private Integer vramGb;

    @NotBlank(message = "Loại VRAM (vram_type: GDDR6, GDDR6X...) không được để trống")
    private String vramType;

    @NotNull(message = "Công suất tiêu thụ (tdp_w) không được để trống")
    @Min(value = 1, message = "Công suất GPU (tdp_w) phải lớn hơn 0")
    private Integer tdpW;

    @NotBlank(message = "Khe cắm PCIe (pcie_slot) không được để trống")
    private String pcieSlot;

    @NotNull(message = "Chiều dài card (length_mm) không được để trống")
    @Min(value = 1, message = "Chiều dài card GPU phải lớn hơn 0")
    private Integer lengthMm;
}
