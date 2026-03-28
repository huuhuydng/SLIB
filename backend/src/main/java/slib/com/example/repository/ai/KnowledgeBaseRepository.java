package slib.com.example.repository.ai;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import slib.com.example.entity.ai.KnowledgeBaseEntity;

import java.util.List;

@Repository
public interface KnowledgeBaseRepository extends JpaRepository<KnowledgeBaseEntity, Long> {

    List<KnowledgeBaseEntity> findByIsActiveTrueOrderByUpdatedAtDesc();

    List<KnowledgeBaseEntity> findByTypeAndIsActiveTrue(KnowledgeBaseEntity.KnowledgeType type);

    @Query("SELECT k FROM KnowledgeBaseEntity k WHERE k.isActive = true ORDER BY k.type, k.updatedAt DESC")
    List<KnowledgeBaseEntity> findAllActiveGroupedByType();
}
