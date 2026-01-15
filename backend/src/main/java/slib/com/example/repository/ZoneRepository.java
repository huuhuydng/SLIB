package slib.com.example.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import slib.com.example.entity.zone_config.ZoneEntity;

import java.util.List;

@Repository
public interface ZoneRepository extends JpaRepository<ZoneEntity, Integer> {
    List<ZoneEntity> findByArea_AreaId(Long areaId);
}
