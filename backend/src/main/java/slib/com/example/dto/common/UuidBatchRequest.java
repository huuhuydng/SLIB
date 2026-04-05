package slib.com.example.dto.common;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class UuidBatchRequest {
    @NotEmpty(message = "Danh sách ID không được trống")
    private List<UUID> ids;
}
