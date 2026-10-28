package com.techcraft.techcraftbackend.service.impl;

import com.techcraft.techcraftbackend.dto.request.CreateReviewRequest;
import com.techcraft.techcraftbackend.dto.request.UpdateReviewRequest;
import com.techcraft.techcraftbackend.dto.response.ReviewResponse;
import com.techcraft.techcraftbackend.entity.Product;
import com.techcraft.techcraftbackend.entity.Review;
import com.techcraft.techcraftbackend.entity.User;
import com.techcraft.techcraftbackend.enums.OrderStatus;
import com.techcraft.techcraftbackend.enums.Role;
import com.techcraft.techcraftbackend.exception.*;
import com.techcraft.techcraftbackend.mapper.ReviewMapper;
import com.techcraft.techcraftbackend.repository.OrderRepository;
import com.techcraft.techcraftbackend.repository.ProductRepository;
import com.techcraft.techcraftbackend.repository.ReviewRepository;
import com.techcraft.techcraftbackend.repository.UserRepository;
import com.techcraft.techcraftbackend.service.ReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ReviewMapper reviewMapper;

    @Override
    @Transactional
    public ReviewResponse createProductReview(String userEmail, UUID productId, CreateReviewRequest request) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UnauthorizedException("Vui lòng đăng nhập để thực hiện chức năng này."));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy linh kiện với ID: " + productId));

        if (!product.isActive() && user.getRole() != Role.ADMIN) {
            throw new ResourceNotFoundException("Không tìm thấy linh kiện với ID: " + productId);
        }

        if (reviewRepository.existsByUserIdAndProductId(user.getId(), productId)) {
            throw new DuplicateResourceException("Bạn đã đánh giá linh kiện này rồi.");
        }

        boolean hasPurchased = orderRepository.hasUserPurchasedProduct(user.getId(), productId, OrderStatus.DELIVERED);
        if (!hasPurchased) {
            throw new BadRequestException("Bạn chỉ có thể đánh giá linh kiện sau khi đã mua và nhận hàng thành công.");
        }

        String comment = (request.getComment() != null && !request.getComment().trim().isEmpty())
                ? request.getComment().trim()
                : null;

        Review review = Review.builder()
                .user(user)
                .rating(request.getRating())
                .comment(comment)
                .build();

        product.addReview(review);
        Review savedReview = reviewRepository.save(review);
        log.info("User {} successfully reviewed product {} with rating {}", user.getEmail(), product.getId(), request.getRating());

        return reviewMapper.toResponse(savedReview);
    }

    @Override
    @Transactional
    public ReviewResponse updateProductReview(String userEmail, UUID productId, UUID reviewId, UpdateReviewRequest request) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UnauthorizedException("Vui lòng đăng nhập để thực hiện chức năng này."));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy linh kiện với ID: " + productId));

        if (!product.isActive() && user.getRole() != Role.ADMIN) {
            throw new ResourceNotFoundException("Không tìm thấy linh kiện với ID: " + productId);
        }

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đánh giá với ID: " + reviewId));

        if (review.getProduct() == null || !review.getProduct().getId().equals(productId)) {
            throw new BadRequestException("Đánh giá này không thuộc về linh kiện đã chỉ định.");
        }

        if (!review.getUser().getId().equals(user.getId())) {
            throw new AppException("Bạn không có quyền cập nhật đánh giá của người dùng khác.", HttpStatus.FORBIDDEN);
        }

        String comment = (request.getComment() != null && !request.getComment().trim().isEmpty())
                ? request.getComment().trim()
                : null;

        review.setRating(request.getRating());
        review.setComment(comment);

        Review updatedReview = reviewRepository.save(review);
        log.info("User {} successfully updated review {} for product {}", user.getEmail(), reviewId, productId);

        return reviewMapper.toResponse(updatedReview);
    }
}
