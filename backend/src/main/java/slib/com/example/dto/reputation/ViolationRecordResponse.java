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
public class ViolationRecordResponse {
    private UUID id;
    private UUID studentId;
    private String studentName;
    private String studentCode;
    private String violationReason;
    private Integer penaltyPoints;
    private String status;
    private String notes;
    private String createdByName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Thông tin về khiếu nại (nếu có)
    private ViolationAppealResponse appeal;
}
