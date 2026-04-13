package slib.com.example.dto.zone_config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LayoutScheduleResponse {
    private Long scheduleId;
    private Long basedOnPublishedVersion;
    private LocalDateTime scheduledFor;
    private String status;
    private String requestedByName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime cancelledAt;
    private LocalDateTime executedAt;
    private String lastError;
}
