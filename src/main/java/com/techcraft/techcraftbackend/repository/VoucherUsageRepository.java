package com.techcraft.techcraftbackend.repository;

import com.techcraft.techcraftbackend.entity.VoucherUsage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VoucherUsageRepository extends JpaRepository<VoucherUsage, UUID> {

    long countByVoucherIdAndUserId(UUID voucherId, UUID userId);

    Optional<VoucherUsage> findByOrderId(UUID orderId);

    void deleteByOrderId(UUID orderId);

    List<VoucherUsage> findByVoucherIdOrderByUsedAtDesc(UUID voucherId);

    Page<VoucherUsage> findByVoucherId(UUID voucherId, Pageable pageable);

    long countByVoucherId(UUID voucherId);

    @Query("SELECT COALESCE(SUM(vu.discountAmount), 0) FROM VoucherUsage vu WHERE vu.voucher.id = :voucherId")
    BigDecimal sumDiscountAmountByVoucherId(@Param("voucherId") UUID voucherId);
}
