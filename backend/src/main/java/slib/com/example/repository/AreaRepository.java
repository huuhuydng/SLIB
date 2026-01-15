package slib.com.example.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import slib.com.example.entity.zone_config.AreaEntity;

public interface AreaRepository extends JpaRepository<AreaEntity, Long> {
}
