package com.techcraft.techcraftbackend.dto.response;

import com.techcraft.techcraftbackend.enums.ProductCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemResponse {

    private UUID id;
    private String itemType;

    // Thông tin linh kiện lẻ (nếu là Product)
    private UUID productId;
    private String productName;
    private String productImage;
    private ProductCategory category;
    private ProductResponse product;

    // Thông tin cấu hình PC (nếu là PcCase)
    private UUID pcCaseId;
    private String pcCaseName;
    private String useCase;
    private PcCaseResponse pcCase;

    // Thông tin thanh toán & số lượng
    private BigDecimal unitPrice;
    private Integer quantity;
    private BigDecimal subtotal;
}
