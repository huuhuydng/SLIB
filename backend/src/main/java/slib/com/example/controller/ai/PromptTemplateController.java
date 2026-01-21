package slib.com.example.controller.ai;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import slib.com.example.entity.ai.PromptTemplateEntity;
import slib.com.example.service.ai.PromptTemplateService;

import java.util.List;
import java.util.Map;

/**
 * Admin endpoints for Prompt Template management
 */
@RestController
@RequestMapping("/slib/ai/admin/prompts")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class PromptTemplateController {

    @Autowired
    private PromptTemplateService promptTemplateService;

    /**
     * Get all prompt templates
     */
    @GetMapping
    public ResponseEntity<List<PromptTemplateEntity>> getAll() {
        return ResponseEntity.ok(promptTemplateService.getAllPrompts());
    }

    /**
     * Get single prompt
     */
    @GetMapping("/{id}")
    public ResponseEntity<PromptTemplateEntity> getById(@PathVariable Long id) {
        return ResponseEntity.ok(promptTemplateService.getById(id));
    }

    /**
     * Create new prompt
     */
    @PostMapping
    public ResponseEntity<?> create(@RequestBody PromptTemplateEntity prompt) {
        try {
            PromptTemplateEntity created = promptTemplateService.create(prompt);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Đã thêm prompt mới",
                    "data", created));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi tạo prompt: " + e.getMessage()));
        }
    }

    /**
     * Update prompt
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody PromptTemplateEntity prompt) {
        try {
            PromptTemplateEntity updated = promptTemplateService.update(id, prompt);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Đã cập nhật prompt",
                    "data", updated));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi cập nhật: " + e.getMessage()));
        }
    }

    /**
     * Delete prompt
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            promptTemplateService.delete(id);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Đã xóa prompt"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi xóa: " + e.getMessage()));
        }
    }
}
