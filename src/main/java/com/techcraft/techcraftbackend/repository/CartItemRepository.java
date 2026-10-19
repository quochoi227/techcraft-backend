package com.techcraft.techcraftbackend.repository;

import com.techcraft.techcraftbackend.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, UUID> {

    Optional<CartItem> findByUserIdAndProductId(UUID userId, UUID productId);

    Optional<CartItem> findByUserIdAndPcCaseId(UUID userId, UUID pcCaseId);

    List<CartItem> findByUserIdOrderByCreatedAtDesc(UUID userId);

    void deleteByUserId(UUID userId);

    void deleteByUserIdAndProductId(UUID userId, UUID productId);
}
