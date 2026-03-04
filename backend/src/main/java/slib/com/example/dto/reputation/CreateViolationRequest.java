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
public class CreateViolationRequest {
    @NotNull(message = "Student ID is required")
    private UUID studentId;
    
    @NotBlank(message = "Violation reason is required")
    private String violationReason;
    
    @NotNull(message = "Penalty points is required")
    private Integer penaltyPoints;
    
    private String notes;
}
