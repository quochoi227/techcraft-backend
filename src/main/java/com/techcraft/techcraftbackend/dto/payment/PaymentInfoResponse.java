package com.techcraft.techcraftbackend.dto.payment;

import com.techcraft.techcraftbackend.enums.PaymentMethod;
import com.techcraft.techcraftbackend.enums.PaymentStatus;
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
public class PaymentInfoResponse {

    private UUID orderId;
    private String orderCode;
    private PaymentMethod paymentMethod;
    private PaymentStatus paymentStatus;
    private BigDecimal amount;
    private String bankName;
    private String accountNumber;
    private String accountName;
    private String transferContent;
    private String qrCodeUrl;
}
