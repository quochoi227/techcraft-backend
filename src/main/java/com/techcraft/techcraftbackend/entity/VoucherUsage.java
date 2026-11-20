package com.techcraft.techcraftbackend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "voucher_usages",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_voucher_usage_user_voucher_order", columnNames = {"user_id", "voucher_id", "order_id"})
        },
        indexes = {
                @Index(name = "idx_voucher_usages_user_voucher", columnList = "user_id, voucher_id"),
                @Index(name = "idx_voucher_usages_order_id", columnList = "order_id"),
                @Index(name = "idx_voucher_usages_voucher_id", columnList = "voucher_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VoucherUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voucher_id", nullable = false)
    private Voucher voucher;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "discount_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal discountAmount;

    @CreationTimestamp
    @Column(name = "used_at", updatable = false)
    private Instant usedAt;
}
