package com.techcraft.techcraftbackend.controller;

import com.techcraft.techcraftbackend.dto.request.OrderStatisticsFilterRequest;
import com.techcraft.techcraftbackend.dto.response.OrderStatisticsResponse;
import com.techcraft.techcraftbackend.dto.response.OrderStatusBreakdownResponse;
import com.techcraft.techcraftbackend.enums.OrderStatus;
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

    private OrderStatisticsResponse createMockResponse() {
        return OrderStatisticsResponse.builder()
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
                                .build(),
                        OrderStatusBreakdownResponse.builder()
                                .status(OrderStatus.CONFIRMED)
                                .statusName("Đã xác nhận")
                                .count(10L)
                                .percentage(20.0)
                                .build(),
                        OrderStatusBreakdownResponse.builder()
                                .status(OrderStatus.SHIPPING)
                                .statusName("Đang giao hàng")
                                .count(15L)
                                .percentage(30.0)
                                .build(),
                        OrderStatusBreakdownResponse.builder()
                                .status(OrderStatus.DELIVERED)
                                .statusName("Đã giao hàng")
                                .count(18L)
                                .percentage(36.0)
                                .build(),
                        OrderStatusBreakdownResponse.builder()
                                .status(OrderStatus.CANCELLED)
                                .statusName("Đã hủy")
                                .count(2L)
                                .percentage(4.0)
                                .build()
                ))
                .build();
    }

    @Test
    @DisplayName("Admin lấy thống kê đơn hàng thành công (200 OK)")
    @WithMockUser(username = "admin@techcraft.com", roles = "ADMIN")
    void getOrderStatistics_AsAdmin_Success() throws Exception {
        when(statisticsService.getOrderStatistics(any())).thenReturn(createMockResponse());

        mockMvc.perform(get("/api/admin/statistics/orders")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Thống kê số lượng đơn hàng theo trạng thái thành công!"))
                .andExpect(jsonPath("$.data.totalOrders").value(50))
                .andExpect(jsonPath("$.data.pendingCount").value(5))
                .andExpect(jsonPath("$.data.confirmedCount").value(10))
                .andExpect(jsonPath("$.data.shippingCount").value(15))
                .andExpect(jsonPath("$.data.deliveredCount").value(18))
                .andExpect(jsonPath("$.data.cancelledCount").value(2))
                .andExpect(jsonPath("$.data.breakdown.length()").value(5))
                .andExpect(jsonPath("$.data.breakdown[0].status").value("PENDING"))
                .andExpect(jsonPath("$.data.breakdown[0].statusName").value("Chờ xác nhận"))
                .andExpect(jsonPath("$.data.breakdown[0].count").value(5))
                .andExpect(jsonPath("$.data.breakdown[0].percentage").value(10.0));
    }

    @Test
    @DisplayName("Admin lấy thống kê đơn hàng với tham số ngày camelCase (200 OK)")
    @WithMockUser(username = "admin@techcraft.com", roles = "ADMIN")
    void getOrderStatistics_WithCamelCaseParams_Success() throws Exception {
        when(statisticsService.getOrderStatistics(any())).thenReturn(createMockResponse());

        mockMvc.perform(get("/api/admin/statistics/orders")
                        .param("startDate", "2026-11-01")
                        .param("endDate", "2026-11-30")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalOrders").value(50));
    }

    @Test
    @DisplayName("Admin lấy thống kê đơn hàng với tham số ngày snake_case (200 OK)")
    @WithMockUser(username = "admin@techcraft.com", roles = "ADMIN")
    void getOrderStatistics_WithSnakeCaseParams_Success() throws Exception {
        when(statisticsService.getOrderStatistics(any())).thenReturn(createMockResponse());

        mockMvc.perform(get("/api/admin/statistics/orders")
                        .param("start_date", "2026-11-01")
                        .param("end_date", "2026-11-30")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalOrders").value(50));
    }

    @Test
    @DisplayName("Người dùng với vai trò USER bị từ chối truy cập (403 Forbidden)")
    @WithMockUser(username = "user@techcraft.com", roles = "USER")
    void getOrderStatistics_AsUser_Returns403Forbidden() throws Exception {
        mockMvc.perform(get("/api/admin/statistics/orders")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Bạn không có quyền truy cập tài nguyên này."));
    }

    @Test
    @DisplayName("Chưa xác thực bị từ chối truy cập (401 Unauthorized)")
    void getOrderStatistics_Unauthenticated_Returns401Unauthorized() throws Exception {
        mockMvc.perform(get("/api/admin/statistics/orders")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Yêu cầu không được xác thực hoặc phiên đăng nhập đã hết hạn."));
    }

    @Test
    @DisplayName("Ném BadRequestException khi thời gian không hợp lệ (400 Bad Request)")
    @WithMockUser(username = "admin@techcraft.com", roles = "ADMIN")
    void getOrderStatistics_InvalidDate_Returns400BadRequest() throws Exception {
        when(statisticsService.getOrderStatistics(any(OrderStatisticsFilterRequest.class)))
                .thenThrow(new BadRequestException("Thời gian bắt đầu không được sau thời gian kết thúc."));

        mockMvc.perform(get("/api/admin/statistics/orders")
                        .param("startDate", "2026-11-30")
                        .param("endDate", "2026-11-01")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Thời gian bắt đầu không được sau thời gian kết thúc."));
    }
}
