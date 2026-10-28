package com.techcraft.techcraftbackend.service;

import com.techcraft.techcraftbackend.dto.request.CreateReviewRequest;
import com.techcraft.techcraftbackend.dto.response.ReviewResponse;

import java.util.UUID;

public interface ReviewService {

    ReviewResponse createProductReview(String userEmail, UUID productId, CreateReviewRequest request);
}
