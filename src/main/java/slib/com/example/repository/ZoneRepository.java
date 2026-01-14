package slib.com.example.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import slib.com.example.entity.ZoneEntity;

import java.util.List;

public interface ZoneRepository extends JpaRepository<ZoneEntity, Integer> {

    List<ZoneEntity> findByArea_AreaId(Long areaId);

}
