package slib.com.example.repository.hce;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import slib.com.example.entity.hce.HceDeviceEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface HceDeviceRepository extends JpaRepository<HceDeviceEntity, Integer> {

    Optional<HceDeviceEntity> findByDeviceId(String deviceId);

    boolean existsByDeviceId(String deviceId);

    List<HceDeviceEntity> findByStatus(HceDeviceEntity.DeviceStatus status);

    List<HceDeviceEntity> findByDeviceType(HceDeviceEntity.DeviceType deviceType);

    @Query("SELECT d FROM HceDeviceEntity d LEFT JOIN FETCH d.area ORDER BY d.createdAt DESC")
    List<HceDeviceEntity> findAllWithArea();
}
