package com.techcraft.techcraftbackend.enums;

public enum NotificationType {
    ORDER_CREATED,           // Khách hàng tạo đơn hàng mới -> gửi Admin
    ORDER_CANCELLED_BY_USER, // Khách hàng hủy đơn hàng -> gửi Admin
    ORDER_STATUS_CHANGED,    // Trạng thái đơn hàng thay đổi -> gửi Khách hàng
    ORDER_PAID               // Đơn hàng thanh toán thành công qua SePay -> gửi Admin & Khách hàng
}
