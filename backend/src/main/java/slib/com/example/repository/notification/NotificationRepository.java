package slib.com.example.repository.notification;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import slib.com.example.entity.notification.NotificationEntity;
import slib.com.example.entity.notification.NotificationEntity.NotificationType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository for NotificationEntity
 */
@Repository
public interface NotificationRepository extends JpaRepository<NotificationEntity, UUID> {

    /**
     * Find notifications by userId, ordered by created date descending
     */
    @Query("SELECT n FROM NotificationEntity n WHERE n.user.id = :userId ORDER BY n.createdAt DESC")
    List<NotificationEntity> findByUserIdOrderByCreatedAtDesc(@Param("userId") UUID userId);

    @Query("SELECT n FROM NotificationEntity n WHERE n.user.id = :userId AND n.isRead = false ORDER BY n.createdAt DESC")
    List<NotificationEntity> findUnreadByUserId(@Param("userId") UUID userId);

    /**
     * Find notifications by userId with limit
     */
    @Query(value = "SELECT * FROM notifications WHERE user_id = :userId ORDER BY created_at DESC LIMIT :limit", nativeQuery = true)
    List<NotificationEntity> findByUserIdWithLimit(@Param("userId") UUID userId, @Param("limit") int limit);

    /**
     * Count unread notifications for a user
     */
    @Query("SELECT COUNT(n) FROM NotificationEntity n WHERE n.user.id = :userId AND n.isRead = false")
    long countUnreadByUserId(@Param("userId") UUID userId);

    @Query("""
            SELECT COUNT(n) > 0
            FROM NotificationEntity n
            WHERE n.user.id = :userId
              AND n.notificationType = :type
              AND n.title = :title
              AND n.content = :content
              AND ((:referenceType IS NULL AND n.referenceType IS NULL) OR n.referenceType = :referenceType)
              AND ((:referenceId IS NULL AND n.referenceId IS NULL) OR n.referenceId = :referenceId)
              AND n.createdAt >= :since
            """)
    boolean existsRecentDuplicate(
            @Param("userId") UUID userId,
            @Param("type") NotificationType type,
            @Param("title") String title,
            @Param("content") String content,
            @Param("referenceType") String referenceType,
            @Param("referenceId") UUID referenceId,
            @Param("since") LocalDateTime since);

    /**
     * Mark all notifications as read for a user
     */
    @Modifying
    @Query("UPDATE NotificationEntity n SET n.isRead = true WHERE n.user.id = :userId AND n.isRead = false")
    void markAllAsReadByUserId(@Param("userId") UUID userId);
}
