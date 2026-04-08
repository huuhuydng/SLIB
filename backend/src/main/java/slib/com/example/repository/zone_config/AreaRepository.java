package slib.com.example.repository.zone_config;

import org.springframework.data.jpa.repository.JpaRepository;

import slib.com.example.entity.zone_config.AreaEntity;

public interface AreaRepository extends JpaRepository<AreaEntity, Long> {
    boolean existsByAreaNameIgnoreCase(String areaName);

    boolean existsByAreaNameIgnoreCaseAndAreaIdNot(String areaName, Long areaId);
}
