package slib.com.example.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import slib.com.example.entity.SeatEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


import java.util.List;

@Repository
public interface SeatRepository extends JpaRepository<SeatEntity, Integer> {
    List<SeatEntity> findByZone_ZoneId(Integer zoneId);

     @Query("SELECT MAX(s.columnNumber) FROM SeatEntity s WHERE s.zone.zoneId = :zoneId AND s.rowNumber = :rowNumber")
    Integer findMaxColumnByZoneIdAndRow(@Param("zoneId") Integer zoneId, @Param("rowNumber") Integer rowNumber);
}

