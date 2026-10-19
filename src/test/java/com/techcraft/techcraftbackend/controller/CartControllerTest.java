package com.techcraft.techcraftbackend.controller;

import com.techcraft.techcraftbackend.dto.request.AddCartItemRequest;
import com.techcraft.techcraftbackend.dto.response.CartItemResponse;
import com.techcraft.techcraftbackend.enums.ProductCategory;
import com.techcraft.techcraftbackend.exception.BadRequestException;
import com.techcraft.techcraftbackend.exception.ResourceNotFoundException;
import com.techcraft.techcraftbackend.service.CartService;
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
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CartService cartService;

    @Test
    @DisplayName("Thêm linh kiện vào giỏ hàng thành công khi đã đăng nhập (201 Created)")
    @WithMockUser(username = "user@example.com", roles = "USER")
    void addCartItem_AsAuthenticatedUser_Returns201Created() throws Exception {
        UUID productId = UUID.randomUUID();
        UUID cartItemId = UUID.randomUUID();

        AddCartItemRequest request = AddCartItemRequest.builder()
                .productId(productId)
                .quantity(2)
                .build();

        CartItemResponse response = CartItemResponse.builder()
                .id(cartItemId)
                .productId(productId)
                .productName("CPU AMD Ryzen 5 7600X")
                .productPrice(BigDecimal.valueOf(6200000))
                .category(ProductCategory.CPU)
                .stockQuantity(15)
                .quantity(2)
                .subtotal(BigDecimal.valueOf(12400000))
                .build();

        when(cartService.addCartItem(eq("user@example.com"), any(AddCartItemRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/cart/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Thêm linh kiện vào giỏ hàng thành công!"))
                .andExpect(jsonPath("$.data.id").value(cartItemId.toString()))
                .andExpect(jsonPath("$.data.product_id").value(productId.toString()))
                .andExpect(jsonPath("$.data.product_name").value("CPU AMD Ryzen 5 7600X"))
                .andExpect(jsonPath("$.data.quantity").value(2))
                .andExpect(jsonPath("$.data.subtotal").value(12400000));
    }

    @Test
    @DisplayName("Chưa đăng nhập -> Trả về 401 Unauthorized")
    void addCartItem_Unauthenticated_Returns401Unauthorized() throws Exception {
        AddCartItemRequest request = AddCartItemRequest.builder()
                .productId(UUID.randomUUID())
                .quantity(1)
                .build();

        mockMvc.perform(post("/api/cart/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Thiếu productId -> Trả về 400 Bad Request kèm validation error")
    @WithMockUser(username = "user@example.com", roles = "USER")
    void addCartItem_MissingProductId_Returns400BadRequest() throws Exception {
        AddCartItemRequest request = AddCartItemRequest.builder()
                .productId(null)
                .quantity(1)
                .build();

        mockMvc.perform(post("/api/cart/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Số lượng < 1 -> Trả về 400 Bad Request kèm validation error")
    @WithMockUser(username = "user@example.com", roles = "USER")
    void addCartItem_QuantityLessThanOne_Returns400BadRequest() throws Exception {
        AddCartItemRequest request = AddCartItemRequest.builder()
                .productId(UUID.randomUUID())
                .quantity(0)
                .build();

        mockMvc.perform(post("/api/cart/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Service ném ResourceNotFoundException -> Trả về 404 Not Found")
    @WithMockUser(username = "user@example.com", roles = "USER")
    void addCartItem_ProductNotFound_Returns404NotFound() throws Exception {
        UUID productId = UUID.randomUUID();
        AddCartItemRequest request = AddCartItemRequest.builder()
                .productId(productId)
                .quantity(1)
                .build();

        when(cartService.addCartItem(eq("user@example.com"), any(AddCartItemRequest.class)))
                .thenThrow(new ResourceNotFoundException("Không tìm thấy linh kiện với ID: " + productId));

        mockMvc.perform(post("/api/cart/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Không tìm thấy linh kiện với ID: " + productId));
    }

    @Test
    @DisplayName("Service ném BadRequestException (vượt tồn kho/hết hàng) -> Trả về 400 Bad Request")
    @WithMockUser(username = "user@example.com", roles = "USER")
    void addCartItem_StockExceeded_Returns400BadRequest() throws Exception {
        UUID productId = UUID.randomUUID();
        AddCartItemRequest request = AddCartItemRequest.builder()
                .productId(productId)
                .quantity(10)
                .build();

        when(cartService.addCartItem(eq("user@example.com"), any(AddCartItemRequest.class)))
                .thenThrow(new BadRequestException("Linh kiện đã hết hàng."));

        mockMvc.perform(post("/api/cart/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Linh kiện đã hết hàng."));
    }
}
