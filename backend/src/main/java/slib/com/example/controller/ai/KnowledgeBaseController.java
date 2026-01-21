package slib.com.example.controller.ai;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import slib.com.example.entity.ai.KnowledgeBaseEntity;
import slib.com.example.service.ai.KnowledgeBaseService;

import java.util.List;
import java.util.Map;

/**
 * Admin endpoints for Knowledge Base management
 */
@RestController
@RequestMapping("/slib/ai/admin/knowledge")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class KnowledgeBaseController {

    @Autowired
    private KnowledgeBaseService knowledgeBaseService;

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
    public ResponseEntity<?> create(@RequestBody KnowledgeBaseEntity knowledge) {
        try {
            KnowledgeBaseEntity created = knowledgeBaseService.create(knowledge);
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
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody KnowledgeBaseEntity knowledge) {
        try {
            KnowledgeBaseEntity updated = knowledgeBaseService.update(id, knowledge);
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
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            knowledgeBaseService.delete(id);
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
