package slib.com.example.dto.news;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class NewBookImportRequest {
    @NotBlank(message = "URL OPAC không được để trống")
    private String url;
}
