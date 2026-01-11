package slib.com.example.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import slib.com.example.entity.ZoneEntity;

@Repository
public interface ZoneRepository extends JpaRepository<ZoneEntity, Integer> {
}
