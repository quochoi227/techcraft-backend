package com.techcraft.techcraftbackend.service;

import com.techcraft.techcraftbackend.dto.request.CreateReviewRequest;
import com.techcraft.techcraftbackend.dto.response.ReviewResponse;
import com.techcraft.techcraftbackend.entity.Product;
import com.techcraft.techcraftbackend.entity.Review;
import com.techcraft.techcraftbackend.entity.User;
import com.techcraft.techcraftbackend.enums.OrderStatus;
import com.techcraft.techcraftbackend.enums.ProductCategory;
import com.techcraft.techcraftbackend.enums.Role;
import com.techcraft.techcraftbackend.exception.BadRequestException;
import com.techcraft.techcraftbackend.exception.DuplicateResourceException;
import com.techcraft.techcraftbackend.exception.ResourceNotFoundException;
import com.techcraft.techcraftbackend.exception.UnauthorizedException;
import com.techcraft.techcraftbackend.mapper.ReviewMapper;
import com.techcraft.techcraftbackend.repository.OrderRepository;
import com.techcraft.techcraftbackend.repository.ProductRepository;
import com.techcraft.techcraftbackend.repository.ReviewRepository;
import com.techcraft.techcraftbackend.repository.UserRepository;
import com.techcraft.techcraftbackend.service.impl.ReviewServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ReviewMapper reviewMapper;

    @InjectMocks
    private ReviewServiceImpl reviewService;

    private User testUser;
    private Product testProduct;
    private UUID userId;
    private UUID productId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        productId = UUID.randomUUID();

        testUser = User.builder()
                .id(userId)
                .email("user@example.com")
                .fullName("Nguyen Van A")
                .role(Role.USER)
                .emailVerified(true)
                .build();

        testProduct = Product.builder()
                .id(productId)
                .name("CPU Intel Core i5 13600K")
                .price(new BigDecimal("7500000.00"))
                .stockQuantity(10)
                .category(ProductCategory.CPU)
                .detail(Map.of("socket", "LGA1700"))
                .isActive(true)
                .build();
    }

    @Test
    @DisplayName("Tạo đánh giá thành công khi người dùng đã mua hàng và giao thành công")
    void createProductReview_Success() {
        CreateReviewRequest request = CreateReviewRequest.builder()
                .rating(5)
                .comment("  Sản phẩm rất tốt, giao hàng nhanh!  ")
                .build();

        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct));
        when(reviewRepository.existsByUserIdAndProductId(userId, productId)).thenReturn(false);
        when(orderRepository.hasUserPurchasedProduct(userId, productId, OrderStatus.DELIVERED)).thenReturn(true);

        Review savedReview = Review.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .product(testProduct)
                .rating(5)
                .comment("Sản phẩm rất tốt, giao hàng nhanh!")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        ReviewResponse expectedResponse = ReviewResponse.builder()
                .id(savedReview.getId())
                .userId(userId)
                .userEmail(testUser.getEmail())
                .userFullName(testUser.getFullName())
                .productId(productId)
                .productName(testProduct.getName())
                .rating(5)
                .comment("Sản phẩm rất tốt, giao hàng nhanh!")
                .createdAt(savedReview.getCreatedAt())
                .build();

        when(reviewRepository.save(any(Review.class))).thenReturn(savedReview);
        when(reviewMapper.toResponse(savedReview)).thenReturn(expectedResponse);

        ReviewResponse response = reviewService.createProductReview(testUser.getEmail(), productId, request);

        assertNotNull(response);
        assertEquals(5, response.getRating());
        assertEquals("Sản phẩm rất tốt, giao hàng nhanh!", response.getComment());
        assertEquals(productId, response.getProductId());
        assertEquals(userId, response.getUserId());

        ArgumentCaptor<Review> captor = ArgumentCaptor.forClass(Review.class);
        verify(reviewRepository).save(captor.capture());
        Review captured = captor.getValue();
        assertEquals(testUser, captured.getUser());
        assertEquals(testProduct, captured.getProduct());
        assertEquals(5, captured.getRating());
        assertEquals("Sản phẩm rất tốt, giao hàng nhanh!", captured.getComment());
    }

    @Test
    @DisplayName("Tạo đánh giá thành công khi comment là null hoặc chỉ chứa khoảng trắng")
    void createProductReview_Success_EmptyComment() {
        CreateReviewRequest request = CreateReviewRequest.builder()
                .rating(4)
                .comment("   ")
                .build();

        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct));
        when(reviewRepository.existsByUserIdAndProductId(userId, productId)).thenReturn(false);
        when(orderRepository.hasUserPurchasedProduct(userId, productId, OrderStatus.DELIVERED)).thenReturn(true);

        Review savedReview = Review.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .product(testProduct)
                .rating(4)
                .comment(null)
                .build();

        when(reviewRepository.save(any(Review.class))).thenReturn(savedReview);
        when(reviewMapper.toResponse(savedReview)).thenReturn(ReviewResponse.builder().id(savedReview.getId()).rating(4).build());

        ReviewResponse response = reviewService.createProductReview(testUser.getEmail(), productId, request);

        assertNotNull(response);
        ArgumentCaptor<Review> captor = ArgumentCaptor.forClass(Review.class);
        verify(reviewRepository).save(captor.capture());
        assertNull(captor.getValue().getComment());
    }

    @Test
    @DisplayName("Ném UnauthorizedException khi không tìm thấy user theo email")
    void createProductReview_ThrowsUnauthorized_WhenUserNotFound() {
        CreateReviewRequest request = CreateReviewRequest.builder().rating(5).build();
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThrows(UnauthorizedException.class, () ->
                reviewService.createProductReview("unknown@example.com", productId, request));

        verify(reviewRepository, never()).save(any());
    }

    @Test
    @DisplayName("Ném ResourceNotFoundException khi không tìm thấy linh kiện")
    void createProductReview_ThrowsNotFound_WhenProductNotFound() {
        CreateReviewRequest request = CreateReviewRequest.builder().rating(5).build();
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                reviewService.createProductReview(testUser.getEmail(), productId, request));

        verify(reviewRepository, never()).save(any());
    }

    @Test
    @DisplayName("Ném ResourceNotFoundException khi linh kiện đã bị vô hiệu hoá và người dùng không phải admin")
    void createProductReview_ThrowsNotFound_WhenProductInactiveForUser() {
        testProduct.setActive(false);
        CreateReviewRequest request = CreateReviewRequest.builder().rating(5).build();
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct));

        assertThrows(ResourceNotFoundException.class, () ->
                reviewService.createProductReview(testUser.getEmail(), productId, request));

        verify(reviewRepository, never()).save(any());
    }

    @Test
    @DisplayName("Cho phép admin đánh giá sản phẩm kể cả khi sản phẩm không active")
    void createProductReview_Success_WhenProductInactiveForAdmin() {
        testUser.setRole(Role.ADMIN);
        testProduct.setActive(false);
        CreateReviewRequest request = CreateReviewRequest.builder().rating(5).build();

        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct));
        when(reviewRepository.existsByUserIdAndProductId(userId, productId)).thenReturn(false);
        when(orderRepository.hasUserPurchasedProduct(userId, productId, OrderStatus.DELIVERED)).thenReturn(true);
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(reviewMapper.toResponse(any(Review.class))).thenReturn(ReviewResponse.builder().rating(5).build());

        ReviewResponse response = reviewService.createProductReview(testUser.getEmail(), productId, request);
        assertNotNull(response);
    }

    @Test
    @DisplayName("Ném DuplicateResourceException khi người dùng đã đánh giá linh kiện này trước đó")
    void createProductReview_ThrowsDuplicate_WhenAlreadyReviewed() {
        CreateReviewRequest request = CreateReviewRequest.builder().rating(5).build();
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct));
        when(reviewRepository.existsByUserIdAndProductId(userId, productId)).thenReturn(true);

        DuplicateResourceException exception = assertThrows(DuplicateResourceException.class, () ->
                reviewService.createProductReview(testUser.getEmail(), productId, request));

        assertEquals("Bạn đã đánh giá linh kiện này rồi.", exception.getMessage());
        verify(orderRepository, never()).hasUserPurchasedProduct(any(), any(), any());
        verify(reviewRepository, never()).save(any());
    }

    @Test
    @DisplayName("Ném BadRequestException khi người dùng chưa mua sản phẩm hoặc đơn hàng chưa giao thành công")
    void createProductReview_ThrowsBadRequest_WhenNotPurchased() {
        CreateReviewRequest request = CreateReviewRequest.builder().rating(5).build();
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct));
        when(reviewRepository.existsByUserIdAndProductId(userId, productId)).thenReturn(false);
        when(orderRepository.hasUserPurchasedProduct(userId, productId, OrderStatus.DELIVERED)).thenReturn(false);

        BadRequestException exception = assertThrows(BadRequestException.class, () ->
                reviewService.createProductReview(testUser.getEmail(), productId, request));

        assertEquals("Bạn chỉ có thể đánh giá linh kiện sau khi đã mua và nhận hàng thành công.", exception.getMessage());
        verify(reviewRepository, never()).save(any());
    }
}
