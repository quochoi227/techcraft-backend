package com.techcraft.techcraftbackend.service.impl;

import com.techcraft.techcraftbackend.dto.notification.OrderNotificationDto;
import com.techcraft.techcraftbackend.dto.response.PageResponse;
import com.techcraft.techcraftbackend.entity.Notification;
import com.techcraft.techcraftbackend.entity.Order;
import com.techcraft.techcraftbackend.entity.User;
import com.techcraft.techcraftbackend.enums.NotificationType;
import com.techcraft.techcraftbackend.enums.OrderStatus;
import com.techcraft.techcraftbackend.enums.Role;
import com.techcraft.techcraftbackend.exception.ResourceNotFoundException;
import com.techcraft.techcraftbackend.repository.NotificationRepository;
import com.techcraft.techcraftbackend.repository.UserRepository;
import com.techcraft.techcraftbackend.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    @Transactional
    public void notifyAdminOrderCreated(Order order) {
        if (order == null) return;

        String orderCode = resolveOrderCode(order);
        String customerName = order.getUser() != null && order.getUser().getFullName() != null
                ? order.getUser().getFullName()
                : (order.getUser() != null ? order.getUser().getEmail() : "Khách hàng");
        String customerEmail = order.getUser() != null ? order.getUser().getEmail() : null;

        String title = "Đơn hàng mới!";
        String message = String.format("Khách hàng %s vừa đặt đơn hàng #%s (Tổng tiền: %,.0f ₫)",
                customerName, orderCode, order.getTotalAmount());

        Notification notification = Notification.builder()
                .targetRole(Role.ADMIN)
                .type(NotificationType.ORDER_CREATED)
                .orderId(order.getId())
                .orderCode(orderCode)
                .title(title)
                .message(message)
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .customerName(customerName)
                .customerEmail(customerEmail)
                .read(false)
                .build();

        Notification saved = notificationRepository.save(notification);
        OrderNotificationDto dto = toDto(saved);

        log.info("Broadcasting ORDER_CREATED to /topic/admin/notifications for order #{}", orderCode);
        messagingTemplate.convertAndSend("/topic/admin/notifications", dto);
    }

    @Override
    @Transactional
    public void notifyAdminOrderCancelled(Order order, User cancelledBy) {
        if (order == null) return;

        String orderCode = resolveOrderCode(order);
        String customerName = cancelledBy != null && cancelledBy.getFullName() != null
                ? cancelledBy.getFullName()
                : (cancelledBy != null ? cancelledBy.getEmail() : "Khách hàng");
        String customerEmail = cancelledBy != null ? cancelledBy.getEmail() : null;

        String title = "Đơn hàng đã bị hủy!";
        String message = String.format("Khách hàng %s đã hủy đơn hàng #%s", customerName, orderCode);

        Notification notification = Notification.builder()
                .targetRole(Role.ADMIN)
                .type(NotificationType.ORDER_CANCELLED_BY_USER)
                .orderId(order.getId())
                .orderCode(orderCode)
                .title(title)
                .message(message)
                .status(OrderStatus.CANCELLED)
                .totalAmount(order.getTotalAmount())
                .customerName(customerName)
                .customerEmail(customerEmail)
                .read(false)
                .build();

        Notification saved = notificationRepository.save(notification);
        OrderNotificationDto dto = toDto(saved);

        log.info("Broadcasting ORDER_CANCELLED_BY_USER to /topic/admin/notifications for order #{}", orderCode);
        messagingTemplate.convertAndSend("/topic/admin/notifications", dto);
    }

    @Override
    @Transactional
    public void notifyUserOrderStatusChanged(Order order, OrderStatus oldStatus, OrderStatus newStatus) {
        if (order == null || order.getUser() == null) return;

        String orderCode = resolveOrderCode(order);
        String statusLabel = getStatusLabel(newStatus);

        String title = "Cập nhật đơn hàng";
        String message = String.format("Đơn hàng #%s của bạn đã chuyển sang trạng thái: %s", orderCode, statusLabel);

        Notification notification = Notification.builder()
                .recipient(order.getUser())
                .type(NotificationType.ORDER_STATUS_CHANGED)
                .orderId(order.getId())
                .orderCode(orderCode)
                .title(title)
                .message(message)
                .status(newStatus)
                .totalAmount(order.getTotalAmount())
                .customerName(order.getUser().getFullName())
                .customerEmail(order.getUser().getEmail())
                .read(false)
                .build();

        Notification saved = notificationRepository.save(notification);
        OrderNotificationDto dto = toDto(saved);

        String userDestination = "/topic/users/" + order.getUser().getId() + "/notifications";
        log.info("Broadcasting ORDER_STATUS_CHANGED to {} for order #{}", userDestination, orderCode);
        messagingTemplate.convertAndSend(userDestination, dto);
    }

    @Override
    @Transactional
    public void notifyOrderPaid(Order order, java.math.BigDecimal paidAmount) {
        if (order == null) return;

        String orderCode = resolveOrderCode(order);
        String customerName = order.getUser() != null && order.getUser().getFullName() != null
                ? order.getUser().getFullName()
                : (order.getUser() != null ? order.getUser().getEmail() : "Khách hàng");
        String customerEmail = order.getUser() != null ? order.getUser().getEmail() : null;

        // 1. Thông báo cho người dùng
        if (order.getUser() != null) {
            String userTitle = "Thanh toán thành công!";
            String userMessage = String.format("Đơn hàng #%s của bạn đã được thanh toán thành công (%,.0f ₫) qua SePay.",
                    orderCode, paidAmount != null ? paidAmount : order.getTotalAmount());

            Notification userNoti = Notification.builder()
                    .recipient(order.getUser())
                    .type(NotificationType.ORDER_PAID)
                    .orderId(order.getId())
                    .orderCode(orderCode)
                    .title(userTitle)
                    .message(userMessage)
                    .status(order.getStatus())
                    .totalAmount(order.getTotalAmount())
                    .customerName(customerName)
                    .customerEmail(customerEmail)
                    .read(false)
                    .build();

            Notification savedUserNoti = notificationRepository.save(userNoti);
            OrderNotificationDto userDto = toDto(savedUserNoti);
            messagingTemplate.convertAndSend("/topic/users/" + order.getUser().getId() + "/notifications", userDto);
        }

        // 2. Thông báo cho Admin
        String adminTitle = "Đơn hàng đã thanh toán!";
        String adminMessage = String.format("Đơn hàng #%s của khách hàng %s đã được thanh toán qua SePay (%,.0f ₫).",
                orderCode, customerName, paidAmount != null ? paidAmount : order.getTotalAmount());

        Notification adminNoti = Notification.builder()
                .targetRole(Role.ADMIN)
                .type(NotificationType.ORDER_PAID)
                .orderId(order.getId())
                .orderCode(orderCode)
                .title(adminTitle)
                .message(adminMessage)
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .customerName(customerName)
                .customerEmail(customerEmail)
                .read(false)
                .build();

        Notification savedAdminNoti = notificationRepository.save(adminNoti);
        OrderNotificationDto adminDto = toDto(savedAdminNoti);
        messagingTemplate.convertAndSend("/topic/admin/notifications", adminDto);

        // 3. Phát sóng trực tiếp tới topic riêng của đơn hàng để Checkout view bắt sự kiện
        java.util.Map<String, Object> paymentPayload = new java.util.HashMap<>();
        paymentPayload.put("orderId", order.getId().toString());
        paymentPayload.put("orderCode", orderCode);
        paymentPayload.put("paymentStatus", "PAID");
        paymentPayload.put("orderStatus", order.getStatus() != null ? order.getStatus().name() : "CONFIRMED");
        messagingTemplate.convertAndSend("/topic/orders/" + order.getId() + "/payment", (Object) paymentPayload);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderNotificationDto> getUserNotifications(String email, Pageable pageable) {
        User user = getUserByEmail(email);
        Page<Notification> page = notificationRepository.findByRecipientIdOrderByCreatedAtDesc(user.getId(), pageable);
        return PageResponse.from(page, this::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderNotificationDto> getAdminNotifications(Pageable pageable) {
        Page<Notification> page = notificationRepository.findAdminNotifications(Role.ADMIN, pageable);
        return PageResponse.from(page, this::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public long getUserUnreadCount(String email) {
        User user = getUserByEmail(email);
        return notificationRepository.countByRecipientIdAndReadFalse(user.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public long getAdminUnreadCount() {
        return notificationRepository.countAdminUnreadNotifications(Role.ADMIN);
    }

    @Override
    @Transactional
    public void markAsRead(UUID notificationId, String email) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông báo với ID: " + notificationId));

        User user = getUserByEmail(email);
        boolean isAdmin = user.getRole() == Role.ADMIN;
        boolean isOwner = notification.getRecipient() != null && notification.getRecipient().getId().equals(user.getId());

        if (isAdmin || isOwner) {
            notification.setRead(true);
            notification.setReadAt(Instant.now());
            notificationRepository.save(notification);
        }
    }

    @Override
    @Transactional
    public void markAllUserNotificationsAsRead(String email) {
        User user = getUserByEmail(email);
        notificationRepository.markAllAsReadForUser(user.getId(), Instant.now());
    }

    @Override
    @Transactional
    public void markAllAdminNotificationsAsRead() {
        notificationRepository.markAllAsReadForAdmin(Role.ADMIN, Instant.now());
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email != null ? email.toLowerCase().trim() : null)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với email: " + email));
    }

    private String resolveOrderCode(Order order) {
        if (order == null) return "";
        if (order.getOrderCode() != null && !order.getOrderCode().isBlank()) {
            return order.getOrderCode();
        }
        return getOrderCode(order.getId());
    }

    private String getOrderCode(UUID orderId) {
        if (orderId == null) return "";
        return orderId.toString().substring(0, 8).toUpperCase();
    }

    private String getStatusLabel(OrderStatus status) {
        if (status == null) return "";
        return switch (status) {
            case PENDING -> "Chờ xác nhận";
            case CONFIRMED -> "Đã xác nhận";
            case SHIPPING -> "Đang giao hàng";
            case DELIVERED -> "Đã giao thành công";
            case CANCELLED -> "Đã hủy";
        };
    }

    private OrderNotificationDto toDto(Notification n) {
        return OrderNotificationDto.builder()
                .id(n.getId())
                .type(n.getType())
                .orderId(n.getOrderId())
                .orderCode(n.getOrderCode())
                .title(n.getTitle())
                .message(n.getMessage())
                .status(n.getStatus())
                .totalAmount(n.getTotalAmount())
                .customerName(n.getCustomerName())
                .customerEmail(n.getCustomerEmail())
                .read(n.isRead())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
