package com.techcraft.techcraftbackend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "reviews",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_reviews_user_product", columnNames = {"user_id", "product_id"}),
                @UniqueConstraint(name = "uk_reviews_user_pc_case", columnNames = {"user_id", "pc_case_id"})
        },
        indexes = {
                @Index(name = "idx_reviews_user_id", columnList = "user_id"),
                @Index(name = "idx_reviews_product_id", columnList = "product_id"),
                @Index(name = "idx_reviews_pc_case_id", columnList = "pc_case_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pc_case_id")
    private PcCase pcCase;

    @Column(name = "rating", nullable = false)
    private Integer rating;

    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    private void validateReview() {
        if ((product == null && pcCase == null) || (product != null && pcCase != null)) {
            throw new IllegalStateException("Đánh giá phải thuộc về linh kiện hoặc cấu hình PC, không được chứa cả hai hoặc để trống cả hai.");
        }
        if (rating == null || rating < 1 || rating > 5) {
            throw new IllegalStateException("Điểm đánh giá phải từ 1 đến 5 sao.");
        }
    }
}
