package com.techcraft.techcraftbackend.mapper;

import com.techcraft.techcraftbackend.dto.response.ProductResponse;
import com.techcraft.techcraftbackend.entity.Product;
import com.techcraft.techcraftbackend.entity.Review;
import com.techcraft.techcraftbackend.entity.User;
import com.techcraft.techcraftbackend.enums.ProductCategory;
import com.techcraft.techcraftbackend.enums.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ProductMapperTest {

    @Autowired
    private ProductMapper productMapper;

    @Test
    @DisplayName("Ánh xạ Product có reviews: lấy đủ list review, tính đúng averageRating và totalReviews")
    void toResponse_WithReviews_CalculatesStatsAndMapsReviews() {
        UUID productId = UUID.randomUUID();
        Product product = Product.builder()
                .id(productId)
                .name("CPU AMD Ryzen 7 7800X3D")
                .price(new BigDecimal("11500000.00"))
                .stockQuantity(10)
                .category(ProductCategory.CPU)
                .detail(Map.of("socket", "AM5"))
                .isActive(true)
                .build();

        User user1 = User.builder()
                .id(UUID.randomUUID())
                .email("user1@example.com")
                .fullName("Nguyen Van A")
                .role(Role.USER)
                .build();

        User user2 = User.builder()
                .id(UUID.randomUUID())
                .email("user2@example.com")
                .fullName("Tran Van B")
                .role(Role.USER)
                .build();

        Review review1 = Review.builder()
                .id(UUID.randomUUID())
                .user(user1)
                .product(product)
                .rating(5)
                .comment("Hiệu năng chơi game cực đỉnh")
                .createdAt(Instant.now())
                .build();

        Review review2 = Review.builder()
                .id(UUID.randomUUID())
                .user(user2)
                .product(product)
                .rating(4)
                .comment("Nhiệt độ hơi cao một chút")
                .createdAt(Instant.now())
                .build();

        product.addReview(review1);
        product.addReview(review2);

        ProductResponse response = productMapper.toResponse(product);

        assertNotNull(response);
        assertEquals(productId, response.getId());
        assertEquals("CPU AMD Ryzen 7 7800X3D", response.getName());
        assertEquals(2, response.getTotalReviews());
        assertEquals(4.5, response.getAverageRating());
        assertNotNull(response.getReviews());
        assertEquals(2, response.getReviews().size());

        assertEquals(5, response.getReviews().get(0).getRating());
        assertEquals("Hiệu năng chơi game cực đỉnh", response.getReviews().get(0).getComment());
        assertEquals("user1@example.com", response.getReviews().get(0).getUserEmail());
        assertEquals("Nguyen Van A", response.getReviews().get(0).getUserFullName());

        assertEquals(4, response.getReviews().get(1).getRating());
        assertEquals("Tran Van B", response.getReviews().get(1).getUserFullName());
    }

    @Test
    @DisplayName("Ánh xạ Product không có review: trả về reviews rỗng, averageRating = 0.0 và totalReviews = 0")
    void toResponse_WithoutReviews_SetsZeroStatsAndEmptyList() {
        Product product = Product.builder()
                .id(UUID.randomUUID())
                .name("CPU Intel Core i3 12100F")
                .price(new BigDecimal("2200000.00"))
                .stockQuantity(15)
                .category(ProductCategory.CPU)
                .detail(Map.of("socket", "LGA1700"))
                .isActive(true)
                .build();

        ProductResponse response = productMapper.toResponse(product);

        assertNotNull(response);
        assertEquals(0, response.getTotalReviews());
        assertEquals(0.0, response.getAverageRating());
        assertNotNull(response.getReviews());
        assertTrue(response.getReviews().isEmpty());
    }
}
