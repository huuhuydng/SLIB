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

    // Find all seats that have NFC tag UID assigned
    List<SeatEntity> findByNfcTagUidIsNotNull();

    // Find seats by area (via zone relationship)
    @Query("SELECT s FROM SeatEntity s WHERE s.zone.area.areaId = :areaId")
    List<SeatEntity> findByAreaId(@Param("areaId") Integer areaId);

    // Delete all seats by zone ID
    void deleteByZone_ZoneId(Integer zoneId);
}