package com.techcraft.techcraftbackend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payment_transactions", indexes = {
        @Index(name = "idx_payment_sepay_id", columnList = "sepay_transaction_id"),
        @Index(name = "idx_payment_order_id", columnList = "order_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @Column(name = "sepay_transaction_id", unique = true)
    private Long sepayTransactionId;

    @Column(name = "gateway", length = 50)
    private String gateway;

    @Column(name = "transaction_date", length = 50)
    private String transactionDate;

    @Column(name = "account_number", length = 50)
    private String accountNumber;

    @Column(name = "sub_account", length = 50)
    private String subAccount;

    @Column(name = "transfer_amount", precision = 15, scale = 2)
    private BigDecimal transferAmount;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "reference_code", length = 100)
    private String referenceCode;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
