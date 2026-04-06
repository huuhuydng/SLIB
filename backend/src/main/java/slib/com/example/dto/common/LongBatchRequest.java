package slib.com.example.dto.common;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class LongBatchRequest {
    @NotEmpty(message = "Danh sách ID không được trống")
    private List<Long> ids;
}
