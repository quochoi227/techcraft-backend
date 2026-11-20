package com.techcraft.techcraftbackend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoucherDetailResponse {

    private VoucherResponse voucher;
    private BigDecimal totalDiscountGiven;
    private long totalOrdersCount;
    private List<VoucherUsageSummaryResponse> recentUsages;
}
