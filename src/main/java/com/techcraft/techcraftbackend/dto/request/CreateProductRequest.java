package com.techcraft.techcraftbackend.dto.request;

import com.techcraft.techcraftbackend.enums.ProductCategory;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateProductRequest {

    @NotBlank(message = "Tên linh kiện không được để trống")
    private String name;

    @NotNull(message = "Giá sản phẩm không được để trống")
    @DecimalMin(value = "0.0", inclusive = false, message = "Giá sản phẩm phải lớn hơn 0")
    private BigDecimal price;

    @NotNull(message = "Số lượng tồn kho không được để trống")
    @Min(value = 0, message = "Số lượng tồn kho không được âm")
    private Integer stockQuantity;

    @NotNull(message = "Danh mục linh kiện không được để trống")
    private ProductCategory category;

    private String description;

    @NotNull(message = "Thông số kỹ thuật chi tiết (detail) không được để trống")
    private Map<String, Object> detail;

    @Builder.Default
    private Boolean isActive = true;

    @Builder.Default
    private List<@Valid ProductImageRequest> images = new ArrayList<>();
}
