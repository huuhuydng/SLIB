package slib.com.example.controller.ai;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import slib.com.example.dto.ai.MaterialDTO;
import slib.com.example.service.ai.MaterialService;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/slib/ai/admin/materials")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class MaterialController {

    private final MaterialService materialService;

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
            @RequestBody MaterialDTO.MaterialRequest request) {
        // TODO: Get createdBy from auth context
        return ResponseEntity.ok(materialService.createMaterial(request, "admin"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MaterialDTO.MaterialResponse> updateMaterial(
            @PathVariable Long id,
            @RequestBody MaterialDTO.MaterialRequest request) {
        return ResponseEntity.ok(materialService.updateMaterial(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteMaterial(@PathVariable Long id) {
        materialService.deleteMaterial(id);
        return ResponseEntity.ok(Map.of("success", true, "message", "Material deleted"));
    }

    @PatchMapping("/{id}/toggle")
    public ResponseEntity<Map<String, Object>> toggleActive(
            @PathVariable Long id,
            @RequestParam boolean active) {
        materialService.toggleMaterialActive(id, active);
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
            @RequestBody MaterialDTO.ItemRequest request) {
        return ResponseEntity.ok(materialService.addTextItem(id, request));
    }

    @PostMapping("/{id}/items/file")
    public ResponseEntity<MaterialDTO.ItemResponse> addFileItem(
            @PathVariable Long id,
            @RequestParam(required = false) String name,
            @RequestParam("file") MultipartFile file) throws IOException {
        return ResponseEntity.ok(materialService.addFileItem(id, name, file));
    }

    @PutMapping("/{materialId}/items/{itemId}")
    public ResponseEntity<MaterialDTO.ItemResponse> updateItem(
            @PathVariable Long materialId,
            @PathVariable Long itemId,
            @RequestBody MaterialDTO.ItemRequest request) {
        return ResponseEntity.ok(materialService.updateItem(materialId, itemId, request));
    }

    @DeleteMapping("/{materialId}/items/{itemId}")
    public ResponseEntity<Map<String, Object>> deleteItem(
            @PathVariable Long materialId,
            @PathVariable Long itemId) {
        materialService.deleteItem(materialId, itemId);
        return ResponseEntity.ok(Map.of("success", true, "message", "Item deleted"));
    }
}
