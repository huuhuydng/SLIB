package slib.com.example.controller.ai;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import slib.com.example.dto.ai.KnowledgeStoreDTO;
import slib.com.example.service.ai.KnowledgeStoreService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/slib/ai/admin/knowledge-stores")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class KnowledgeStoreController {

    private final KnowledgeStoreService knowledgeStoreService;

    @GetMapping
    public ResponseEntity<List<KnowledgeStoreDTO.Response>> getAllKnowledgeStores() {
        return ResponseEntity.ok(knowledgeStoreService.getAllKnowledgeStores());
    }

    @GetMapping("/{id}")
    public ResponseEntity<KnowledgeStoreDTO.Response> getKnowledgeStoreById(@PathVariable Long id) {
        return ResponseEntity.ok(knowledgeStoreService.getKnowledgeStoreById(id));
    }

    @PostMapping
    public ResponseEntity<KnowledgeStoreDTO.Response> createKnowledgeStore(
            @RequestBody KnowledgeStoreDTO.CreateRequest request) {
        // TODO: Get createdBy from auth context
        return ResponseEntity.ok(knowledgeStoreService.createKnowledgeStore(request, "admin"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<KnowledgeStoreDTO.Response> updateKnowledgeStore(
            @PathVariable Long id,
            @RequestBody KnowledgeStoreDTO.UpdateRequest request) {
        return ResponseEntity.ok(knowledgeStoreService.updateKnowledgeStore(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteKnowledgeStore(@PathVariable Long id) {
        knowledgeStoreService.deleteKnowledgeStore(id);
        return ResponseEntity.ok(Map.of("success", true, "message", "Knowledge store deleted"));
    }

    @PostMapping("/{id}/sync")
    public ResponseEntity<KnowledgeStoreDTO.SyncResult> syncKnowledgeStore(@PathVariable Long id) {
        return ResponseEntity.ok(knowledgeStoreService.syncKnowledgeStore(id));
    }
}
