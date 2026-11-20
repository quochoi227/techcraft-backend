package com.techcraft.techcraftbackend.repository;

import com.techcraft.techcraftbackend.entity.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, UUID> {

    boolean existsBySepayTransactionId(Long sepayTransactionId);

    Optional<PaymentTransaction> findBySepayTransactionId(Long sepayTransactionId);

    List<PaymentTransaction> findByOrderIdOrderByCreatedAtDesc(UUID orderId);
}
