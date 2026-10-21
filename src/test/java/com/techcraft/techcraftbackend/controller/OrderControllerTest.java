package com.techcraft.techcraftbackend.controller;

import com.techcraft.techcraftbackend.dto.request.CreateOrderItemRequest;
import com.techcraft.techcraftbackend.dto.request.CreateOrderRequest;
import com.techcraft.techcraftbackend.dto.response.OrderItemResponse;
import com.techcraft.techcraftbackend.dto.response.OrderResponse;
import com.techcraft.techcraftbackend.enums.OrderStatus;
import com.techcraft.techcraftbackend.enums.PaymentMethod;
import com.techcraft.techcraftbackend.exception.BadRequestException;
import com.techcraft.techcraftbackend.service.OrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OrderService orderService;

    @Test
    @DisplayName("Tạo đơn hàng thành công khi đã đăng nhập (201 Created)")
    @WithMockUser(username = "customer@example.com", roles = "USER")
    void createOrder_AsAuthenticatedUser_Returns201Created() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        CreateOrderRequest request = CreateOrderRequest.builder()
                .shippingAddress("123 Duong Le Loi, TP.HCM")
                .paymentMethod(PaymentMethod.COD)
                .note("Giao gio hanh chinh")
                .items(List.of(
                        CreateOrderItemRequest.builder()
                                .productId(productId)
                                .quantity(1)
                                .build()
                ))
                .build();

        OrderResponse expectedResponse = OrderResponse.builder()
                .id(orderId)
                .userId(UUID.randomUUID())
                .userEmail("customer@example.com")
                .status(OrderStatus.PENDING)
                .paymentMethod(PaymentMethod.COD)
                .shippingAddress("123 Duong Le Loi, TP.HCM")
                .totalAmount(BigDecimal.valueOf(5000000))
                .totalQuantity(1)
                .items(List.of(
                        OrderItemResponse.builder()
                                .id(UUID.randomUUID())
                                .itemType("PRODUCT")
                                .productId(productId)
                                .productName("Intel Core i5")
                                .quantity(1)
                                .unitPrice(BigDecimal.valueOf(5000000))
                                .subtotal(BigDecimal.valueOf(5000000))
                                .build()
                ))
                .build();

        when(orderService.createOrder(eq("customer@example.com"), any(CreateOrderRequest.class)))
                .thenReturn(expectedResponse);

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Tạo đơn hàng thành công!"))
                .andExpect(jsonPath("$.data.id").value(orderId.toString()))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.paymentMethod").value("COD"))
                .andExpect(jsonPath("$.data.shippingAddress").value("123 Duong Le Loi, TP.HCM"))
                .andExpect(jsonPath("$.data.totalAmount").value(5000000))
                .andExpect(jsonPath("$.data.items[0].productName").value("Intel Core i5"));
    }

    @Test
    @DisplayName("Tạo đơn hàng thất bại khi chưa đăng nhập (401 Unauthorized)")
    void createOrder_AsGuest_Returns401Unauthorized() throws Exception {
        CreateOrderRequest request = CreateOrderRequest.builder()
                .shippingAddress("123 Duong Le Loi, TP.HCM")
                .build();

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Tạo đơn hàng thất bại khi Service ném BadRequestException (400 Bad Request)")
    @WithMockUser(username = "customer@example.com", roles = "USER")
    void createOrder_WhenServiceThrowsBadRequest_Returns400BadRequest() throws Exception {
        CreateOrderRequest request = CreateOrderRequest.builder()
                .shippingAddress("123 Duong Le Loi, TP.HCM")
                .build();

        when(orderService.createOrder(eq("customer@example.com"), any(CreateOrderRequest.class)))
                .thenThrow(new BadRequestException("Linh kiện 'Intel Core i5' không đủ số lượng tồn kho."));

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Linh kiện 'Intel Core i5' không đủ số lượng tồn kho."));
    }

    @Test
    @DisplayName("Tạo đơn hàng thành công với body rỗng khi thanh toán toàn bộ giỏ hàng (201 Created)")
    @WithMockUser(username = "customer@example.com", roles = "USER")
    void createOrder_WithEmptyBody_Returns201Created() throws Exception {
        UUID orderId = UUID.randomUUID();

        OrderResponse expectedResponse = OrderResponse.builder()
                .id(orderId)
                .status(OrderStatus.PENDING)
                .totalAmount(BigDecimal.valueOf(10000000))
                .build();

        when(orderService.createOrder(eq("customer@example.com"), any(CreateOrderRequest.class)))
                .thenReturn(expectedResponse);

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(orderId.toString()));
    }
}
