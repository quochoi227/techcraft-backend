package com.techcraft.techcraftbackend.service;

import com.techcraft.techcraftbackend.dto.request.OrderStatisticsFilterRequest;
import com.techcraft.techcraftbackend.dto.response.OrderStatisticsResponse;

public interface StatisticsService {

    OrderStatisticsResponse getOrderStatistics(OrderStatisticsFilterRequest filter);
}
