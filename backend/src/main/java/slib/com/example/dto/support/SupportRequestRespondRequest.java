package slib.com.example.dto.support;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SupportRequestRespondRequest {
    @NotBlank(message = "Phản hồi không được để trống")
    @Size(max = 2000, message = "Phản hồi không được vượt quá 2000 ký tự")
    private String response;
}
