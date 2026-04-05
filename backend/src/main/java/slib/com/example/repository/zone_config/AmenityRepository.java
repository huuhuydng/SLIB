package slib.com.example.repository.zone_config;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import slib.com.example.entity.zone_config.AmenityEntity;

import java.util.List;

@Repository
public interface AmenityRepository extends JpaRepository<AmenityEntity, Integer> {
    List<AmenityEntity> findByZone_ZoneId(Integer zoneId);

    void deleteByZone_ZoneId(Integer zoneId);
}
