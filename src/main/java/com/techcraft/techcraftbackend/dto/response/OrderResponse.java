package com.techcraft.techcraftbackend.dto.response;

import com.techcraft.techcraftbackend.enums.OrderStatus;
import com.techcraft.techcraftbackend.enums.PaymentMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {

    private UUID id;
    private UUID userId;
    private String userEmail;
    private String userFullName;
    private String userPhone;
    private OrderStatus status;
    private PaymentMethod paymentMethod;
    private String shippingAddress;
    private String note;
    private BigDecimal totalAmount;
    private Integer totalQuantity;
    private List<OrderItemResponse> items;
    private Instant createdAt;
    private Instant updatedAt;
}
