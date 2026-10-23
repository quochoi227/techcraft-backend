package com.techcraft.techcraftbackend.controller;

import com.techcraft.techcraftbackend.dto.request.AdminOrderFilterRequest;
import com.techcraft.techcraftbackend.dto.request.CreateOrderItemRequest;
import com.techcraft.techcraftbackend.dto.request.CreateOrderRequest;
import com.techcraft.techcraftbackend.dto.request.OrderFilterRequest;
import com.techcraft.techcraftbackend.dto.request.UpdateOrderStatusRequest;
import com.techcraft.techcraftbackend.dto.response.OrderItemResponse;
import com.techcraft.techcraftbackend.dto.response.OrderResponse;
import com.techcraft.techcraftbackend.dto.response.PageResponse;
import com.techcraft.techcraftbackend.enums.OrderStatus;
import com.techcraft.techcraftbackend.enums.PaymentMethod;
import com.techcraft.techcraftbackend.exception.AppException;
import com.techcraft.techcraftbackend.exception.BadRequestException;
import com.techcraft.techcraftbackend.exception.ResourceNotFoundException;
import com.techcraft.techcraftbackend.service.OrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

    @Test
    @DisplayName("Hủy đơn hàng thành công khi đã đăng nhập (200 OK)")
    @WithMockUser(username = "customer@example.com", roles = "USER")
    void cancelOrder_AsAuthenticatedUser_Returns200Ok() throws Exception {
        UUID orderId = UUID.randomUUID();

        OrderResponse expectedResponse = OrderResponse.builder()
                .id(orderId)
                .userEmail("customer@example.com")
                .status(OrderStatus.CANCELLED)
                .totalAmount(BigDecimal.valueOf(5000000))
                .build();

        when(orderService.cancelOrder(eq("customer@example.com"), eq(orderId)))
                .thenReturn(expectedResponse);

        mockMvc.perform(put("/api/orders/{id}/cancel", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Hủy đơn hàng thành công!"))
                .andExpect(jsonPath("$.data.id").value(orderId.toString()))
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));
    }

    @Test
    @DisplayName("Hủy đơn hàng thất bại khi chưa đăng nhập (401 Unauthorized)")
    void cancelOrder_AsGuest_Returns401Unauthorized() throws Exception {
        UUID orderId = UUID.randomUUID();

        mockMvc.perform(put("/api/orders/{id}/cancel", orderId))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Hủy đơn hàng thất bại khi không tìm thấy đơn hàng (404 Not Found)")
    @WithMockUser(username = "customer@example.com", roles = "USER")
    void cancelOrder_WhenOrderNotFound_Returns404NotFound() throws Exception {
        UUID orderId = UUID.randomUUID();

        when(orderService.cancelOrder(eq("customer@example.com"), eq(orderId)))
                .thenThrow(new ResourceNotFoundException("Không tìm thấy đơn hàng với ID: " + orderId));

        mockMvc.perform(put("/api/orders/{id}/cancel", orderId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Không tìm thấy đơn hàng với ID: " + orderId));
    }

    @Test
    @DisplayName("Hủy đơn hàng thất bại khi không phải chủ sở hữu hoặc admin (403 Forbidden)")
    @WithMockUser(username = "customer@example.com", roles = "USER")
    void cancelOrder_WhenNotOwnerAndNotAdmin_Returns403Forbidden() throws Exception {
        UUID orderId = UUID.randomUUID();

        when(orderService.cancelOrder(eq("customer@example.com"), eq(orderId)))
                .thenThrow(new AppException("Bạn không có quyền hủy đơn hàng này.", HttpStatus.FORBIDDEN));

        mockMvc.perform(put("/api/orders/{id}/cancel", orderId))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Bạn không có quyền hủy đơn hàng này."));
    }

    @Test
    @DisplayName("Hủy đơn hàng thất bại khi trạng thái đơn hàng không hợp lệ (400 Bad Request)")
    @WithMockUser(username = "customer@example.com", roles = "USER")
    void cancelOrder_WhenStatusCannotBeCancelled_Returns400BadRequest() throws Exception {
        UUID orderId = UUID.randomUUID();

        when(orderService.cancelOrder(eq("customer@example.com"), eq(orderId)))
                .thenThrow(new BadRequestException("Không thể hủy đơn hàng đang trong quá trình vận chuyển."));

        mockMvc.perform(put("/api/orders/{id}/cancel", orderId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Không thể hủy đơn hàng đang trong quá trình vận chuyển."));
    }

    @Test
    @DisplayName("Admin cập nhật trạng thái đơn hàng thành công (200 OK)")
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    void updateOrderStatus_AsAdmin_Returns200Ok() throws Exception {
        UUID orderId = UUID.randomUUID();
        UpdateOrderStatusRequest request = UpdateOrderStatusRequest.builder()
                .status(OrderStatus.CONFIRMED)
                .build();

        OrderResponse expectedResponse = OrderResponse.builder()
                .id(orderId)
                .status(OrderStatus.CONFIRMED)
                .build();

        when(orderService.updateOrderStatus(eq(orderId), any(UpdateOrderStatusRequest.class)))
                .thenReturn(expectedResponse);

        mockMvc.perform(put("/api/admin/orders/{id}/status", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Cập nhật trạng thái đơn hàng thành công!"))
                .andExpect(jsonPath("$.data.id").value(orderId.toString()))
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"));
    }

    @Test
    @DisplayName("Admin cập nhật trạng thái đơn hàng thất bại khi là User thường (403 Forbidden)")
    @WithMockUser(username = "user@example.com", roles = "USER")
    void updateOrderStatus_AsUser_Returns403Forbidden() throws Exception {
        UUID orderId = UUID.randomUUID();
        UpdateOrderStatusRequest request = UpdateOrderStatusRequest.builder()
                .status(OrderStatus.CONFIRMED)
                .build();

        mockMvc.perform(put("/api/admin/orders/{id}/status", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Admin cập nhật trạng thái đơn hàng thất bại khi chưa đăng nhập (401 Unauthorized)")
    void updateOrderStatus_AsGuest_Returns401Unauthorized() throws Exception {
        UUID orderId = UUID.randomUUID();
        UpdateOrderStatusRequest request = UpdateOrderStatusRequest.builder()
                .status(OrderStatus.CONFIRMED)
                .build();

        mockMvc.perform(put("/api/admin/orders/{id}/status", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Admin cập nhật trạng thái đơn hàng thất bại khi body không hợp lệ (400 Bad Request)")
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    void updateOrderStatus_WhenInvalidBody_Returns400BadRequest() throws Exception {
        UUID orderId = UUID.randomUUID();

        mockMvc.perform(put("/api/admin/orders/{id}/status", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Dữ liệu không hợp lệ"))
                .andExpect(jsonPath("$.data.status").value("Trạng thái đơn hàng không được để trống"));
    }

    @Test
    @DisplayName("Admin cập nhật trạng thái đơn hàng thất bại khi không tìm thấy đơn (404 Not Found)")
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    void updateOrderStatus_WhenOrderNotFound_Returns404NotFound() throws Exception {
        UUID orderId = UUID.randomUUID();
        UpdateOrderStatusRequest request = UpdateOrderStatusRequest.builder()
                .status(OrderStatus.CONFIRMED)
                .build();

        when(orderService.updateOrderStatus(eq(orderId), any(UpdateOrderStatusRequest.class)))
                .thenThrow(new ResourceNotFoundException("Không tìm thấy đơn hàng với ID: " + orderId));

        mockMvc.perform(put("/api/admin/orders/{id}/status", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Không tìm thấy đơn hàng với ID: " + orderId));
    }

    @Test
    @DisplayName("Admin cập nhật trạng thái đơn hàng thất bại khi vi phạm chuyển đổi trạng thái (400 Bad Request)")
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    void updateOrderStatus_WhenBadRequestFromService_Returns400BadRequest() throws Exception {
        UUID orderId = UUID.randomUUID();
        UpdateOrderStatusRequest request = UpdateOrderStatusRequest.builder()
                .status(OrderStatus.DELIVERED)
                .build();

        when(orderService.updateOrderStatus(eq(orderId), any(UpdateOrderStatusRequest.class)))
                .thenThrow(new BadRequestException("Đơn hàng đang chờ xử lý chỉ có thể chuyển sang CONFIRMED hoặc CANCELLED."));

        mockMvc.perform(put("/api/admin/orders/{id}/status", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Đơn hàng đang chờ xử lý chỉ có thể chuyển sang CONFIRMED hoặc CANCELLED."));
    }

    @Test
    @DisplayName("Lấy lịch sử đơn hàng thành công khi đã đăng nhập (200 OK)")
    @WithMockUser(username = "customer@example.com", roles = "USER")
    void getUserOrders_AsAuthenticatedUser_Returns200Ok() throws Exception {
        UUID orderId = UUID.randomUUID();
        OrderResponse orderResponse = OrderResponse.builder()
                .id(orderId)
                .status(OrderStatus.PENDING)
                .totalAmount(BigDecimal.valueOf(1000000))
                .build();

        PageResponse<OrderResponse> pageResponse = PageResponse.<OrderResponse>builder()
                .content(List.of(orderResponse))
                .page(0)
                .size(10)
                .totalElements(1)
                .totalPages(1)
                .first(true)
                .last(true)
                .empty(false)
                .build();

        when(orderService.getUserOrders(eq("customer@example.com"), any(OrderFilterRequest.class)))
                .thenReturn(pageResponse);

        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Lấy lịch sử đơn hàng thành công!"))
                .andExpect(jsonPath("$.data.content[0].id").value(orderId.toString()))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(10))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    @DisplayName("Lấy lịch sử đơn hàng thành công kèm lọc trạng thái (200 OK)")
    @WithMockUser(username = "customer@example.com", roles = "USER")
    void getUserOrders_WithStatusFilter_Returns200Ok() throws Exception {
        UUID orderId = UUID.randomUUID();
        OrderResponse orderResponse = OrderResponse.builder()
                .id(orderId)
                .status(OrderStatus.PENDING)
                .build();

        PageResponse<OrderResponse> pageResponse = PageResponse.<OrderResponse>builder()
                .content(List.of(orderResponse))
                .page(0)
                .size(5)
                .totalElements(1)
                .totalPages(1)
                .first(true)
                .last(true)
                .empty(false)
                .build();

        when(orderService.getUserOrders(eq("customer@example.com"), any(OrderFilterRequest.class)))
                .thenReturn(pageResponse);

        mockMvc.perform(get("/api/orders")
                        .param("status", "PENDING")
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].status").value("PENDING"));
    }

    @Test
    @DisplayName("Lấy lịch sử đơn hàng thất bại khi chưa đăng nhập (401 Unauthorized)")
    void getUserOrders_AsGuest_Returns401Unauthorized() throws Exception {
        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Lấy lịch sử đơn hàng thất bại khi tham số phân trang không hợp lệ (400 Bad Request)")
    @WithMockUser(username = "customer@example.com", roles = "USER")
    void getUserOrders_WhenInvalidPagination_Returns400BadRequest() throws Exception {
        mockMvc.perform(get("/api/orders")
                        .param("page", "-1")
                        .param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Dữ liệu không hợp lệ"));
    }

    @Test
    @DisplayName("Admin lấy tất cả đơn hàng thành công (200 OK)")
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    void getAllOrders_AsAdmin_Returns200Ok() throws Exception {
        UUID orderId = UUID.randomUUID();
        OrderResponse orderResponse = OrderResponse.builder()
                .id(orderId)
                .status(OrderStatus.PENDING)
                .build();

        PageResponse<OrderResponse> pageResponse = PageResponse.<OrderResponse>builder()
                .content(List.of(orderResponse))
                .page(0)
                .size(10)
                .totalElements(1)
                .totalPages(1)
                .first(true)
                .last(true)
                .empty(false)
                .build();

        when(orderService.getAllOrders(any(AdminOrderFilterRequest.class))).thenReturn(pageResponse);

        mockMvc.perform(get("/api/admin/orders")
                        .param("status", "PENDING")
                        .param("search", "customer")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Lấy danh sách tất cả đơn hàng thành công!"))
                .andExpect(jsonPath("$.data.content[0].id").value(orderId.toString()))
                .andExpect(jsonPath("$.data.content[0].status").value("PENDING"));
    }

    @Test
    @DisplayName("Admin lấy tất cả đơn hàng thất bại khi user thường gọi (403 Forbidden)")
    @WithMockUser(username = "customer@example.com", roles = "USER")
    void getAllOrders_AsUser_Returns403Forbidden() throws Exception {
        mockMvc.perform(get("/api/admin/orders"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Admin lấy tất cả đơn hàng thất bại khi khách chưa đăng nhập (401 Unauthorized)")
    void getAllOrders_AsGuest_Returns401Unauthorized() throws Exception {
        mockMvc.perform(get("/api/admin/orders"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Admin lấy tất cả đơn hàng thất bại khi tham số phân trang không hợp lệ (400 Bad Request)")
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    void getAllOrders_WhenInvalidPagination_Returns400BadRequest() throws Exception {
        mockMvc.perform(get("/api/admin/orders")
                        .param("page", "-1")
                        .param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Dữ liệu không hợp lệ"));
    }

    @Test
    @DisplayName("Lấy chi tiết đơn hàng thành công khi là chủ sở hữu (200 OK)")
    @WithMockUser(username = "customer@example.com", roles = "USER")
    void getOrderById_WhenOwner_ReturnsOk() throws Exception {
        UUID orderId = UUID.randomUUID();
        OrderResponse orderResponse = OrderResponse.builder()
                .id(orderId)
                .status(OrderStatus.PENDING)
                .shippingAddress("123 Can Tho")
                .totalAmount(new BigDecimal("15000000"))
                .build();

        when(orderService.getOrderById(eq("customer@example.com"), eq(orderId))).thenReturn(orderResponse);

        mockMvc.perform(get("/api/orders/{id}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Lấy thông tin chi tiết đơn hàng thành công!"))
                .andExpect(jsonPath("$.data.id").value(orderId.toString()))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    @DisplayName("Admin lấy chi tiết đơn hàng thành công qua endpoint /admin/orders/{id} (200 OK)")
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    void getOrderById_WhenAdminViaAdminPath_ReturnsOk() throws Exception {
        UUID orderId = UUID.randomUUID();
        OrderResponse orderResponse = OrderResponse.builder()
                .id(orderId)
                .status(OrderStatus.CONFIRMED)
                .shippingAddress("123 Can Tho")
                .totalAmount(new BigDecimal("20000000"))
                .build();

        when(orderService.getOrderById(eq("admin@example.com"), eq(orderId))).thenReturn(orderResponse);

        mockMvc.perform(get("/api/admin/orders/{id}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Lấy thông tin chi tiết đơn hàng thành công!"))
                .andExpect(jsonPath("$.data.id").value(orderId.toString()))
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"));
    }

    @Test
    @DisplayName("Lấy chi tiết đơn hàng thất bại khi không phải chủ sở hữu (403 Forbidden)")
    @WithMockUser(username = "other@example.com", roles = "USER")
    void getOrderById_WhenOtherUser_ReturnsForbidden() throws Exception {
        UUID orderId = UUID.randomUUID();
        when(orderService.getOrderById(eq("other@example.com"), eq(orderId)))
                .thenThrow(new AppException("Bạn không có quyền xem thông tin chi tiết đơn hàng này.", HttpStatus.FORBIDDEN));

        mockMvc.perform(get("/api/orders/{id}", orderId))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Bạn không có quyền xem thông tin chi tiết đơn hàng này."));
    }

    @Test
    @DisplayName("Lấy chi tiết đơn hàng thất bại khi chưa đăng nhập (401 Unauthorized)")
    void getOrderById_WhenGuest_ReturnsUnauthorized() throws Exception {
        UUID orderId = UUID.randomUUID();
        mockMvc.perform(get("/api/orders/{id}", orderId))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Lấy chi tiết đơn hàng thất bại khi không tìm thấy đơn hàng (404 Not Found)")
    @WithMockUser(username = "customer@example.com", roles = "USER")
    void getOrderById_WhenNotFound_ReturnsNotFound() throws Exception {
        UUID orderId = UUID.randomUUID();
        when(orderService.getOrderById(eq("customer@example.com"), eq(orderId)))
                .thenThrow(new ResourceNotFoundException("Không tìm thấy đơn hàng với ID: " + orderId));

        mockMvc.perform(get("/api/orders/{id}", orderId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Không tìm thấy đơn hàng với ID: " + orderId));
    }
}
