package slib.com.example.service.ai;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import slib.com.example.dto.ai.MaterialDTO;
import slib.com.example.entity.ai.KnowledgeStoreEntity;
import slib.com.example.entity.ai.MaterialEntity;
import slib.com.example.entity.ai.MaterialItemEntity;
import slib.com.example.repository.ai.KnowledgeStoreRepository;
import slib.com.example.repository.ai.MaterialItemRepository;
import slib.com.example.repository.ai.MaterialRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MaterialService {

    private static final Set<String> ALLOWED_FILE_EXTENSIONS = Set.of("pdf", "doc", "docx", "txt", "md");

    private final MaterialRepository materialRepository;
    private final MaterialItemRepository materialItemRepository;
    private final KnowledgeStoreRepository knowledgeStoreRepository;

    @Value("${app.upload.dir:uploads/materials}")
    private String uploadDir;

    @Value("${slib.ai.material.max-file-size-bytes:15728640}")
    private long maxFileSizeBytes;

    @Transactional(readOnly = true)
    public List<MaterialDTO.MaterialResponse> getAllMaterials() {
        return materialRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public MaterialDTO.MaterialResponse getMaterialById(Long id) {
        MaterialEntity material = materialRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Material not found: " + id));
        return toResponse(material);
    }

    @Transactional
    public MaterialDTO.MaterialResponse createMaterial(MaterialDTO.MaterialRequest request, String createdBy) {
        validateMaterialRequest(request);

        MaterialEntity material = MaterialEntity.builder()
                .name(normalizeRequiredText(request.getName(), "Tên nhóm tài liệu"))
                .description(normalizeOptionalText(request.getDescription()))
                .createdBy(createdBy)
                .active(true)
                .build();
        material = materialRepository.save(material);
        log.info("Created material: {} by {}", material.getName(), createdBy);
        return toResponse(material);
    }

    @Transactional
    public MaterialDTO.MaterialResponse updateMaterial(Long id, MaterialDTO.MaterialRequest request) {
        validateMaterialRequest(request);

        MaterialEntity material = materialRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Material not found: " + id));
        material.setName(normalizeRequiredText(request.getName(), "Tên nhóm tài liệu"));
        material.setDescription(normalizeOptionalText(request.getDescription()));
        material = materialRepository.save(material);
        return toResponse(material);
    }

    @Transactional
    public void deleteMaterial(Long id) {
        materialRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Material not found: " + id));

        markKnowledgeStoresChangedByMaterialId(id);
        materialItemRepository.findByMaterialId(id).forEach(this::deleteFileIfPresent);
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

    @Transactional
    public MaterialDTO.ItemResponse addTextItem(Long materialId, MaterialDTO.ItemRequest request) {
        MaterialEntity material = materialRepository.findById(materialId)
                .orElseThrow(() -> new RuntimeException("Material not found: " + materialId));

        MaterialItemEntity item = MaterialItemEntity.builder()
                .material(material)
                .name(normalizeRequiredText(request.getName(), "Tên mục tài liệu"))
                .type(MaterialItemEntity.ItemType.TEXT)
                .content(normalizeRequiredText(request.getContent(), "Nội dung mục văn bản"))
                .build();
        item = materialItemRepository.save(item);
        log.info("Added text item {} to material {}", item.getName(), materialId);
        return toItemResponse(item);
    }

    @Transactional
    public MaterialDTO.ItemResponse addFileItem(Long materialId, String name, MultipartFile file) throws IOException {
        MaterialEntity material = materialRepository.findById(materialId)
                .orElseThrow(() -> new RuntimeException("Material not found: " + materialId));
        validateFileUpload(file);

        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String originalFilename = normalizeRequiredText(file.getOriginalFilename(), "Tên tệp");
        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String storedFilename = UUID.randomUUID() + extension;
        Path filePath = uploadPath.resolve(storedFilename);
        file.transferTo(filePath.toFile());

        MaterialItemEntity item = MaterialItemEntity.builder()
                .material(material)
                .name(name != null && !name.isBlank() ? normalizeRequiredText(name, "Tên mục tài liệu") : originalFilename)
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

        markKnowledgeStoresChangedByItemId(itemId);
        deleteFileIfPresent(item);
        materialItemRepository.delete(item);
        log.info("Deleted item {} from material {}", itemId, materialId);
    }

    @Transactional
    public MaterialDTO.ItemResponse updateItem(Long materialId, Long itemId, MaterialDTO.ItemRequest request) {
        MaterialItemEntity item = materialItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Item not found: " + itemId));

        if (request.getName() != null) {
            item.setName(normalizeRequiredText(request.getName(), "Tên mục tài liệu"));
        }
        if (item.getType() == MaterialItemEntity.ItemType.TEXT && request.getContent() != null) {
            item.setContent(normalizeRequiredText(request.getContent(), "Nội dung mục văn bản"));
        }

        item = materialItemRepository.save(item);
        markKnowledgeStoresChangedByItemId(itemId);
        log.info("Updated item {} in material {}", itemId, materialId);
        return toItemResponse(item);
    }

    @Transactional(readOnly = true)
    public List<MaterialDTO.ItemResponse> getItemsByMaterialId(Long materialId) {
        return materialItemRepository.findByMaterialId(materialId)
                .stream()
                .map(this::toItemResponse)
                .collect(Collectors.toList());
    }

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

    private void validateMaterialRequest(MaterialDTO.MaterialRequest request) {
        normalizeRequiredText(request.getName(), "Tên nhóm tài liệu");
    }

    private void validateFileUpload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn tệp tài liệu để tải lên");
        }
        String originalFilename = normalizeRequiredText(file.getOriginalFilename(), "Tên tệp");
        if (!originalFilename.contains(".")) {
            throw new IllegalArgumentException("Tệp tải lên phải có phần mở rộng hợp lệ");
        }
        String extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase(Locale.ROOT);
        if (!ALLOWED_FILE_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("Chỉ hỗ trợ các tệp PDF, DOC, DOCX, TXT hoặc MD");
        }
        if (file.getSize() > maxFileSizeBytes) {
            throw new IllegalArgumentException("Tệp tải lên vượt quá dung lượng cho phép");
        }
    }

    private void deleteFileIfPresent(MaterialItemEntity item) {
        if (item.getType() != MaterialItemEntity.ItemType.FILE || item.getFilePath() == null) {
            return;
        }
        try {
            Files.deleteIfExists(Paths.get(item.getFilePath()));
        } catch (IOException e) {
            log.warn("Could not delete file: {}", item.getFilePath());
        }
    }

    private void markKnowledgeStoresChangedByItemId(Long itemId) {
        if (itemId == null) {
            return;
        }

        List<KnowledgeStoreEntity> stores = knowledgeStoreRepository.findDistinctByItems_Id(itemId);
        markKnowledgeStoresChanged(stores, "item " + itemId);
    }

    private void markKnowledgeStoresChangedByMaterialId(Long materialId) {
        if (materialId == null) {
            return;
        }

        List<KnowledgeStoreEntity> stores = knowledgeStoreRepository.findDistinctByItems_Material_Id(materialId);
        markKnowledgeStoresChanged(stores, "material " + materialId);
    }

    private void markKnowledgeStoresChanged(List<KnowledgeStoreEntity> stores, String trigger) {
        if (stores == null || stores.isEmpty()) {
            return;
        }

        for (KnowledgeStoreEntity store : stores) {
            store.setStatus(KnowledgeStoreEntity.SyncStatus.CHANGED);
        }

        knowledgeStoreRepository.saveAll(stores);
        log.info("Marked {} knowledge stores as CHANGED after {}", stores.size(), trigger);
    }

    private String normalizeRequiredText(String value, String fieldName) {
        String normalized = normalizeOptionalText(value);
        if (normalized == null) {
            throw new IllegalArgumentException(fieldName + " không được để trống");
        }
        return normalized;
    }

    private String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
