package slib.com.example.repository.ai;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import slib.com.example.entity.ai.PromptTemplateEntity;

import java.util.List;

@Repository
public interface PromptTemplateRepository extends JpaRepository<PromptTemplateEntity, Long> {

    List<PromptTemplateEntity> findByIsActiveTrueOrderByContextAsc();

    List<PromptTemplateEntity> findByContextAndIsActiveTrue(PromptTemplateEntity.PromptContext context);
}
