package com.techcraft.techcraftbackend.controller;

import com.techcraft.techcraftbackend.dto.ApiResponse;
import com.techcraft.techcraftbackend.dto.notification.OrderNotificationDto;
import com.techcraft.techcraftbackend.dto.response.PageResponse;
import com.techcraft.techcraftbackend.exception.UnauthorizedException;
import com.techcraft.techcraftbackend.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    // ==================== DÀNH CHO USER ====================

    @GetMapping("/notifications")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PageResponse<OrderNotificationDto>>> getUserNotifications(
            Principal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        validatePrincipal(principal);
        Pageable pageable = PageRequest.of(page, size);
        PageResponse<OrderNotificationDto> response = notificationService.getUserNotifications(principal.getName(), pageable);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách thông báo thành công!", response));
    }

    @GetMapping("/notifications/unread-count")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getUserUnreadCount(Principal principal) {
        validatePrincipal(principal);
        long count = notificationService.getUserUnreadCount(principal.getName());
        return ResponseEntity.ok(ApiResponse.success("Lấy số lượng thông báo chưa đọc thành công!", Map.of("unreadCount", count)));
    }

    @PutMapping("/notifications/{id}/read")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> markUserAsRead(
            Principal principal,
            @PathVariable UUID id) {
        validatePrincipal(principal);
        notificationService.markAsRead(id, principal.getName());
        return ResponseEntity.ok(ApiResponse.success("Đã đánh dấu thông báo là đã đọc!", null));
    }

    @PutMapping("/notifications/read-all")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> markAllUserAsRead(Principal principal) {
        validatePrincipal(principal);
        notificationService.markAllUserNotificationsAsRead(principal.getName());
        return ResponseEntity.ok(ApiResponse.success("Đã đánh dấu tất cả thông báo là đã đọc!", null));
    }

    // ==================== DÀNH CHO ADMIN ====================

    @GetMapping("/admin/notifications")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<OrderNotificationDto>>> getAdminNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        PageResponse<OrderNotificationDto> response = notificationService.getAdminNotifications(pageable);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách thông báo quản trị thành công!", response));
    }

    @GetMapping("/admin/notifications/unread-count")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getAdminUnreadCount() {
        long count = notificationService.getAdminUnreadCount();
        return ResponseEntity.ok(ApiResponse.success("Lấy số lượng thông báo quản trị chưa đọc thành công!", Map.of("unreadCount", count)));
    }

    @PutMapping("/admin/notifications/{id}/read")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> markAdminAsRead(
            Principal principal,
            @PathVariable UUID id) {
        validatePrincipal(principal);
        notificationService.markAsRead(id, principal.getName());
        return ResponseEntity.ok(ApiResponse.success("Đã đánh dấu thông báo quản trị là đã đọc!", null));
    }

    @PutMapping("/admin/notifications/read-all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> markAllAdminAsRead() {
        notificationService.markAllAdminNotificationsAsRead();
        return ResponseEntity.ok(ApiResponse.success("Đã đánh dấu tất cả thông báo quản trị là đã đọc!", null));
    }

    private void validatePrincipal(Principal principal) {
        if (principal == null || principal.getName() == null) {
            throw new UnauthorizedException("Vui lòng đăng nhập để thực hiện chức năng này.");
        }
    }
}
