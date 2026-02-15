package slib.com.example.service.ai;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import slib.com.example.dto.ai.KnowledgeStoreDTO;
import slib.com.example.dto.ai.MaterialDTO;
import slib.com.example.entity.ai.KnowledgeStoreEntity;
import slib.com.example.entity.ai.MaterialItemEntity;
import slib.com.example.repository.ai.KnowledgeStoreRepository;
import slib.com.example.repository.ai.MaterialItemRepository;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class KnowledgeStoreService {

    private final KnowledgeStoreRepository knowledgeStoreRepository;
    private final MaterialItemRepository materialItemRepository;
    private final RestTemplate restTemplate;

    @Value("${app.ai-service.url:http://localhost:8001}")
    private String aiServiceUrl;

    // ==================== KNOWLEDGE STORE CRUD ====================

    public List<KnowledgeStoreDTO.Response> getAllKnowledgeStores() {
        return knowledgeStoreRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public KnowledgeStoreDTO.Response getKnowledgeStoreById(Long id) {
        KnowledgeStoreEntity ks = knowledgeStoreRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("KnowledgeStore not found: " + id));
        return toResponse(ks);
    }

    @Transactional
    public KnowledgeStoreDTO.Response createKnowledgeStore(KnowledgeStoreDTO.CreateRequest request, String createdBy) {
        KnowledgeStoreEntity ks = KnowledgeStoreEntity.builder()
                .name(request.getName())
                .description(request.getDescription())
                .createdBy(createdBy)
                .status(KnowledgeStoreEntity.SyncStatus.CHANGED)
                .active(true)
                .build();

        // Add items if provided
        if (request.getItemIds() != null && !request.getItemIds().isEmpty()) {
            Set<MaterialItemEntity> items = new HashSet<>(materialItemRepository.findAllById(request.getItemIds()));
            ks.setItems(items);
        }

        ks = knowledgeStoreRepository.save(ks);
        log.info("Created knowledge store: {} by {}", ks.getName(), createdBy);
        return toResponse(ks);
    }

    @Transactional
    public KnowledgeStoreDTO.Response updateKnowledgeStore(Long id, KnowledgeStoreDTO.UpdateRequest request) {
        KnowledgeStoreEntity ks = knowledgeStoreRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("KnowledgeStore not found: " + id));

        if (request.getName() != null)
            ks.setName(request.getName());
        if (request.getDescription() != null)
            ks.setDescription(request.getDescription());
        if (request.getActive() != null)
            ks.setActive(request.getActive());

        // Update items if provided
        if (request.getItemIds() != null) {
            Set<MaterialItemEntity> items = new HashSet<>(materialItemRepository.findAllById(request.getItemIds()));
            ks.setItems(items);
        }

        // Mark as changed (needs re-sync)
        ks.setStatus(KnowledgeStoreEntity.SyncStatus.CHANGED);
        ks = knowledgeStoreRepository.save(ks);
        return toResponse(ks);
    }

    @Transactional
    public void deleteKnowledgeStore(Long id) {
        // Get KS name first to delete vectors
        KnowledgeStoreEntity ks = knowledgeStoreRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("KnowledgeStore not found: " + id));

        // Delete vectors from Qdrant FIRST
        deleteVectorsFromQdrant(ks.getName());

        // Then delete from DB
        knowledgeStoreRepository.deleteById(id);
        log.info("Deleted knowledge store and vectors: {} ({})", ks.getName(), id);
    }

    /**
     * Delete all vectors for a Knowledge Store from Qdrant
     */
    private void deleteVectorsFromQdrant(String ksName) {
        try {
            String url = aiServiceUrl + "/api/v1/ingest/knowledge-store/" + ksName;
            log.info("Deleting vectors from Qdrant for: {}", ksName);

            restTemplate.delete(url);
            log.info("Successfully deleted vectors for: {}", ksName);
        } catch (Exception e) {
            log.warn("Failed to delete vectors from Qdrant for {}: {}", ksName, e.getMessage());
            // Don't throw - allow DB delete to proceed even if vector delete fails
        }
    }

    // ==================== SYNC TO VECTOR DB ====================

    @Transactional
    public KnowledgeStoreDTO.SyncResult syncKnowledgeStore(Long id) {
        KnowledgeStoreEntity ks = knowledgeStoreRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("KnowledgeStore not found: " + id));

        log.info("Starting sync for knowledge store: {} ({})", ks.getName(), id);
        ks.setStatus(KnowledgeStoreEntity.SyncStatus.SYNCING);
        knowledgeStoreRepository.save(ks);

        // DELETE OLD VECTORS FIRST before syncing new ones
        deleteVectorsFromQdrant(ks.getName());

        int totalChunks = 0;
        try {
            // Sync each item
            for (MaterialItemEntity item : ks.getItems()) {
                int chunks = syncItem(item, ks.getName());
                totalChunks += chunks;
            }

            ks.setStatus(KnowledgeStoreEntity.SyncStatus.SYNCED);
            ks.setLastSyncedAt(LocalDateTime.now());
            knowledgeStoreRepository.save(ks);

            log.info("Sync completed for {}: {} chunks created", ks.getName(), totalChunks);
            return KnowledgeStoreDTO.SyncResult.builder()
                    .knowledgeStoreId(id)
                    .knowledgeStoreName(ks.getName())
                    .chunksCreated(totalChunks)
                    .newStatus(KnowledgeStoreEntity.SyncStatus.SYNCED)
                    .message("Sync completed successfully")
                    .build();

        } catch (Exception e) {
            log.error("Sync failed for {}: {}", ks.getName(), e.getMessage());
            ks.setStatus(KnowledgeStoreEntity.SyncStatus.ERROR);
            knowledgeStoreRepository.save(ks);

            return KnowledgeStoreDTO.SyncResult.builder()
                    .knowledgeStoreId(id)
                    .knowledgeStoreName(ks.getName())
                    .chunksCreated(0)
                    .newStatus(KnowledgeStoreEntity.SyncStatus.ERROR)
                    .message("Sync failed: " + e.getMessage())
                    .build();
        }
    }

    private int syncItem(MaterialItemEntity item, String storeName) throws Exception {
        String content;
        String source = storeName + "_" + item.getName();

        if (item.getType() == MaterialItemEntity.ItemType.TEXT) {
            content = item.getContent();
        } else {
            // Read file content
            content = Files.readString(Paths.get(item.getFilePath()));
        }

        // Call Python AI Service to ingest
        String url = aiServiceUrl + "/api/v1/ingest/text";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new HashMap<>();
        body.put("content", content);
        body.put("source", source);
        body.put("category", storeName);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            return (Integer) response.getBody().getOrDefault("chunks_created", 0);
        }
        return 0;
    }

    // ==================== MAPPERS ====================

    private KnowledgeStoreDTO.Response toResponse(KnowledgeStoreEntity entity) {
        List<MaterialDTO.ItemResponse> items = entity.getItems() != null
                ? entity.getItems().stream().map(this::toItemResponse).collect(Collectors.toList())
                : List.of();

        return KnowledgeStoreDTO.Response.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .createdBy(entity.getCreatedBy())
                .status(entity.getStatus())
                .active(entity.getActive())
                .lastSyncedAt(entity.getLastSyncedAt())
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
