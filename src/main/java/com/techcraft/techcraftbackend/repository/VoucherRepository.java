package com.techcraft.techcraftbackend.repository;

import com.techcraft.techcraftbackend.entity.Voucher;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VoucherRepository extends JpaRepository<Voucher, UUID>, JpaSpecificationExecutor<Voucher> {

    Optional<Voucher> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM Voucher v WHERE UPPER(v.code) = UPPER(:code)")
    Optional<Voucher> findByCodeWithLock(@Param("code") String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM Voucher v WHERE v.id = :id")
    Optional<Voucher> findByIdWithLock(@Param("id") UUID id);

    @Query("SELECT v FROM Voucher v WHERE v.active = true AND v.startDate <= :now AND v.endDate >= :now AND (v.usageLimit IS NULL OR v.usedCount < v.usageLimit) ORDER BY v.createdAt DESC")
    List<Voucher> findAvailableVouchers(@Param("now") Instant now);
}
