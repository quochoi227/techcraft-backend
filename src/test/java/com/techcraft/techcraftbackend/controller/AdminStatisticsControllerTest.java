package com.techcraft.techcraftbackend.controller;

import com.techcraft.techcraftbackend.dto.request.OrderStatisticsFilterRequest;
import com.techcraft.techcraftbackend.dto.request.RevenueStatisticsFilterRequest;
import com.techcraft.techcraftbackend.dto.request.UserStatisticsFilterRequest;
import com.techcraft.techcraftbackend.dto.response.*;
import com.techcraft.techcraftbackend.enums.OrderStatus;
import com.techcraft.techcraftbackend.enums.TimePeriod;
import com.techcraft.techcraftbackend.exception.BadRequestException;
import com.techcraft.techcraftbackend.service.StatisticsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AdminStatisticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StatisticsService statisticsService;

    // ==========================================
    // 1. Tests cho /api/admin/statistics/orders
    // ==========================================

    @Test
    @DisplayName("Admin lấy thống kê đơn hàng thành công (200 OK)")
    @WithMockUser(username = "admin@techcraft.com", roles = "ADMIN")
    void getOrderStatistics_AsAdmin_Success() throws Exception {
        OrderStatisticsResponse mockResponse = OrderStatisticsResponse.builder()
                .totalOrders(50L)
                .pendingCount(5L)
                .confirmedCount(10L)
                .shippingCount(15L)
                .deliveredCount(18L)
                .cancelledCount(2L)
                .breakdown(List.of(
                        OrderStatusBreakdownResponse.builder()
                                .status(OrderStatus.PENDING)
                                .statusName("Chờ xác nhận")
                                .count(5L)
                                .percentage(10.0)
                                .build()
                ))
                .build();

        when(statisticsService.getOrderStatistics(any(OrderStatisticsFilterRequest.class))).thenReturn(mockResponse);

        mockMvc.perform(get("/api/admin/statistics/orders")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Thống kê số lượng đơn hàng theo trạng thái thành công!"))
                .andExpect(jsonPath("$.data.totalOrders").value(50))
                .andExpect(jsonPath("$.data.pendingCount").value(5));
    }

    // ==========================================
    // 2. Tests cho /api/admin/statistics/revenue
    // ==========================================

    @Test
    @DisplayName("Admin lấy thống kê doanh thu thành công (200 OK)")
    @WithMockUser(username = "admin@techcraft.com", roles = "ADMIN")
    void getRevenueStatistics_AsAdmin_Success() throws Exception {
        RevenueStatisticsResponse mockResponse = RevenueStatisticsResponse.builder()
                .totalRevenue(BigDecimal.valueOf(150000000))
                .totalOrders(25L)
                .averageOrderValue(BigDecimal.valueOf(6000000))
                .period(TimePeriod.DAY)
                .timeline(List.of(
                        RevenueTimelinePointResponse.builder()
                                .date("2026-11-01")
                                .label("01/11/2026")
                                .revenue(BigDecimal.valueOf(150000000))
                                .orderCount(25L)
                                .build()
                ))
                .build();

        when(statisticsService.getRevenueStatistics(any(RevenueStatisticsFilterRequest.class))).thenReturn(mockResponse);

        mockMvc.perform(get("/api/admin/statistics/revenue")
                        .param("period", "DAY")
                        .param("startDate", "2026-11-01")
                        .param("endDate", "2026-11-30")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Thống kê doanh thu thành công!"))
                .andExpect(jsonPath("$.data.totalRevenue").value(150000000))
                .andExpect(jsonPath("$.data.totalOrders").value(25))
                .andExpect(jsonPath("$.data.timeline.length()").value(1));
    }

    // ==========================================
    // 3. Tests cho /api/admin/statistics/users
    // ==========================================

    @Test
    @DisplayName("Admin lấy thống kê người dùng mới thành công (200 OK)")
    @WithMockUser(username = "admin@techcraft.com", roles = "ADMIN")
    void getUserStatistics_AsAdmin_Success() throws Exception {
        UserStatisticsResponse mockResponse = UserStatisticsResponse.builder()
                .totalUsers(250L)
                .newUsersInPeriod(15L)
                .activeUsers(240L)
                .verifiedUsers(230L)
                .period(TimePeriod.DAY)
                .timeline(List.of(
                        UserTimelinePointResponse.builder()
                                .date("2026-11-01")
                                .label("01/11/2026")
                                .newUsersCount(15L)
                                .build()
                ))
                .build();

        when(statisticsService.getUserStatistics(any(UserStatisticsFilterRequest.class))).thenReturn(mockResponse);

        mockMvc.perform(get("/api/admin/statistics/users")
                        .param("period", "DAY")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Thống kê người dùng mới thành công!"))
                .andExpect(jsonPath("$.data.totalUsers").value(250))
                .andExpect(jsonPath("$.data.newUsersInPeriod").value(15));
    }

    // ==========================================
    // 4. Tests Phân quyền & Bảo mật (Security)
    // ==========================================

    @Test
    @DisplayName("Người dùng với vai trò USER bị từ chối truy cập (403 Forbidden)")
    @WithMockUser(username = "user@techcraft.com", roles = "USER")
    void getStatistics_AsUser_Returns403Forbidden() throws Exception {
        mockMvc.perform(get("/api/admin/statistics/revenue"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Bạn không có quyền truy cập tài nguyên này."));

        mockMvc.perform(get("/api/admin/statistics/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Chưa đăng nhập bị từ chối truy cập (401 Unauthorized)")
    void getStatistics_Unauthenticated_Returns401Unauthorized() throws Exception {
        mockMvc.perform(get("/api/admin/statistics/revenue"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));

        mockMvc.perform(get("/api/admin/statistics/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Ném BadRequestException khi thời gian không hợp lệ (400 Bad Request)")
    @WithMockUser(username = "admin@techcraft.com", roles = "ADMIN")
    void getRevenueStatistics_InvalidDate_Returns400BadRequest() throws Exception {
        when(statisticsService.getRevenueStatistics(any(RevenueStatisticsFilterRequest.class)))
                .thenThrow(new BadRequestException("Thời gian bắt đầu không được sau thời gian kết thúc."));

        mockMvc.perform(get("/api/admin/statistics/revenue")
                        .param("startDate", "2026-11-30")
                        .param("endDate", "2026-11-01")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Thời gian bắt đầu không được sau thời gian kết thúc."));
    }
}
