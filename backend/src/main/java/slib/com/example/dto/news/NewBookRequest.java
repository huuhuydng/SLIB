package slib.com.example.dto.news;

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
}
