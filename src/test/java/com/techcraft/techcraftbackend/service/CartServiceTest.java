package com.techcraft.techcraftbackend.service;

import com.techcraft.techcraftbackend.dto.request.AddCartItemRequest;
import com.techcraft.techcraftbackend.dto.request.AddCartPcCaseRequest;
import com.techcraft.techcraftbackend.dto.request.UpdateCartItemRequest;
import com.techcraft.techcraftbackend.dto.request.UpdateCartPcCaseRequest;
import com.techcraft.techcraftbackend.dto.response.CartItemResponse;
import com.techcraft.techcraftbackend.dto.response.CartResponse;
import com.techcraft.techcraftbackend.entity.CartItem;
import com.techcraft.techcraftbackend.entity.PcCase;
import com.techcraft.techcraftbackend.entity.PcCaseItem;
import com.techcraft.techcraftbackend.entity.Product;
import com.techcraft.techcraftbackend.entity.ProductImage;
import com.techcraft.techcraftbackend.entity.User;
import com.techcraft.techcraftbackend.enums.ProductCategory;
import com.techcraft.techcraftbackend.exception.BadRequestException;
import com.techcraft.techcraftbackend.exception.ResourceNotFoundException;
import com.techcraft.techcraftbackend.mapper.CartMapper;
import com.techcraft.techcraftbackend.repository.CartItemRepository;
import com.techcraft.techcraftbackend.repository.PcCaseRepository;
import com.techcraft.techcraftbackend.repository.ProductRepository;
import com.techcraft.techcraftbackend.repository.UserRepository;
import com.techcraft.techcraftbackend.service.impl.CartServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private PcCaseRepository pcCaseRepository;

    @Mock
    private CartMapper cartMapper;

    @InjectMocks
    private CartServiceImpl cartService;

    private User mockUser;
    private Product mockProduct;
    private PcCase mockPcCase;
    private UUID userId;
    private UUID productId;
    private UUID pcCaseId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        productId = UUID.randomUUID();
        pcCaseId = UUID.randomUUID();

        mockUser = User.builder()
                .id(userId)
                .email("user@example.com")
                .fullName("Nguyễn Văn A")
                .build();

        mockProduct = Product.builder()
                .id(productId)
                .name("CPU Intel Core i5-13400")
                .price(BigDecimal.valueOf(5500000))
                .stockQuantity(10)
                .category(ProductCategory.CPU)
                .isActive(true)
                .images(new ArrayList<>(List.of(
                        ProductImage.builder()
                                .imageUrl("https://example.com/cpu.jpg")
                                .isPrimary(true)
                                .build()
                )))
                .build();

        Product mockGpu = Product.builder()
                .id(UUID.randomUUID())
                .name("GPU RTX 4060")
                .price(BigDecimal.valueOf(8000000))
                .stockQuantity(4)
                .category(ProductCategory.GPU)
                .isActive(true)
                .build();

        mockPcCase = PcCase.builder()
                .id(pcCaseId)
                .name("Gaming Entry 2026")
                .useCase("gaming")
                .totalPrice(BigDecimal.valueOf(13500000))
                .isActive(true)
                .items(new ArrayList<>())
                .build();

        PcCaseItem itemCpu = PcCaseItem.builder()
                .id(UUID.randomUUID())
                .pcCase(mockPcCase)
                .product(mockProduct) // stock = 10
                .category(ProductCategory.CPU)
                .build();

        PcCaseItem itemGpu = PcCaseItem.builder()
                .id(UUID.randomUUID())
                .pcCase(mockPcCase)
                .product(mockGpu) // stock = 4 -> min stock = 4
                .category(ProductCategory.GPU)
                .build();

        mockPcCase.getItems().add(itemCpu);
        mockPcCase.getItems().add(itemGpu);
    }

    // ==================== Tests cho linh kiện lẻ (addCartItem) ====================

    @Test
    @DisplayName("Thêm linh kiện mới vào giỏ hàng thành công")
    void addCartItem_Success_WhenNewItem() {
        AddCartItemRequest request = AddCartItemRequest.builder()
                .productId(productId)
                .quantity(2)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        when(productRepository.findById(productId)).thenReturn(Optional.of(mockProduct));
        when(cartItemRepository.findByUserIdAndProductId(userId, productId)).thenReturn(Optional.empty());

        CartItem savedCartItem = CartItem.builder()
                .id(UUID.randomUUID())
                .user(mockUser)
                .product(mockProduct)
                .quantity(2)
                .build();

        when(cartItemRepository.save(any(CartItem.class))).thenReturn(savedCartItem);

        CartItemResponse expectedResponse = CartItemResponse.builder()
                .id(savedCartItem.getId())
                .productId(productId)
                .productName(mockProduct.getName())
                .quantity(2)
                .subtotal(BigDecimal.valueOf(11000000))
                .build();

        when(cartMapper.toResponse(savedCartItem)).thenReturn(expectedResponse);

        CartItemResponse actualResponse = cartService.addCartItem(userId, request);

        assertNotNull(actualResponse);
        assertEquals(productId, actualResponse.getProductId());
        assertEquals(2, actualResponse.getQuantity());

        ArgumentCaptor<CartItem> captor = ArgumentCaptor.forClass(CartItem.class);
        verify(cartItemRepository).save(captor.capture());
        assertEquals(2, captor.getValue().getQuantity());
        assertEquals(mockProduct, captor.getValue().getProduct());
        assertEquals(mockUser, captor.getValue().getUser());
    }

    @Test
    @DisplayName("Thêm linh kiện đã tồn tại trong giỏ hàng -> tăng số lượng thành công")
    void addCartItem_Success_WhenExistingItemIncreasesQuantity() {
        AddCartItemRequest request = AddCartItemRequest.builder()
                .productId(productId)
                .quantity(3)
                .build();

        CartItem existingCartItem = CartItem.builder()
                .id(UUID.randomUUID())
                .user(mockUser)
                .product(mockProduct)
                .quantity(2)
                .build();

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(mockUser));
        when(productRepository.findById(productId)).thenReturn(Optional.of(mockProduct));
        when(cartItemRepository.findByUserIdAndProductId(userId, productId)).thenReturn(Optional.of(existingCartItem));
        when(cartItemRepository.save(existingCartItem)).thenReturn(existingCartItem);

        CartItemResponse expectedResponse = CartItemResponse.builder()
                .id(existingCartItem.getId())
                .productId(productId)
                .quantity(5)
                .build();

        when(cartMapper.toResponse(existingCartItem)).thenReturn(expectedResponse);

        CartItemResponse actualResponse = cartService.addCartItem("user@example.com", request);

        assertNotNull(actualResponse);
        assertEquals(5, actualResponse.getQuantity());
        assertEquals(5, existingCartItem.getQuantity());
        verify(cartItemRepository).save(existingCartItem);
    }

    @Test
    @DisplayName("Thêm số lượng null -> mặc định là 1")
    void addCartItem_DefaultsQuantityToOne_WhenNull() {
        AddCartItemRequest request = AddCartItemRequest.builder()
                .productId(productId)
                .quantity(null)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        when(productRepository.findById(productId)).thenReturn(Optional.of(mockProduct));
        when(cartItemRepository.findByUserIdAndProductId(userId, productId)).thenReturn(Optional.empty());

        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        cartService.addCartItem(userId, request);

        ArgumentCaptor<CartItem> captor = ArgumentCaptor.forClass(CartItem.class);
        verify(cartItemRepository).save(captor.capture());
        assertEquals(1, captor.getValue().getQuantity());
    }

    @Test
    @DisplayName("Ném ResourceNotFoundException khi không tìm thấy user")
    void addCartItem_ThrowsResourceNotFoundException_WhenUserNotFound() {
        AddCartItemRequest request = AddCartItemRequest.builder()
                .productId(productId)
                .quantity(1)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> cartService.addCartItem(userId, request));
        verify(productRepository, never()).findById(any());
        verify(cartItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("Ném ResourceNotFoundException khi không tìm thấy linh kiện")
    void addCartItem_ThrowsResourceNotFoundException_WhenProductNotFound() {
        AddCartItemRequest request = AddCartItemRequest.builder()
                .productId(productId)
                .quantity(1)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> cartService.addCartItem(userId, request));
        verify(cartItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("Ném BadRequestException khi linh kiện ngừng kinh doanh (isActive = false)")
    void addCartItem_ThrowsBadRequestException_WhenProductInactive() {
        mockProduct.setActive(false);

        AddCartItemRequest request = AddCartItemRequest.builder()
                .productId(productId)
                .quantity(1)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        when(productRepository.findById(productId)).thenReturn(Optional.of(mockProduct));

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> cartService.addCartItem(userId, request));
        assertTrue(exception.getMessage().contains("ngừng kinh doanh"));
        verify(cartItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("Ném BadRequestException khi linh kiện đã hết hàng (stock = 0)")
    void addCartItem_ThrowsBadRequestException_WhenProductOutOfStock() {
        mockProduct.setStockQuantity(0);

        AddCartItemRequest request = AddCartItemRequest.builder()
                .productId(productId)
                .quantity(1)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        when(productRepository.findById(productId)).thenReturn(Optional.of(mockProduct));

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> cartService.addCartItem(userId, request));
        assertTrue(exception.getMessage().contains("hết hàng"));
        verify(cartItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("Ném BadRequestException khi số lượng yêu cầu vượt quá tồn kho (item mới)")
    void addCartItem_ThrowsBadRequestException_WhenNewItemExceedsStock() {
        mockProduct.setStockQuantity(5);

        AddCartItemRequest request = AddCartItemRequest.builder()
                .productId(productId)
                .quantity(6)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        when(productRepository.findById(productId)).thenReturn(Optional.of(mockProduct));
        when(cartItemRepository.findByUserIdAndProductId(userId, productId)).thenReturn(Optional.empty());

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> cartService.addCartItem(userId, request));
        assertTrue(exception.getMessage().contains("vượt quá số lượng tồn kho"));
        verify(cartItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("Ném BadRequestException khi số lượng sau khi cộng dồn vượt quá tồn kho (item đã có)")
    void addCartItem_ThrowsBadRequestException_WhenExistingItemExceedsStock() {
        mockProduct.setStockQuantity(5);

        CartItem existingCartItem = CartItem.builder()
                .id(UUID.randomUUID())
                .user(mockUser)
                .product(mockProduct)
                .quantity(4)
                .build();

        AddCartItemRequest request = AddCartItemRequest.builder()
                .productId(productId)
                .quantity(2)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        when(productRepository.findById(productId)).thenReturn(Optional.of(mockProduct));
        when(cartItemRepository.findByUserIdAndProductId(userId, productId)).thenReturn(Optional.of(existingCartItem));

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> cartService.addCartItem(userId, request));
        assertTrue(exception.getMessage().contains("vượt quá số lượng tồn kho"));
        verify(cartItemRepository, never()).save(any());
    }

    // ==================== Tests cho cập nhật số lượng linh kiện (updateCartItemQuantity) ====================

    @Test
    @DisplayName("Cập nhật số lượng linh kiện thành công khi tìm thấy theo cartItemId")
    void updateCartItemQuantity_Success_WhenFoundById() {
        UUID cartItemId = UUID.randomUUID();
        UpdateCartItemRequest request = UpdateCartItemRequest.builder()
                .quantity(3)
                .build();

        CartItem existingCartItem = CartItem.builder()
                .id(cartItemId)
                .user(mockUser)
                .product(mockProduct)
                .quantity(1)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        when(cartItemRepository.findByIdAndUserId(cartItemId, userId)).thenReturn(Optional.of(existingCartItem));
        when(productRepository.findById(productId)).thenReturn(Optional.of(mockProduct));
        when(cartItemRepository.save(existingCartItem)).thenReturn(existingCartItem);

        CartItemResponse expectedResponse = CartItemResponse.builder()
                .id(cartItemId)
                .productId(productId)
                .productName(mockProduct.getName())
                .quantity(3)
                .subtotal(BigDecimal.valueOf(16500000))
                .build();

        when(cartMapper.toResponse(existingCartItem)).thenReturn(expectedResponse);

        CartItemResponse actualResponse = cartService.updateCartItemQuantity(userId, cartItemId, request);

        assertNotNull(actualResponse);
        assertEquals(3, actualResponse.getQuantity());
        assertEquals(3, existingCartItem.getQuantity());
        verify(cartItemRepository).save(existingCartItem);
    }

    @Test
    @DisplayName("Cập nhật số lượng linh kiện thành công khi tìm fallback theo productId")
    void updateCartItemQuantity_Success_WhenFoundByProductIdFallback() {
        UpdateCartItemRequest request = UpdateCartItemRequest.builder()
                .quantity(4)
                .build();

        CartItem existingCartItem = CartItem.builder()
                .id(UUID.randomUUID())
                .user(mockUser)
                .product(mockProduct)
                .quantity(2)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        when(cartItemRepository.findByIdAndUserId(productId, userId)).thenReturn(Optional.empty());
        when(cartItemRepository.findByUserIdAndProductId(userId, productId)).thenReturn(Optional.of(existingCartItem));
        when(productRepository.findById(productId)).thenReturn(Optional.of(mockProduct));
        when(cartItemRepository.save(existingCartItem)).thenReturn(existingCartItem);

        CartItemResponse expectedResponse = CartItemResponse.builder()
                .id(existingCartItem.getId())
                .productId(productId)
                .productName(mockProduct.getName())
                .quantity(4)
                .build();

        when(cartMapper.toResponse(existingCartItem)).thenReturn(expectedResponse);

        CartItemResponse actualResponse = cartService.updateCartItemQuantity(userId, productId, request);

        assertNotNull(actualResponse);
        assertEquals(4, actualResponse.getQuantity());
        assertEquals(4, existingCartItem.getQuantity());
        verify(cartItemRepository).save(existingCartItem);
    }

    @Test
    @DisplayName("Cập nhật số lượng linh kiện theo email thành công")
    void updateCartItemQuantity_Success_ByEmail() {
        UUID cartItemId = UUID.randomUUID();
        UpdateCartItemRequest request = UpdateCartItemRequest.builder()
                .quantity(2)
                .build();

        CartItem existingCartItem = CartItem.builder()
                .id(cartItemId)
                .user(mockUser)
                .product(mockProduct)
                .quantity(1)
                .build();

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(mockUser));
        when(cartItemRepository.findByIdAndUserId(cartItemId, userId)).thenReturn(Optional.of(existingCartItem));
        when(productRepository.findById(productId)).thenReturn(Optional.of(mockProduct));
        when(cartItemRepository.save(existingCartItem)).thenReturn(existingCartItem);

        CartItemResponse expectedResponse = CartItemResponse.builder()
                .id(cartItemId)
                .quantity(2)
                .build();

        when(cartMapper.toResponse(existingCartItem)).thenReturn(expectedResponse);

        CartItemResponse actualResponse = cartService.updateCartItemQuantity("user@example.com", cartItemId, request);

        assertNotNull(actualResponse);
        assertEquals(2, actualResponse.getQuantity());
        verify(cartItemRepository).save(existingCartItem);
    }

    @Test
    @DisplayName("Ném ResourceNotFoundException khi không tìm thấy user khi cập nhật")
    void updateCartItemQuantity_ThrowsResourceNotFoundException_WhenUserNotFound() {
        UUID cartItemId = UUID.randomUUID();
        UpdateCartItemRequest request = UpdateCartItemRequest.builder()
                .quantity(2)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                cartService.updateCartItemQuantity(userId, cartItemId, request));
        verify(cartItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("Ném ResourceNotFoundException khi không tìm thấy linh kiện trong giỏ hàng")
    void updateCartItemQuantity_ThrowsResourceNotFoundException_WhenCartItemNotFound() {
        UUID cartItemId = UUID.randomUUID();
        UpdateCartItemRequest request = UpdateCartItemRequest.builder()
                .quantity(2)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        when(cartItemRepository.findByIdAndUserId(cartItemId, userId)).thenReturn(Optional.empty());
        when(cartItemRepository.findByUserIdAndProductId(userId, cartItemId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                cartService.updateCartItemQuantity(userId, cartItemId, request));
        verify(cartItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("Ném BadRequestException khi mục trong giỏ là PC Case chứ không phải linh kiện lẻ")
    void updateCartItemQuantity_ThrowsBadRequestException_WhenCartItemIsPcCase() {
        UUID cartItemId = UUID.randomUUID();
        UpdateCartItemRequest request = UpdateCartItemRequest.builder()
                .quantity(2)
                .build();

        CartItem pcCaseCartItem = CartItem.builder()
                .id(cartItemId)
                .user(mockUser)
                .pcCase(mockPcCase)
                .product(null)
                .quantity(1)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        when(cartItemRepository.findByIdAndUserId(cartItemId, userId)).thenReturn(Optional.of(pcCaseCartItem));

        BadRequestException exception = assertThrows(BadRequestException.class, () ->
                cartService.updateCartItemQuantity(userId, cartItemId, request));
        assertTrue(exception.getMessage().contains("không phải là linh kiện lẻ"));
        verify(cartItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("Ném ResourceNotFoundException khi không tìm thấy thông tin sản phẩm tương ứng")
    void updateCartItemQuantity_ThrowsResourceNotFoundException_WhenProductNotFound() {
        UUID cartItemId = UUID.randomUUID();
        UpdateCartItemRequest request = UpdateCartItemRequest.builder()
                .quantity(2)
                .build();

        CartItem existingCartItem = CartItem.builder()
                .id(cartItemId)
                .user(mockUser)
                .product(mockProduct)
                .quantity(1)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        when(cartItemRepository.findByIdAndUserId(cartItemId, userId)).thenReturn(Optional.of(existingCartItem));
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                cartService.updateCartItemQuantity(userId, cartItemId, request));
        verify(cartItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("Ném BadRequestException khi sản phẩm ngừng kinh doanh khi cập nhật")
    void updateCartItemQuantity_ThrowsBadRequestException_WhenProductInactive() {
        UUID cartItemId = UUID.randomUUID();
        mockProduct.setActive(false);

        UpdateCartItemRequest request = UpdateCartItemRequest.builder()
                .quantity(2)
                .build();

        CartItem existingCartItem = CartItem.builder()
                .id(cartItemId)
                .user(mockUser)
                .product(mockProduct)
                .quantity(1)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        when(cartItemRepository.findByIdAndUserId(cartItemId, userId)).thenReturn(Optional.of(existingCartItem));
        when(productRepository.findById(productId)).thenReturn(Optional.of(mockProduct));

        BadRequestException exception = assertThrows(BadRequestException.class, () ->
                cartService.updateCartItemQuantity(userId, cartItemId, request));
        assertTrue(exception.getMessage().contains("ngừng kinh doanh"));
        verify(cartItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("Ném BadRequestException khi sản phẩm đã hết hàng khi cập nhật")
    void updateCartItemQuantity_ThrowsBadRequestException_WhenProductOutOfStock() {
        UUID cartItemId = UUID.randomUUID();
        mockProduct.setStockQuantity(0);

        UpdateCartItemRequest request = UpdateCartItemRequest.builder()
                .quantity(2)
                .build();

        CartItem existingCartItem = CartItem.builder()
                .id(cartItemId)
                .user(mockUser)
                .product(mockProduct)
                .quantity(1)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        when(cartItemRepository.findByIdAndUserId(cartItemId, userId)).thenReturn(Optional.of(existingCartItem));
        when(productRepository.findById(productId)).thenReturn(Optional.of(mockProduct));

        BadRequestException exception = assertThrows(BadRequestException.class, () ->
                cartService.updateCartItemQuantity(userId, cartItemId, request));
        assertTrue(exception.getMessage().contains("hết hàng"));
        verify(cartItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("Ném BadRequestException khi số lượng yêu cầu vượt quá tồn kho")
    void updateCartItemQuantity_ThrowsBadRequestException_WhenQuantityExceedsStock() {
        UUID cartItemId = UUID.randomUUID();
        mockProduct.setStockQuantity(5);

        UpdateCartItemRequest request = UpdateCartItemRequest.builder()
                .quantity(6)
                .build();

        CartItem existingCartItem = CartItem.builder()
                .id(cartItemId)
                .user(mockUser)
                .product(mockProduct)
                .quantity(1)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        when(cartItemRepository.findByIdAndUserId(cartItemId, userId)).thenReturn(Optional.of(existingCartItem));
        when(productRepository.findById(productId)).thenReturn(Optional.of(mockProduct));

        BadRequestException exception = assertThrows(BadRequestException.class, () ->
                cartService.updateCartItemQuantity(userId, cartItemId, request));
        assertTrue(exception.getMessage().contains("vượt quá số lượng tồn kho"));
        verify(cartItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("Ném BadRequestException khi số lượng yêu cầu < 1 hoặc null")
    void updateCartItemQuantity_ThrowsBadRequestException_WhenQuantityInvalid() {
        UUID cartItemId = UUID.randomUUID();
        UpdateCartItemRequest requestNull = UpdateCartItemRequest.builder()
                .quantity(null)
                .build();
        UpdateCartItemRequest requestZero = UpdateCartItemRequest.builder()
                .quantity(0)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));

        assertThrows(BadRequestException.class, () ->
                cartService.updateCartItemQuantity(userId, cartItemId, requestNull));
        assertThrows(BadRequestException.class, () ->
                cartService.updateCartItemQuantity(userId, cartItemId, requestZero));
        verify(cartItemRepository, never()).save(any());
    }

    // ==================== Tests cho PC Case (addPcCaseToCart) ====================

    @Test
    @DisplayName("Thêm cấu hình PC mới vào giỏ hàng thành công")
    void addPcCaseToCart_Success_WhenNewItem() {
        AddCartPcCaseRequest request = AddCartPcCaseRequest.builder()
                .pcCaseId(pcCaseId)
                .quantity(2)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        when(pcCaseRepository.findById(pcCaseId)).thenReturn(Optional.of(mockPcCase));
        when(cartItemRepository.findByUserIdAndPcCaseId(userId, pcCaseId)).thenReturn(Optional.empty());

        CartItem savedCartItem = CartItem.builder()
                .id(UUID.randomUUID())
                .user(mockUser)
                .pcCase(mockPcCase)
                .quantity(2)
                .build();

        when(cartItemRepository.save(any(CartItem.class))).thenReturn(savedCartItem);

        CartItemResponse expectedResponse = CartItemResponse.builder()
                .id(savedCartItem.getId())
                .pcCaseId(pcCaseId)
                .pcCaseName(mockPcCase.getName())
                .itemType("PC_CASE")
                .quantity(2)
                .subtotal(BigDecimal.valueOf(27000000))
                .build();

        when(cartMapper.toResponse(savedCartItem)).thenReturn(expectedResponse);

        CartItemResponse actualResponse = cartService.addPcCaseToCart(userId, request);

        assertNotNull(actualResponse);
        assertEquals(pcCaseId, actualResponse.getPcCaseId());
        assertEquals("PC_CASE", actualResponse.getItemType());
        assertEquals(2, actualResponse.getQuantity());

        ArgumentCaptor<CartItem> captor = ArgumentCaptor.forClass(CartItem.class);
        verify(cartItemRepository).save(captor.capture());
        assertEquals(2, captor.getValue().getQuantity());
        assertEquals(mockPcCase, captor.getValue().getPcCase());
        assertNull(captor.getValue().getProduct());
        assertEquals(mockUser, captor.getValue().getUser());
    }

    @Test
    @DisplayName("Thêm cấu hình PC đã có trong giỏ -> tăng số lượng thành công")
    void addPcCaseToCart_Success_WhenExistingItemIncreasesQuantity() {
        AddCartPcCaseRequest request = AddCartPcCaseRequest.builder()
                .pcCaseId(pcCaseId)
                .quantity(2)
                .build();

        CartItem existingCartItem = CartItem.builder()
                .id(UUID.randomUUID())
                .user(mockUser)
                .pcCase(mockPcCase)
                .quantity(1)
                .build();

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(mockUser));
        when(pcCaseRepository.findById(pcCaseId)).thenReturn(Optional.of(mockPcCase));
        when(cartItemRepository.findByUserIdAndPcCaseId(userId, pcCaseId)).thenReturn(Optional.of(existingCartItem));
        when(cartItemRepository.save(existingCartItem)).thenReturn(existingCartItem);

        CartItemResponse expectedResponse = CartItemResponse.builder()
                .id(existingCartItem.getId())
                .pcCaseId(pcCaseId)
                .quantity(3) // 1 + 2 = 3 <= minStock (4)
                .build();

        when(cartMapper.toResponse(existingCartItem)).thenReturn(expectedResponse);

        CartItemResponse actualResponse = cartService.addPcCaseToCart("user@example.com", request);

        assertNotNull(actualResponse);
        assertEquals(3, actualResponse.getQuantity());
        assertEquals(3, existingCartItem.getQuantity());
        verify(cartItemRepository).save(existingCartItem);
    }

    @Test
    @DisplayName("Thêm PC Case với quantity null -> mặc định là 1")
    void addPcCaseToCart_DefaultsQuantityToOne_WhenNull() {
        AddCartPcCaseRequest request = AddCartPcCaseRequest.builder()
                .pcCaseId(pcCaseId)
                .quantity(null)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        when(pcCaseRepository.findById(pcCaseId)).thenReturn(Optional.of(mockPcCase));
        when(cartItemRepository.findByUserIdAndPcCaseId(userId, pcCaseId)).thenReturn(Optional.empty());
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        cartService.addPcCaseToCart(userId, request);

        ArgumentCaptor<CartItem> captor = ArgumentCaptor.forClass(CartItem.class);
        verify(cartItemRepository).save(captor.capture());
        assertEquals(1, captor.getValue().getQuantity());
    }

    @Test
    @DisplayName("Ném ResourceNotFoundException khi không tìm thấy PC Case")
    void addPcCaseToCart_ThrowsResourceNotFoundException_WhenPcCaseNotFound() {
        AddCartPcCaseRequest request = AddCartPcCaseRequest.builder()
                .pcCaseId(pcCaseId)
                .quantity(1)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        when(pcCaseRepository.findById(pcCaseId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> cartService.addPcCaseToCart(userId, request));
        verify(cartItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("Ném BadRequestException khi PC Case ngừng kinh doanh (isActive = false)")
    void addPcCaseToCart_ThrowsBadRequestException_WhenPcCaseInactive() {
        mockPcCase.setActive(false);

        AddCartPcCaseRequest request = AddCartPcCaseRequest.builder()
                .pcCaseId(pcCaseId)
                .quantity(1)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        when(pcCaseRepository.findById(pcCaseId)).thenReturn(Optional.of(mockPcCase));

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> cartService.addPcCaseToCart(userId, request));
        assertTrue(exception.getMessage().contains("ngừng kinh doanh"));
        verify(cartItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("Ném BadRequestException khi PC Case chưa có danh sách linh kiện")
    void addPcCaseToCart_ThrowsBadRequestException_WhenPcCaseHasNoItems() {
        mockPcCase.setItems(new ArrayList<>());

        AddCartPcCaseRequest request = AddCartPcCaseRequest.builder()
                .pcCaseId(pcCaseId)
                .quantity(1)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        when(pcCaseRepository.findById(pcCaseId)).thenReturn(Optional.of(mockPcCase));

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> cartService.addPcCaseToCart(userId, request));
        assertTrue(exception.getMessage().contains("chưa có danh sách linh kiện"));
        verify(cartItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("Ném BadRequestException khi một linh kiện thành phần bị ngừng kinh doanh")
    void addPcCaseToCart_ThrowsBadRequestException_WhenComponentInactive() {
        // GPU bị inactive
        mockPcCase.getItems().get(1).getProduct().setActive(false);

        AddCartPcCaseRequest request = AddCartPcCaseRequest.builder()
                .pcCaseId(pcCaseId)
                .quantity(1)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        when(pcCaseRepository.findById(pcCaseId)).thenReturn(Optional.of(mockPcCase));

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> cartService.addPcCaseToCart(userId, request));
        assertTrue(exception.getMessage().contains("ngừng kinh doanh"));
        verify(cartItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("Ném BadRequestException khi một linh kiện thành phần hết hàng (stock = 0)")
    void addPcCaseToCart_ThrowsBadRequestException_WhenComponentOutOfStock() {
        // GPU stock = 0
        mockPcCase.getItems().get(1).getProduct().setStockQuantity(0);

        AddCartPcCaseRequest request = AddCartPcCaseRequest.builder()
                .pcCaseId(pcCaseId)
                .quantity(1)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        when(pcCaseRepository.findById(pcCaseId)).thenReturn(Optional.of(mockPcCase));

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> cartService.addPcCaseToCart(userId, request));
        assertTrue(exception.getMessage().contains("hết hàng"));
        verify(cartItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("Ném BadRequestException khi số lượng yêu cầu vượt quá khả năng lắp ráp (item mới)")
    void addPcCaseToCart_ThrowsBadRequestException_WhenQuantityExceedsAvailableStock_NewItem() {
        // min stock = 4 (GPU stock = 4)
        AddCartPcCaseRequest request = AddCartPcCaseRequest.builder()
                .pcCaseId(pcCaseId)
                .quantity(5)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        when(pcCaseRepository.findById(pcCaseId)).thenReturn(Optional.of(mockPcCase));
        when(cartItemRepository.findByUserIdAndPcCaseId(userId, pcCaseId)).thenReturn(Optional.empty());

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> cartService.addPcCaseToCart(userId, request));
        assertTrue(exception.getMessage().contains("vượt quá số lượng"));
        verify(cartItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("Ném BadRequestException khi số lượng sau khi cộng dồn vượt quá khả năng lắp ráp (item đã có)")
    void addPcCaseToCart_ThrowsBadRequestException_WhenQuantityExceedsAvailableStock_ExistingItem() {
        // min stock = 4
        CartItem existingCartItem = CartItem.builder()
                .id(UUID.randomUUID())
                .user(mockUser)
                .pcCase(mockPcCase)
                .quantity(3)
                .build();

        AddCartPcCaseRequest request = AddCartPcCaseRequest.builder()
                .pcCaseId(pcCaseId)
                .quantity(2) // 3 + 2 = 5 > 4
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        when(pcCaseRepository.findById(pcCaseId)).thenReturn(Optional.of(mockPcCase));
        when(cartItemRepository.findByUserIdAndPcCaseId(userId, pcCaseId)).thenReturn(Optional.of(existingCartItem));

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> cartService.addPcCaseToCart(userId, request));
        assertTrue(exception.getMessage().contains("vượt quá số lượng"));
        verify(cartItemRepository, never()).save(any());
    }

    // ==================== Tests cho cập nhật số lượng PC Case (updateCartPcCaseQuantity) ====================

    @Test
    @DisplayName("Cập nhật số lượng PC Case thành công khi tìm thấy theo cartItemId")
    void updateCartPcCaseQuantity_Success_WhenFoundById() {
        UUID cartItemId = UUID.randomUUID();
        UpdateCartPcCaseRequest request = UpdateCartPcCaseRequest.builder()
                .quantity(3)
                .build();

        CartItem existingCartItem = CartItem.builder()
                .id(cartItemId)
                .user(mockUser)
                .pcCase(mockPcCase)
                .quantity(1)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        when(cartItemRepository.findByIdAndUserId(cartItemId, userId)).thenReturn(Optional.of(existingCartItem));
        when(pcCaseRepository.findById(pcCaseId)).thenReturn(Optional.of(mockPcCase));
        when(cartItemRepository.save(existingCartItem)).thenReturn(existingCartItem);

        CartItemResponse expectedResponse = CartItemResponse.builder()
                .id(cartItemId)
                .pcCaseId(pcCaseId)
                .pcCaseName(mockPcCase.getName())
                .itemType("PC_CASE")
                .quantity(3)
                .subtotal(BigDecimal.valueOf(40500000))
                .build();

        when(cartMapper.toResponse(existingCartItem)).thenReturn(expectedResponse);

        CartItemResponse actualResponse = cartService.updateCartPcCaseQuantity(userId, cartItemId, request);

        assertNotNull(actualResponse);
        assertEquals(3, actualResponse.getQuantity());
        assertEquals(3, existingCartItem.getQuantity());
        verify(cartItemRepository).save(existingCartItem);
    }

    @Test
    @DisplayName("Cập nhật số lượng PC Case thành công khi tìm fallback theo pcCaseId")
    void updateCartPcCaseQuantity_Success_WhenFoundByPcCaseIdFallback() {
        UpdateCartPcCaseRequest request = UpdateCartPcCaseRequest.builder()
                .quantity(2)
                .build();

        CartItem existingCartItem = CartItem.builder()
                .id(UUID.randomUUID())
                .user(mockUser)
                .pcCase(mockPcCase)
                .quantity(1)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        when(cartItemRepository.findByIdAndUserId(pcCaseId, userId)).thenReturn(Optional.empty());
        when(cartItemRepository.findByUserIdAndPcCaseId(userId, pcCaseId)).thenReturn(Optional.of(existingCartItem));
        when(pcCaseRepository.findById(pcCaseId)).thenReturn(Optional.of(mockPcCase));
        when(cartItemRepository.save(existingCartItem)).thenReturn(existingCartItem);

        CartItemResponse expectedResponse = CartItemResponse.builder()
                .id(existingCartItem.getId())
                .pcCaseId(pcCaseId)
                .pcCaseName(mockPcCase.getName())
                .itemType("PC_CASE")
                .quantity(2)
                .build();

        when(cartMapper.toResponse(existingCartItem)).thenReturn(expectedResponse);

        CartItemResponse actualResponse = cartService.updateCartPcCaseQuantity(userId, pcCaseId, request);

        assertNotNull(actualResponse);
        assertEquals(2, actualResponse.getQuantity());
        assertEquals(2, existingCartItem.getQuantity());
        verify(cartItemRepository).save(existingCartItem);
    }

    @Test
    @DisplayName("Cập nhật số lượng PC Case theo email thành công")
    void updateCartPcCaseQuantity_Success_ByEmail() {
        UUID cartItemId = UUID.randomUUID();
        UpdateCartPcCaseRequest request = UpdateCartPcCaseRequest.builder()
                .quantity(2)
                .build();

        CartItem existingCartItem = CartItem.builder()
                .id(cartItemId)
                .user(mockUser)
                .pcCase(mockPcCase)
                .quantity(1)
                .build();

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(mockUser));
        when(cartItemRepository.findByIdAndUserId(cartItemId, userId)).thenReturn(Optional.of(existingCartItem));
        when(pcCaseRepository.findById(pcCaseId)).thenReturn(Optional.of(mockPcCase));
        when(cartItemRepository.save(existingCartItem)).thenReturn(existingCartItem);

        CartItemResponse expectedResponse = CartItemResponse.builder()
                .id(cartItemId)
                .quantity(2)
                .build();

        when(cartMapper.toResponse(existingCartItem)).thenReturn(expectedResponse);

        CartItemResponse actualResponse = cartService.updateCartPcCaseQuantity("user@example.com", cartItemId, request);

        assertNotNull(actualResponse);
        assertEquals(2, actualResponse.getQuantity());
        verify(cartItemRepository).save(existingCartItem);
    }

    @Test
    @DisplayName("Ném ResourceNotFoundException khi không tìm thấy user khi cập nhật PC Case")
    void updateCartPcCaseQuantity_ThrowsResourceNotFoundException_WhenUserNotFound() {
        UUID cartItemId = UUID.randomUUID();
        UpdateCartPcCaseRequest request = UpdateCartPcCaseRequest.builder()
                .quantity(2)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                cartService.updateCartPcCaseQuantity(userId, cartItemId, request));
        verify(cartItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("Ném ResourceNotFoundException khi không tìm thấy cấu hình PC trong giỏ hàng")
    void updateCartPcCaseQuantity_ThrowsResourceNotFoundException_WhenCartItemNotFound() {
        UUID cartItemId = UUID.randomUUID();
        UpdateCartPcCaseRequest request = UpdateCartPcCaseRequest.builder()
                .quantity(2)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        when(cartItemRepository.findByIdAndUserId(cartItemId, userId)).thenReturn(Optional.empty());
        when(cartItemRepository.findByUserIdAndPcCaseId(userId, cartItemId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                cartService.updateCartPcCaseQuantity(userId, cartItemId, request));
        verify(cartItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("Ném BadRequestException khi mục trong giỏ là linh kiện lẻ chứ không phải cấu hình PC")
    void updateCartPcCaseQuantity_ThrowsBadRequestException_WhenCartItemIsProduct() {
        UUID cartItemId = UUID.randomUUID();
        UpdateCartPcCaseRequest request = UpdateCartPcCaseRequest.builder()
                .quantity(2)
                .build();

        CartItem productCartItem = CartItem.builder()
                .id(cartItemId)
                .user(mockUser)
                .product(mockProduct)
                .pcCase(null)
                .quantity(1)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        when(cartItemRepository.findByIdAndUserId(cartItemId, userId)).thenReturn(Optional.of(productCartItem));

        BadRequestException exception = assertThrows(BadRequestException.class, () ->
                cartService.updateCartPcCaseQuantity(userId, cartItemId, request));
        assertTrue(exception.getMessage().contains("không phải là cấu hình PC"));
        verify(cartItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("Ném ResourceNotFoundException khi không tìm thấy thông tin cấu hình PC tương ứng")
    void updateCartPcCaseQuantity_ThrowsResourceNotFoundException_WhenPcCaseNotFound() {
        UUID cartItemId = UUID.randomUUID();
        UpdateCartPcCaseRequest request = UpdateCartPcCaseRequest.builder()
                .quantity(2)
                .build();

        CartItem existingCartItem = CartItem.builder()
                .id(cartItemId)
                .user(mockUser)
                .pcCase(mockPcCase)
                .quantity(1)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        when(cartItemRepository.findByIdAndUserId(cartItemId, userId)).thenReturn(Optional.of(existingCartItem));
        when(pcCaseRepository.findById(pcCaseId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                cartService.updateCartPcCaseQuantity(userId, cartItemId, request));
        verify(cartItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("Ném BadRequestException khi cấu hình PC ngừng kinh doanh khi cập nhật")
    void updateCartPcCaseQuantity_ThrowsBadRequestException_WhenPcCaseInactive() {
        UUID cartItemId = UUID.randomUUID();
        mockPcCase.setActive(false);

        UpdateCartPcCaseRequest request = UpdateCartPcCaseRequest.builder()
                .quantity(2)
                .build();

        CartItem existingCartItem = CartItem.builder()
                .id(cartItemId)
                .user(mockUser)
                .pcCase(mockPcCase)
                .quantity(1)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        when(cartItemRepository.findByIdAndUserId(cartItemId, userId)).thenReturn(Optional.of(existingCartItem));
        when(pcCaseRepository.findById(pcCaseId)).thenReturn(Optional.of(mockPcCase));

        BadRequestException exception = assertThrows(BadRequestException.class, () ->
                cartService.updateCartPcCaseQuantity(userId, cartItemId, request));
        assertTrue(exception.getMessage().contains("ngừng kinh doanh"));
        verify(cartItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("Ném BadRequestException khi PC Case chưa có danh sách linh kiện khi cập nhật")
    void updateCartPcCaseQuantity_ThrowsBadRequestException_WhenPcCaseHasNoItems() {
        UUID cartItemId = UUID.randomUUID();
        mockPcCase.setItems(new ArrayList<>());

        UpdateCartPcCaseRequest request = UpdateCartPcCaseRequest.builder()
                .quantity(2)
                .build();

        CartItem existingCartItem = CartItem.builder()
                .id(cartItemId)
                .user(mockUser)
                .pcCase(mockPcCase)
                .quantity(1)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        when(cartItemRepository.findByIdAndUserId(cartItemId, userId)).thenReturn(Optional.of(existingCartItem));
        when(pcCaseRepository.findById(pcCaseId)).thenReturn(Optional.of(mockPcCase));

        BadRequestException exception = assertThrows(BadRequestException.class, () ->
                cartService.updateCartPcCaseQuantity(userId, cartItemId, request));
        assertTrue(exception.getMessage().contains("chưa có danh sách linh kiện"));
        verify(cartItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("Ném BadRequestException khi một linh kiện thành phần bị ngừng kinh doanh khi cập nhật")
    void updateCartPcCaseQuantity_ThrowsBadRequestException_WhenComponentInactive() {
        UUID cartItemId = UUID.randomUUID();
        mockPcCase.getItems().get(1).getProduct().setActive(false);

        UpdateCartPcCaseRequest request = UpdateCartPcCaseRequest.builder()
                .quantity(2)
                .build();

        CartItem existingCartItem = CartItem.builder()
                .id(cartItemId)
                .user(mockUser)
                .pcCase(mockPcCase)
                .quantity(1)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        when(cartItemRepository.findByIdAndUserId(cartItemId, userId)).thenReturn(Optional.of(existingCartItem));
        when(pcCaseRepository.findById(pcCaseId)).thenReturn(Optional.of(mockPcCase));

        BadRequestException exception = assertThrows(BadRequestException.class, () ->
                cartService.updateCartPcCaseQuantity(userId, cartItemId, request));
        assertTrue(exception.getMessage().contains("ngừng kinh doanh"));
        verify(cartItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("Ném BadRequestException khi linh kiện thành phần trong cấu hình PC hết hàng khi cập nhật")
    void updateCartPcCaseQuantity_ThrowsBadRequestException_WhenComponentOutOfStock() {
        UUID cartItemId = UUID.randomUUID();
        // GPU stock = 0
        mockPcCase.getItems().get(1).getProduct().setStockQuantity(0);

        UpdateCartPcCaseRequest request = UpdateCartPcCaseRequest.builder()
                .quantity(2)
                .build();

        CartItem existingCartItem = CartItem.builder()
                .id(cartItemId)
                .user(mockUser)
                .pcCase(mockPcCase)
                .quantity(1)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        when(cartItemRepository.findByIdAndUserId(cartItemId, userId)).thenReturn(Optional.of(existingCartItem));
        when(pcCaseRepository.findById(pcCaseId)).thenReturn(Optional.of(mockPcCase));

        BadRequestException exception = assertThrows(BadRequestException.class, () ->
                cartService.updateCartPcCaseQuantity(userId, cartItemId, request));
        assertTrue(exception.getMessage().contains("hết hàng"));
        verify(cartItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("Ném BadRequestException khi số lượng PC Case yêu cầu vượt quá khả năng lắp ráp")
    void updateCartPcCaseQuantity_ThrowsBadRequestException_WhenQuantityExceedsAvailableStock() {
        UUID cartItemId = UUID.randomUUID();
        // min stock = 4 (GPU stock = 4)
        UpdateCartPcCaseRequest request = UpdateCartPcCaseRequest.builder()
                .quantity(5)
                .build();

        CartItem existingCartItem = CartItem.builder()
                .id(cartItemId)
                .user(mockUser)
                .pcCase(mockPcCase)
                .quantity(1)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        when(cartItemRepository.findByIdAndUserId(cartItemId, userId)).thenReturn(Optional.of(existingCartItem));
        when(pcCaseRepository.findById(pcCaseId)).thenReturn(Optional.of(mockPcCase));

        BadRequestException exception = assertThrows(BadRequestException.class, () ->
                cartService.updateCartPcCaseQuantity(userId, cartItemId, request));
        assertTrue(exception.getMessage().contains("vượt quá số lượng"));
        verify(cartItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("Ném BadRequestException khi số lượng PC Case yêu cầu < 1 hoặc null")
    void updateCartPcCaseQuantity_ThrowsBadRequestException_WhenQuantityInvalid() {
        UUID cartItemId = UUID.randomUUID();
        UpdateCartPcCaseRequest requestNull = UpdateCartPcCaseRequest.builder()
                .quantity(null)
                .build();
        UpdateCartPcCaseRequest requestZero = UpdateCartPcCaseRequest.builder()
                .quantity(0)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));

        assertThrows(BadRequestException.class, () ->
                cartService.updateCartPcCaseQuantity(userId, cartItemId, requestNull));
        assertThrows(BadRequestException.class, () ->
                cartService.updateCartPcCaseQuantity(userId, cartItemId, requestZero));
        verify(cartItemRepository, never()).save(any());
    }

    // ==================== Tests cho xem giỏ hàng (getCart) ====================

    @Test
    @DisplayName("Lấy thông tin giỏ hàng thành công khi giỏ có linh kiện lẻ và cấu hình PC")
    void getCart_Success_WhenCartNotEmpty() {
        CartItem productItem = CartItem.builder()
                .id(UUID.randomUUID())
                .user(mockUser)
                .product(mockProduct)
                .quantity(2)
                .build();

        CartItem pcCaseItem = CartItem.builder()
                .id(UUID.randomUUID())
                .user(mockUser)
                .pcCase(mockPcCase)
                .quantity(1)
                .build();

        List<CartItem> cartItems = List.of(productItem, pcCaseItem);

        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        when(cartItemRepository.findByUserIdWithDetailsOrderByCreatedAtDesc(userId)).thenReturn(cartItems);

        CartItemResponse productResponse = CartItemResponse.builder()
                .id(productItem.getId())
                .productId(mockProduct.getId())
                .productName(mockProduct.getName())
                .itemType("PRODUCT")
                .unitPrice(mockProduct.getPrice())
                .quantity(2)
                .subtotal(BigDecimal.valueOf(11000000))
                .build();

        CartItemResponse pcCaseResponse = CartItemResponse.builder()
                .id(pcCaseItem.getId())
                .pcCaseId(mockPcCase.getId())
                .pcCaseName(mockPcCase.getName())
                .itemType("PC_CASE")
                .unitPrice(mockPcCase.getTotalPrice())
                .quantity(1)
                .subtotal(BigDecimal.valueOf(13500000))
                .build();

        when(cartMapper.toResponse(productItem)).thenReturn(productResponse);
        when(cartMapper.toResponse(pcCaseItem)).thenReturn(pcCaseResponse);

        CartResponse cartResponse = cartService.getCart(userId);

        assertNotNull(cartResponse);
        assertEquals(2, cartResponse.getTotalItems());
        assertEquals(3, cartResponse.getTotalQuantity()); // 2 + 1
        assertEquals(BigDecimal.valueOf(24500000), cartResponse.getTotalAmount()); // 11000000 + 13500000
        assertEquals(2, cartResponse.getItems().size());
    }

    @Test
    @DisplayName("Lấy thông tin giỏ hàng thành công khi giỏ hàng trống")
    void getCart_Success_WhenCartEmpty() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        when(cartItemRepository.findByUserIdWithDetailsOrderByCreatedAtDesc(userId)).thenReturn(List.of());

        CartResponse cartResponse = cartService.getCart(userId);

        assertNotNull(cartResponse);
        assertEquals(0, cartResponse.getTotalItems());
        assertEquals(0, cartResponse.getTotalQuantity());
        assertEquals(BigDecimal.ZERO, cartResponse.getTotalAmount());
        assertTrue(cartResponse.getItems().isEmpty());
    }

    @Test
    @DisplayName("Lấy thông tin giỏ hàng theo email thành công")
    void getCart_ByEmail_Success() {
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(mockUser));
        when(cartItemRepository.findByUserIdWithDetailsOrderByCreatedAtDesc(userId)).thenReturn(List.of());

        CartResponse cartResponse = cartService.getCart("user@example.com");

        assertNotNull(cartResponse);
        assertEquals(0, cartResponse.getTotalItems());
        assertEquals(0, cartResponse.getTotalQuantity());
        assertEquals(BigDecimal.ZERO, cartResponse.getTotalAmount());
    }

    @Test
    @DisplayName("Ném ResourceNotFoundException khi không tìm thấy user theo userId khi lấy giỏ")
    void getCart_ThrowsResourceNotFoundException_WhenUserNotFound() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> cartService.getCart(userId));
        verify(cartItemRepository, never()).findByUserIdWithDetailsOrderByCreatedAtDesc(any());
    }

    @Test
    @DisplayName("Ném ResourceNotFoundException khi không tìm thấy user theo email khi lấy giỏ")
    void getCart_ByEmail_ThrowsResourceNotFoundException_WhenUserNotFound() {
        when(userRepository.findByEmail("notfound@example.com")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> cartService.getCart("notfound@example.com"));
        verify(cartItemRepository, never()).findByUserIdWithDetailsOrderByCreatedAtDesc(any());
    }
}
