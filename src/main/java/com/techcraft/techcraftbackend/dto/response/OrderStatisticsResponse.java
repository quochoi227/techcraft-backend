package com.techcraft.techcraftbackend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatisticsResponse {

    private Long totalOrders;
    private Long pendingCount;
    private Long confirmedCount;
    private Long shippingCount;
    private Long deliveredCount;
    private Long cancelledCount;
    private List<OrderStatusBreakdownResponse> breakdown;
}
