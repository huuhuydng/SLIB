package slib.com.example.repository.kiosk;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import slib.com.example.entity.kiosk.KioskImageEntity;

import java.util.List;

@Repository
public interface KioskImageRepository extends JpaRepository<KioskImageEntity, Integer> {
    // Get list sorted by display order (asc) and then created time (desc)
    List<KioskImageEntity> findAllByOrderByDisplayOrderAscCreatedAtDesc();
}