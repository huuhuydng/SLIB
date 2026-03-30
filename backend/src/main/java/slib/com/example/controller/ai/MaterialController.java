package slib.com.example.controller.ai;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import slib.com.example.dto.ai.MaterialDTO;
import slib.com.example.service.ai.MaterialService;
import slib.com.example.service.system.SystemLogService;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/slib/ai/admin/materials")
@RequiredArgsConstructor
public class MaterialController {

    private final MaterialService materialService;
    private final SystemLogService systemLogService;

    @GetMapping
    public ResponseEntity<List<MaterialDTO.MaterialResponse>> getAllMaterials() {
        return ResponseEntity.ok(materialService.getAllMaterials());
    }

    @GetMapping("/{id}")
    public ResponseEntity<MaterialDTO.MaterialResponse> getMaterialById(@PathVariable Long id) {
        return ResponseEntity.ok(materialService.getMaterialById(id));
    }

    @PostMapping
    public ResponseEntity<MaterialDTO.MaterialResponse> createMaterial(
            @RequestBody MaterialDTO.MaterialRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        String createdBy = userDetails != null ? userDetails.getUsername() : "system";
        MaterialDTO.MaterialResponse response = materialService.createMaterial(request, createdBy);
        systemLogService.logAudit("MaterialController",
                "Tạo material AI: " + response.getName(),
                null, createdBy);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<MaterialDTO.MaterialResponse> updateMaterial(
            @PathVariable Long id,
            @RequestBody MaterialDTO.MaterialRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        MaterialDTO.MaterialResponse response = materialService.updateMaterial(id, request);
        systemLogService.logAudit("MaterialController",
                "Cập nhật material AI: " + response.getName(),
                null, userDetails != null ? userDetails.getUsername() : null);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteMaterial(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        materialService.deleteMaterial(id);
        systemLogService.logAudit("MaterialController",
                "Xoá material AI: " + id,
                null, userDetails != null ? userDetails.getUsername() : null);
        return ResponseEntity.ok(Map.of("success", true, "message", "Material deleted"));
    }

    @PatchMapping("/{id}/toggle")
    public ResponseEntity<Map<String, Object>> toggleActive(
            @PathVariable Long id,
            @RequestParam boolean active,
            @AuthenticationPrincipal UserDetails userDetails) {
        materialService.toggleMaterialActive(id, active);
        systemLogService.logAudit("MaterialController",
                (active ? "Bật" : "Tắt") + " material AI: " + id,
                null, userDetails != null ? userDetails.getUsername() : null);
        return ResponseEntity.ok(Map.of("success", true, "active", active));
    }

    // ==================== ITEMS ====================

    @GetMapping("/{id}/items")
    public ResponseEntity<List<MaterialDTO.ItemResponse>> getItems(@PathVariable Long id) {
        return ResponseEntity.ok(materialService.getItemsByMaterialId(id));
    }

    @PostMapping("/{id}/items/text")
    public ResponseEntity<MaterialDTO.ItemResponse> addTextItem(
            @PathVariable Long id,
            @RequestBody MaterialDTO.ItemRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        MaterialDTO.ItemResponse response = materialService.addTextItem(id, request);
        systemLogService.logAudit("MaterialController",
                "Thêm text item vào material AI: " + id,
                null, userDetails != null ? userDetails.getUsername() : null);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/items/file")
    public ResponseEntity<MaterialDTO.ItemResponse> addFileItem(
            @PathVariable Long id,
            @RequestParam(required = false) String name,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails userDetails) throws IOException {
        MaterialDTO.ItemResponse response = materialService.addFileItem(id, name, file);
        systemLogService.logAudit("MaterialController",
                "Thêm file item vào material AI: " + id,
                null, userDetails != null ? userDetails.getUsername() : null);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{materialId}/items/{itemId}")
    public ResponseEntity<MaterialDTO.ItemResponse> updateItem(
            @PathVariable Long materialId,
            @PathVariable Long itemId,
            @RequestBody MaterialDTO.ItemRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        MaterialDTO.ItemResponse response = materialService.updateItem(materialId, itemId, request);
        systemLogService.logAudit("MaterialController",
                "Cập nhật item %d của material AI %d".formatted(itemId, materialId),
                null, userDetails != null ? userDetails.getUsername() : null);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{materialId}/items/{itemId}")
    public ResponseEntity<Map<String, Object>> deleteItem(
            @PathVariable Long materialId,
            @PathVariable Long itemId,
            @AuthenticationPrincipal UserDetails userDetails) {
        materialService.deleteItem(materialId, itemId);
        systemLogService.logAudit("MaterialController",
                "Xoá item %d của material AI %d".formatted(itemId, materialId),
                null, userDetails != null ? userDetails.getUsername() : null);
        return ResponseEntity.ok(Map.of("success", true, "message", "Item deleted"));
    }
}
