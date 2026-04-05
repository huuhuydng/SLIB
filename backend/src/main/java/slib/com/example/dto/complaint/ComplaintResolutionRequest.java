package slib.com.example.dto.complaint;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ComplaintResolutionRequest {
    @Size(max = 1000, message = "Ghi chú xử lý không được vượt quá 1000 ký tự")
    private String note;
}
