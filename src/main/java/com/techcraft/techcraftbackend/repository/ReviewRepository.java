package com.techcraft.techcraftbackend.repository;

import com.techcraft.techcraftbackend.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReviewRepository extends JpaRepository<Review, UUID> {

    boolean existsByUserIdAndProductId(UUID userId, UUID productId);

    Optional<Review> findByUserIdAndProductId(UUID userId, UUID productId);

    List<Review> findByProductIdOrderByCreatedAtDesc(UUID productId);

    Page<Review> findByProductId(UUID productId, Pageable pageable);

    boolean existsByUserIdAndPcCaseId(UUID userId, UUID pcCaseId);

    Optional<Review> findByUserIdAndPcCaseId(UUID userId, UUID pcCaseId);

    List<Review> findByPcCaseIdOrderByCreatedAtDesc(UUID pcCaseId);

    Page<Review> findByPcCaseId(UUID pcCaseId, Pageable pageable);
}
