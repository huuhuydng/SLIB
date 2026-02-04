package slib.com.example.repository.ai;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import slib.com.example.entity.ai.MaterialItemEntity;

import java.util.List;

@Repository
public interface MaterialItemRepository extends JpaRepository<MaterialItemEntity, Long> {
    List<MaterialItemEntity> findByMaterialId(Long materialId);

    void deleteByMaterialId(Long materialId);
}
