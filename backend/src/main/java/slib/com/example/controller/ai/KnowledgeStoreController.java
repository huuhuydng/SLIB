package slib.com.example.controller.ai;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import slib.com.example.dto.ai.KnowledgeStoreDTO;
import slib.com.example.service.ai.KnowledgeStoreService;
import slib.com.example.service.system.SystemLogService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/slib/ai/admin/knowledge-stores")
@RequiredArgsConstructor
public class KnowledgeStoreController {

    private final KnowledgeStoreService knowledgeStoreService;
    private final SystemLogService systemLogService;

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
            @RequestBody KnowledgeStoreDTO.CreateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        String createdBy = userDetails != null ? userDetails.getUsername() : "system";
        KnowledgeStoreDTO.Response response = knowledgeStoreService.createKnowledgeStore(request, createdBy);
        systemLogService.logAudit("KnowledgeStoreController",
                "Tạo knowledge store: " + response.getName(),
                null, createdBy);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<KnowledgeStoreDTO.Response> updateKnowledgeStore(
            @PathVariable Long id,
            @RequestBody KnowledgeStoreDTO.UpdateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        KnowledgeStoreDTO.Response response = knowledgeStoreService.updateKnowledgeStore(id, request);
        systemLogService.logAudit("KnowledgeStoreController",
                "Cập nhật knowledge store: " + response.getName(),
                null, userDetails != null ? userDetails.getUsername() : null);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteKnowledgeStore(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        knowledgeStoreService.deleteKnowledgeStore(id);
        systemLogService.logAudit("KnowledgeStoreController",
                "Xoá knowledge store: " + id,
                null, userDetails != null ? userDetails.getUsername() : null);
        return ResponseEntity.ok(Map.of("success", true, "message", "Knowledge store deleted"));
    }

    @PostMapping("/{id}/sync")
    public ResponseEntity<KnowledgeStoreDTO.SyncResult> syncKnowledgeStore(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        KnowledgeStoreDTO.SyncResult response = knowledgeStoreService.syncKnowledgeStore(id);
        systemLogService.logAudit("KnowledgeStoreController",
                "Đồng bộ knowledge store: " + id,
                null, userDetails != null ? userDetails.getUsername() : null);
        return ResponseEntity.ok(response);
    }
}
