package slib.com.example.dto.system;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class SystemLogCleanupRequest {

    @NotNull(message = "Vui lòng chọn ngày mốc để dọn nhật ký")
    private LocalDate beforeDate;
}
