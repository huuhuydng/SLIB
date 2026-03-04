package slib.com.example.dto.reputation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAppealRequest {
    @NotNull(message = "Violation ID is required")
    private UUID violationId;
    
    @NotBlank(message = "Appeal reason is required")
    private String appealReason;
}
