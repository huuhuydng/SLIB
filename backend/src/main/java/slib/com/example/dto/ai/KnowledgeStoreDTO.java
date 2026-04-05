package slib.com.example.dto.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import slib.com.example.entity.ai.KnowledgeStoreEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public class KnowledgeStoreDTO {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {
        @NotBlank(message = "Tên kho tri thức không được để trống")
        @Size(max = 255, message = "Tên kho tri thức không được vượt quá 255 ký tự")
        private String name;

        @Size(max = 5000, message = "Mô tả kho tri thức không được vượt quá 5000 ký tự")
        private String description;
        private Set<Long> itemIds; // MaterialItem IDs to include
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateRequest {
        @Size(max = 255, message = "Tên kho tri thức không được vượt quá 255 ký tự")
        private String name;

        @Size(max = 5000, message = "Mô tả kho tri thức không được vượt quá 5000 ký tự")
        private String description;
        private Set<Long> itemIds;
        private Boolean active;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private Long id;
        private String name;
        private String description;
        private String createdBy;
        private KnowledgeStoreEntity.SyncStatus status;
        private Boolean active;
        private LocalDateTime lastSyncedAt;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private List<MaterialDTO.ItemResponse> items;
        private Integer itemCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SyncResult {
        private Long knowledgeStoreId;
        private String knowledgeStoreName;
        private Integer chunksCreated;
        private KnowledgeStoreEntity.SyncStatus newStatus;
        private String message;
    }
}
