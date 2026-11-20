package com.techcraft.techcraftbackend.service.impl;

import com.techcraft.techcraftbackend.config.SepayProperties;
import com.techcraft.techcraftbackend.dto.payment.PaymentInfoResponse;
import com.techcraft.techcraftbackend.dto.payment.SepayWebhookPayload;
import com.techcraft.techcraftbackend.entity.Order;
import com.techcraft.techcraftbackend.entity.PaymentTransaction;
import com.techcraft.techcraftbackend.entity.User;
import com.techcraft.techcraftbackend.enums.OrderStatus;
import com.techcraft.techcraftbackend.enums.PaymentStatus;
import com.techcraft.techcraftbackend.enums.Role;
import com.techcraft.techcraftbackend.exception.BadRequestException;
import com.techcraft.techcraftbackend.exception.ForbiddenException;
import com.techcraft.techcraftbackend.exception.ResourceNotFoundException;
import com.techcraft.techcraftbackend.exception.UnauthorizedException;
import com.techcraft.techcraftbackend.repository.OrderRepository;
import com.techcraft.techcraftbackend.repository.PaymentTransactionRepository;
import com.techcraft.techcraftbackend.repository.UserRepository;
import com.techcraft.techcraftbackend.service.NotificationService;
import com.techcraft.techcraftbackend.service.SepayPaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class SepayPaymentServiceImpl implements SepayPaymentService {

    private final SepayProperties sepayProperties;
    private final OrderRepository orderRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    // Pattern nhận diện mã đơn hàng dạng TC + 8 ký tự alphanumeric (VD: TC94B2C81E)
    private static final Pattern ORDER_CODE_PATTERN = Pattern.compile("TC[A-Za-z0-9]{8}", Pattern.CASE_INSENSITIVE);

    @Override
    @Transactional(readOnly = true)
    public PaymentInfoResponse getPaymentInfo(UUID orderId, String username) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng với ID: " + orderId));

        if (username != null && !username.isBlank()) {
            User user = userRepository.findByEmail(username.toLowerCase().trim())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

            boolean isOwner = order.getUser() != null && order.getUser().getId().equals(user.getId());
            boolean isAdmin = user.getRole() == Role.ADMIN;
            if (!isOwner && !isAdmin) {
                throw new ForbiddenException("Bạn không có quyền xem thông tin thanh toán của đơn hàng này.");
            }
        }

        String qrUrl = generateQrUrl(order);
        String transferContent = (order.getOrderCode() != null && !order.getOrderCode().isBlank())
                ? order.getOrderCode()
                : order.getId().toString().substring(0, 8).toUpperCase();

        return PaymentInfoResponse.builder()
                .orderId(order.getId())
                .orderCode(transferContent)
                .paymentMethod(order.getPaymentMethod())
                .paymentStatus(order.getPaymentStatus())
                .amount(order.getTotalAmount())
                .bankName(sepayProperties.getBankName())
                .accountNumber(sepayProperties.getBankAccount())
                .accountName(sepayProperties.getAccountName())
                .transferContent(transferContent)
                .qrCodeUrl(qrUrl)
                .build();
    }

    @Override
    @Transactional
    public void processWebhook(SepayWebhookPayload payload, String authHeader) {
        // 1. Xác thực API Key bảo mật (nếu có cấu hình trong .env)
        String configuredApiKey = sepayProperties.getWebhookApiKey();
        if (configuredApiKey != null && !configuredApiKey.isBlank()) {
            if (authHeader == null || authHeader.isBlank()) {
                log.warn("SePay webhook rejected: Missing Authorization header");
                throw new UnauthorizedException("Yêu cầu webhook không có Authorization header.");
            }

            String token = authHeader.trim();
            if (token.startsWith("Apikey ")) {
                token = token.substring(7).trim();
            } else if (token.startsWith("Bearer ")) {
                token = token.substring(7).trim();
            }

            if (!configuredApiKey.equals(token)) {
                log.warn("SePay webhook rejected: Invalid API Key");
                throw new UnauthorizedException("SePay API Key không hợp lệ.");
            }
        }

        if (payload == null) {
            throw new BadRequestException("Payload webhook rỗng");
        }

        log.info("Received SePay webhook: id={}, gateway={}, transferType={}, amount={}, content={}",
                payload.getId(), payload.getGateway(), payload.getTransferType(),
                payload.getTransferAmount(), payload.getContent());

        // 2. Chỉ xử lý tiền vào (transferType == "in")
        if (payload.getTransferType() != null && !"in".equalsIgnoreCase(payload.getTransferType().trim())) {
            log.info("Bỏ qua giao dịch không phải tiền vào (transferType = {})", payload.getTransferType());
            return;
        }

        // 3. Kiểm tra chống xử lý lặp lại (Idempotency)
        if (payload.getId() != null && paymentTransactionRepository.existsBySepayTransactionId(payload.getId())) {
            log.info("Giao dịch SePay ID {} đã được xử lý thành công trước đó, bỏ qua.", payload.getId());
            return;
        }

        // 4. Trích xuất mã đơn hàng từ content / code / description
        String extractedCode = extractOrderCode(payload);
        log.info("Extracted order code: {}", extractedCode);

        Order matchedOrder = null;
        if (extractedCode != null) {
            matchedOrder = orderRepository.findByOrderCode(extractedCode.toUpperCase()).orElse(null);
        }

        // Nếu không khớp theo orderCode, kiểm tra đối soát theo UUID prefix (8 ký tự đầu)
        if (matchedOrder == null && extractedCode != null) {
            final String cleanCode = extractedCode.toUpperCase().startsWith("TC")
                    ? extractedCode.substring(2)
                    : extractedCode;
            matchedOrder = orderRepository.findAll().stream()
                    .filter(o -> o.getId().toString().replace("-", "").toUpperCase().startsWith(cleanCode))
                    .findFirst()
                    .orElse(null);
        }

        BigDecimal transferAmount = payload.getTransferAmount() != null
                ? payload.getTransferAmount()
                : BigDecimal.ZERO;

        // Lưu bản ghi giao dịch thanh toán
        PaymentTransaction transaction = PaymentTransaction.builder()
                .order(matchedOrder)
                .sepayTransactionId(payload.getId())
                .gateway(payload.getGateway())
                .transactionDate(payload.getTransactionDate())
                .accountNumber(payload.getAccountNumber())
                .subAccount(payload.getSubAccount())
                .transferAmount(transferAmount)
                .content(payload.getContent())
                .referenceCode(payload.getReferenceCode())
                .build();

        paymentTransactionRepository.save(transaction);

        if (matchedOrder == null) {
            log.warn("Không tìm thấy đơn hàng khớp với nội dung chuyển khoản: {}", payload.getContent());
            return;
        }

        // 5. Kiểm tra số tiền chuyển khoản
        if (transferAmount.compareTo(matchedOrder.getTotalAmount()) < 0) {
            log.warn("Đơn hàng #{} yêu cầu số tiền {}, nhưng nhận được {}. Giữ trạng thái để xử lý thủ công.",
                    matchedOrder.getOrderCode(), matchedOrder.getTotalAmount(), transferAmount);
            return;
        }

        // 6. Cập nhật trạng thái đơn hàng sang PAID và CONFIRMED
        if (matchedOrder.getPaymentStatus() != PaymentStatus.PAID) {
            matchedOrder.setPaymentStatus(PaymentStatus.PAID);
            if (matchedOrder.getStatus() == OrderStatus.PENDING) {
                matchedOrder.setStatus(OrderStatus.CONFIRMED);
            }
            Order savedOrder = orderRepository.save(matchedOrder);

            log.info("Đơn hàng #{} đã cập nhật sang PAID và CONFIRMED thành công qua SePay.",
                    savedOrder.getOrderCode());

            // 7. Bắn thông báo realtime qua WebSocket & lưu Notification
            notificationService.notifyOrderPaid(savedOrder, transferAmount);
        }
    }

    @Override
    public String generateQrUrl(Order order) {
        if (order == null) return null;

        String bank = sepayProperties.getBankName() != null ? sepayProperties.getBankName() : "Vietcombank";
        String acc = sepayProperties.getBankAccount() != null ? sepayProperties.getBankAccount() : "0000000001";
        String template = sepayProperties.getQrTemplate() != null ? sepayProperties.getQrTemplate() : "compact";
        long amount = order.getTotalAmount() != null ? order.getTotalAmount().longValue() : 0L;

        String des = (order.getOrderCode() != null && !order.getOrderCode().isBlank())
                ? order.getOrderCode()
                : order.getId().toString().substring(0, 8).toUpperCase();

        String encodedDes = URLEncoder.encode(des, StandardCharsets.UTF_8);

        return String.format("https://qr.sepay.vn/img?bank=%s&acc=%s&template=%s&amount=%d&des=%s",
                bank, acc, template, amount, encodedDes);
    }

    private String extractOrderCode(SepayWebhookPayload payload) {
        if (payload == null) return null;

        // Ưu tiên 1: Trường code SePay tự tách nếu có
        if (payload.getCode() != null && !payload.getCode().isBlank()) {
            Matcher m = ORDER_CODE_PATTERN.matcher(payload.getCode());
            if (m.find()) {
                return m.group();
            }
        }

        // Ưu tiên 2: Trường content (Nội dung chuyển khoản gốc)
        if (payload.getContent() != null && !payload.getContent().isBlank()) {
            Matcher m = ORDER_CODE_PATTERN.matcher(payload.getContent());
            if (m.find()) {
                return m.group();
            }
        }

        // Ưu tiên 3: Trường description
        if (payload.getDescription() != null && !payload.getDescription().isBlank()) {
            Matcher m = ORDER_CODE_PATTERN.matcher(payload.getDescription());
            if (m.find()) {
                return m.group();
            }
        }

        return null;
    }
}
