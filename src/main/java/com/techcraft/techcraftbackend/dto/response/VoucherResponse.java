package com.techcraft.techcraftbackend.dto.response;

import com.techcraft.techcraftbackend.enums.DiscountType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoucherResponse {

    private UUID id;
    private String code;
    private String name;
    private String description;
    private DiscountType discountType;
    private BigDecimal discountValue;
    private BigDecimal maxDiscountAmount;
    private BigDecimal minOrderAmount;
    private Instant startDate;
    private Instant endDate;
    private Integer usageLimit;
    private Integer usedCount;
    private Integer remainingUses;
    private Integer userUsageLimit;
    private boolean active;
    private boolean valid;
    private Instant createdAt;
    private Instant updatedAt;
}
