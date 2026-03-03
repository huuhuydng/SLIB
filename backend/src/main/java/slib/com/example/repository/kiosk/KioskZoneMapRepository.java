package slib.com.example.repository.kiosk;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import slib.com.example.entity.kiosk.KioskZoneMapEntity;

import java.util.List;

@Repository
public interface KioskZoneMapRepository extends JpaRepository<KioskZoneMapEntity, Integer> {
    // Get all zones for a specific library map
    List<KioskZoneMapEntity> findByLibraryMapIdOrderByZoneName(Integer libraryMapId);
}
