package slib.com.example.repository;

import org.springframework.data.jpa.repository.JpaRepository;
<<<<<<< HEAD
import slib.com.example.entity.ZoneEntity;

import java.util.List;

public interface ZoneRepository extends JpaRepository<ZoneEntity, Integer> {

    List<ZoneEntity> findByArea_AreaId(Long areaId);

=======
import org.springframework.stereotype.Repository;
import slib.com.example.entity.ZoneEntity;

@Repository
public interface ZoneRepository extends JpaRepository<ZoneEntity, Integer> {
>>>>>>> 9e7981680528c51139544e478f7f9919199c239c
}
