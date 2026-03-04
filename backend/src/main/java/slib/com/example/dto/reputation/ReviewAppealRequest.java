package slib.com.example.dto.reputation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewAppealRequest {
    @NotNull(message = "Approve decision is required")
    private Boolean approved; // true = chấp nhận, false = từ chối
    
    @NotBlank(message = "Review notes is required")
    private String reviewNotes;
}
