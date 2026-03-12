package slib.com.example.repository.kiosk;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import slib.com.example.entity.kiosk.KioskConfigEntity;

import java.util.List;
import java.util.Optional;

/**
 * Kiosk Config Repository
 */
@Repository
public interface KioskConfigRepository extends JpaRepository<KioskConfigEntity, Integer> {

    Optional<KioskConfigEntity> findByKioskCode(String kioskCode);

    List<KioskConfigEntity> findByIsActive(Boolean isActive);

    boolean existsByKioskCode(String kioskCode);

    Optional<KioskConfigEntity> findByDeviceToken(String deviceToken);
}
