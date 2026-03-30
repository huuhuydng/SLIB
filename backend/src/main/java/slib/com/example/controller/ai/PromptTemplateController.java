package slib.com.example.controller.ai;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import slib.com.example.entity.ai.PromptTemplateEntity;
import slib.com.example.service.ai.PromptTemplateService;
import slib.com.example.service.system.SystemLogService;

import java.util.List;
import java.util.Map;

/**
 * Admin endpoints for Prompt Template management
 */
@RestController
@RequestMapping("/slib/ai/admin/prompts")
public class PromptTemplateController {

    @Autowired
    private PromptTemplateService promptTemplateService;
    @Autowired
    private SystemLogService systemLogService;

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
    public ResponseEntity<?> create(
            @RequestBody PromptTemplateEntity prompt,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            PromptTemplateEntity created = promptTemplateService.create(prompt);
            systemLogService.logAudit("PromptTemplateController",
                    "Tạo prompt template: " + created.getName(),
                    null, userDetails != null ? userDetails.getUsername() : null);
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
    public ResponseEntity<?> update(
            @PathVariable Long id,
            @RequestBody PromptTemplateEntity prompt,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            PromptTemplateEntity updated = promptTemplateService.update(id, prompt);
            systemLogService.logAudit("PromptTemplateController",
                    "Cập nhật prompt template: " + id,
                    null, userDetails != null ? userDetails.getUsername() : null);
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
    public ResponseEntity<?> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            promptTemplateService.delete(id);
            systemLogService.logAudit("PromptTemplateController",
                    "Xoá prompt template: " + id,
                    null, userDetails != null ? userDetails.getUsername() : null);
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
