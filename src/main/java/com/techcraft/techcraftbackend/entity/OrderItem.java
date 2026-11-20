package com.techcraft.techcraftbackend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(
        name = "order_items",
        indexes = {
                @Index(name = "idx_order_items_order_id", columnList = "order_id"),
                @Index(name = "idx_order_items_product_id", columnList = "product_id"),
                @Index(name = "idx_order_items_pc_case_id", columnList = "pc_case_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pc_case_id")
    private PcCase pcCase;

    @Column(name = "quantity", nullable = false)
    @Builder.Default
    private Integer quantity = 1;

    @Column(name = "unit_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal unitPrice;

    @PrePersist
    @PreUpdate
    private void validateItem() {
        if ((product == null && pcCase == null) || (product != null && pcCase != null)) {
            throw new IllegalStateException("Mỗi mục đơn hàng chỉ được là linh kiện lẻ hoặc cấu hình PC, không được chứa cả hai hoặc để trống cả hai.");
        }
        if (quantity == null || quantity < 1) {
            throw new IllegalStateException("Số lượng mục đơn hàng phải lớn hơn hoặc bằng 1.");
        }
    }
}
