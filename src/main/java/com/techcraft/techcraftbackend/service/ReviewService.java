package com.techcraft.techcraftbackend.service;

import com.techcraft.techcraftbackend.dto.request.CreateReviewRequest;
import com.techcraft.techcraftbackend.dto.request.UpdateReviewRequest;
import com.techcraft.techcraftbackend.dto.response.ReviewResponse;

import java.util.UUID;

public interface ReviewService {

    ReviewResponse createProductReview(String userEmail, UUID productId, CreateReviewRequest request);

    ReviewResponse updateProductReview(String userEmail, UUID productId, UUID reviewId, UpdateReviewRequest request);

    void deleteProductReview(String userEmail, UUID productId, UUID reviewId);

    ReviewResponse createPcCaseReview(String userEmail, UUID pcCaseId, CreateReviewRequest request);

    ReviewResponse updatePcCaseReview(String userEmail, UUID pcCaseId, UUID reviewId, UpdateReviewRequest request);

    void deletePcCaseReview(String userEmail, UUID pcCaseId, UUID reviewId);
}
