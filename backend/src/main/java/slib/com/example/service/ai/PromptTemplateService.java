package slib.com.example.service.ai;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import slib.com.example.entity.ai.PromptTemplateEntity;
import slib.com.example.repository.ai.PromptTemplateRepository;

import java.util.List;

@Service
public class PromptTemplateService {

    @Autowired
    private PromptTemplateRepository promptTemplateRepository;

    public List<PromptTemplateEntity> getAllPrompts() {
        return promptTemplateRepository.findAll();
    }

    public List<PromptTemplateEntity> getActivePrompts() {
        return promptTemplateRepository.findByIsActiveTrueOrderByContextAsc();
    }

    public PromptTemplateEntity getById(Long id) {
        return promptTemplateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Prompt template not found with id: " + id));
    }

    @Transactional
    public PromptTemplateEntity create(PromptTemplateEntity prompt) {
        return promptTemplateRepository.save(prompt);
    }

    @Transactional
    public PromptTemplateEntity update(Long id, PromptTemplateEntity promptDetails) {
        PromptTemplateEntity existing = getById(id);
        existing.setName(promptDetails.getName());
        existing.setPrompt(promptDetails.getPrompt());
        existing.setContext(promptDetails.getContext());
        existing.setIsActive(promptDetails.getIsActive());
        return promptTemplateRepository.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        if (!promptTemplateRepository.existsById(id)) {
            throw new RuntimeException("Prompt template not found to delete");
        }
        promptTemplateRepository.deleteById(id);
    }

    /**
     * Get prompt for specific context, fallback to GENERAL
     */
    public String getPromptForContext(PromptTemplateEntity.PromptContext context) {
        List<PromptTemplateEntity> prompts = promptTemplateRepository.findByContextAndIsActiveTrue(context);
        if (!prompts.isEmpty()) {
            return prompts.get(0).getPrompt();
        }

        // Fallback to GENERAL
        prompts = promptTemplateRepository.findByContextAndIsActiveTrue(PromptTemplateEntity.PromptContext.GENERAL);
        if (!prompts.isEmpty()) {
            return prompts.get(0).getPrompt();
        }

        return "";
    }
}
