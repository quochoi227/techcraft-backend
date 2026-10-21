package com.techcraft.techcraftbackend.service;

import com.techcraft.techcraftbackend.dto.request.CreateOrderRequest;
import com.techcraft.techcraftbackend.dto.response.OrderResponse;
import com.techcraft.techcraftbackend.entity.Order;

import java.util.UUID;

public interface OrderService {

    OrderResponse createOrder(String email, CreateOrderRequest request);

    OrderResponse createOrder(UUID userId, CreateOrderRequest request);

    OrderResponse cancelOrder(String email, UUID orderId);

    OrderResponse cancelOrder(UUID userId, UUID orderId);

    void restoreOrderStock(Order order);
}
