package com.techcraft.techcraftbackend.controller;

import com.techcraft.techcraftbackend.dto.request.AddCartItemRequest;
import com.techcraft.techcraftbackend.dto.request.AddCartPcCaseRequest;
import com.techcraft.techcraftbackend.dto.request.UpdateCartItemRequest;
import com.techcraft.techcraftbackend.dto.request.UpdateCartPcCaseRequest;
import com.techcraft.techcraftbackend.dto.response.CartItemResponse;
import com.techcraft.techcraftbackend.dto.response.CartResponse;
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
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

    // ==================== Tests cho POST /api/cart/items ====================

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
                .andExpect(jsonPath("$.data.productId").value(productId.toString()))
                .andExpect(jsonPath("$.data.productName").value("CPU AMD Ryzen 5 7600X"))
                .andExpect(jsonPath("$.data.quantity").value(2))
                .andExpect(jsonPath("$.data.subtotal").value(12400000));
    }

    @Test
    @DisplayName("Chưa đăng nhập khi thêm linh kiện -> Trả về 401 Unauthorized")
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
    @DisplayName("Số lượng linh kiện < 1 -> Trả về 400 Bad Request kèm validation error")
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
    @DisplayName("Service ném ResourceNotFoundException (linh kiện) -> Trả về 404 Not Found")
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
    @DisplayName("Service ném BadRequestException (hết hàng linh kiện) -> Trả về 400 Bad Request")
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

    // ==================== Tests cho PUT /api/cart/items/{id} ====================

    @Test
    @DisplayName("Cập nhật số lượng linh kiện trong giỏ hàng thành công khi đã đăng nhập (200 OK)")
    @WithMockUser(username = "user@example.com", roles = "USER")
    void updateCartItemQuantity_AsAuthenticatedUser_Returns200Ok() throws Exception {
        UUID cartItemId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        UpdateCartItemRequest request = UpdateCartItemRequest.builder()
                .quantity(3)
                .build();

        CartItemResponse response = CartItemResponse.builder()
                .id(cartItemId)
                .productId(productId)
                .productName("CPU AMD Ryzen 5 7600X")
                .productPrice(BigDecimal.valueOf(6200000))
                .category(ProductCategory.CPU)
                .stockQuantity(15)
                .quantity(3)
                .subtotal(BigDecimal.valueOf(18600000))
                .build();

        when(cartService.updateCartItemQuantity(eq("user@example.com"), eq(cartItemId), any(UpdateCartItemRequest.class)))
                .thenReturn(response);

        mockMvc.perform(put("/api/cart/items/{id}", cartItemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Cập nhật số lượng linh kiện trong giỏ hàng thành công!"))
                .andExpect(jsonPath("$.data.id").value(cartItemId.toString()))
                .andExpect(jsonPath("$.data.productId").value(productId.toString()))
                .andExpect(jsonPath("$.data.productName").value("CPU AMD Ryzen 5 7600X"))
                .andExpect(jsonPath("$.data.quantity").value(3))
                .andExpect(jsonPath("$.data.subtotal").value(18600000));
    }

    @Test
    @DisplayName("Chưa đăng nhập khi cập nhật số lượng linh kiện -> Trả về 401 Unauthorized")
    void updateCartItemQuantity_Unauthenticated_Returns401Unauthorized() throws Exception {
        UUID cartItemId = UUID.randomUUID();
        UpdateCartItemRequest request = UpdateCartItemRequest.builder()
                .quantity(2)
                .build();

        mockMvc.perform(put("/api/cart/items/{id}", cartItemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Thiếu quantity khi cập nhật -> Trả về 400 Bad Request kèm validation error")
    @WithMockUser(username = "user@example.com", roles = "USER")
    void updateCartItemQuantity_MissingQuantity_Returns400BadRequest() throws Exception {
        UUID cartItemId = UUID.randomUUID();
        UpdateCartItemRequest request = UpdateCartItemRequest.builder()
                .quantity(null)
                .build();

        mockMvc.perform(put("/api/cart/items/{id}", cartItemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Số lượng cập nhật < 1 -> Trả về 400 Bad Request kèm validation error")
    @WithMockUser(username = "user@example.com", roles = "USER")
    void updateCartItemQuantity_QuantityLessThanOne_Returns400BadRequest() throws Exception {
        UUID cartItemId = UUID.randomUUID();
        UpdateCartItemRequest request = UpdateCartItemRequest.builder()
                .quantity(0)
                .build();

        mockMvc.perform(put("/api/cart/items/{id}", cartItemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Service ném ResourceNotFoundException khi không tìm thấy linh kiện trong giỏ -> Trả về 404 Not Found")
    @WithMockUser(username = "user@example.com", roles = "USER")
    void updateCartItemQuantity_CartItemNotFound_Returns404NotFound() throws Exception {
        UUID cartItemId = UUID.randomUUID();
        UpdateCartItemRequest request = UpdateCartItemRequest.builder()
                .quantity(2)
                .build();

        when(cartService.updateCartItemQuantity(eq("user@example.com"), eq(cartItemId), any(UpdateCartItemRequest.class)))
                .thenThrow(new ResourceNotFoundException("Không tìm thấy linh kiện trong giỏ hàng với ID: " + cartItemId));

        mockMvc.perform(put("/api/cart/items/{id}", cartItemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Không tìm thấy linh kiện trong giỏ hàng với ID: " + cartItemId));
    }

    @Test
    @DisplayName("Service ném BadRequestException khi số lượng cập nhật vượt quá tồn kho -> Trả về 400 Bad Request")
    @WithMockUser(username = "user@example.com", roles = "USER")
    void updateCartItemQuantity_StockExceeded_Returns400BadRequest() throws Exception {
        UUID cartItemId = UUID.randomUUID();
        UpdateCartItemRequest request = UpdateCartItemRequest.builder()
                .quantity(20)
                .build();

        when(cartService.updateCartItemQuantity(eq("user@example.com"), eq(cartItemId), any(UpdateCartItemRequest.class)))
                .thenThrow(new BadRequestException("Số lượng yêu cầu (20) vượt quá số lượng tồn kho hiện có (15)."));

        mockMvc.perform(put("/api/cart/items/{id}", cartItemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Số lượng yêu cầu (20) vượt quá số lượng tồn kho hiện có (15)."));
    }

    // ==================== Tests cho POST /api/cart/pc-cases ====================

    @Test
    @DisplayName("Thêm cấu hình PC vào giỏ hàng thành công khi đã đăng nhập (201 Created)")
    @WithMockUser(username = "user@example.com", roles = "USER")
    void addPcCaseToCart_AsAuthenticatedUser_Returns201Created() throws Exception {
        UUID pcCaseId = UUID.randomUUID();
        UUID cartItemId = UUID.randomUUID();

        AddCartPcCaseRequest request = AddCartPcCaseRequest.builder()
                .pcCaseId(pcCaseId)
                .quantity(1)
                .build();

        CartItemResponse response = CartItemResponse.builder()
                .id(cartItemId)
                .pcCaseId(pcCaseId)
                .pcCaseName("Gaming Ultra 2026")
                .itemType("PC_CASE")
                .unitPrice(BigDecimal.valueOf(25000000))
                .quantity(1)
                .subtotal(BigDecimal.valueOf(25000000))
                .build();

        when(cartService.addPcCaseToCart(eq("user@example.com"), any(AddCartPcCaseRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/cart/pc-cases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Thêm cấu hình PC vào giỏ hàng thành công!"))
                .andExpect(jsonPath("$.data.id").value(cartItemId.toString()))
                .andExpect(jsonPath("$.data.pcCaseId").value(pcCaseId.toString()))
                .andExpect(jsonPath("$.data.pcCaseName").value("Gaming Ultra 2026"))
                .andExpect(jsonPath("$.data.itemType").value("PC_CASE"))
                .andExpect(jsonPath("$.data.quantity").value(1))
                .andExpect(jsonPath("$.data.subtotal").value(25000000));
    }

    @Test
    @DisplayName("Chưa đăng nhập khi thêm PC Case -> Trả về 401 Unauthorized")
    void addPcCaseToCart_Unauthenticated_Returns401Unauthorized() throws Exception {
        AddCartPcCaseRequest request = AddCartPcCaseRequest.builder()
                .pcCaseId(UUID.randomUUID())
                .quantity(1)
                .build();

        mockMvc.perform(post("/api/cart/pc-cases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Thiếu pcCaseId -> Trả về 400 Bad Request kèm validation error")
    @WithMockUser(username = "user@example.com", roles = "USER")
    void addPcCaseToCart_MissingPcCaseId_Returns400BadRequest() throws Exception {
        AddCartPcCaseRequest request = AddCartPcCaseRequest.builder()
                .pcCaseId(null)
                .quantity(1)
                .build();

        mockMvc.perform(post("/api/cart/pc-cases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Số lượng PC Case < 1 -> Trả về 400 Bad Request kèm validation error")
    @WithMockUser(username = "user@example.com", roles = "USER")
    void addPcCaseToCart_QuantityLessThanOne_Returns400BadRequest() throws Exception {
        AddCartPcCaseRequest request = AddCartPcCaseRequest.builder()
                .pcCaseId(UUID.randomUUID())
                .quantity(0)
                .build();

        mockMvc.perform(post("/api/cart/pc-cases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Service ném ResourceNotFoundException (PC Case) -> Trả về 404 Not Found")
    @WithMockUser(username = "user@example.com", roles = "USER")
    void addPcCaseToCart_PcCaseNotFound_Returns404NotFound() throws Exception {
        UUID pcCaseId = UUID.randomUUID();
        AddCartPcCaseRequest request = AddCartPcCaseRequest.builder()
                .pcCaseId(pcCaseId)
                .quantity(1)
                .build();

        when(cartService.addPcCaseToCart(eq("user@example.com"), any(AddCartPcCaseRequest.class)))
                .thenThrow(new ResourceNotFoundException("Không tìm thấy cấu hình PC với ID: " + pcCaseId));

        mockMvc.perform(post("/api/cart/pc-cases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Không tìm thấy cấu hình PC với ID: " + pcCaseId));
    }

    @Test
    @DisplayName("Service ném BadRequestException (hết linh kiện lắp ráp) -> Trả về 400 Bad Request")
    @WithMockUser(username = "user@example.com", roles = "USER")
    void addPcCaseToCart_StockExceeded_Returns400BadRequest() throws Exception {
        UUID pcCaseId = UUID.randomUUID();
        AddCartPcCaseRequest request = AddCartPcCaseRequest.builder()
                .pcCaseId(pcCaseId)
                .quantity(10)
                .build();

        when(cartService.addPcCaseToCart(eq("user@example.com"), any(AddCartPcCaseRequest.class)))
                .thenThrow(new BadRequestException("Cấu hình PC này hiện không đủ linh kiện trong kho để lắp ráp."));

        mockMvc.perform(post("/api/cart/pc-cases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Cấu hình PC này hiện không đủ linh kiện trong kho để lắp ráp."));
    }

    // ==================== Tests cho PUT /api/cart/pc-cases/{id} ====================

    @Test
    @DisplayName("Cập nhật số lượng PC Case trong giỏ hàng thành công khi đã đăng nhập (200 OK)")
    @WithMockUser(username = "user@example.com", roles = "USER")
    void updateCartPcCaseQuantity_AsAuthenticatedUser_Returns200Ok() throws Exception {
        UUID cartItemId = UUID.randomUUID();
        UUID pcCaseId = UUID.randomUUID();

        UpdateCartPcCaseRequest request = UpdateCartPcCaseRequest.builder()
                .quantity(2)
                .build();

        CartItemResponse response = CartItemResponse.builder()
                .id(cartItemId)
                .pcCaseId(pcCaseId)
                .pcCaseName("Gaming Ultra 2026")
                .itemType("PC_CASE")
                .unitPrice(BigDecimal.valueOf(25000000))
                .quantity(2)
                .subtotal(BigDecimal.valueOf(50000000))
                .build();

        when(cartService.updateCartPcCaseQuantity(eq("user@example.com"), eq(cartItemId), any(UpdateCartPcCaseRequest.class)))
                .thenReturn(response);

        mockMvc.perform(put("/api/cart/pc-cases/{id}", cartItemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Cập nhật số lượng cấu hình PC trong giỏ hàng thành công!"))
                .andExpect(jsonPath("$.data.id").value(cartItemId.toString()))
                .andExpect(jsonPath("$.data.pcCaseId").value(pcCaseId.toString()))
                .andExpect(jsonPath("$.data.pcCaseName").value("Gaming Ultra 2026"))
                .andExpect(jsonPath("$.data.itemType").value("PC_CASE"))
                .andExpect(jsonPath("$.data.quantity").value(2))
                .andExpect(jsonPath("$.data.subtotal").value(50000000));
    }

    @Test
    @DisplayName("Chưa đăng nhập khi cập nhật số lượng PC Case -> Trả về 401 Unauthorized")
    void updateCartPcCaseQuantity_Unauthenticated_Returns401Unauthorized() throws Exception {
        UUID cartItemId = UUID.randomUUID();
        UpdateCartPcCaseRequest request = UpdateCartPcCaseRequest.builder()
                .quantity(2)
                .build();

        mockMvc.perform(put("/api/cart/pc-cases/{id}", cartItemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Thiếu quantity khi cập nhật PC Case -> Trả về 400 Bad Request kèm validation error")
    @WithMockUser(username = "user@example.com", roles = "USER")
    void updateCartPcCaseQuantity_MissingQuantity_Returns400BadRequest() throws Exception {
        UUID cartItemId = UUID.randomUUID();
        UpdateCartPcCaseRequest request = UpdateCartPcCaseRequest.builder()
                .quantity(null)
                .build();

        mockMvc.perform(put("/api/cart/pc-cases/{id}", cartItemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Số lượng cập nhật PC Case < 1 -> Trả về 400 Bad Request kèm validation error")
    @WithMockUser(username = "user@example.com", roles = "USER")
    void updateCartPcCaseQuantity_QuantityLessThanOne_Returns400BadRequest() throws Exception {
        UUID cartItemId = UUID.randomUUID();
        UpdateCartPcCaseRequest request = UpdateCartPcCaseRequest.builder()
                .quantity(0)
                .build();

        mockMvc.perform(put("/api/cart/pc-cases/{id}", cartItemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Service ném ResourceNotFoundException khi không tìm thấy PC Case trong giỏ -> Trả về 404 Not Found")
    @WithMockUser(username = "user@example.com", roles = "USER")
    void updateCartPcCaseQuantity_CartItemNotFound_Returns404NotFound() throws Exception {
        UUID cartItemId = UUID.randomUUID();
        UpdateCartPcCaseRequest request = UpdateCartPcCaseRequest.builder()
                .quantity(2)
                .build();

        when(cartService.updateCartPcCaseQuantity(eq("user@example.com"), eq(cartItemId), any(UpdateCartPcCaseRequest.class)))
                .thenThrow(new ResourceNotFoundException("Không tìm thấy cấu hình PC trong giỏ hàng với ID: " + cartItemId));

        mockMvc.perform(put("/api/cart/pc-cases/{id}", cartItemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Không tìm thấy cấu hình PC trong giỏ hàng với ID: " + cartItemId));
    }

    @Test
    @DisplayName("Service ném BadRequestException khi số lượng cập nhật PC Case vượt quá khả năng lắp ráp -> Trả về 400 Bad Request")
    @WithMockUser(username = "user@example.com", roles = "USER")
    void updateCartPcCaseQuantity_StockExceeded_Returns400BadRequest() throws Exception {
        UUID cartItemId = UUID.randomUUID();
        UpdateCartPcCaseRequest request = UpdateCartPcCaseRequest.builder()
                .quantity(10)
                .build();

        when(cartService.updateCartPcCaseQuantity(eq("user@example.com"), eq(cartItemId), any(UpdateCartPcCaseRequest.class)))
                .thenThrow(new BadRequestException("Số lượng yêu cầu (10) vượt quá số lượng tối đa có thể lắp ráp hiện tại (4)."));

        mockMvc.perform(put("/api/cart/pc-cases/{id}", cartItemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Số lượng yêu cầu (10) vượt quá số lượng tối đa có thể lắp ráp hiện tại (4)."));
    }

    // ==================== Tests cho GET /api/cart ====================

    @Test
    @DisplayName("Lấy thông tin giỏ hàng thành công khi đã đăng nhập (200 OK)")
    @WithMockUser(username = "user@example.com", roles = "USER")
    void getCart_AsAuthenticatedUser_Returns200Ok() throws Exception {
        UUID cartItemId1 = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID cartItemId2 = UUID.randomUUID();
        UUID pcCaseId = UUID.randomUUID();

        CartItemResponse productItem = CartItemResponse.builder()
                .id(cartItemId1)
                .productId(productId)
                .productName("CPU Intel Core i5-13400")
                .itemType("PRODUCT")
                .unitPrice(BigDecimal.valueOf(5500000))
                .quantity(2)
                .subtotal(BigDecimal.valueOf(11000000))
                .build();

        CartItemResponse pcCaseItem = CartItemResponse.builder()
                .id(cartItemId2)
                .pcCaseId(pcCaseId)
                .pcCaseName("Gaming Ultra 2026")
                .itemType("PC_CASE")
                .unitPrice(BigDecimal.valueOf(25000000))
                .quantity(1)
                .subtotal(BigDecimal.valueOf(25000000))
                .build();

        CartResponse response = CartResponse.builder()
                .items(List.of(productItem, pcCaseItem))
                .totalItems(2)
                .totalQuantity(3)
                .totalAmount(BigDecimal.valueOf(36000000))
                .build();

        when(cartService.getCart("user@example.com")).thenReturn(response);

        mockMvc.perform(get("/api/cart"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Lấy thông tin giỏ hàng thành công!"))
                .andExpect(jsonPath("$.data.totalItems").value(2))
                .andExpect(jsonPath("$.data.totalQuantity").value(3))
                .andExpect(jsonPath("$.data.totalAmount").value(36000000))
                .andExpect(jsonPath("$.data.items[0].id").value(cartItemId1.toString()))
                .andExpect(jsonPath("$.data.items[0].itemType").value("PRODUCT"))
                .andExpect(jsonPath("$.data.items[1].id").value(cartItemId2.toString()))
                .andExpect(jsonPath("$.data.items[1].itemType").value("PC_CASE"));
    }

    @Test
    @DisplayName("Chưa đăng nhập khi lấy giỏ hàng -> Trả về 401 Unauthorized")
    void getCart_Unauthenticated_Returns401Unauthorized() throws Exception {
        mockMvc.perform(get("/api/cart"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("User không tồn tại khi lấy giỏ hàng -> Trả về 404 Not Found")
    @WithMockUser(username = "notfound@example.com", roles = "USER")
    void getCart_UserNotFound_Returns404NotFound() throws Exception {
        when(cartService.getCart("notfound@example.com"))
                .thenThrow(new ResourceNotFoundException("Không tìm thấy người dùng"));

        mockMvc.perform(get("/api/cart"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Không tìm thấy người dùng"));
    }

    // ==================== Tests cho DELETE /api/cart/items/{id} ====================

    @Test
    @DisplayName("Xoá linh kiện khỏi giỏ hàng thành công khi đã đăng nhập (200 OK)")
    @WithMockUser(username = "user@example.com", roles = "USER")
    void deleteCartItem_AsAuthenticatedUser_Returns200Ok() throws Exception {
        UUID cartItemId = UUID.randomUUID();

        doNothing().when(cartService).deleteCartItem("user@example.com", cartItemId);

        mockMvc.perform(delete("/api/cart/items/{id}", cartItemId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Xoá linh kiện khỏi giỏ hàng thành công!"))
                .andExpect(jsonPath("$.data").doesNotExist());

        verify(cartService).deleteCartItem("user@example.com", cartItemId);
    }

    @Test
    @DisplayName("Chưa đăng nhập khi xoá linh kiện -> Trả về 401 Unauthorized")
    void deleteCartItem_Unauthenticated_Returns401Unauthorized() throws Exception {
        UUID cartItemId = UUID.randomUUID();

        mockMvc.perform(delete("/api/cart/items/{id}", cartItemId))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Không tìm thấy linh kiện trong giỏ hàng khi xoá -> Trả về 404 Not Found")
    @WithMockUser(username = "user@example.com", roles = "USER")
    void deleteCartItem_NotFound_Returns404NotFound() throws Exception {
        UUID cartItemId = UUID.randomUUID();

        doThrow(new ResourceNotFoundException("Không tìm thấy linh kiện trong giỏ hàng với ID: " + cartItemId))
                .when(cartService).deleteCartItem("user@example.com", cartItemId);

        mockMvc.perform(delete("/api/cart/items/{id}", cartItemId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Không tìm thấy linh kiện trong giỏ hàng với ID: " + cartItemId));
    }

    @Test
    @DisplayName("Mục giỏ hàng không phải linh kiện lẻ (ví dụ PC Case) khi xoá -> Trả về 400 Bad Request")
    @WithMockUser(username = "user@example.com", roles = "USER")
    void deleteCartItem_NotAProductItem_Returns400BadRequest() throws Exception {
        UUID cartItemId = UUID.randomUUID();

        doThrow(new BadRequestException("Mục này trong giỏ hàng không phải là linh kiện lẻ."))
                .when(cartService).deleteCartItem("user@example.com", cartItemId);

        mockMvc.perform(delete("/api/cart/items/{id}", cartItemId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Mục này trong giỏ hàng không phải là linh kiện lẻ."));
    }

    @Test
    @DisplayName("UUID không hợp lệ khi xoá linh kiện -> Trả về 400 Bad Request")
    @WithMockUser(username = "user@example.com", roles = "USER")
    void deleteCartItem_InvalidUuid_Returns400BadRequest() throws Exception {
        mockMvc.perform(delete("/api/cart/items/invalid-uuid-format"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
}
