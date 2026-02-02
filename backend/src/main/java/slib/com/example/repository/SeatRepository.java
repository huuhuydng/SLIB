package slib.com.example.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import slib.com.example.entity.zone_config.SeatEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface SeatRepository extends JpaRepository<SeatEntity, Integer> {
    List<SeatEntity> findByZone_ZoneId(Integer zoneId);

    // Find only active seats in a zone
    List<SeatEntity> findByZone_ZoneIdAndIsActiveTrue(Integer zoneId);

    Optional<SeatEntity> findBySeatCode(String seatCode);

    Optional<SeatEntity> findBySeatCodeAndZone_ZoneId(String seatCode, Integer zoneId);

    @Query("SELECT MAX(s.columnNumber) FROM SeatEntity s WHERE s.zone.zoneId = :zoneId AND s.rowNumber = :rowNumber")
    Integer findMaxColumnByZoneIdAndRow(@Param("zoneId") Integer zoneId, @Param("rowNumber") Integer rowNumber);

    // Find seat by NFC tag UID (for UID Mapping Strategy)
    Optional<SeatEntity> findByNfcTagUid(String nfcTagUid);

    // Delete all seats by zone ID
    void deleteByZone_ZoneId(Integer zoneId);
}