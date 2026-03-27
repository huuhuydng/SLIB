package slib.com.example.dto.news;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NewBookResponse {
    private Integer id;
    private String title;
    private String author;
    private String isbn;
    private String coverUrl;
    private String description;
    private String category;
    private Integer publishYear;
    private LocalDate arrivalDate;
    private Boolean isActive;
    private Boolean isPinned;
    private String sourceUrl;
    private String publisher;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
