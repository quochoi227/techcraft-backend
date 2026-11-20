package com.techcraft.techcraftbackend.service;

import com.techcraft.techcraftbackend.config.SepayProperties;
import com.techcraft.techcraftbackend.dto.payment.PaymentInfoResponse;
import com.techcraft.techcraftbackend.dto.payment.SepayWebhookPayload;
import com.techcraft.techcraftbackend.entity.Order;
import com.techcraft.techcraftbackend.entity.PaymentTransaction;
import com.techcraft.techcraftbackend.entity.User;
import com.techcraft.techcraftbackend.enums.OrderStatus;
import com.techcraft.techcraftbackend.enums.PaymentMethod;
import com.techcraft.techcraftbackend.enums.PaymentStatus;
import com.techcraft.techcraftbackend.exception.UnauthorizedException;
import com.techcraft.techcraftbackend.repository.OrderRepository;
import com.techcraft.techcraftbackend.repository.PaymentTransactionRepository;
import com.techcraft.techcraftbackend.repository.UserRepository;
import com.techcraft.techcraftbackend.service.impl.SepayPaymentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SepayPaymentServiceTest {

    @Mock
    private SepayProperties sepayProperties;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private PaymentTransactionRepository paymentTransactionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private SepayPaymentServiceImpl sepayPaymentService;

    private Order testOrder;

    @BeforeEach
    void setUp() {
        testOrder = Order.builder()
                .id(UUID.randomUUID())
                .orderCode("TC12345678")
                .status(OrderStatus.PENDING)
                .paymentMethod(PaymentMethod.SEPAY)
                .paymentStatus(PaymentStatus.UNPAID)
                .totalAmount(new BigDecimal("1500000.00"))
                .build();
    }

    @Test
    @DisplayName("processWebhook: Thanh toán thành công, đối soát khớp mã đơn và số tiền")
    void processWebhook_Success() {
        when(sepayProperties.getWebhookApiKey()).thenReturn(null);
        when(paymentTransactionRepository.existsBySepayTransactionId(99999L)).thenReturn(false);
        when(orderRepository.findByOrderCode("TC12345678")).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SepayWebhookPayload payload = SepayWebhookPayload.builder()
                .id(99999L)
                .gateway("Vietcombank")
                .accountNumber("0000000001")
                .transferType("in")
                .transferAmount(new BigDecimal("1500000.00"))
                .content("TC12345678 thanh toan don hang")
                .referenceCode("FT123456789")
                .build();

        sepayPaymentService.processWebhook(payload, null);

        assertThat(testOrder.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(testOrder.getStatus()).isEqualTo(OrderStatus.CONFIRMED);

        verify(paymentTransactionRepository, times(1)).save(any(PaymentTransaction.class));
        verify(notificationService, times(1)).notifyOrderPaid(eq(testOrder), eq(new BigDecimal("1500000.00")));
    }

    @Test
    @DisplayName("processWebhook: Chống xử lý trùng lặp giao dịch (Idempotency)")
    void processWebhook_Idempotent() {
        when(sepayProperties.getWebhookApiKey()).thenReturn(null);
        when(paymentTransactionRepository.existsBySepayTransactionId(99999L)).thenReturn(true);

        SepayWebhookPayload payload = SepayWebhookPayload.builder()
                .id(99999L)
                .transferType("in")
                .build();

        sepayPaymentService.processWebhook(payload, null);

        verify(orderRepository, never()).findByOrderCode(anyString());
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    @DisplayName("processWebhook: Từ chối nếu API Key không khớp")
    void processWebhook_ApiKeyMismatch() {
        when(sepayProperties.getWebhookApiKey()).thenReturn("secret_token_123");

        SepayWebhookPayload payload = SepayWebhookPayload.builder()
                .id(99999L)
                .transferType("in")
                .build();

        assertThatThrownBy(() -> sepayPaymentService.processWebhook(payload, "Apikey wrong_token"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("không hợp lệ");

        verify(paymentTransactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("generateQrUrl: Tạo URL VietQR chính xác với thông tin đơn hàng")
    void generateQrUrl_Success() {
        when(sepayProperties.getBankName()).thenReturn("Vietcombank");
        when(sepayProperties.getBankAccount()).thenReturn("0000000001");
        when(sepayProperties.getQrTemplate()).thenReturn("compact");

        String qrUrl = sepayPaymentService.generateQrUrl(testOrder);

        assertThat(qrUrl).contains("bank=Vietcombank");
        assertThat(qrUrl).contains("acc=0000000001");
        assertThat(qrUrl).contains("amount=1500000");
        assertThat(qrUrl).contains("des=TC12345678");
    }
}
