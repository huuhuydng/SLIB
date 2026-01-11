package slib.com.example.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import slib.com.example.entity.AccessLogEntity;

import java.util.List;
import java.util.UUID;

@Repository
public interface AccessLogRepository extends JpaRepository<AccessLogEntity, UUID> {
    List<AccessLogEntity> findByUserId(UUID userId);
    List<AccessLogEntity> findByReservation_ReservationId(UUID reservationId);
}
