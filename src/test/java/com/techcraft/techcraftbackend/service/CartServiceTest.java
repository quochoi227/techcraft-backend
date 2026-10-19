package com.techcraft.techcraftbackend.service;

import com.techcraft.techcraftbackend.dto.request.AddCartItemRequest;
import com.techcraft.techcraftbackend.dto.response.CartItemResponse;
import com.techcraft.techcraftbackend.entity.CartItem;
import com.techcraft.techcraftbackend.entity.Product;
import com.techcraft.techcraftbackend.entity.ProductImage;
import com.techcraft.techcraftbackend.entity.User;
import com.techcraft.techcraftbackend.enums.ProductCategory;
import com.techcraft.techcraftbackend.exception.BadRequestException;
import com.techcraft.techcraftbackend.exception.ResourceNotFoundException;
import com.techcraft.techcraftbackend.mapper.CartMapper;
import com.techcraft.techcraftbackend.repository.CartItemRepository;
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
    private CartMapper cartMapper;

    @InjectMocks
    private CartServiceImpl cartService;

    private User mockUser;
    private Product mockProduct;
    private UUID userId;
    private UUID productId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        productId = UUID.randomUUID();

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
    }

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
}
