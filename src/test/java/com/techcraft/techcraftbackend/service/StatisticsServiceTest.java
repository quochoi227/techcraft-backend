package com.techcraft.techcraftbackend.service;

import com.techcraft.techcraftbackend.dto.request.OrderStatisticsFilterRequest;
import com.techcraft.techcraftbackend.dto.response.OrderStatisticsResponse;
import com.techcraft.techcraftbackend.dto.response.OrderStatusBreakdownResponse;
import com.techcraft.techcraftbackend.enums.OrderStatus;
import com.techcraft.techcraftbackend.exception.BadRequestException;
import com.techcraft.techcraftbackend.repository.OrderRepository;
import com.techcraft.techcraftbackend.repository.projection.OrderStatusCountProjection;
import com.techcraft.techcraftbackend.service.impl.StatisticsServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StatisticsServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private StatisticsServiceImpl statisticsService;

    private OrderStatusCountProjection createProjection(OrderStatus status, Long count) {
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

    @Test
    @DisplayName("Thống kê đơn hàng toàn thời gian thành công khi không truyền ngày")
    void getOrderStatistics_AllTime_Success() {
        List<OrderStatusCountProjection> mockDbCounts = List.of(
                createProjection(OrderStatus.PENDING, 5L),
                createProjection(OrderStatus.CONFIRMED, 10L),
                createProjection(OrderStatus.SHIPPING, 15L),
                createProjection(OrderStatus.DELIVERED, 20L)
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

        OrderStatusBreakdownResponse cancelledBreakdown = response.getBreakdown().stream()
                .filter(b -> b.getStatus() == OrderStatus.CANCELLED)
                .findFirst()
                .orElse(null);
        assertNotNull(cancelledBreakdown);
        assertEquals("Đã hủy", cancelledBreakdown.getStatusName());
        assertEquals(0L, cancelledBreakdown.getCount());
        assertEquals(0.0, cancelledBreakdown.getPercentage());

        verify(orderRepository, times(1)).countOrdersByStatus();
    }

    @Test
    @DisplayName("Thống kê đơn hàng theo khoảng thời gian hợp lệ")
    void getOrderStatistics_DateRange_CallsBetween() {
        OrderStatisticsFilterRequest filter = OrderStatisticsFilterRequest.builder()
                .startDate("2026-11-01")
                .endDate("2026-11-15")
                .build();

        List<OrderStatusCountProjection> mockDbCounts = List.of(
                createProjection(OrderStatus.DELIVERED, 8L)
        );

        when(orderRepository.countOrdersByStatusBetween(any(Instant.class), any(Instant.class)))
                .thenReturn(mockDbCounts);

        OrderStatisticsResponse response = statisticsService.getOrderStatistics(filter);

        assertNotNull(response);
        assertEquals(8L, response.getTotalOrders());
        assertEquals(8L, response.getDeliveredCount());
        assertEquals(0L, response.getPendingCount());

        verify(orderRepository, times(1))
                .countOrdersByStatusBetween(any(Instant.class), any(Instant.class));
    }

    @Test
    @DisplayName("Thống kê chỉ truyền startDate thì gọi countOrdersByStatusFrom")
    void getOrderStatistics_OnlyStartDate_CallsFrom() {
        OrderStatisticsFilterRequest filter = OrderStatisticsFilterRequest.builder()
                .startDate("2026-11-01T00:00:00Z")
                .build();

        when(orderRepository.countOrdersByStatusFrom(any(Instant.class)))
                .thenReturn(List.of(createProjection(OrderStatus.SHIPPING, 3L)));

        OrderStatisticsResponse response = statisticsService.getOrderStatistics(filter);

        assertNotNull(response);
        assertEquals(3L, response.getTotalOrders());
        assertEquals(3L, response.getShippingCount());
        verify(orderRepository, times(1)).countOrdersByStatusFrom(any(Instant.class));
    }

    @Test
    @DisplayName("Thống kê chỉ truyền endDate thì gọi countOrdersByStatusTo")
    void getOrderStatistics_OnlyEndDate_CallsTo() {
        OrderStatisticsFilterRequest filter = OrderStatisticsFilterRequest.builder()
                .endDate("2026-11-30")
                .build();

        when(orderRepository.countOrdersByStatusTo(any(Instant.class)))
                .thenReturn(List.of(createProjection(OrderStatus.CONFIRMED, 4L)));

        OrderStatisticsResponse response = statisticsService.getOrderStatistics(filter);

        assertNotNull(response);
        assertEquals(4L, response.getTotalOrders());
        assertEquals(4L, response.getConfirmedCount());
        verify(orderRepository, times(1)).countOrdersByStatusTo(any(Instant.class));
    }

    @Test
    @DisplayName("Ném BadRequestException khi startDate sau endDate")
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

    @Test
    @DisplayName("Ném BadRequestException khi định dạng ngày không hợp lệ")
    void getOrderStatistics_InvalidDateFormat_ThrowsBadRequestException() {
        OrderStatisticsFilterRequest filter = OrderStatisticsFilterRequest.builder()
                .startDate("invalid-date-format")
                .build();

        assertThrows(BadRequestException.class,
                () -> statisticsService.getOrderStatistics(filter));

        verifyNoInteractions(orderRepository);
    }

    @Test
    @DisplayName("Database rỗng thì trả về toàn bộ count = 0 và percentage = 0.0")
    void getOrderStatistics_EmptyDatabase_ReturnsZeros() {
        when(orderRepository.countOrdersByStatus()).thenReturn(List.of());

        OrderStatisticsResponse response = statisticsService.getOrderStatistics(new OrderStatisticsFilterRequest());

        assertNotNull(response);
        assertEquals(0L, response.getTotalOrders());
        assertEquals(0L, response.getPendingCount());
        assertEquals(0L, response.getConfirmedCount());
        assertEquals(0L, response.getShippingCount());
        assertEquals(0L, response.getDeliveredCount());
        assertEquals(0L, response.getCancelledCount());
        assertEquals(5, response.getBreakdown().size());
        response.getBreakdown().forEach(b -> {
            assertEquals(0L, b.getCount());
            assertEquals(0.0, b.getPercentage());
        });
    }
}
