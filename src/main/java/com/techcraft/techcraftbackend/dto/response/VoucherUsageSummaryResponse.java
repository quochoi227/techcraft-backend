package com.techcraft.techcraftbackend.dto.response;

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
public class VoucherUsageSummaryResponse {

    private UUID id;
    private UUID orderId;
    private UUID userId;
    private String userEmail;
    private String userFullName;
    private BigDecimal discountAmount;
    private Instant usedAt;
}
