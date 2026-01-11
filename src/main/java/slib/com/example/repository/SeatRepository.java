package slib.com.example.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import slib.com.example.entity.SeatEntity;
import slib.com.example.entity.SeatStatus;

import java.util.List;

@Repository
public interface SeatRepository extends JpaRepository<SeatEntity, Integer> {
    List<SeatEntity> findByZone_ZoneId(Integer zoneId);  
    long countByZone_ZoneIdAndSeatStatus(Integer zoneId, SeatStatus seatStatus);
}