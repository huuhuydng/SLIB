package slib.com.example.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import slib.com.example.entity.SeatEntity;
import slib.com.example.entity.SeatStatus;

import java.util.List;
import java.util.Optional;

@Repository
public interface SeatRepository extends JpaRepository<SeatEntity, Integer> {
    
    // === Mobile App Methods ===
    List<SeatEntity> findByZone_ZoneId(Integer zoneId);
    
    long countByZone_ZoneIdAndSeatStatus(Integer zoneId, SeatStatus seatStatus);
    
    // === Web Admin Methods ===
    Optional<SeatEntity> findBySeatCode(String seatCode);
    
    List<SeatEntity> findByZone_ZoneName(String zoneName);
    
    List<SeatEntity> findBySeatStatus(SeatStatus seatStatus);
    
    /**
     * Lấy tất cả ghế available theo zone name
     * Dùng cho web admin filter seats
     */
    @Query("SELECT s FROM SeatEntity s WHERE s.zone.zoneName = :zoneName AND s.seatStatus = slib.com.example.entity.SeatStatus.AVAILABLE")
    List<SeatEntity> findAvailableByZoneName(@Param("zoneName") String zoneName);
    
    /**
     * Lấy tất cả ghế available trong toàn bộ thư viện
     * Dùng cho quick statistics
     */
    @Query("SELECT s FROM SeatEntity s WHERE s.seatStatus = slib.com.example.entity.SeatStatus.AVAILABLE")
    List<SeatEntity> findAllAvailableSeats();
}