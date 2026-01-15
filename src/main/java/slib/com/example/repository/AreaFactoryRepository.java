package slib.com.example.repository;

import slib.com.example.entity.AreaFactoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AreaFactoryRepository
        extends JpaRepository<AreaFactoryEntity, Long> {

    List<AreaFactoryEntity> findByArea_AreaId(Long areaId);
}
