package slib.com.example.controller.ai;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import slib.com.example.entity.ai.KnowledgeBaseEntity;
import slib.com.example.service.ai.KnowledgeBaseService;
import slib.com.example.service.system.SystemLogService;

import java.util.List;
import java.util.Map;

/**
 * Admin endpoints for Knowledge Base management
 */
@RestController
@RequestMapping("/slib/ai/admin/knowledge")
public class KnowledgeBaseController {

    @Autowired
    private KnowledgeBaseService knowledgeBaseService;
    @Autowired
    private SystemLogService systemLogService;

    /**
     * Get all knowledge items
     */
    @GetMapping
    public ResponseEntity<List<KnowledgeBaseEntity>> getAll() {
        return ResponseEntity.ok(knowledgeBaseService.getAllKnowledge());
    }

    /**
     * Get single knowledge item
     */
    @GetMapping("/{id}")
    public ResponseEntity<KnowledgeBaseEntity> getById(@PathVariable Long id) {
        return ResponseEntity.ok(knowledgeBaseService.getById(id));
    }

    /**
     * Create new knowledge
     */
    @PostMapping
    public ResponseEntity<?> create(
            @RequestBody KnowledgeBaseEntity knowledge,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            KnowledgeBaseEntity created = knowledgeBaseService.create(knowledge);
            systemLogService.logAudit("KnowledgeBaseController",
                    "Tạo knowledge base item: " + created.getTitle(),
                    null, userDetails != null ? userDetails.getUsername() : null);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Đã thêm kiến thức mới",
                    "data", created));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi tạo kiến thức: " + e.getMessage()));
        }
    }

    /**
     * Update knowledge
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> update(
            @PathVariable Long id,
            @RequestBody KnowledgeBaseEntity knowledge,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            KnowledgeBaseEntity updated = knowledgeBaseService.update(id, knowledge);
            systemLogService.logAudit("KnowledgeBaseController",
                    "Cập nhật knowledge base item: " + id,
                    null, userDetails != null ? userDetails.getUsername() : null);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Đã cập nhật kiến thức",
                    "data", updated));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi cập nhật: " + e.getMessage()));
        }
    }

    /**
     * Delete knowledge
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            knowledgeBaseService.delete(id);
            systemLogService.logAudit("KnowledgeBaseController",
                    "Xoá knowledge base item: " + id,
                    null, userDetails != null ? userDetails.getUsername() : null);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Đã xóa kiến thức"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi xóa: " + e.getMessage()));
        }
    }
}
