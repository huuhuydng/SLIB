package slib.com.example.repository.activity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import slib.com.example.entity.activity.ActivityLogEntity;

import java.util.List;
import java.util.UUID;

@Repository
public interface ActivityLogRepository extends JpaRepository<ActivityLogEntity, UUID> {

    List<ActivityLogEntity> findByUserIdOrderByCreatedAtDesc(UUID userId);

    @Query("SELECT a FROM ActivityLogEntity a WHERE a.userId = :userId ORDER BY a.createdAt DESC LIMIT :limit")
    List<ActivityLogEntity> findByUserIdWithLimit(@Param("userId") UUID userId, @Param("limit") int limit);

    @Query("SELECT COUNT(a) FROM ActivityLogEntity a WHERE a.userId = :userId AND a.activityType = :type")
    long countByUserIdAndType(@Param("userId") UUID userId, @Param("type") String type);

    @Query("SELECT COALESCE(SUM(a.durationMinutes), 0) FROM ActivityLogEntity a WHERE a.userId = :userId AND a.activityType = 'CHECK_OUT'")
    long getTotalStudyMinutes(@Param("userId") UUID userId);

    /**
     * Check if a reservation has been NFC-confirmed (checked in)
     * @param reservationId the reservation ID
     * @return true if NFC_CONFIRM activity exists for this reservation
     */
    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END FROM ActivityLogEntity a WHERE a.reservationId = :reservationId AND a.activityType = 'NFC_CONFIRM'")
    boolean hasNfcConfirmation(@Param("reservationId") UUID reservationId);

    /**
     * Check if a late check-in penalty has already been applied for a reservation
     * @param reservationId the reservation ID
     * @return true if LATE_CHECKIN_PENALTY activity exists for this reservation
     */
    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END FROM ActivityLogEntity a WHERE a.reservationId = :reservationId AND a.activityType = 'LATE_CHECKIN_PENALTY'")
    boolean hasLateCheckinPenalty(@Param("reservationId") UUID reservationId);

    // Delete all activity logs by user ID (for cascade delete when user is deleted)
    void deleteByUserId(UUID userId);
}
