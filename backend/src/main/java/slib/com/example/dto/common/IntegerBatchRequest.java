package slib.com.example.dto.common;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class IntegerBatchRequest {
    @NotEmpty(message = "Danh sách ID không được trống")
    private List<Integer> ids;
}
