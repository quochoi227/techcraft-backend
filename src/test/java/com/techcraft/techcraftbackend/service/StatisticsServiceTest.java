package com.techcraft.techcraftbackend.service;

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
import com.techcraft.techcraftbackend.service.impl.StatisticsServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StatisticsServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private StatisticsServiceImpl statisticsService;

    private OrderStatusCountProjection createOrderStatusProjection(OrderStatus status, Long count) {
        return new OrderStatusCountProjection() {
            @Override
            public OrderStatus getStatus() {
                return status;
            }

            @Override
            public Long getCount() {
                return count;
            }
        };
    }

    private OrderRevenueProjection createRevenueProjection(Instant createdAt, BigDecimal totalAmount) {
        return new OrderRevenueProjection() {
            @Override
            public Instant getCreatedAt() {
                return createdAt;
            }

            @Override
            public BigDecimal getTotalAmount() {
                return totalAmount;
            }
        };
    }

    // ==========================================
    // 1. Tests cho Thống kê số lượng đơn hàng theo trạng thái
    // ==========================================

    @Test
    @DisplayName("Thống kê đơn hàng toàn thời gian thành công khi không truyền ngày")
    void getOrderStatistics_AllTime_Success() {
        List<OrderStatusCountProjection> mockDbCounts = List.of(
                createOrderStatusProjection(OrderStatus.PENDING, 5L),
                createOrderStatusProjection(OrderStatus.CONFIRMED, 10L),
                createOrderStatusProjection(OrderStatus.SHIPPING, 15L),
                createOrderStatusProjection(OrderStatus.DELIVERED, 20L)
        );

        when(orderRepository.countOrdersByStatus()).thenReturn(mockDbCounts);

        OrderStatisticsResponse response = statisticsService.getOrderStatistics(null);

        assertNotNull(response);
        assertEquals(50L, response.getTotalOrders());
        assertEquals(5L, response.getPendingCount());
        assertEquals(10L, response.getConfirmedCount());
        assertEquals(15L, response.getShippingCount());
        assertEquals(20L, response.getDeliveredCount());
        assertEquals(0L, response.getCancelledCount());

        assertEquals(5, response.getBreakdown().size());

        OrderStatusBreakdownResponse deliveredBreakdown = response.getBreakdown().stream()
                .filter(b -> b.getStatus() == OrderStatus.DELIVERED)
                .findFirst()
                .orElse(null);
        assertNotNull(deliveredBreakdown);
        assertEquals("Đã giao hàng", deliveredBreakdown.getStatusName());
        assertEquals(20L, deliveredBreakdown.getCount());
        assertEquals(40.0, deliveredBreakdown.getPercentage());

        verify(orderRepository, times(1)).countOrdersByStatus();
    }

    @Test
    @DisplayName("Thống kê đơn hàng ném BadRequestException khi startDate sau endDate")
    void getOrderStatistics_StartDateAfterEndDate_ThrowsBadRequestException() {
        OrderStatisticsFilterRequest filter = OrderStatisticsFilterRequest.builder()
                .startDate("2026-11-20")
                .endDate("2026-11-10")
                .build();

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> statisticsService.getOrderStatistics(filter));

        assertEquals("Thời gian bắt đầu không được sau thời gian kết thúc.", ex.getMessage());
        verifyNoInteractions(orderRepository);
    }

    // ==========================================
    // 2. Tests cho Thống kê Doanh thu
    // ==========================================

    @Test
    @DisplayName("Thống kê doanh thu theo ngày thành công kèm bù 0 cho ngày trống")
    void getRevenueStatistics_DayPeriod_Success() {
        RevenueStatisticsFilterRequest filter = RevenueStatisticsFilterRequest.builder()
                .period(TimePeriod.DAY)
                .startDate("2026-11-01")
                .endDate("2026-11-03")
                .build();

        Instant day1 = ZonedDateTime.of(2026, 11, 1, 10, 0, 0, 0, ZoneId.of("Asia/Ho_Chi_Minh")).toInstant();
        Instant day3 = ZonedDateTime.of(2026, 11, 3, 15, 30, 0, 0, ZoneId.of("Asia/Ho_Chi_Minh")).toInstant();

        List<OrderRevenueProjection> orders = List.of(
                createRevenueProjection(day1, BigDecimal.valueOf(1000000)),
                createRevenueProjection(day3, BigDecimal.valueOf(2000000))
        );

        when(orderRepository.findRevenueOrdersBetween(eq(OrderStatus.DELIVERED), any(Instant.class), any(Instant.class)))
                .thenReturn(orders);

        RevenueStatisticsResponse response = statisticsService.getRevenueStatistics(filter);

        assertNotNull(response);
        assertEquals(BigDecimal.valueOf(3000000), response.getTotalRevenue());
        assertEquals(2L, response.getTotalOrders());
        assertEquals(BigDecimal.valueOf(1500000).setScale(2), response.getAverageOrderValue());
        assertEquals(3, response.getTimeline().size()); // 01, 02 (0đ), 03

        assertEquals(BigDecimal.valueOf(1000000), response.getTimeline().get(0).getRevenue());
        assertEquals(1L, response.getTimeline().get(0).getOrderCount());

        assertEquals(BigDecimal.ZERO, response.getTimeline().get(1).getRevenue());
        assertEquals(0L, response.getTimeline().get(1).getOrderCount());

        assertEquals(BigDecimal.valueOf(2000000), response.getTimeline().get(2).getRevenue());
        assertEquals(1L, response.getTimeline().get(2).getOrderCount());
    }

    @Test
    @DisplayName("Thống kê doanh thu ném BadRequestException khi startDate sau endDate")
    void getRevenueStatistics_StartDateAfterEndDate_ThrowsBadRequestException() {
        RevenueStatisticsFilterRequest filter = RevenueStatisticsFilterRequest.builder()
                .startDate("2026-12-01")
                .endDate("2026-11-01")
                .build();

        assertThrows(BadRequestException.class, () -> statisticsService.getRevenueStatistics(filter));
    }

    @Test
    @DisplayName("Thống kê doanh thu khi không có đơn hàng nào trả về 0đ và AOV 0đ")
    void getRevenueStatistics_EmptyOrders_ReturnsZeros() {
        RevenueStatisticsFilterRequest filter = RevenueStatisticsFilterRequest.builder()
                .period(TimePeriod.DAY)
                .startDate("2026-11-01")
                .endDate("2026-11-02")
                .build();

        when(orderRepository.findRevenueOrdersBetween(eq(OrderStatus.DELIVERED), any(Instant.class), any(Instant.class)))
                .thenReturn(Collections.emptyList());

        RevenueStatisticsResponse response = statisticsService.getRevenueStatistics(filter);

        assertNotNull(response);
        assertEquals(BigDecimal.ZERO, response.getTotalRevenue());
        assertEquals(0L, response.getTotalOrders());
        assertEquals(BigDecimal.ZERO, response.getAverageOrderValue());
        assertEquals(2, response.getTimeline().size());
    }

    // ==========================================
    // 3. Tests cho Thống kê người dùng mới
    // ==========================================

    @Test
    @DisplayName("Thống kê người dùng mới thành công")
    void getUserStatistics_Success() {
        UserStatisticsFilterRequest filter = UserStatisticsFilterRequest.builder()
                .period(TimePeriod.DAY)
                .startDate("2026-11-01")
                .endDate("2026-11-02")
                .build();

        Instant u1 = ZonedDateTime.of(2026, 11, 1, 9, 0, 0, 0, ZoneId.of("Asia/Ho_Chi_Minh")).toInstant();
        Instant u2 = ZonedDateTime.of(2026, 11, 1, 14, 0, 0, 0, ZoneId.of("Asia/Ho_Chi_Minh")).toInstant();

        when(userRepository.findUserCreatedAtBetween(any(Instant.class), any(Instant.class)))
                .thenReturn(List.of(u1, u2));
        when(userRepository.count()).thenReturn(100L);
        when(userRepository.countByActiveTrue()).thenReturn(95L);
        when(userRepository.countByEmailVerifiedTrue()).thenReturn(90L);

        UserStatisticsResponse response = statisticsService.getUserStatistics(filter);

        assertNotNull(response);
        assertEquals(100L, response.getTotalUsers());
        assertEquals(2L, response.getNewUsersInPeriod());
        assertEquals(95L, response.getActiveUsers());
        assertEquals(90L, response.getVerifiedUsers());
        assertEquals(2, response.getTimeline().size()); // 01/11 (2 users), 02/11 (0 users)
        assertEquals(2L, response.getTimeline().get(0).getNewUsersCount());
        assertEquals(0L, response.getTimeline().get(1).getNewUsersCount());
    }
}
