package slib.com.example.service.ai;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import slib.com.example.dto.ai.MaterialDTO;
import slib.com.example.entity.ai.MaterialEntity;
import slib.com.example.entity.ai.MaterialItemEntity;
import slib.com.example.repository.ai.MaterialItemRepository;
import slib.com.example.repository.ai.MaterialRepository;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MaterialService {

    private final MaterialRepository materialRepository;
    private final MaterialItemRepository materialItemRepository;

    @Value("${app.upload.dir:uploads/materials}")
    private String uploadDir;

    // ==================== MATERIAL CRUD ====================

    public List<MaterialDTO.MaterialResponse> getAllMaterials() {
        return materialRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public MaterialDTO.MaterialResponse getMaterialById(Long id) {
        MaterialEntity material = materialRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Material not found: " + id));
        return toResponse(material);
    }

    @Transactional
    public MaterialDTO.MaterialResponse createMaterial(MaterialDTO.MaterialRequest request, String createdBy) {
        MaterialEntity material = MaterialEntity.builder()
                .name(request.getName())
                .description(request.getDescription())
                .createdBy(createdBy)
                .active(true)
                .build();
        material = materialRepository.save(material);
        log.info("Created material: {} by {}", material.getName(), createdBy);
        return toResponse(material);
    }

    @Transactional
    public MaterialDTO.MaterialResponse updateMaterial(Long id, MaterialDTO.MaterialRequest request) {
        MaterialEntity material = materialRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Material not found: " + id));
        material.setName(request.getName());
        material.setDescription(request.getDescription());
        material = materialRepository.save(material);
        return toResponse(material);
    }

    @Transactional
    public void deleteMaterial(Long id) {
        materialRepository.deleteById(id);
        log.info("Deleted material: {}", id);
    }

    @Transactional
    public void toggleMaterialActive(Long id, boolean active) {
        MaterialEntity material = materialRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Material not found: " + id));
        material.setActive(active);
        materialRepository.save(material);
    }

    // ==================== ITEM CRUD ====================

    @Transactional
    public MaterialDTO.ItemResponse addTextItem(Long materialId, MaterialDTO.ItemRequest request) {
        MaterialEntity material = materialRepository.findById(materialId)
                .orElseThrow(() -> new RuntimeException("Material not found: " + materialId));

        MaterialItemEntity item = MaterialItemEntity.builder()
                .material(material)
                .name(request.getName())
                .type(MaterialItemEntity.ItemType.TEXT)
                .content(request.getContent())
                .build();
        item = materialItemRepository.save(item);
        log.info("Added text item {} to material {}", item.getName(), materialId);
        return toItemResponse(item);
    }

    @Transactional
    public MaterialDTO.ItemResponse addFileItem(Long materialId, String name, MultipartFile file) throws IOException {
        MaterialEntity material = materialRepository.findById(materialId)
                .orElseThrow(() -> new RuntimeException("Material not found: " + materialId));

        // Ensure upload directory exists
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String storedFilename = UUID.randomUUID().toString() + extension;
        Path filePath = uploadPath.resolve(storedFilename);

        // Save file
        file.transferTo(filePath.toFile());

        MaterialItemEntity item = MaterialItemEntity.builder()
                .material(material)
                .name(name != null ? name : originalFilename)
                .type(MaterialItemEntity.ItemType.FILE)
                .fileName(originalFilename)
                .filePath(filePath.toString())
                .fileSize(file.getSize())
                .build();
        item = materialItemRepository.save(item);
        log.info("Added file item {} to material {}", item.getName(), materialId);
        return toItemResponse(item);
    }

    @Transactional
    public void deleteItem(Long materialId, Long itemId) {
        MaterialItemEntity item = materialItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Item not found: " + itemId));

        // Delete file if exists
        if (item.getType() == MaterialItemEntity.ItemType.FILE && item.getFilePath() != null) {
            try {
                Files.deleteIfExists(Paths.get(item.getFilePath()));
            } catch (IOException e) {
                log.warn("Could not delete file: {}", item.getFilePath());
            }
        }

        materialItemRepository.delete(item);
        log.info("Deleted item {} from material {}", itemId, materialId);
    }

    @Transactional
    public MaterialDTO.ItemResponse updateItem(Long materialId, Long itemId, MaterialDTO.ItemRequest request) {
        MaterialItemEntity item = materialItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Item not found: " + itemId));

        // Only allow updating TEXT items' content
        if (request.getName() != null) {
            item.setName(request.getName());
        }
        if (item.getType() == MaterialItemEntity.ItemType.TEXT && request.getContent() != null) {
            item.setContent(request.getContent());
        }

        item = materialItemRepository.save(item);
        log.info("Updated item {} in material {}", itemId, materialId);
        return toItemResponse(item);
    }

    public List<MaterialDTO.ItemResponse> getItemsByMaterialId(Long materialId) {
        return materialItemRepository.findByMaterialId(materialId)
                .stream()
                .map(this::toItemResponse)
                .collect(Collectors.toList());
    }

    // ==================== MAPPERS ====================

    private MaterialDTO.MaterialResponse toResponse(MaterialEntity entity) {
        List<MaterialDTO.ItemResponse> items = entity.getItems() != null
                ? entity.getItems().stream().map(this::toItemResponse).collect(Collectors.toList())
                : List.of();

        return MaterialDTO.MaterialResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .createdBy(entity.getCreatedBy())
                .active(entity.getActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .items(items)
                .itemCount(items.size())
                .build();
    }

    private MaterialDTO.ItemResponse toItemResponse(MaterialItemEntity entity) {
        return MaterialDTO.ItemResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .type(entity.getType())
                .content(entity.getContent())
                .fileName(entity.getFileName())
                .fileSize(entity.getFileSize())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
