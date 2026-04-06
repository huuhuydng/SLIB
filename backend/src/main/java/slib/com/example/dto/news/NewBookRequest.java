package slib.com.example.dto.news;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NewBookRequest {
    @NotBlank(message = "Tiêu đề sách không được để trống")
    @Size(max = 300, message = "Tiêu đề sách không được vượt quá 300 ký tự")
    private String title;
    @Size(max = 200, message = "Tác giả không được vượt quá 200 ký tự")
    private String author;
    @Size(max = 20, message = "ISBN không được vượt quá 20 ký tự")
    private String isbn;
    @Size(max = 1000, message = "Đường dẫn ảnh bìa không được vượt quá 1000 ký tự")
    private String coverUrl;
    private String description;
    @Size(max = 255, message = "Thể loại không được vượt quá 255 ký tự")
    private String category;
    private Integer publishYear;
    private LocalDate arrivalDate;
    private Boolean isActive;
    private Boolean isPinned;
    @Size(max = 1000, message = "Đường dẫn nguồn không được vượt quá 1000 ký tự")
    private String sourceUrl;
    @Size(max = 255, message = "Nhà xuất bản không được vượt quá 255 ký tự")
    private String publisher;
}
