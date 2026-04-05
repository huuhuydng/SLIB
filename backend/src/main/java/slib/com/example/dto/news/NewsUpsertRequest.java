package slib.com.example.dto.news;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NewsUpsertRequest {
    @NotBlank(message = "Tiêu đề tin tức không được để trống")
    @Size(max = 255, message = "Tiêu đề tin tức không được vượt quá 255 ký tự")
    private String title;

    @Size(max = 1000, message = "Tóm tắt không được vượt quá 1000 ký tự")
    private String summary;

    private String content;

    @Size(max = 1000, message = "Đường dẫn ảnh không được vượt quá 1000 ký tự")
    private String imageUrl;

    private Long categoryId;
    private Boolean isPublished;
    private Boolean isPinned;
    private LocalDateTime publishedAt;
}
