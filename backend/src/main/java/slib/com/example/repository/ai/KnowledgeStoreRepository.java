package slib.com.example.repository.ai;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import slib.com.example.entity.ai.KnowledgeStoreEntity;

import java.util.List;

@Repository
public interface KnowledgeStoreRepository extends JpaRepository<KnowledgeStoreEntity, Long> {
    List<KnowledgeStoreEntity> findByActiveTrue();

    List<KnowledgeStoreEntity> findAllByOrderByCreatedAtDesc();

    List<KnowledgeStoreEntity> findByStatus(KnowledgeStoreEntity.SyncStatus status);

    List<KnowledgeStoreEntity> findDistinctByItems_Id(Long itemId);

    List<KnowledgeStoreEntity> findDistinctByItems_Material_Id(Long materialId);
}
