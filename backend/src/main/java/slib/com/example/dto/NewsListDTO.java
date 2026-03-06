package slib.com.example.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NewsListDTO {
    private Long id;
    private String title;
    private String summary;
    private String content;
    private Long categoryId;
    private String categoryName;
    private Boolean isPublished;
    private Boolean isPinned;
    private Integer viewCount;
    private LocalDateTime createdAt;
    private LocalDateTime publishedAt;
    private String imageUrl;
}
