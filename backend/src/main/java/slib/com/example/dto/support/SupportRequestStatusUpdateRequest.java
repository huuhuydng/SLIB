package slib.com.example.dto.support;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import slib.com.example.entity.support.SupportRequestStatus;

@Data
public class SupportRequestStatusUpdateRequest {
    @NotNull(message = "Trạng thái không được để trống")
    private SupportRequestStatus status;

    @Size(max = 2000, message = "Nội dung xử lý không được vượt quá 2000 ký tự")
    private String response;
}
