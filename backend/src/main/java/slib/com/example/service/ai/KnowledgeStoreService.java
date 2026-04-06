package slib.com.example.service.ai;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    @Value("${slib.internal.api-key:}")
    private String internalApiKey;

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
                .name(validateKnowledgeStoreName(request.getName()))
                .description(normalizeOptionalText(request.getDescription()))
                .createdBy(createdBy)
                .status(KnowledgeStoreEntity.SyncStatus.CHANGED)
                .active(true)
                .build();

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

        if (request.getName() != null) {
            ks.setName(validateKnowledgeStoreName(request.getName()));
        }
        if (request.getDescription() != null) {
            ks.setDescription(normalizeOptionalText(request.getDescription()));
        }
        if (request.getActive() != null) {
            ks.setActive(request.getActive());
        }

        if (request.getItemIds() != null) {
            Set<MaterialItemEntity> items = new HashSet<>(materialItemRepository.findAllById(request.getItemIds()));
            ks.setItems(items);
        }

        ks.setStatus(KnowledgeStoreEntity.SyncStatus.CHANGED);
        ks = knowledgeStoreRepository.save(ks);
        return toResponse(ks);
    }

    @Transactional
    public void deleteKnowledgeStore(Long id) {
        KnowledgeStoreEntity ks = knowledgeStoreRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("KnowledgeStore not found: " + id));

        deleteVectorsFromQdrant(ks.getName());
        knowledgeStoreRepository.deleteById(id);
        log.info("Deleted knowledge store and vectors: {} ({})", ks.getName(), id);
    }

    private void deleteVectorsFromQdrant(String ksName) {
        try {
            String url = aiServiceUrl + "/api/v1/ingest/knowledge-store/" + ksName;
            RequestEntity<Void> request = RequestEntity
                    .delete(url)
                    .headers(buildAiServiceHeaders(null))
                    .build();
            restTemplate.exchange(request, Void.class);
            log.info("Successfully deleted vectors for: {}", ksName);
        } catch (Exception e) {
            log.warn("Failed to delete vectors from Qdrant for {}: {}", ksName, e.getMessage());
        }
    }

    @Transactional
    public KnowledgeStoreDTO.SyncResult syncKnowledgeStore(Long id) {
        KnowledgeStoreEntity ks = knowledgeStoreRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("KnowledgeStore not found: " + id));

        log.info("Starting sync for knowledge store: {} ({})", ks.getName(), id);
        ks.setStatus(KnowledgeStoreEntity.SyncStatus.SYNCING);
        knowledgeStoreRepository.save(ks);

        deleteVectorsFromQdrant(ks.getName());

        int totalChunks = 0;
        try {
            for (MaterialItemEntity item : ks.getItems()) {
                totalChunks += syncItem(item, ks.getName());
            }

            ks.setStatus(KnowledgeStoreEntity.SyncStatus.SYNCED);
            ks.setLastSyncedAt(LocalDateTime.now());
            knowledgeStoreRepository.save(ks);

            return KnowledgeStoreDTO.SyncResult.builder()
                    .knowledgeStoreId(id)
                    .knowledgeStoreName(ks.getName())
                    .chunksCreated(totalChunks)
                    .newStatus(KnowledgeStoreEntity.SyncStatus.SYNCED)
                    .message("Đồng bộ kho tri thức thành công")
                    .build();

        } catch (Exception e) {
            log.error("Sync failed for {}: {}", ks.getName(), e.getMessage(), e);
            ks.setStatus(KnowledgeStoreEntity.SyncStatus.ERROR);
            knowledgeStoreRepository.save(ks);

            return KnowledgeStoreDTO.SyncResult.builder()
                    .knowledgeStoreId(id)
                    .knowledgeStoreName(ks.getName())
                    .chunksCreated(0)
                    .newStatus(KnowledgeStoreEntity.SyncStatus.ERROR)
                    .message("Đồng bộ thất bại: " + e.getMessage())
                    .build();
        }
    }

    private int syncItem(MaterialItemEntity item, String storeName) {
        String source = storeName + "_" + item.getName();
        if (item.getType() == MaterialItemEntity.ItemType.TEXT) {
            return syncTextItem(item.getContent(), source, storeName);
        }
        return syncFileItem(item, source, storeName);
    }

    private int syncTextItem(String content, String source, String storeName) {
        String url = aiServiceUrl + "/api/v1/ingest/text";
        Map<String, Object> body = Map.of(
                "content", content,
                "source", source,
                "category", storeName);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, buildAiServiceHeaders(MediaType.APPLICATION_JSON));
        ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
        return extractChunksCreated(response);
    }

    private int syncFileItem(MaterialItemEntity item, String source, String storeName) {
        if (item.getFilePath() == null || !Files.exists(Paths.get(item.getFilePath()))) {
            throw new IllegalArgumentException("Không tìm thấy tệp nguồn để đồng bộ: " + item.getName());
        }

        String url = aiServiceUrl + "/api/v1/ingest/upload";
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new FileSystemResource(item.getFilePath()));
        body.add("category", storeName);
        body.add("source", source);

        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(
                body,
                buildAiServiceHeaders(MediaType.MULTIPART_FORM_DATA));
        ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
        return extractChunksCreated(response);
    }

    private int extractChunksCreated(ResponseEntity<Map> response) {
        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            Object chunksCreated = response.getBody().getOrDefault("chunks_created", 0);
            if (chunksCreated instanceof Number number) {
                return number.intValue();
            }
        }
        return 0;
    }

    private HttpHeaders buildAiServiceHeaders(MediaType contentType) {
        HttpHeaders headers = new HttpHeaders();
        if (contentType != null) {
            headers.setContentType(contentType);
        }
        if (internalApiKey != null && !internalApiKey.isBlank()) {
            headers.set("X-Internal-Api-Key", internalApiKey);
        }
        return headers;
    }

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

    private String validateKnowledgeStoreName(String name) {
        String normalized = normalizeOptionalText(name);
        if (normalized == null) {
            throw new IllegalArgumentException("Tên kho tri thức không được để trống");
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
