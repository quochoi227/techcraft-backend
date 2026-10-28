package com.techcraft.techcraftbackend.repository;

import com.techcraft.techcraftbackend.entity.Order;
import com.techcraft.techcraftbackend.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID>, JpaSpecificationExecutor<Order> {

    List<Order> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Page<Order> findByUserId(UUID userId, Pageable pageable);

    Page<Order> findByUserIdAndStatus(UUID userId, OrderStatus status, Pageable pageable);

    @Query("SELECT o FROM Order o " +
           "JOIN FETCH o.user u " +
           "LEFT JOIN FETCH o.items i " +
           "LEFT JOIN FETCH i.product p " +
           "LEFT JOIN FETCH i.pcCase pc " +
           "WHERE o.id = :orderId")
    Optional<Order> findByIdWithDetails(@Param("orderId") UUID orderId);

    @Query("SELECT COUNT(o) > 0 FROM Order o " +
           "JOIN o.items item " +
           "WHERE o.user.id = :userId " +
           "AND o.status = :status " +
           "AND (item.product.id = :productId OR EXISTS (" +
           "    SELECT pci FROM PcCaseItem pci WHERE pci.pcCase = item.pcCase AND pci.product.id = :productId" +
           "))")
    boolean hasUserPurchasedProduct(@Param("userId") UUID userId,
                                   @Param("productId") UUID productId,
                                   @Param("status") OrderStatus status);
}
