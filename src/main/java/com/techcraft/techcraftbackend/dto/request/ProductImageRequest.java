package com.techcraft.techcraftbackend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductImageRequest {

    @NotBlank(message = "Đường dẫn hình ảnh (imageUrl) không được để trống")
    private String imageUrl;

    @Builder.Default
    private Boolean isPrimary = false;

    @Builder.Default
    private Integer displayOrder = 0;
}
