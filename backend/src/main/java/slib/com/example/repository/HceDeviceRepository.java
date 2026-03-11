package slib.com.example.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    @Query("SELECT d FROM HceDeviceEntity d LEFT JOIN FETCH d.area " +
            "WHERE (CAST(:search AS text) IS NULL OR LOWER(d.deviceId) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%')) " +
            "OR LOWER(d.deviceName) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%')) " +
            "OR LOWER(d.location) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%'))) " +
            "AND (CAST(:status AS text) IS NULL OR d.status = :status) " +
            "AND (CAST(:deviceType AS text) IS NULL OR d.deviceType = :deviceType) " +
            "ORDER BY d.createdAt DESC")
    List<HceDeviceEntity> findAllWithFilters(
            @Param("search") String search,
            @Param("status") HceDeviceEntity.DeviceStatus status,
            @Param("deviceType") HceDeviceEntity.DeviceType deviceType);

    @Query("SELECT d FROM HceDeviceEntity d LEFT JOIN FETCH d.area ORDER BY d.createdAt DESC")
    List<HceDeviceEntity> findAllWithArea();
}
