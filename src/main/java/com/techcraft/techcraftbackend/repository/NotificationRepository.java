package com.techcraft.techcraftbackend.repository;

import com.techcraft.techcraftbackend.entity.Notification;
import com.techcraft.techcraftbackend.enums.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    // Danh sách thông báo của User
    Page<Notification> findByRecipientIdOrderByCreatedAtDesc(UUID recipientId, Pageable pageable);

    // Số lượng thông báo chưa đọc của User
    long countByRecipientIdAndReadFalse(UUID recipientId);

    // Danh sách thông báo của Admin (những thông báo có targetRole = ADMIN)
    @Query("SELECT n FROM Notification n WHERE n.targetRole = :role ORDER BY n.createdAt DESC")
    Page<Notification> findAdminNotifications(@Param("role") Role role, Pageable pageable);

    // Số lượng thông báo chưa đọc của Admin
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.targetRole = :role AND n.read = false")
    long countAdminUnreadNotifications(@Param("role") Role role);

    // Đánh dấu tất cả thông báo của User là đã đọc
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Notification n SET n.read = true, n.readAt = :now WHERE n.recipient.id = :recipientId AND n.read = false")
    int markAllAsReadForUser(@Param("recipientId") UUID recipientId, @Param("now") Instant now);

    // Đánh dấu tất cả thông báo của Admin là đã đọc
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Notification n SET n.read = true, n.readAt = :now WHERE n.targetRole = :role AND n.read = false")
    int markAllAsReadForAdmin(@Param("role") Role role, @Param("now") Instant now);
}
