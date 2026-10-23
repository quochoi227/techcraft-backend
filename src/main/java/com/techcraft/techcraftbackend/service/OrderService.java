package com.techcraft.techcraftbackend.service;

import com.techcraft.techcraftbackend.dto.request.AdminOrderFilterRequest;
import com.techcraft.techcraftbackend.dto.request.CreateOrderRequest;
import com.techcraft.techcraftbackend.dto.request.OrderFilterRequest;
import com.techcraft.techcraftbackend.dto.request.UpdateOrderStatusRequest;
import com.techcraft.techcraftbackend.dto.response.PageResponse;
import com.techcraft.techcraftbackend.dto.response.OrderResponse;
import com.techcraft.techcraftbackend.entity.Order;

import java.util.UUID;

public interface OrderService {

    OrderResponse createOrder(String email, CreateOrderRequest request);

    OrderResponse createOrder(UUID userId, CreateOrderRequest request);

    OrderResponse cancelOrder(String email, UUID orderId);

    OrderResponse cancelOrder(UUID userId, UUID orderId);

    OrderResponse updateOrderStatus(UUID orderId, UpdateOrderStatusRequest request);

    PageResponse<OrderResponse> getUserOrders(String email, OrderFilterRequest filter);

    PageResponse<OrderResponse> getUserOrders(UUID userId, OrderFilterRequest filter);

    PageResponse<OrderResponse> getAllOrders(AdminOrderFilterRequest filter);

    void restoreOrderStock(Order order);
}
