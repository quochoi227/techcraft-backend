package com.techcraft.techcraftbackend.service;

import com.techcraft.techcraftbackend.dto.request.CreateOrderItemRequest;
import com.techcraft.techcraftbackend.dto.request.CreateOrderRequest;
import com.techcraft.techcraftbackend.dto.request.UpdateOrderStatusRequest;
import com.techcraft.techcraftbackend.dto.response.OrderItemResponse;
import com.techcraft.techcraftbackend.dto.response.OrderResponse;
import com.techcraft.techcraftbackend.entity.*;
import com.techcraft.techcraftbackend.enums.OrderStatus;
import com.techcraft.techcraftbackend.enums.PaymentMethod;
import com.techcraft.techcraftbackend.enums.ProductCategory;
import com.techcraft.techcraftbackend.enums.Role;
import com.techcraft.techcraftbackend.exception.AppException;
import org.springframework.http.HttpStatus;
import com.techcraft.techcraftbackend.exception.BadRequestException;
import com.techcraft.techcraftbackend.exception.ResourceNotFoundException;
import com.techcraft.techcraftbackend.mapper.OrderMapper;
import com.techcraft.techcraftbackend.repository.*;
import com.techcraft.techcraftbackend.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private PcCaseRepository pcCaseRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private OrderMapper orderMapper;

    @InjectMocks
    private OrderServiceImpl orderService;

    private User sampleUser;
    private Product sampleCpu;
    private Product sampleGpu;
    private PcCase samplePcCase;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id(UUID.randomUUID())
                .email("customer@example.com")
                .fullName("Nguyen Van A")
                .phone("0987654321")
                .address("123 Duong Le Loi, Quan 1, TP.HCM")
                .role(Role.USER)
                .emailVerified(true)
                .build();

        sampleCpu = Product.builder()
                .id(UUID.randomUUID())
                .name("Intel Core i5-13400F")
                .price(BigDecimal.valueOf(4500000))
                .stockQuantity(10)
                .category(ProductCategory.CPU)
                .isActive(true)
                .build();

        sampleGpu = Product.builder()
                .id(UUID.randomUUID())
                .name("NVIDIA RTX 4060 8GB")
                .price(BigDecimal.valueOf(8000000))
                .stockQuantity(5)
                .category(ProductCategory.GPU)
                .isActive(true)
                .build();

        samplePcCase = PcCase.builder()
                .id(UUID.randomUUID())
                .name("Gaming Entry 2026")
                .useCase("gaming")
                .totalPrice(BigDecimal.valueOf(12500000))
                .isActive(true)
                .items(new ArrayList<>())
                .build();

        PcCaseItem item1 = PcCaseItem.builder()
                .id(UUID.randomUUID())
                .pcCase(samplePcCase)
                .product(sampleCpu)
                .category(ProductCategory.CPU)
                .build();

        PcCaseItem item2 = PcCaseItem.builder()
                .id(UUID.randomUUID())
                .pcCase(samplePcCase)
                .product(sampleGpu)
                .category(ProductCategory.GPU)
                .build();

        samplePcCase.getItems().add(item1);
        samplePcCase.getItems().add(item2);
    }

    @Test
    @DisplayName("Đặt hàng thành công với linh kiện lẻ")
    void createOrder_WithSingleProduct_Success() {
        CreateOrderRequest request = CreateOrderRequest.builder()
                .shippingAddress("789 Nguyen Hue, Quan 1, TP.HCM")
                .paymentMethod(PaymentMethod.COD)
                .note("Giao buoi sang")
                .items(List.of(
                        CreateOrderItemRequest.builder()
                                .productId(sampleCpu.getId())
                                .quantity(2)
                                .build()
                ))
                .build();

        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(sampleUser));
        when(productRepository.findById(sampleCpu.getId())).thenReturn(Optional.of(sampleCpu));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse expectedResponse = OrderResponse.builder()
                .id(UUID.randomUUID())
                .userId(sampleUser.getId())
                .shippingAddress("789 Nguyen Hue, Quan 1, TP.HCM")
                .totalAmount(BigDecimal.valueOf(9000000))
                .build();
        when(orderMapper.toResponse(any(Order.class))).thenReturn(expectedResponse);

        OrderResponse response = orderService.createOrder("customer@example.com", request);

        assertNotNull(response);
        assertEquals(expectedResponse.getTotalAmount(), response.getTotalAmount());
        // Tồn kho CPU từ 10 giảm đi 2 còn 8
        assertEquals(8, sampleCpu.getStockQuantity());
        verify(productRepository).save(sampleCpu);

        // Lưu đơn hàng
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());
        Order savedOrder = orderCaptor.getValue();
        assertEquals(OrderStatus.PENDING, savedOrder.getStatus());
        assertEquals(PaymentMethod.COD, savedOrder.getPaymentMethod());
        assertEquals("789 Nguyen Hue, Quan 1, TP.HCM", savedOrder.getShippingAddress());
        assertEquals(BigDecimal.valueOf(9000000), savedOrder.getTotalAmount());
        assertEquals(1, savedOrder.getItems().size());

        // Dọn giỏ hàng theo productId
        verify(cartItemRepository).deleteByUserIdAndProductId(sampleUser.getId(), sampleCpu.getId());
    }

    @Test
    @DisplayName("Đặt hàng thành công với cấu hình PC Case")
    void createOrder_WithPcCase_Success() {
        CreateOrderRequest request = CreateOrderRequest.builder()
                .shippingAddress("123 Duong Le Loi, TP.HCM")
                .items(List.of(
                        CreateOrderItemRequest.builder()
                                .pcCaseId(samplePcCase.getId())
                                .quantity(1)
                                .build()
                ))
                .build();

        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(sampleUser));
        when(pcCaseRepository.findById(samplePcCase.getId())).thenReturn(Optional.of(samplePcCase));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse expectedResponse = OrderResponse.builder()
                .id(UUID.randomUUID())
                .userId(sampleUser.getId())
                .totalAmount(BigDecimal.valueOf(12500000))
                .build();
        when(orderMapper.toResponse(any(Order.class))).thenReturn(expectedResponse);

        OrderResponse response = orderService.createOrder("customer@example.com", request);

        assertNotNull(response);
        // Tồn kho các linh kiện con trong PC Case đều bị trừ 1
        assertEquals(9, sampleCpu.getStockQuantity());
        assertEquals(4, sampleGpu.getStockQuantity());
        verify(productRepository).save(sampleCpu);
        verify(productRepository).save(sampleGpu);

        // Dọn giỏ hàng theo pcCaseId
        verify(cartItemRepository).deleteByUserIdAndPcCaseId(sampleUser.getId(), samplePcCase.getId());
    }

    @Test
    @DisplayName("Đặt hàng thành công kết hợp cả linh kiện lẻ và PC Case")
    void createOrder_WithBothProductAndPcCase_Success() {
        CreateOrderRequest request = CreateOrderRequest.builder()
                .shippingAddress("123 Duong Le Loi, TP.HCM")
                .items(List.of(
                        CreateOrderItemRequest.builder()
                                .productId(sampleCpu.getId())
                                .quantity(1)
                                .build(),
                        CreateOrderItemRequest.builder()
                                .pcCaseId(samplePcCase.getId())
                                .quantity(1)
                                .build()
                ))
                .build();

        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(sampleUser));
        when(productRepository.findById(sampleCpu.getId())).thenReturn(Optional.of(sampleCpu));
        when(pcCaseRepository.findById(samplePcCase.getId())).thenReturn(Optional.of(samplePcCase));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        when(orderMapper.toResponse(any(Order.class))).thenReturn(OrderResponse.builder().build());

        orderService.createOrder("customer@example.com", request);

        // CPU: 1 cái lẻ + 1 cái trong PC Case = cần 2 cái. Tồn kho 10 -> còn 8
        assertEquals(8, sampleCpu.getStockQuantity());
        // GPU: 1 cái trong PC Case. Tồn kho 5 -> còn 4
        assertEquals(4, sampleGpu.getStockQuantity());

        verify(cartItemRepository).deleteByUserIdAndProductId(sampleUser.getId(), sampleCpu.getId());
        verify(cartItemRepository).deleteByUserIdAndPcCaseId(sampleUser.getId(), samplePcCase.getId());
    }

    @Test
    @DisplayName("Báo lỗi khi tổng nhu cầu linh kiện (lẻ + trong PC Case) vượt quá tồn kho")
    void createOrder_SharedComponentStockOverflow_ThrowsBadRequest() {
        // CPU tồn kho chỉ còn 2 cái
        sampleCpu.setStockQuantity(2);

        CreateOrderRequest request = CreateOrderRequest.builder()
                .shippingAddress("123 Duong Le Loi, TP.HCM")
                .items(List.of(
                        CreateOrderItemRequest.builder()
                                .productId(sampleCpu.getId())
                                .quantity(2)
                                .build(),
                        CreateOrderItemRequest.builder()
                                .pcCaseId(samplePcCase.getId())
                                .quantity(1)
                                .build()
                ))
                .build();

        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(sampleUser));
        when(productRepository.findById(sampleCpu.getId())).thenReturn(Optional.of(sampleCpu));
        when(pcCaseRepository.findById(samplePcCase.getId())).thenReturn(Optional.of(samplePcCase));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                orderService.createOrder("customer@example.com", request));

        assertTrue(ex.getMessage().contains("không đủ số lượng tồn kho"));
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("Đặt hàng tự động từ giỏ hàng khi items để trống")
    void createOrder_FromCartWhenItemsEmpty_Success() {
        CartItem cartItem1 = CartItem.builder()
                .id(UUID.randomUUID())
                .user(sampleUser)
                .product(sampleCpu)
                .quantity(1)
                .build();

        CartItem cartItem2 = CartItem.builder()
                .id(UUID.randomUUID())
                .user(sampleUser)
                .pcCase(samplePcCase)
                .quantity(1)
                .build();

        CreateOrderRequest request = CreateOrderRequest.builder()
                .shippingAddress("123 Duong Le Loi, TP.HCM")
                .items(null) // Để trống để checkout giỏ hàng
                .build();

        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(sampleUser));
        when(cartItemRepository.findByUserIdWithDetailsOrderByCreatedAtDesc(sampleUser.getId()))
                .thenReturn(List.of(cartItem1, cartItem2));
        when(productRepository.findById(sampleCpu.getId())).thenReturn(Optional.of(sampleCpu));
        when(pcCaseRepository.findById(samplePcCase.getId())).thenReturn(Optional.of(samplePcCase));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderMapper.toResponse(any(Order.class))).thenReturn(OrderResponse.builder().build());

        orderService.createOrder("customer@example.com", request);

        // Khấu trừ tồn kho
        assertEquals(8, sampleCpu.getStockQuantity());
        assertEquals(4, sampleGpu.getStockQuantity());

        // Dọn toàn bộ giỏ hàng
        verify(cartItemRepository).deleteByUserId(sampleUser.getId());
    }

    @Test
    @DisplayName("Tự động lấy địa chỉ từ profile user khi request không truyền địa chỉ")
    void createOrder_FallbackToUserAddress_WhenRequestAddressBlank() {
        CreateOrderRequest request = CreateOrderRequest.builder()
                .shippingAddress("   ") // Rỗng
                .items(List.of(
                        CreateOrderItemRequest.builder()
                                .productId(sampleCpu.getId())
                                .quantity(1)
                                .build()
                ))
                .build();

        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(sampleUser));
        when(productRepository.findById(sampleCpu.getId())).thenReturn(Optional.of(sampleCpu));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderMapper.toResponse(any(Order.class))).thenReturn(OrderResponse.builder().build());

        orderService.createOrder("customer@example.com", request);

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());
        assertEquals("123 Duong Le Loi, Quan 1, TP.HCM", orderCaptor.getValue().getShippingAddress());
    }

    @Test
    @DisplayName("Báo lỗi khi cả request và profile user đều không có địa chỉ")
    void createOrder_ThrowsBadRequest_WhenBothRequestAndUserAddressBlank() {
        sampleUser.setAddress(null);

        CreateOrderRequest request = CreateOrderRequest.builder()
                .shippingAddress("")
                .items(List.of(
                        CreateOrderItemRequest.builder()
                                .productId(sampleCpu.getId())
                                .quantity(1)
                                .build()
                ))
                .build();

        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(sampleUser));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                orderService.createOrder("customer@example.com", request));

        assertEquals("Vui lòng cung cấp địa chỉ nhận hàng.", ex.getMessage());
    }

    @Test
    @DisplayName("Báo lỗi khi giỏ hàng rỗng và request không truyền items")
    void createOrder_ThrowsBadRequest_WhenEmptyCartAndNoItems() {
        CreateOrderRequest request = CreateOrderRequest.builder()
                .shippingAddress("123 Duong Le Loi, TP.HCM")
                .items(Collections.emptyList())
                .build();

        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(sampleUser));
        when(cartItemRepository.findByUserIdWithDetailsOrderByCreatedAtDesc(sampleUser.getId()))
                .thenReturn(Collections.emptyList());

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                orderService.createOrder("customer@example.com", request));

        assertTrue(ex.getMessage().contains("Giỏ hàng của bạn đang trống"));
    }

    @Test
    @DisplayName("Báo lỗi khi một mục đơn hàng chứa cả productId và pcCaseId")
    void createOrder_ThrowsBadRequest_WhenItemHasBothProductAndPcCase() {
        CreateOrderRequest request = CreateOrderRequest.builder()
                .shippingAddress("123 Duong Le Loi, TP.HCM")
                .items(List.of(
                        CreateOrderItemRequest.builder()
                                .productId(sampleCpu.getId())
                                .pcCaseId(samplePcCase.getId())
                                .quantity(1)
                                .build()
                ))
                .build();

        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(sampleUser));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                orderService.createOrder("customer@example.com", request));

        assertTrue(ex.getMessage().contains("không được chứa cả hai"));
    }

    @Test
    @DisplayName("Báo lỗi khi một mục đơn hàng không có cả productId lẫn pcCaseId")
    void createOrder_ThrowsBadRequest_WhenItemHasNeitherProductNorPcCase() {
        CreateOrderRequest request = CreateOrderRequest.builder()
                .shippingAddress("123 Duong Le Loi, TP.HCM")
                .items(List.of(
                        CreateOrderItemRequest.builder()
                                .quantity(1)
                                .build()
                ))
                .build();

        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(sampleUser));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                orderService.createOrder("customer@example.com", request));

        assertTrue(ex.getMessage().contains("phải chứa mã linh kiện hoặc mã cấu hình PC"));
    }

    @Test
    @DisplayName("Báo lỗi khi linh kiện lẻ đã ngừng kinh doanh")
    void createOrder_ThrowsBadRequest_WhenProductInactive() {
        sampleCpu.setActive(false);

        CreateOrderRequest request = CreateOrderRequest.builder()
                .shippingAddress("123 Duong Le Loi, TP.HCM")
                .items(List.of(
                        CreateOrderItemRequest.builder()
                                .productId(sampleCpu.getId())
                                .quantity(1)
                                .build()
                ))
                .build();

        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(sampleUser));
        when(productRepository.findById(sampleCpu.getId())).thenReturn(Optional.of(sampleCpu));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                orderService.createOrder("customer@example.com", request));

        assertTrue(ex.getMessage().contains("hiện đang ngừng kinh doanh"));
    }

    @Test
    @DisplayName("Báo lỗi khi cấu hình PC đã ngừng kinh doanh")
    void createOrder_ThrowsBadRequest_WhenPcCaseInactive() {
        samplePcCase.setActive(false);

        CreateOrderRequest request = CreateOrderRequest.builder()
                .shippingAddress("123 Duong Le Loi, TP.HCM")
                .items(List.of(
                        CreateOrderItemRequest.builder()
                                .pcCaseId(samplePcCase.getId())
                                .quantity(1)
                                .build()
                ))
                .build();

        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(sampleUser));
        when(pcCaseRepository.findById(samplePcCase.getId())).thenReturn(Optional.of(samplePcCase));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                orderService.createOrder("customer@example.com", request));

        assertTrue(ex.getMessage().contains("hiện đang ngừng kinh doanh"));
    }

    @Test
    @DisplayName("Báo lỗi khi linh kiện con trong cấu hình PC đã ngừng kinh doanh")
    void createOrder_ThrowsBadRequest_WhenPcCaseComponentInactive() {
        sampleGpu.setActive(false);

        CreateOrderRequest request = CreateOrderRequest.builder()
                .shippingAddress("123 Duong Le Loi, TP.HCM")
                .items(List.of(
                        CreateOrderItemRequest.builder()
                                .pcCaseId(samplePcCase.getId())
                                .quantity(1)
                                .build()
                ))
                .build();

        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(sampleUser));
        when(pcCaseRepository.findById(samplePcCase.getId())).thenReturn(Optional.of(samplePcCase));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                orderService.createOrder("customer@example.com", request));

        assertTrue(ex.getMessage().contains("hiện đang ngừng kinh doanh"));
    }

    @Test
    @DisplayName("Báo lỗi khi cấu hình PC không có linh kiện nào")
    void createOrder_ThrowsBadRequest_WhenPcCaseHasNoComponents() {
        samplePcCase.setItems(Collections.emptyList());

        CreateOrderRequest request = CreateOrderRequest.builder()
                .shippingAddress("123 Duong Le Loi, TP.HCM")
                .items(List.of(
                        CreateOrderItemRequest.builder()
                                .pcCaseId(samplePcCase.getId())
                                .quantity(1)
                                .build()
                ))
                .build();

        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(sampleUser));
        when(pcCaseRepository.findById(samplePcCase.getId())).thenReturn(Optional.of(samplePcCase));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                orderService.createOrder("customer@example.com", request));

        assertTrue(ex.getMessage().contains("chưa có danh sách linh kiện hợp lệ"));
    }

    @Test
    @DisplayName("Báo lỗi không tìm thấy người dùng")
    void createOrder_ThrowsResourceNotFound_WhenUserNotFound() {
        CreateOrderRequest request = CreateOrderRequest.builder()
                .shippingAddress("123 Duong Le Loi, TP.HCM")
                .build();

        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                orderService.createOrder("unknown@example.com", request));
    }

    @Test
    @DisplayName("Đặt hàng theo userId thành công")
    void createOrder_ByUserId_Success() {
        CreateOrderRequest request = CreateOrderRequest.builder()
                .shippingAddress("123 Duong Le Loi, TP.HCM")
                .items(List.of(
                        CreateOrderItemRequest.builder()
                                .productId(sampleCpu.getId())
                                .quantity(1)
                                .build()
                ))
                .build();

        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
        when(productRepository.findById(sampleCpu.getId())).thenReturn(Optional.of(sampleCpu));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderMapper.toResponse(any(Order.class))).thenReturn(OrderResponse.builder().build());

        OrderResponse response = orderService.createOrder(sampleUser.getId(), request);
        assertNotNull(response);
    }

    @Test
    @DisplayName("Hoàn kho thành công khi hủy đơn hàng (restoreOrderStock)")
    void restoreOrderStock_SuccessfullyRestoresStockForProductsAndPcCases() {
        Order order = Order.builder()
                .id(UUID.randomUUID())
                .user(sampleUser)
                .items(new ArrayList<>())
                .build();

        // 1 linh kiện lẻ số lượng 2
        sampleCpu.setStockQuantity(8);
        OrderItem item1 = OrderItem.builder()
                .id(UUID.randomUUID())
                .order(order)
                .product(sampleCpu)
                .quantity(2)
                .build();

        // 1 PC Case số lượng 1 (chứa CPU và GPU)
        sampleGpu.setStockQuantity(4);
        OrderItem item2 = OrderItem.builder()
                .id(UUID.randomUUID())
                .order(order)
                .pcCase(samplePcCase)
                .quantity(1)
                .build();

        order.getItems().add(item1);
        order.getItems().add(item2);

        orderService.restoreOrderStock(order);

        // CPU ban đầu 8 + 2 (lẻ) + 1 (trong PC case) = 11
        assertEquals(11, sampleCpu.getStockQuantity());
        // GPU ban đầu 4 + 1 (trong PC case) = 5
        assertEquals(5, sampleGpu.getStockQuantity());

        verify(productRepository, times(2)).save(sampleCpu);
        verify(productRepository, times(1)).save(sampleGpu);
    }

    @Test
    @DisplayName("Hủy đơn hàng thành công qua email: trạng thái chuyển CANCELLED và hoàn kho")
    void cancelOrder_ByEmail_SuccessfullyCancelsOrder_AndRestoresStock() {
        Order order = Order.builder()
                .id(UUID.randomUUID())
                .user(sampleUser)
                .status(OrderStatus.PENDING)
                .items(new ArrayList<>())
                .build();

        sampleCpu.setStockQuantity(8);
        OrderItem item1 = OrderItem.builder()
                .id(UUID.randomUUID())
                .order(order)
                .product(sampleCpu)
                .quantity(2)
                .build();

        sampleGpu.setStockQuantity(4);
        OrderItem item2 = OrderItem.builder()
                .id(UUID.randomUUID())
                .order(order)
                .pcCase(samplePcCase)
                .quantity(1)
                .build();

        order.getItems().add(item1);
        order.getItems().add(item2);

        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(sampleUser));
        when(orderRepository.findByIdWithDetails(order.getId())).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderMapper.toResponse(any(Order.class))).thenAnswer(invocation -> {
            Order o = invocation.getArgument(0);
            return OrderResponse.builder()
                    .id(o.getId())
                    .status(o.getStatus())
                    .build();
        });

        OrderResponse response = orderService.cancelOrder("customer@example.com", order.getId());

        assertNotNull(response);
        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        assertEquals(OrderStatus.CANCELLED, response.getStatus());

        assertEquals(11, sampleCpu.getStockQuantity());
        assertEquals(5, sampleGpu.getStockQuantity());

        verify(orderRepository).save(order);
    }

    @Test
    @DisplayName("Hủy đơn hàng thành công qua userId")
    void cancelOrder_ByUserId_SuccessfullyCancelsOrder() {
        Order order = Order.builder()
                .id(UUID.randomUUID())
                .user(sampleUser)
                .status(OrderStatus.PENDING)
                .items(new ArrayList<>())
                .build();

        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
        when(orderRepository.findByIdWithDetails(order.getId())).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderMapper.toResponse(any(Order.class))).thenReturn(OrderResponse.builder().id(order.getId()).status(OrderStatus.CANCELLED).build());

        OrderResponse response = orderService.cancelOrder(sampleUser.getId(), order.getId());

        assertNotNull(response);
        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        verify(orderRepository).save(order);
    }

    @Test
    @DisplayName("Hủy đơn hàng thất bại khi không tìm thấy người dùng theo email")
    void cancelOrder_WhenUserNotFoundByEmail_ThrowsResourceNotFoundException() {
        UUID orderId = UUID.randomUUID();
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                orderService.cancelOrder("unknown@example.com", orderId));
    }

    @Test
    @DisplayName("Hủy đơn hàng thất bại khi không tìm thấy người dùng theo userId")
    void cancelOrder_WhenUserNotFoundById_ThrowsResourceNotFoundException() {
        UUID userId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                orderService.cancelOrder(userId, orderId));
    }

    @Test
    @DisplayName("Hủy đơn hàng thất bại khi mã đơn hàng là null")
    void cancelOrder_WhenOrderIdIsNull_ThrowsBadRequestException() {
        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(sampleUser));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                orderService.cancelOrder("customer@example.com", (UUID) null));
        assertEquals("Mã đơn hàng không được để trống.", ex.getMessage());
    }

    @Test
    @DisplayName("Hủy đơn hàng thất bại khi không tìm thấy đơn hàng")
    void cancelOrder_WhenOrderNotFound_ThrowsResourceNotFoundException() {
        UUID orderId = UUID.randomUUID();
        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(sampleUser));
        when(orderRepository.findByIdWithDetails(orderId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                orderService.cancelOrder("customer@example.com", orderId));
    }

    @Test
    @DisplayName("Hủy đơn hàng thất bại khi người dùng không phải chủ sở hữu đơn hàng (403 Forbidden)")
    void cancelOrder_WhenNotOwnerAndNotAdmin_ThrowsAppExceptionForbidden() {
        User otherUser = User.builder()
                .id(UUID.randomUUID())
                .email("other@example.com")
                .role(Role.USER)
                .build();

        Order order = Order.builder()
                .id(UUID.randomUUID())
                .user(otherUser)
                .status(OrderStatus.PENDING)
                .build();

        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(sampleUser));
        when(orderRepository.findByIdWithDetails(order.getId())).thenReturn(Optional.of(order));

        AppException ex = assertThrows(AppException.class, () ->
                orderService.cancelOrder("customer@example.com", order.getId()));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertEquals("Bạn không có quyền hủy đơn hàng này.", ex.getMessage());
    }

    @Test
    @DisplayName("Admin có quyền hủy đơn hàng của người dùng khác")
    void cancelOrder_AsAdmin_SuccessfullyCancelsOtherUsersOrder() {
        User adminUser = User.builder()
                .id(UUID.randomUUID())
                .email("admin@example.com")
                .role(Role.ADMIN)
                .build();

        Order order = Order.builder()
                .id(UUID.randomUUID())
                .user(sampleUser)
                .status(OrderStatus.PENDING)
                .items(new ArrayList<>())
                .build();

        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(adminUser));
        when(orderRepository.findByIdWithDetails(order.getId())).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderMapper.toResponse(any(Order.class))).thenReturn(OrderResponse.builder().id(order.getId()).status(OrderStatus.CANCELLED).build());

        OrderResponse response = orderService.cancelOrder("admin@example.com", order.getId());

        assertNotNull(response);
        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        verify(orderRepository).save(order);
    }

    @Test
    @DisplayName("Hủy đơn hàng thất bại khi đơn hàng đã bị hủy trước đó")
    void cancelOrder_WhenStatusIsCancelled_ThrowsBadRequestException() {
        Order order = Order.builder()
                .id(UUID.randomUUID())
                .user(sampleUser)
                .status(OrderStatus.CANCELLED)
                .build();

        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(sampleUser));
        when(orderRepository.findByIdWithDetails(order.getId())).thenReturn(Optional.of(order));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                orderService.cancelOrder("customer@example.com", order.getId()));
        assertEquals("Đơn hàng đã được hủy trước đó.", ex.getMessage());
    }

    @Test
    @DisplayName("Hủy đơn hàng thất bại khi đơn hàng đang vận chuyển (SHIPPING)")
    void cancelOrder_WhenStatusIsShipping_ThrowsBadRequestException() {
        Order order = Order.builder()
                .id(UUID.randomUUID())
                .user(sampleUser)
                .status(OrderStatus.SHIPPING)
                .build();

        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(sampleUser));
        when(orderRepository.findByIdWithDetails(order.getId())).thenReturn(Optional.of(order));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                orderService.cancelOrder("customer@example.com", order.getId()));
        assertEquals("Không thể hủy đơn hàng đang trong quá trình vận chuyển.", ex.getMessage());
    }

    @Test
    @DisplayName("Hủy đơn hàng thất bại khi đơn hàng đã giao thành công (DELIVERED)")
    void cancelOrder_WhenStatusIsDelivered_ThrowsBadRequestException() {
        Order order = Order.builder()
                .id(UUID.randomUUID())
                .user(sampleUser)
                .status(OrderStatus.DELIVERED)
                .build();

        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(sampleUser));
        when(orderRepository.findByIdWithDetails(order.getId())).thenReturn(Optional.of(order));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                orderService.cancelOrder("customer@example.com", order.getId()));
        assertEquals("Không thể hủy đơn hàng đã giao thành công.", ex.getMessage());
    }

    @Test
    @DisplayName("Hủy đơn hàng thất bại khi đơn hàng đã được xác nhận (CONFIRMED)")
    void cancelOrder_WhenStatusIsConfirmed_ThrowsBadRequestException() {
        Order order = Order.builder()
                .id(UUID.randomUUID())
                .user(sampleUser)
                .status(OrderStatus.CONFIRMED)
                .build();

        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(sampleUser));
        when(orderRepository.findByIdWithDetails(order.getId())).thenReturn(Optional.of(order));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                orderService.cancelOrder("customer@example.com", order.getId()));
        assertEquals("Không thể hủy đơn hàng khi đã được xác nhận.", ex.getMessage());
    }

    // ==========================================
    // Tests for updateOrderStatus
    // ==========================================

    @Test
    @DisplayName("Cập nhật trạng thái thành công: PENDING sang CONFIRMED")
    void updateOrderStatus_Success_WhenPendingToConfirmed() {
        Order order = Order.builder()
                .id(UUID.randomUUID())
                .status(OrderStatus.PENDING)
                .build();

        UpdateOrderStatusRequest request = UpdateOrderStatusRequest.builder()
                .status(OrderStatus.CONFIRMED)
                .build();

        OrderResponse expectedResponse = OrderResponse.builder()
                .id(order.getId())
                .status(OrderStatus.CONFIRMED)
                .build();

        when(orderRepository.findByIdWithDetails(order.getId())).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderMapper.toResponse(any(Order.class))).thenReturn(expectedResponse);

        OrderResponse response = orderService.updateOrderStatus(order.getId(), request);

        assertNotNull(response);
        assertEquals(OrderStatus.CONFIRMED, response.getStatus());
        assertEquals(OrderStatus.CONFIRMED, order.getStatus());
        verify(orderRepository).save(order);
        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("Cập nhật trạng thái thành công: CONFIRMED sang SHIPPING")
    void updateOrderStatus_Success_WhenConfirmedToShipping() {
        Order order = Order.builder()
                .id(UUID.randomUUID())
                .status(OrderStatus.CONFIRMED)
                .build();

        UpdateOrderStatusRequest request = UpdateOrderStatusRequest.builder()
                .status(OrderStatus.SHIPPING)
                .build();

        OrderResponse expectedResponse = OrderResponse.builder()
                .id(order.getId())
                .status(OrderStatus.SHIPPING)
                .build();

        when(orderRepository.findByIdWithDetails(order.getId())).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderMapper.toResponse(any(Order.class))).thenReturn(expectedResponse);

        OrderResponse response = orderService.updateOrderStatus(order.getId(), request);

        assertNotNull(response);
        assertEquals(OrderStatus.SHIPPING, response.getStatus());
        assertEquals(OrderStatus.SHIPPING, order.getStatus());
        verify(orderRepository).save(order);
        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("Cập nhật trạng thái thành công: SHIPPING sang DELIVERED")
    void updateOrderStatus_Success_WhenShippingToDelivered() {
        Order order = Order.builder()
                .id(UUID.randomUUID())
                .status(OrderStatus.SHIPPING)
                .build();

        UpdateOrderStatusRequest request = UpdateOrderStatusRequest.builder()
                .status(OrderStatus.DELIVERED)
                .build();

        OrderResponse expectedResponse = OrderResponse.builder()
                .id(order.getId())
                .status(OrderStatus.DELIVERED)
                .build();

        when(orderRepository.findByIdWithDetails(order.getId())).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderMapper.toResponse(any(Order.class))).thenReturn(expectedResponse);

        OrderResponse response = orderService.updateOrderStatus(order.getId(), request);

        assertNotNull(response);
        assertEquals(OrderStatus.DELIVERED, response.getStatus());
        assertEquals(OrderStatus.DELIVERED, order.getStatus());
        verify(orderRepository).save(order);
        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("Cập nhật trạng thái thành công: PENDING sang CANCELLED và tự động hoàn trả tồn kho")
    void updateOrderStatus_Success_WhenPendingToCancelled_RestoresStock() {
        Order order = Order.builder()
                .id(UUID.randomUUID())
                .status(OrderStatus.PENDING)
                .build();

        OrderItem item = OrderItem.builder()
                .order(order)
                .product(sampleCpu)
                .quantity(2)
                .build();
        order.addItem(item);

        int initialStock = sampleCpu.getStockQuantity();

        UpdateOrderStatusRequest request = UpdateOrderStatusRequest.builder()
                .status(OrderStatus.CANCELLED)
                .build();

        when(orderRepository.findByIdWithDetails(order.getId())).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderMapper.toResponse(any(Order.class))).thenReturn(OrderResponse.builder().id(order.getId()).status(OrderStatus.CANCELLED).build());

        OrderResponse response = orderService.updateOrderStatus(order.getId(), request);

        assertNotNull(response);
        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        assertEquals(initialStock + 2, sampleCpu.getStockQuantity());
        verify(productRepository).save(sampleCpu);
        verify(orderRepository).save(order);
    }

    @Test
    @DisplayName("Cập nhật trạng thái thành công: CONFIRMED sang CANCELLED và hoàn tồn kho")
    void updateOrderStatus_Success_WhenConfirmedToCancelled_RestoresStock() {
        Order order = Order.builder()
                .id(UUID.randomUUID())
                .status(OrderStatus.CONFIRMED)
                .build();

        OrderItem item = OrderItem.builder()
                .order(order)
                .product(sampleCpu)
                .quantity(1)
                .build();
        order.addItem(item);

        int initialStock = sampleCpu.getStockQuantity();

        UpdateOrderStatusRequest request = UpdateOrderStatusRequest.builder()
                .status(OrderStatus.CANCELLED)
                .build();

        when(orderRepository.findByIdWithDetails(order.getId())).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderMapper.toResponse(any(Order.class))).thenReturn(OrderResponse.builder().id(order.getId()).status(OrderStatus.CANCELLED).build());

        OrderResponse response = orderService.updateOrderStatus(order.getId(), request);

        assertNotNull(response);
        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        assertEquals(initialStock + 1, sampleCpu.getStockQuantity());
        verify(productRepository).save(sampleCpu);
    }

    @Test
    @DisplayName("Cập nhật trạng thái thành công: SHIPPING sang CANCELLED và hoàn tồn kho")
    void updateOrderStatus_Success_WhenShippingToCancelled_RestoresStock() {
        Order order = Order.builder()
                .id(UUID.randomUUID())
                .status(OrderStatus.SHIPPING)
                .build();

        OrderItem item = OrderItem.builder()
                .order(order)
                .product(sampleCpu)
                .quantity(1)
                .build();
        order.addItem(item);

        int initialStock = sampleCpu.getStockQuantity();

        UpdateOrderStatusRequest request = UpdateOrderStatusRequest.builder()
                .status(OrderStatus.CANCELLED)
                .build();

        when(orderRepository.findByIdWithDetails(order.getId())).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderMapper.toResponse(any(Order.class))).thenReturn(OrderResponse.builder().id(order.getId()).status(OrderStatus.CANCELLED).build());

        OrderResponse response = orderService.updateOrderStatus(order.getId(), request);

        assertNotNull(response);
        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        assertEquals(initialStock + 1, sampleCpu.getStockQuantity());
        verify(productRepository).save(sampleCpu);
    }

    @Test
    @DisplayName("Cập nhật trạng thái thất bại: Trạng thái mới trùng với trạng thái hiện tại (400 Bad Request)")
    void updateOrderStatus_ThrowsBadRequest_WhenCurrentStatusEqualsNewStatus() {
        Order order = Order.builder()
                .id(UUID.randomUUID())
                .status(OrderStatus.CONFIRMED)
                .build();

        UpdateOrderStatusRequest request = UpdateOrderStatusRequest.builder()
                .status(OrderStatus.CONFIRMED)
                .build();

        when(orderRepository.findByIdWithDetails(order.getId())).thenReturn(Optional.of(order));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                orderService.updateOrderStatus(order.getId(), request));
        assertEquals("Đơn hàng hiện tại đã ở trạng thái CONFIRMED.", ex.getMessage());
    }

    @Test
    @DisplayName("Cập nhật trạng thái thất bại: Đơn hàng đã bị hủy (CANCELLED) không thể thay đổi")
    void updateOrderStatus_ThrowsBadRequest_WhenCurrentStatusIsCancelled() {
        Order order = Order.builder()
                .id(UUID.randomUUID())
                .status(OrderStatus.CANCELLED)
                .build();

        UpdateOrderStatusRequest request = UpdateOrderStatusRequest.builder()
                .status(OrderStatus.CONFIRMED)
                .build();

        when(orderRepository.findByIdWithDetails(order.getId())).thenReturn(Optional.of(order));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                orderService.updateOrderStatus(order.getId(), request));
        assertEquals("Không thể thay đổi trạng thái của đơn hàng đã bị hủy.", ex.getMessage());
    }

    @Test
    @DisplayName("Cập nhật trạng thái thất bại: Đơn hàng đã giao thành công (DELIVERED) không thể thay đổi")
    void updateOrderStatus_ThrowsBadRequest_WhenCurrentStatusIsDelivered() {
        Order order = Order.builder()
                .id(UUID.randomUUID())
                .status(OrderStatus.DELIVERED)
                .build();

        UpdateOrderStatusRequest request = UpdateOrderStatusRequest.builder()
                .status(OrderStatus.CANCELLED)
                .build();

        when(orderRepository.findByIdWithDetails(order.getId())).thenReturn(Optional.of(order));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                orderService.updateOrderStatus(order.getId(), request));
        assertEquals("Không thể thay đổi trạng thái của đơn hàng đã giao thành công.", ex.getMessage());
    }

    @Test
    @DisplayName("Cập nhật trạng thái thất bại: Nhảy cóc từ PENDING sang SHIPPING")
    void updateOrderStatus_ThrowsBadRequest_WhenPendingToShipping() {
        Order order = Order.builder()
                .id(UUID.randomUUID())
                .status(OrderStatus.PENDING)
                .build();

        UpdateOrderStatusRequest request = UpdateOrderStatusRequest.builder()
                .status(OrderStatus.SHIPPING)
                .build();

        when(orderRepository.findByIdWithDetails(order.getId())).thenReturn(Optional.of(order));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                orderService.updateOrderStatus(order.getId(), request));
        assertEquals("Đơn hàng đang chờ xử lý chỉ có thể chuyển sang CONFIRMED hoặc CANCELLED.", ex.getMessage());
    }

    @Test
    @DisplayName("Cập nhật trạng thái thất bại: Nhảy cóc từ PENDING sang DELIVERED")
    void updateOrderStatus_ThrowsBadRequest_WhenPendingToDelivered() {
        Order order = Order.builder()
                .id(UUID.randomUUID())
                .status(OrderStatus.PENDING)
                .build();

        UpdateOrderStatusRequest request = UpdateOrderStatusRequest.builder()
                .status(OrderStatus.DELIVERED)
                .build();

        when(orderRepository.findByIdWithDetails(order.getId())).thenReturn(Optional.of(order));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                orderService.updateOrderStatus(order.getId(), request));
        assertEquals("Đơn hàng đang chờ xử lý chỉ có thể chuyển sang CONFIRMED hoặc CANCELLED.", ex.getMessage());
    }

    @Test
    @DisplayName("Cập nhật trạng thái thất bại: Quay ngược từ CONFIRMED về PENDING")
    void updateOrderStatus_ThrowsBadRequest_WhenConfirmedToPending() {
        Order order = Order.builder()
                .id(UUID.randomUUID())
                .status(OrderStatus.CONFIRMED)
                .build();

        UpdateOrderStatusRequest request = UpdateOrderStatusRequest.builder()
                .status(OrderStatus.PENDING)
                .build();

        when(orderRepository.findByIdWithDetails(order.getId())).thenReturn(Optional.of(order));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                orderService.updateOrderStatus(order.getId(), request));
        assertEquals("Đơn hàng đã xác nhận chỉ có thể chuyển sang SHIPPING hoặc CANCELLED.", ex.getMessage());
    }

    @Test
    @DisplayName("Cập nhật trạng thái thất bại: Nhảy cóc từ CONFIRMED sang DELIVERED")
    void updateOrderStatus_ThrowsBadRequest_WhenConfirmedToDelivered() {
        Order order = Order.builder()
                .id(UUID.randomUUID())
                .status(OrderStatus.CONFIRMED)
                .build();

        UpdateOrderStatusRequest request = UpdateOrderStatusRequest.builder()
                .status(OrderStatus.DELIVERED)
                .build();

        when(orderRepository.findByIdWithDetails(order.getId())).thenReturn(Optional.of(order));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                orderService.updateOrderStatus(order.getId(), request));
        assertEquals("Đơn hàng đã xác nhận chỉ có thể chuyển sang SHIPPING hoặc CANCELLED.", ex.getMessage());
    }

    @Test
    @DisplayName("Cập nhật trạng thái thất bại: Quay ngược từ SHIPPING về CONFIRMED")
    void updateOrderStatus_ThrowsBadRequest_WhenShippingToConfirmed() {
        Order order = Order.builder()
                .id(UUID.randomUUID())
                .status(OrderStatus.SHIPPING)
                .build();

        UpdateOrderStatusRequest request = UpdateOrderStatusRequest.builder()
                .status(OrderStatus.CONFIRMED)
                .build();

        when(orderRepository.findByIdWithDetails(order.getId())).thenReturn(Optional.of(order));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                orderService.updateOrderStatus(order.getId(), request));
        assertEquals("Đơn hàng đang giao chỉ có thể chuyển sang DELIVERED hoặc CANCELLED.", ex.getMessage());
    }

    @Test
    @DisplayName("Cập nhật trạng thái thất bại: Không tìm thấy đơn hàng (404 Not Found)")
    void updateOrderStatus_ThrowsNotFound_WhenOrderDoesNotExist() {
        UUID orderId = UUID.randomUUID();
        UpdateOrderStatusRequest request = UpdateOrderStatusRequest.builder()
                .status(OrderStatus.CONFIRMED)
                .build();

        when(orderRepository.findByIdWithDetails(orderId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                orderService.updateOrderStatus(orderId, request));
    }

    @Test
    @DisplayName("Cập nhật trạng thái thất bại: orderId hoặc request/status bị null")
    void updateOrderStatus_ThrowsBadRequest_WhenOrderIdOrStatusIsNull() {
        assertThrows(BadRequestException.class, () ->
                orderService.updateOrderStatus(null, UpdateOrderStatusRequest.builder().status(OrderStatus.CONFIRMED).build()));

        assertThrows(BadRequestException.class, () ->
                orderService.updateOrderStatus(UUID.randomUUID(), null));

        assertThrows(BadRequestException.class, () ->
                orderService.updateOrderStatus(UUID.randomUUID(), UpdateOrderStatusRequest.builder().status(null).build()));
    }
}
