package slib.com.example.service.ai;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import slib.com.example.entity.ai.KnowledgeBaseEntity;
import slib.com.example.repository.ai.KnowledgeBaseRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class KnowledgeBaseService {

    @Autowired
    private KnowledgeBaseRepository knowledgeBaseRepository;

    public List<KnowledgeBaseEntity> getAllKnowledge() {
        return knowledgeBaseRepository.findAll();
    }

    public List<KnowledgeBaseEntity> getActiveKnowledge() {
        return knowledgeBaseRepository.findByIsActiveTrueOrderByUpdatedAtDesc();
    }

    public KnowledgeBaseEntity getById(Long id) {
        return knowledgeBaseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Knowledge not found with id: " + id));
    }

    @Transactional
    public KnowledgeBaseEntity create(KnowledgeBaseEntity knowledge) {
        return knowledgeBaseRepository.save(knowledge);
    }

    @Transactional
    public KnowledgeBaseEntity update(Long id, KnowledgeBaseEntity knowledgeDetails) {
        KnowledgeBaseEntity existing = getById(id);
        existing.setTitle(knowledgeDetails.getTitle());
        existing.setContent(knowledgeDetails.getContent());
        existing.setType(knowledgeDetails.getType());
        existing.setIsActive(knowledgeDetails.getIsActive());
        return knowledgeBaseRepository.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        if (!knowledgeBaseRepository.existsById(id)) {
            throw new RuntimeException("Knowledge not found to delete");
        }
        knowledgeBaseRepository.deleteById(id);
    }

    /**
     * Build knowledge context string for AI prompt
     * Format: "## [Type]\n- [Title]: [Content]\n..."
     */
    public String buildKnowledgeContext() {
        List<KnowledgeBaseEntity> activeKnowledge = knowledgeBaseRepository.findAllActiveGroupedByType();

        if (activeKnowledge.isEmpty()) {
            return "";
        }

        StringBuilder context = new StringBuilder();
        context.append("\n\n--- KIẾN THỨC VỀ THƯ VIỆN ---\n");

        // Group by type
        var groupedByType = activeKnowledge.stream()
                .collect(Collectors.groupingBy(KnowledgeBaseEntity::getType));

        for (var entry : groupedByType.entrySet()) {
            String typeName = switch (entry.getKey()) {
                case RULES -> "QUY ĐỊNH";
                case GUIDE -> "HƯỚNG DẪN";
                case INFO -> "THÔNG TIN CHUNG";
            };

            context.append("\n## ").append(typeName).append("\n");
            for (KnowledgeBaseEntity k : entry.getValue()) {
                context.append("- ").append(k.getTitle()).append(": ").append(k.getContent()).append("\n");
            }
        }

        context.append("\n--- HẾT KIẾN THỨC ---\n");
        return context.toString();
    }
}
