package slib.com.example.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import slib.com.example.entity.zone_config.SeatEntity;
import slib.com.example.entity.zone_config.SeatStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SeatRepository extends JpaRepository<SeatEntity, Integer> {
    List<SeatEntity> findByZone_ZoneId(Integer zoneId);

    long countByZone_ZoneIdAndSeatStatus(Integer zoneId, SeatStatus seatStatus);

    Optional<SeatEntity> findBySeatCode(String seatCode);

    Optional<SeatEntity> findBySeatCodeAndZone_ZoneId(String seatCode, Integer zoneId);

    @Query("SELECT MAX(s.columnNumber) FROM SeatEntity s WHERE s.zone.zoneId = :zoneId AND s.rowNumber = :rowNumber")
    Integer findMaxColumnByZoneIdAndRow(@Param("zoneId") Integer zoneId, @Param("rowNumber") Integer rowNumber);

    // Delete all seats by zone ID
    void deleteByZone_ZoneId(Integer zoneId);

    // Find expired HOLDING seats
    @Query("SELECT s FROM SeatEntity s WHERE s.seatStatus = :status AND s.holdExpiresAt < :now")
    List<SeatEntity> findByStatusAndExpired(@Param("status") SeatStatus status, @Param("now") LocalDateTime now);
}