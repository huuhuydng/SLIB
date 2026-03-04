package slib.com.example.dto.reputation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ViolationAppealResponse {
    private UUID id;
    private UUID violationId;
    private UUID studentId;
    private String studentName;
    private String studentCode;
    private String appealReason;
    private String status;
    private String reviewedByName;
    private String reviewNotes;
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Thông tin vi phạm
    private String violationReason;
    private Integer penaltyPoints;
    private LocalDateTime violationDate;
}
