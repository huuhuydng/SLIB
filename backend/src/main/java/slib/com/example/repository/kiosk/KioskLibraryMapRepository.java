package slib.com.example.repository.kiosk;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import slib.com.example.entity.kiosk.KioskLibraryMapEntity;

import java.util.Optional;

@Repository
public interface KioskLibraryMapRepository extends JpaRepository<KioskLibraryMapEntity, Integer> {
    // Get the active/current library map
    Optional<KioskLibraryMapEntity> findByIsActiveTrue();
}
