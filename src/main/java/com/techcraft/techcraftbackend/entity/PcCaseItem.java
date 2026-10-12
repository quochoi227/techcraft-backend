package com.techcraft.techcraftbackend.entity;

import com.techcraft.techcraftbackend.enums.ProductCategory;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(
        name = "pc_case_items",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_pc_case_items_case_category", columnNames = {"pc_case_id", "category"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PcCaseItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pc_case_id", nullable = false)
    private PcCase pcCase;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private ProductCategory category;
}
