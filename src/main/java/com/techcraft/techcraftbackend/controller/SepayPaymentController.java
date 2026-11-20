package com.techcraft.techcraftbackend.controller;

import com.techcraft.techcraftbackend.dto.ApiResponse;
import com.techcraft.techcraftbackend.dto.payment.PaymentInfoResponse;
import com.techcraft.techcraftbackend.dto.payment.SepayWebhookPayload;
import com.techcraft.techcraftbackend.exception.UnauthorizedException;
import com.techcraft.techcraftbackend.service.SepayPaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class SepayPaymentController {

    private final SepayPaymentService sepayPaymentService;

    /**
     * Endpoint tiếp nhận Webhook từ SePay khi có biến động số dư.
     * Endpoint này là public (permitAll trong SecurityConfig), xác thực qua Authorization header (nếu cấu hình API Key).
     */
    @PostMapping("/sepay/webhook")
    public ResponseEntity<Map<String, Object>> handleSepayWebhook(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody SepayWebhookPayload payload) {

        log.info("Incoming SePay Webhook payload: {}", payload);
        sepayPaymentService.processWebhook(payload, authHeader);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Webhook processed successfully"
        ));
    }

    /**
     * Lấy thông tin thanh toán (bao gồm mã VietQR, STK, cú pháp) cho một đơn hàng cụ thể.
     */
    @GetMapping("/orders/{orderId}/info")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PaymentInfoResponse>> getPaymentInfo(
            @PathVariable UUID orderId,
            Principal principal) {
        if (principal == null || principal.getName() == null) {
            throw new UnauthorizedException("Vui lòng đăng nhập để thực hiện chức năng này.");
        }

        PaymentInfoResponse response = sepayPaymentService.getPaymentInfo(orderId, principal.getName());
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin thanh toán thành công!", response));
    }

    /**
     * Tra cứu trạng thái thanh toán nhanh cho frontend (phục vụ polling hoặc kiểm tra sau khi chuyển khoản).
     */
    @GetMapping("/orders/{orderId}/status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPaymentStatus(
            @PathVariable UUID orderId,
            Principal principal) {
        if (principal == null || principal.getName() == null) {
            throw new UnauthorizedException("Vui lòng đăng nhập để thực hiện chức năng này.");
        }

        PaymentInfoResponse info = sepayPaymentService.getPaymentInfo(orderId, principal.getName());
        Map<String, Object> statusData = Map.of(
                "orderId", info.getOrderId(),
                "orderCode", info.getOrderCode(),
                "paymentMethod", info.getPaymentMethod(),
                "paymentStatus", info.getPaymentStatus()
        );

        return ResponseEntity.ok(ApiResponse.success("Lấy trạng thái thanh toán thành công!", statusData));
    }
}
