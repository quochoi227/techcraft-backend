package com.techcraft.techcraftbackend.dto.notification;

import com.techcraft.techcraftbackend.enums.NotificationType;
import com.techcraft.techcraftbackend.enums.OrderStatus;
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
public class OrderNotificationDto {
    private UUID id;
    private NotificationType type;
    private UUID orderId;
    private String orderCode;
    private String title;
    private String message;
    private OrderStatus status;
    private BigDecimal totalAmount;
    private String customerName;
    private String customerEmail;
    private boolean read;
    private Instant createdAt;
}
