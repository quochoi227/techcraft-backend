package com.techcraft.techcraftbackend.controller;

import com.techcraft.techcraftbackend.dto.request.CreateReviewRequest;
import com.techcraft.techcraftbackend.dto.request.UpdateReviewRequest;
import com.techcraft.techcraftbackend.dto.response.ReviewResponse;
import com.techcraft.techcraftbackend.exception.AppException;
import com.techcraft.techcraftbackend.exception.BadRequestException;
import com.techcraft.techcraftbackend.exception.DuplicateResourceException;
import com.techcraft.techcraftbackend.exception.ResourceNotFoundException;
import com.techcraft.techcraftbackend.service.ReviewService;
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

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ReviewService reviewService;

    @Test
    @WithMockUser(username = "user@example.com", roles = "USER")
    @DisplayName("Gửi đánh giá thành công khi người dùng hợp lệ và đã mua hàng")
    void createProductReview_AsAuthenticatedUser_Returns201Created() throws Exception {
        UUID productId = UUID.randomUUID();
        CreateReviewRequest request = CreateReviewRequest.builder()
                .rating(5)
                .comment("Linh kiện hoạt động rất ổn định, nhiệt độ mát mẻ.")
                .build();

        ReviewResponse response = ReviewResponse.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .userEmail("user@example.com")
                .userFullName("Nguyen Van A")
                .productId(productId)
                .productName("Intel Core i5-13600K")
                .rating(5)
                .comment("Linh kiện hoạt động rất ổn định, nhiệt độ mát mẻ.")
                .createdAt(Instant.now())
                .build();

        when(reviewService.createProductReview(eq("user@example.com"), eq(productId), any(CreateReviewRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/products/{id}/reviews", productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Đánh giá linh kiện thành công!"))
                .andExpect(jsonPath("$.data.id").value(response.getId().toString()))
                .andExpect(jsonPath("$.data.rating").value(5))
                .andExpect(jsonPath("$.data.comment").value("Linh kiện hoạt động rất ổn định, nhiệt độ mát mẻ."))
                .andExpect(jsonPath("$.data.productName").value("Intel Core i5-13600K"));
    }

    @Test
    @DisplayName("Trả về 401 Unauthorized khi người dùng chưa đăng nhập")
    void createProductReview_Unauthenticated_Returns401Unauthorized() throws Exception {
        UUID productId = UUID.randomUUID();
        CreateReviewRequest request = CreateReviewRequest.builder()
                .rating(5)
                .comment("Đánh giá không đăng nhập")
                .build();

        mockMvc.perform(post("/api/products/{id}/reviews", productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "user@example.com", roles = "USER")
    @DisplayName("Trả về 400 Bad Request khi điểm đánh giá vượt ngoài phạm vi 1-5")
    void createProductReview_InvalidRating_Returns400BadRequest() throws Exception {
        UUID productId = UUID.randomUUID();
        CreateReviewRequest request = CreateReviewRequest.builder()
                .rating(6)
                .comment("Điểm vượt quá 5")
                .build();

        mockMvc.perform(post("/api/products/{id}/reviews", productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.rating").exists());
    }

    @Test
    @WithMockUser(username = "user@example.com", roles = "USER")
    @DisplayName("Trả về 400 Bad Request khi điểm đánh giá bị để trống")
    void createProductReview_NullRating_Returns400BadRequest() throws Exception {
        UUID productId = UUID.randomUUID();
        CreateReviewRequest request = CreateReviewRequest.builder()
                .rating(null)
                .comment("Thiếu rating")
                .build();

        mockMvc.perform(post("/api/products/{id}/reviews", productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.rating").exists());
    }

    @Test
    @WithMockUser(username = "user@example.com", roles = "USER")
    @DisplayName("Trả về 400 Bad Request khi người dùng chưa mua sản phẩm hoặc chưa giao thành công")
    void createProductReview_WhenNotPurchased_Returns400BadRequest() throws Exception {
        UUID productId = UUID.randomUUID();
        CreateReviewRequest request = CreateReviewRequest.builder()
                .rating(5)
                .comment("Chưa mua nhưng đánh giá")
                .build();

        when(reviewService.createProductReview(eq("user@example.com"), eq(productId), any(CreateReviewRequest.class)))
                .thenThrow(new BadRequestException("Bạn chỉ có thể đánh giá linh kiện sau khi đã mua và nhận hàng thành công."));

        mockMvc.perform(post("/api/products/{id}/reviews", productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Bạn chỉ có thể đánh giá linh kiện sau khi đã mua và nhận hàng thành công."));
    }

    @Test
    @WithMockUser(username = "user@example.com", roles = "USER")
    @DisplayName("Trả về 409 Conflict khi người dùng đã đánh giá sản phẩm trước đó")
    void createProductReview_WhenAlreadyReviewed_Returns409Conflict() throws Exception {
        UUID productId = UUID.randomUUID();
        CreateReviewRequest request = CreateReviewRequest.builder()
                .rating(5)
                .comment("Đánh giá lần hai")
                .build();

        when(reviewService.createProductReview(eq("user@example.com"), eq(productId), any(CreateReviewRequest.class)))
                .thenThrow(new DuplicateResourceException("Bạn đã đánh giá linh kiện này rồi."));

        mockMvc.perform(post("/api/products/{id}/reviews", productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Bạn đã đánh giá linh kiện này rồi."));
    }

    @Test
    @WithMockUser(username = "user@example.com", roles = "USER")
    @DisplayName("Trả về 404 Not Found khi linh kiện không tồn tại")
    void createProductReview_WhenProductNotFound_Returns404NotFound() throws Exception {
        UUID productId = UUID.randomUUID();
        CreateReviewRequest request = CreateReviewRequest.builder()
                .rating(5)
                .comment("Đánh giá sản phẩm ảo")
                .build();

        when(reviewService.createProductReview(eq("user@example.com"), eq(productId), any(CreateReviewRequest.class)))
                .thenThrow(new ResourceNotFoundException("Không tìm thấy linh kiện với ID: " + productId));

        mockMvc.perform(post("/api/products/{id}/reviews", productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Không tìm thấy linh kiện với ID: " + productId));
    }

    // ================== TESTS CHO PUT /api/products/{id}/reviews/{reviewId} ==================

    @Test
    @WithMockUser(username = "user@example.com", roles = "USER")
    @DisplayName("Cập nhật đánh giá thành công khi là chính chủ (200 OK)")
    void updateProductReview_AsOwner_Returns200Ok() throws Exception {
        UUID productId = UUID.randomUUID();
        UUID reviewId = UUID.randomUUID();
        UpdateReviewRequest request = UpdateReviewRequest.builder()
                .rating(4)
                .comment("Đã chỉnh sửa: rất bền")
                .build();

        ReviewResponse response = ReviewResponse.builder()
                .id(reviewId)
                .userId(UUID.randomUUID())
                .userEmail("user@example.com")
                .productId(productId)
                .rating(4)
                .comment("Đã chỉnh sửa: rất bền")
                .updatedAt(Instant.now())
                .build();

        when(reviewService.updateProductReview(eq("user@example.com"), eq(productId), eq(reviewId), any(UpdateReviewRequest.class)))
                .thenReturn(response);

        mockMvc.perform(put("/api/products/{id}/reviews/{reviewId}", productId, reviewId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Cập nhật đánh giá linh kiện thành công!"))
                .andExpect(jsonPath("$.data.id").value(reviewId.toString()))
                .andExpect(jsonPath("$.data.rating").value(4))
                .andExpect(jsonPath("$.data.comment").value("Đã chỉnh sửa: rất bền"));
    }

    @Test
    @DisplayName("Trả về 401 Unauthorized khi cập nhật đánh giá mà chưa đăng nhập")
    void updateProductReview_Unauthenticated_Returns401Unauthorized() throws Exception {
        UUID productId = UUID.randomUUID();
        UUID reviewId = UUID.randomUUID();
        UpdateReviewRequest request = UpdateReviewRequest.builder().rating(4).build();

        mockMvc.perform(put("/api/products/{id}/reviews/{reviewId}", productId, reviewId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "user@example.com", roles = "USER")
    @DisplayName("Trả về 400 Bad Request khi rating cập nhật không hợp lệ")
    void updateProductReview_InvalidRating_Returns400BadRequest() throws Exception {
        UUID productId = UUID.randomUUID();
        UUID reviewId = UUID.randomUUID();
        UpdateReviewRequest request = UpdateReviewRequest.builder().rating(0).build();

        mockMvc.perform(put("/api/products/{id}/reviews/{reviewId}", productId, reviewId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.rating").exists());
    }

    @Test
    @WithMockUser(username = "user@example.com", roles = "USER")
    @DisplayName("Trả về 403 Forbidden khi người dùng cố tình cập nhật đánh giá của người khác")
    void updateProductReview_WhenNotOwner_Returns403Forbidden() throws Exception {
        UUID productId = UUID.randomUUID();
        UUID reviewId = UUID.randomUUID();
        UpdateReviewRequest request = UpdateReviewRequest.builder().rating(4).build();

        when(reviewService.updateProductReview(eq("user@example.com"), eq(productId), eq(reviewId), any(UpdateReviewRequest.class)))
                .thenThrow(new AppException("Bạn không có quyền cập nhật đánh giá của người dùng khác.", HttpStatus.FORBIDDEN));

        mockMvc.perform(put("/api/products/{id}/reviews/{reviewId}", productId, reviewId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Bạn không có quyền cập nhật đánh giá của người dùng khác."));
    }

    @Test
    @WithMockUser(username = "user@example.com", roles = "USER")
    @DisplayName("Trả về 404 Not Found khi không tìm thấy đánh giá cần cập nhật")
    void updateProductReview_WhenNotFound_Returns404NotFound() throws Exception {
        UUID productId = UUID.randomUUID();
        UUID reviewId = UUID.randomUUID();
        UpdateReviewRequest request = UpdateReviewRequest.builder().rating(4).build();

        when(reviewService.updateProductReview(eq("user@example.com"), eq(productId), eq(reviewId), any(UpdateReviewRequest.class)))
                .thenThrow(new ResourceNotFoundException("Không tìm thấy đánh giá với ID: " + reviewId));

        mockMvc.perform(put("/api/products/{id}/reviews/{reviewId}", productId, reviewId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Không tìm thấy đánh giá với ID: " + reviewId));
    }

    @Test
    @WithMockUser(username = "user@example.com", roles = "USER")
    @DisplayName("Trả về 400 Bad Request khi review không thuộc sản phẩm trên URL")
    void updateProductReview_WhenMismatchProduct_Returns400BadRequest() throws Exception {
        UUID productId = UUID.randomUUID();
        UUID reviewId = UUID.randomUUID();
        UpdateReviewRequest request = UpdateReviewRequest.builder().rating(4).build();

        when(reviewService.updateProductReview(eq("user@example.com"), eq(productId), eq(reviewId), any(UpdateReviewRequest.class)))
                .thenThrow(new BadRequestException("Đánh giá này không thuộc về linh kiện đã chỉ định."));

        mockMvc.perform(put("/api/products/{id}/reviews/{reviewId}", productId, reviewId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Đánh giá này không thuộc về linh kiện đã chỉ định."));
    }
}
