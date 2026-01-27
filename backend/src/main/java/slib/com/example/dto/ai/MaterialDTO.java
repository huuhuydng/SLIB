package slib.com.example.dto.ai;

import lombok.*;
import slib.com.example.entity.ai.MaterialItemEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public class MaterialDTO {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MaterialRequest {
        private String name;
        private String description;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MaterialResponse {
        private Long id;
        private String name;
        private String description;
        private String createdBy;
        private Boolean active;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private List<ItemResponse> items;
        private Integer itemCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemRequest {
        private String name;
        private MaterialItemEntity.ItemType type;
        private String content; // For TEXT type
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemResponse {
        private Long id;
        private String name;
        private MaterialItemEntity.ItemType type;
        private String content;
        private String fileName;
        private Long fileSize;
        private LocalDateTime createdAt;
    }
}
