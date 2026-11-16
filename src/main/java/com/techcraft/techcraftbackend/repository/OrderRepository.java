package com.techcraft.techcraftbackend.repository;

import com.techcraft.techcraftbackend.entity.Order;
import com.techcraft.techcraftbackend.enums.OrderStatus;
import com.techcraft.techcraftbackend.repository.projection.OrderRevenueProjection;
import com.techcraft.techcraftbackend.repository.projection.OrderStatusCountProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
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
           "LEFT JOIN FETCH o.voucher v " +
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

    @Query("SELECT COUNT(o) > 0 FROM Order o " +
           "JOIN o.items item " +
           "WHERE o.user.id = :userId " +
           "AND o.status = :status " +
           "AND item.pcCase.id = :pcCaseId")
    boolean hasUserPurchasedPcCase(@Param("userId") UUID userId,
                                  @Param("pcCaseId") UUID pcCaseId,
                                  @Param("status") OrderStatus status);

    @Query("SELECT o.status AS status, COUNT(o) AS count FROM Order o GROUP BY o.status")
    List<OrderStatusCountProjection> countOrdersByStatus();

    @Query("SELECT o.status AS status, COUNT(o) AS count FROM Order o " +
           "WHERE o.createdAt >= :startDate AND o.createdAt <= :endDate GROUP BY o.status")
    List<OrderStatusCountProjection> countOrdersByStatusBetween(
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate);

    @Query("SELECT o.status AS status, COUNT(o) AS count FROM Order o " +
           "WHERE o.createdAt >= :startDate GROUP BY o.status")
    List<OrderStatusCountProjection> countOrdersByStatusFrom(@Param("startDate") Instant startDate);

    @Query("SELECT o.status AS status, COUNT(o) AS count FROM Order o " +
           "WHERE o.createdAt <= :endDate GROUP BY o.status")
    List<OrderStatusCountProjection> countOrdersByStatusTo(@Param("endDate") Instant endDate);

    @Query("SELECT o.createdAt AS createdAt, o.totalAmount AS totalAmount FROM Order o " +
           "WHERE o.status = :status AND o.createdAt >= :startDate AND o.createdAt <= :endDate " +
           "ORDER BY o.createdAt ASC")
    List<OrderRevenueProjection> findRevenueOrdersBetween(
            @Param("status") OrderStatus status,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate);

    @Query("SELECT o.createdAt AS createdAt, o.totalAmount AS totalAmount FROM Order o " +
           "WHERE o.status = :status " +
           "ORDER BY o.createdAt ASC")
    List<OrderRevenueProjection> findAllRevenueOrders(
            @Param("status") OrderStatus status);
}
