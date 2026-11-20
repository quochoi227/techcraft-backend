package com.techcraft.techcraftbackend.service;

import com.techcraft.techcraftbackend.dto.notification.OrderNotificationDto;
import com.techcraft.techcraftbackend.dto.response.PageResponse;
import com.techcraft.techcraftbackend.entity.Order;
import com.techcraft.techcraftbackend.entity.User;
import com.techcraft.techcraftbackend.enums.OrderStatus;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface NotificationService {

    // Trigger phát realtime & lưu DB
    void notifyAdminOrderCreated(Order order);

    void notifyAdminOrderCancelled(Order order, User cancelledBy);

    void notifyUserOrderStatusChanged(Order order, OrderStatus oldStatus, OrderStatus newStatus);

    void notifyOrderPaid(Order order, java.math.BigDecimal paidAmount);

    // Truy vấn thông báo
    PageResponse<OrderNotificationDto> getUserNotifications(String email, Pageable pageable);

    PageResponse<OrderNotificationDto> getAdminNotifications(Pageable pageable);

    long getUserUnreadCount(String email);

    long getAdminUnreadCount();

    // Đánh dấu đã đọc
    void markAsRead(UUID notificationId, String email);

    void markAllUserNotificationsAsRead(String email);

    void markAllAdminNotificationsAsRead();
}
