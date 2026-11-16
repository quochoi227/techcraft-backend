package com.techcraft.techcraftbackend.service;

import com.techcraft.techcraftbackend.dto.request.OrderStatisticsFilterRequest;
import com.techcraft.techcraftbackend.dto.request.RevenueStatisticsFilterRequest;
import com.techcraft.techcraftbackend.dto.request.UserStatisticsFilterRequest;
import com.techcraft.techcraftbackend.dto.response.OrderStatisticsResponse;
import com.techcraft.techcraftbackend.dto.response.RevenueStatisticsResponse;
import com.techcraft.techcraftbackend.dto.response.UserStatisticsResponse;

public interface StatisticsService {

    OrderStatisticsResponse getOrderStatistics(OrderStatisticsFilterRequest filter);

    RevenueStatisticsResponse getRevenueStatistics(RevenueStatisticsFilterRequest filter);

    UserStatisticsResponse getUserStatistics(UserStatisticsFilterRequest filter);
}
