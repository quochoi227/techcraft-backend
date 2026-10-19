package com.techcraft.techcraftbackend.dto.response;

import com.techcraft.techcraftbackend.enums.ProductCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItemResponse {

    private UUID id;
    private String itemType;

    // Thông tin linh kiện lẻ (nếu item là Product)
    private UUID productId;
    private String productName;
    private BigDecimal productPrice;
    private String productImage;
    private ProductCategory category;
    private ProductResponse product;

    // Thông tin cấu hình PC (nếu item là PcCase)
    private UUID pcCaseId;
    private String pcCaseName;
    private String useCase;
    private PcCaseResponse pcCase;

    // Thông tin thanh toán & số lượng chung
    private BigDecimal unitPrice;
    private Integer stockQuantity;
    private Integer quantity;
    private BigDecimal subtotal;
    private Instant createdAt;
    private Instant updatedAt;
}
