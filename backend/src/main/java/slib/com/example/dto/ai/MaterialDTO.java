package slib.com.example.dto.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
        @NotBlank(message = "Tên nhóm tài liệu không được để trống")
        @Size(max = 255, message = "Tên nhóm tài liệu không được vượt quá 255 ký tự")
        private String name;

        @Size(max = 5000, message = "Mô tả nhóm tài liệu không được vượt quá 5000 ký tự")
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
        @Size(max = 255, message = "Tên mục tài liệu không được vượt quá 255 ký tự")
        private String name;
        private MaterialItemEntity.ItemType type;

        @Size(max = 200000, message = "Nội dung mục tài liệu không được vượt quá 200000 ký tự")
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
