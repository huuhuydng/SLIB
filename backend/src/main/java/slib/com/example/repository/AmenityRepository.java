package slib.com.example.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import slib.com.example.entity.zone_config.AmenityEntity;

import java.util.List;

@Repository
public interface AmenityRepository extends JpaRepository<AmenityEntity, Integer> {
    List<AmenityEntity> findByZone_ZoneId(Integer zoneId);
}