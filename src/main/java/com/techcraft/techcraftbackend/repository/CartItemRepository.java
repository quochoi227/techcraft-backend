package com.techcraft.techcraftbackend.repository;

import com.techcraft.techcraftbackend.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, UUID> {

    Optional<CartItem> findByIdAndUserId(UUID id, UUID userId);

    Optional<CartItem> findByUserIdAndProductId(UUID userId, UUID productId);

    Optional<CartItem> findByUserIdAndPcCaseId(UUID userId, UUID pcCaseId);

    List<CartItem> findByUserIdOrderByCreatedAtDesc(UUID userId);

    @Query("SELECT c FROM CartItem c " +
           "LEFT JOIN FETCH c.product p " +
           "LEFT JOIN FETCH c.pcCase pc " +
           "WHERE c.user.id = :userId " +
           "ORDER BY c.createdAt DESC")
    List<CartItem> findByUserIdWithDetailsOrderByCreatedAtDesc(@Param("userId") UUID userId);

    void deleteByUserId(UUID userId);

    void deleteByUserIdAndProductId(UUID userId, UUID productId);

    void deleteByUserIdAndPcCaseId(UUID userId, UUID pcCaseId);
}
