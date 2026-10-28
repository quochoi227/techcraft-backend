package com.techcraft.techcraftbackend.mapper;

import com.techcraft.techcraftbackend.dto.response.PcCaseResponse;
import com.techcraft.techcraftbackend.entity.PcCase;
import com.techcraft.techcraftbackend.entity.Review;
import com.techcraft.techcraftbackend.entity.User;
import com.techcraft.techcraftbackend.enums.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class PcCaseMapperTest {

    @Autowired
    private PcCaseMapper pcCaseMapper;

    @Test
    @DisplayName("Ánh xạ PcCase có reviews: lấy đủ list review, tính đúng averageRating và totalReviews")
    void toResponse_WithReviews_CalculatesStatsAndMapsReviews() {
        UUID pcCaseId = UUID.randomUUID();
        PcCase pcCase = PcCase.builder()
                .id(pcCaseId)
                .name("PC Gaming Ultimate 2026")
                .totalPrice(new BigDecimal("35000000.00"))
                .useCase("gaming")
                .description("Cấu hình siêu mạnh")
                .isActive(true)
                .build();

        User user1 = User.builder()
                .id(UUID.randomUUID())
                .email("gamer1@example.com")
                .fullName("Le Van C")
                .role(Role.USER)
                .build();

        Review review1 = Review.builder()
                .id(UUID.randomUUID())
                .user(user1)
                .pcCase(pcCase)
                .rating(5)
                .comment("Case lắp ráp rất gọn gàng và đẹp mắt")
                .createdAt(Instant.now())
                .build();

        pcCase.addReview(review1);

        PcCaseResponse response = pcCaseMapper.toResponse(pcCase);

        assertNotNull(response);
        assertEquals(pcCaseId, response.getId());
        assertEquals("PC Gaming Ultimate 2026", response.getName());
        assertEquals(1, response.getTotalReviews());
        assertEquals(5.0, response.getAverageRating());
        assertNotNull(response.getReviews());
        assertEquals(1, response.getReviews().size());
        assertEquals(5, response.getReviews().get(0).getRating());
        assertEquals("gamer1@example.com", response.getReviews().get(0).getUserEmail());
        assertEquals("Case lắp ráp rất gọn gàng và đẹp mắt", response.getReviews().get(0).getComment());
    }

    @Test
    @DisplayName("Ánh xạ PcCase không có review: trả về reviews rỗng, averageRating = 0.0 và totalReviews = 0")
    void toResponse_WithoutReviews_SetsZeroStatsAndEmptyList() {
        PcCase pcCase = PcCase.builder()
                .id(UUID.randomUUID())
                .name("PC Office Basic")
                .totalPrice(new BigDecimal("8000000.00"))
                .useCase("office")
                .isActive(true)
                .build();

        PcCaseResponse response = pcCaseMapper.toResponse(pcCase);

        assertNotNull(response);
        assertEquals(0, response.getTotalReviews());
        assertEquals(0.0, response.getAverageRating());
        assertNotNull(response.getReviews());
        assertTrue(response.getReviews().isEmpty());
    }
}
