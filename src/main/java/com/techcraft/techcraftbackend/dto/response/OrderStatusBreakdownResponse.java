package com.techcraft.techcraftbackend.dto.response;

import com.techcraft.techcraftbackend.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusBreakdownResponse {

    private OrderStatus status;
    private String statusName;
    private Long count;
    private Double percentage;
}
