package slib.com.example.repository.kiosk;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import slib.com.example.entity.kiosk.KioskQrSessionEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Kiosk QR Session Repository
 */
@Repository
public interface KioskQrSessionRepository extends JpaRepository<KioskQrSessionEntity, Integer> {

    Optional<KioskQrSessionEntity> findBySessionToken(String sessionToken);

    Optional<KioskQrSessionEntity> findByKioskIdAndStatus(Integer kioskId, String status);

    List<KioskQrSessionEntity> findByKioskIdAndStatusIn(Integer kioskId, List<String> statuses);

    @EntityGraph(attributePaths = { "student", "kiosk" })
    Optional<KioskQrSessionEntity> findFirstByKiosk_KioskCodeAndStatusInAndStudentIsNotNullOrderByUpdatedAtDesc(
            String kioskCode,
            List<String> statuses);

    @Modifying
    @Query("UPDATE KioskQrSessionEntity q SET q.status = 'EXPIRED' WHERE q.qrExpiresAt < :now AND q.status = 'ACTIVE'")
    int expireOldSessions(@Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE KioskQrSessionEntity q SET q.status = 'USED' WHERE q.id = :id")
    int markAsUsed(@Param("id") Integer id);

    Optional<KioskQrSessionEntity> findByQrPayloadAndStatus(String qrPayload, String status);
}
