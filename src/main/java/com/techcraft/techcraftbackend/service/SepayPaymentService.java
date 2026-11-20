package com.techcraft.techcraftbackend.service;

import com.techcraft.techcraftbackend.dto.payment.PaymentInfoResponse;
import com.techcraft.techcraftbackend.dto.payment.SepayWebhookPayload;
import com.techcraft.techcraftbackend.entity.Order;

import java.util.UUID;

public interface SepayPaymentService {

    PaymentInfoResponse getPaymentInfo(UUID orderId, String username);

    void processWebhook(SepayWebhookPayload payload, String authHeader);

    String generateQrUrl(Order order);
}
