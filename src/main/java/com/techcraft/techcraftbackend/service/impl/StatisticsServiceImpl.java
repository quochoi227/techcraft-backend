package com.techcraft.techcraftbackend.service.impl;

import com.techcraft.techcraftbackend.dto.request.OrderStatisticsFilterRequest;
import com.techcraft.techcraftbackend.dto.request.RevenueStatisticsFilterRequest;
import com.techcraft.techcraftbackend.dto.request.UserStatisticsFilterRequest;
import com.techcraft.techcraftbackend.dto.response.*;
import com.techcraft.techcraftbackend.enums.OrderStatus;
import com.techcraft.techcraftbackend.enums.TimePeriod;
import com.techcraft.techcraftbackend.exception.BadRequestException;
import com.techcraft.techcraftbackend.repository.OrderRepository;
import com.techcraft.techcraftbackend.repository.UserRepository;
import com.techcraft.techcraftbackend.repository.projection.OrderRevenueProjection;
import com.techcraft.techcraftbackend.repository.projection.OrderStatusCountProjection;
import com.techcraft.techcraftbackend.service.StatisticsService;
import com.techcraft.techcraftbackend.util.DateFilterUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatisticsServiceImpl implements StatisticsService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

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

    @Override
    public RevenueStatisticsResponse getRevenueStatistics(RevenueStatisticsFilterRequest filter) {
        TimePeriod period = (filter != null && filter.getPeriod() != null) ? filter.getPeriod() : TimePeriod.DAY;
        Instant startDate = filter != null ? filter.getParsedStartDate() : null;
        Instant endDate = filter != null ? filter.getParsedEndDate() : null;

        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new BadRequestException("Thời gian bắt đầu không được sau thời gian kết thúc.");
        }

        // Nếu người dùng không truyền khoảng ngày, áp dụng khoảng mặc định theo period
        if (startDate == null && endDate == null) {
            ZonedDateTime now = ZonedDateTime.now(DateFilterUtil.DEFAULT_ZONE);
            switch (period) {
                case DAY -> {
                    startDate = now.minusDays(29).toLocalDate().atStartOfDay(DateFilterUtil.DEFAULT_ZONE).toInstant();
                    endDate = now.toLocalDate().atTime(LocalTime.MAX).atZone(DateFilterUtil.DEFAULT_ZONE).toInstant();
                }
                case MONTH -> {
                    startDate = now.minusMonths(11).withDayOfMonth(1).toLocalDate().atStartOfDay(DateFilterUtil.DEFAULT_ZONE).toInstant();
                    endDate = now.toLocalDate().atTime(LocalTime.MAX).atZone(DateFilterUtil.DEFAULT_ZONE).toInstant();
                }
                case YEAR -> {
                    startDate = now.minusYears(4).withDayOfYear(1).toLocalDate().atStartOfDay(DateFilterUtil.DEFAULT_ZONE).toInstant();
                    endDate = now.toLocalDate().atTime(LocalTime.MAX).atZone(DateFilterUtil.DEFAULT_ZONE).toInstant();
                }
            }
        }

        List<OrderRevenueProjection> orders;
        if (startDate != null && endDate != null) {
            orders = orderRepository.findRevenueOrdersBetween(OrderStatus.DELIVERED, startDate, endDate);
        } else {
            orders = orderRepository.findAllRevenueOrders(OrderStatus.DELIVERED);
        }

        // Tạo khung timeline với điểm 0 (zero-filling)
        Map<String, RevenueTimelinePointResponse> timelineMap = generateRevenueTimelineTemplate(period, startDate, endDate);

        // Bổ sung doanh thu thực tế từ database vào timeline
        if (orders != null) {
            for (OrderRevenueProjection order : orders) {
                if (order != null && order.getCreatedAt() != null && order.getTotalAmount() != null) {
                    String key = formatInstantToPeriodKey(order.getCreatedAt(), period);
                    RevenueTimelinePointResponse point = timelineMap.get(key);
                    if (point != null) {
                        point.setRevenue(point.getRevenue().add(order.getTotalAmount()));
                        point.setOrderCount(point.getOrderCount() + 1);
                    }
                }
            }
        }

        List<RevenueTimelinePointResponse> timeline = new ArrayList<>(timelineMap.values());

        BigDecimal totalRevenue = timeline.stream()
                .map(RevenueTimelinePointResponse::getRevenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long totalOrders = timeline.stream()
                .mapToLong(RevenueTimelinePointResponse::getOrderCount)
                .sum();

        BigDecimal averageOrderValue = totalOrders > 0
                ? totalRevenue.divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return RevenueStatisticsResponse.builder()
                .totalRevenue(totalRevenue)
                .totalOrders(totalOrders)
                .averageOrderValue(averageOrderValue)
                .period(period)
                .timeline(timeline)
                .build();
    }

    @Override
    public UserStatisticsResponse getUserStatistics(UserStatisticsFilterRequest filter) {
        TimePeriod period = (filter != null && filter.getPeriod() != null) ? filter.getPeriod() : TimePeriod.DAY;
        Instant startDate = filter != null ? filter.getParsedStartDate() : null;
        Instant endDate = filter != null ? filter.getParsedEndDate() : null;

        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new BadRequestException("Thời gian bắt đầu không được sau thời gian kết thúc.");
        }

        // Khoảng mặc định nếu không truyền
        if (startDate == null && endDate == null) {
            ZonedDateTime now = ZonedDateTime.now(DateFilterUtil.DEFAULT_ZONE);
            switch (period) {
                case DAY -> {
                    startDate = now.minusDays(29).toLocalDate().atStartOfDay(DateFilterUtil.DEFAULT_ZONE).toInstant();
                    endDate = now.toLocalDate().atTime(LocalTime.MAX).atZone(DateFilterUtil.DEFAULT_ZONE).toInstant();
                }
                case MONTH -> {
                    startDate = now.minusMonths(11).withDayOfMonth(1).toLocalDate().atStartOfDay(DateFilterUtil.DEFAULT_ZONE).toInstant();
                    endDate = now.toLocalDate().atTime(LocalTime.MAX).atZone(DateFilterUtil.DEFAULT_ZONE).toInstant();
                }
                case YEAR -> {
                    startDate = now.minusYears(4).withDayOfYear(1).toLocalDate().atStartOfDay(DateFilterUtil.DEFAULT_ZONE).toInstant();
                    endDate = now.toLocalDate().atTime(LocalTime.MAX).atZone(DateFilterUtil.DEFAULT_ZONE).toInstant();
                }
            }
        }

        List<Instant> createdDates;
        if (startDate != null && endDate != null) {
            createdDates = userRepository.findUserCreatedAtBetween(startDate, endDate);
        } else {
            createdDates = userRepository.findAllUserCreatedAt();
        }

        Map<String, UserTimelinePointResponse> timelineMap = generateUserTimelineTemplate(period, startDate, endDate);

        if (createdDates != null) {
            for (Instant createdAt : createdDates) {
                if (createdAt != null) {
                    String key = formatInstantToPeriodKey(createdAt, period);
                    UserTimelinePointResponse point = timelineMap.get(key);
                    if (point != null) {
                        point.setNewUsersCount(point.getNewUsersCount() + 1);
                    }
                }
            }
        }

        List<UserTimelinePointResponse> timeline = new ArrayList<>(timelineMap.values());
        long newUsersInPeriod = timeline.stream().mapToLong(UserTimelinePointResponse::getNewUsersCount).sum();

        return UserStatisticsResponse.builder()
                .totalUsers(userRepository.count())
                .newUsersInPeriod(newUsersInPeriod)
                .activeUsers(userRepository.countByActiveTrue())
                .verifiedUsers(userRepository.countByEmailVerifiedTrue())
                .period(period)
                .timeline(timeline)
                .build();
    }

    private Map<String, RevenueTimelinePointResponse> generateRevenueTimelineTemplate(
            TimePeriod period, Instant startDate, Instant endDate) {
        Map<String, RevenueTimelinePointResponse> map = new LinkedHashMap<>();

        if (startDate == null || endDate == null) {
            return map;
        }

        ZonedDateTime start = startDate.atZone(DateFilterUtil.DEFAULT_ZONE);
        ZonedDateTime end = endDate.atZone(DateFilterUtil.DEFAULT_ZONE);

        switch (period) {
            case DAY -> {
                LocalDate cur = start.toLocalDate();
                LocalDate last = end.toLocalDate();
                DateTimeFormatter labelFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                while (!cur.isAfter(last)) {
                    String key = cur.toString();
                    map.put(key, RevenueTimelinePointResponse.builder()
                            .date(key)
                            .label(cur.format(labelFmt))
                            .revenue(BigDecimal.ZERO)
                            .orderCount(0L)
                            .build());
                    cur = cur.plusDays(1);
                }
            }
            case MONTH -> {
                YearMonth cur = YearMonth.from(start);
                YearMonth last = YearMonth.from(end);
                DateTimeFormatter labelFmt = DateTimeFormatter.ofPattern("'Tháng' MM/yyyy");
                while (!cur.isAfter(last)) {
                    String key = cur.toString();
                    map.put(key, RevenueTimelinePointResponse.builder()
                            .date(key)
                            .label(cur.format(labelFmt))
                            .revenue(BigDecimal.ZERO)
                            .orderCount(0L)
                            .build());
                    cur = cur.plusMonths(1);
                }
            }
            case YEAR -> {
                int cur = start.getYear();
                int last = end.getYear();
                while (cur <= last) {
                    String key = String.valueOf(cur);
                    map.put(key, RevenueTimelinePointResponse.builder()
                            .date(key)
                            .label("Năm " + cur)
                            .revenue(BigDecimal.ZERO)
                            .orderCount(0L)
                            .build());
                    cur++;
                }
            }
        }

        return map;
    }

    private Map<String, UserTimelinePointResponse> generateUserTimelineTemplate(
            TimePeriod period, Instant startDate, Instant endDate) {
        Map<String, UserTimelinePointResponse> map = new LinkedHashMap<>();

        if (startDate == null || endDate == null) {
            return map;
        }

        ZonedDateTime start = startDate.atZone(DateFilterUtil.DEFAULT_ZONE);
        ZonedDateTime end = endDate.atZone(DateFilterUtil.DEFAULT_ZONE);

        switch (period) {
            case DAY -> {
                LocalDate cur = start.toLocalDate();
                LocalDate last = end.toLocalDate();
                DateTimeFormatter labelFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                while (!cur.isAfter(last)) {
                    String key = cur.toString();
                    map.put(key, UserTimelinePointResponse.builder()
                            .date(key)
                            .label(cur.format(labelFmt))
                            .newUsersCount(0L)
                            .build());
                    cur = cur.plusDays(1);
                }
            }
            case MONTH -> {
                YearMonth cur = YearMonth.from(start);
                YearMonth last = YearMonth.from(end);
                DateTimeFormatter labelFmt = DateTimeFormatter.ofPattern("'Tháng' MM/yyyy");
                while (!cur.isAfter(last)) {
                    String key = cur.toString();
                    map.put(key, UserTimelinePointResponse.builder()
                            .date(key)
                            .label(cur.format(labelFmt))
                            .newUsersCount(0L)
                            .build());
                    cur = cur.plusMonths(1);
                }
            }
            case YEAR -> {
                int cur = start.getYear();
                int last = end.getYear();
                while (cur <= last) {
                    String key = String.valueOf(cur);
                    map.put(key, UserTimelinePointResponse.builder()
                            .date(key)
                            .label("Năm " + cur)
                            .newUsersCount(0L)
                            .build());
                    cur++;
                }
            }
        }

        return map;
    }

    private String formatInstantToPeriodKey(Instant instant, TimePeriod period) {
        ZonedDateTime zdt = instant.atZone(DateFilterUtil.DEFAULT_ZONE);
        return switch (period) {
            case DAY -> zdt.toLocalDate().toString();
            case MONTH -> YearMonth.from(zdt).toString();
            case YEAR -> String.valueOf(zdt.getYear());
        };
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
