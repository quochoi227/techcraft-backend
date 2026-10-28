package com.techcraft.techcraftbackend.controller;

import com.techcraft.techcraftbackend.dto.ApiResponse;
import com.techcraft.techcraftbackend.dto.request.CreateReviewRequest;
import com.techcraft.techcraftbackend.dto.request.UpdateReviewRequest;
import com.techcraft.techcraftbackend.dto.response.ReviewResponse;
import com.techcraft.techcraftbackend.exception.UnauthorizedException;
import com.techcraft.techcraftbackend.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping("/products/{id}/reviews")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ReviewResponse>> createProductReview(
            Principal principal,
            @PathVariable("id") UUID id,
            @Valid @RequestBody CreateReviewRequest request) {
        if (principal == null || principal.getName() == null) {
            throw new UnauthorizedException("Vui lòng đăng nhập để thực hiện chức năng này.");
        }

        ReviewResponse response = reviewService.createProductReview(principal.getName(), id, request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Đánh giá linh kiện thành công!", response));
    }

    @PutMapping("/products/{id}/reviews/{reviewId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ReviewResponse>> updateProductReview(
            Principal principal,
            @PathVariable("id") UUID id,
            @PathVariable("reviewId") UUID reviewId,
            @Valid @RequestBody UpdateReviewRequest request) {
        if (principal == null || principal.getName() == null) {
            throw new UnauthorizedException("Vui lòng đăng nhập để thực hiện chức năng này.");
        }

        ReviewResponse response = reviewService.updateProductReview(principal.getName(), id, reviewId, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật đánh giá linh kiện thành công!", response));
    }

    @DeleteMapping("/products/{id}/reviews/{reviewId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> deleteProductReview(
            Principal principal,
            @PathVariable("id") UUID id,
            @PathVariable("reviewId") UUID reviewId) {
        if (principal == null || principal.getName() == null) {
            throw new UnauthorizedException("Vui lòng đăng nhập để thực hiện chức năng này.");
        }

        reviewService.deleteProductReview(principal.getName(), id, reviewId);
        return ResponseEntity.ok(ApiResponse.success("Xoá đánh giá linh kiện thành công!", null));
    }

    @PostMapping("/pc-cases/{id}/reviews")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ReviewResponse>> createPcCaseReview(
            Principal principal,
            @PathVariable("id") UUID id,
            @Valid @RequestBody CreateReviewRequest request) {
        if (principal == null || principal.getName() == null) {
            throw new UnauthorizedException("Vui lòng đăng nhập để thực hiện chức năng này.");
        }

        ReviewResponse response = reviewService.createPcCaseReview(principal.getName(), id, request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Đánh giá cấu hình PC thành công!", response));
    }

    @PutMapping("/pc-cases/{id}/reviews/{reviewId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ReviewResponse>> updatePcCaseReview(
            Principal principal,
            @PathVariable("id") UUID id,
            @PathVariable("reviewId") UUID reviewId,
            @Valid @RequestBody UpdateReviewRequest request) {
        if (principal == null || principal.getName() == null) {
            throw new UnauthorizedException("Vui lòng đăng nhập để thực hiện chức năng này.");
        }

        ReviewResponse response = reviewService.updatePcCaseReview(principal.getName(), id, reviewId, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật đánh giá cấu hình PC thành công!", response));
    }
}
