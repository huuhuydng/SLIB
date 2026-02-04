package slib.com.example.repository.ai;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import slib.com.example.entity.ai.MaterialEntity;

import java.util.List;

@Repository
public interface MaterialRepository extends JpaRepository<MaterialEntity, Long> {
    List<MaterialEntity> findByActiveTrue();

    List<MaterialEntity> findAllByOrderByCreatedAtDesc();
}
