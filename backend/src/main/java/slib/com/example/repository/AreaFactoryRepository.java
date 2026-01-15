package slib.com.example.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import slib.com.example.entity.zone_config.AreaFactoryEntity;

import java.util.List;

public interface AreaFactoryRepository
        extends JpaRepository<AreaFactoryEntity, Long> {

    List<AreaFactoryEntity> findByArea_AreaId(Long areaId);
}
