package com.techcraft.techcraftbackend.service.impl;

import com.techcraft.techcraftbackend.dto.request.OrderStatisticsFilterRequest;
import com.techcraft.techcraftbackend.dto.response.OrderStatisticsResponse;
import com.techcraft.techcraftbackend.dto.response.OrderStatusBreakdownResponse;
import com.techcraft.techcraftbackend.enums.OrderStatus;
import com.techcraft.techcraftbackend.exception.BadRequestException;
import com.techcraft.techcraftbackend.repository.OrderRepository;
import com.techcraft.techcraftbackend.repository.projection.OrderStatusCountProjection;
import com.techcraft.techcraftbackend.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatisticsServiceImpl implements StatisticsService {

    private final OrderRepository orderRepository;

    @Override
    public OrderStatisticsResponse getOrderStatistics(OrderStatisticsFilterRequest filter) {
        Instant startDate = null;
        Instant endDate = null;

        if (filter != null) {
            startDate = filter.getParsedStartDate();
            endDate = filter.getParsedEndDate();
        }

        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new BadRequestException("Thời gian bắt đầu không được sau thời gian kết thúc.");
        }

        List<OrderStatusCountProjection> dbCounts;
        if (startDate != null && endDate != null) {
            dbCounts = orderRepository.countOrdersByStatusBetween(startDate, endDate);
        } else if (startDate != null) {
            dbCounts = orderRepository.countOrdersByStatusFrom(startDate);
        } else if (endDate != null) {
            dbCounts = orderRepository.countOrdersByStatusTo(endDate);
        } else {
            dbCounts = orderRepository.countOrdersByStatus();
        }

        Map<OrderStatus, Long> countsMap = new EnumMap<>(OrderStatus.class);
        for (OrderStatus status : OrderStatus.values()) {
            countsMap.put(status, 0L);
        }

        if (dbCounts != null) {
            for (OrderStatusCountProjection item : dbCounts) {
                if (item != null && item.getStatus() != null) {
                    countsMap.put(item.getStatus(), item.getCount() != null ? item.getCount() : 0L);
                }
            }
        }

        long totalOrders = countsMap.values().stream().mapToLong(Long::longValue).sum();

        List<OrderStatusBreakdownResponse> breakdown = new ArrayList<>();
        for (OrderStatus status : OrderStatus.values()) {
            long count = countsMap.get(status);
            double percentage = totalOrders > 0
                    ? Math.round(((double) count / totalOrders * 100.0) * 100.0) / 100.0
                    : 0.0;

            breakdown.add(OrderStatusBreakdownResponse.builder()
                    .status(status)
                    .statusName(getStatusDisplayName(status))
                    .count(count)
                    .percentage(percentage)
                    .build());
        }

        return OrderStatisticsResponse.builder()
                .totalOrders(totalOrders)
                .pendingCount(countsMap.get(OrderStatus.PENDING))
                .confirmedCount(countsMap.get(OrderStatus.CONFIRMED))
                .shippingCount(countsMap.get(OrderStatus.SHIPPING))
                .deliveredCount(countsMap.get(OrderStatus.DELIVERED))
                .cancelledCount(countsMap.get(OrderStatus.CANCELLED))
                .breakdown(breakdown)
                .build();
    }

    private String getStatusDisplayName(OrderStatus status) {
        return switch (status) {
            case PENDING -> "Chờ xác nhận";
            case CONFIRMED -> "Đã xác nhận";
            case SHIPPING -> "Đang giao hàng";
            case DELIVERED -> "Đã giao hàng";
            case CANCELLED -> "Đã hủy";
        };
    }
}
