package com.techcraft.techcraftbackend.controller;

import com.techcraft.techcraftbackend.dto.ApiResponse;
import com.techcraft.techcraftbackend.dto.request.OrderStatisticsFilterRequest;
import com.techcraft.techcraftbackend.dto.request.RevenueStatisticsFilterRequest;
import com.techcraft.techcraftbackend.dto.request.UserStatisticsFilterRequest;
import com.techcraft.techcraftbackend.dto.response.OrderStatisticsResponse;
import com.techcraft.techcraftbackend.dto.response.RevenueStatisticsResponse;
import com.techcraft.techcraftbackend.dto.response.UserStatisticsResponse;
import com.techcraft.techcraftbackend.service.StatisticsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/statistics")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminStatisticsController {

    private final StatisticsService statisticsService;

    @GetMapping("/orders")
    public ResponseEntity<ApiResponse<OrderStatisticsResponse>> getOrderStatistics(
            @Valid @ModelAttribute OrderStatisticsFilterRequest filter) {
        OrderStatisticsResponse response = statisticsService.getOrderStatistics(filter);
        return ResponseEntity.ok(ApiResponse.success("Thống kê số lượng đơn hàng theo trạng thái thành công!", response));
    }

    @GetMapping("/revenue")
    public ResponseEntity<ApiResponse<RevenueStatisticsResponse>> getRevenueStatistics(
            @Valid @ModelAttribute RevenueStatisticsFilterRequest filter) {
        RevenueStatisticsResponse response = statisticsService.getRevenueStatistics(filter);
        return ResponseEntity.ok(ApiResponse.success("Thống kê doanh thu thành công!", response));
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<UserStatisticsResponse>> getUserStatistics(
            @Valid @ModelAttribute UserStatisticsFilterRequest filter) {
        UserStatisticsResponse response = statisticsService.getUserStatistics(filter);
        return ResponseEntity.ok(ApiResponse.success("Thống kê người dùng mới thành công!", response));
    }
}
