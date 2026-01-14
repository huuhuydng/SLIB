package slib.com.example.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import slib.com.example.entity.SeatEntity;
<<<<<<< HEAD
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

=======
import slib.com.example.entity.SeatStatus;
>>>>>>> 9e7981680528c51139544e478f7f9919199c239c

import java.util.List;

@Repository
public interface SeatRepository extends JpaRepository<SeatEntity, Integer> {
<<<<<<< HEAD
    List<SeatEntity> findByZone_ZoneId(Integer zoneId);

     @Query("SELECT MAX(s.columnNumber) FROM SeatEntity s WHERE s.zone.zoneId = :zoneId AND s.rowNumber = :rowNumber")
    Integer findMaxColumnByZoneIdAndRow(@Param("zoneId") Integer zoneId, @Param("rowNumber") Integer rowNumber);
}

=======
    List<SeatEntity> findByZone_ZoneId(Integer zoneId);  
    long countByZone_ZoneIdAndSeatStatus(Integer zoneId, SeatStatus seatStatus);
}
>>>>>>> 9e7981680528c51139544e478f7f9919199c239c
