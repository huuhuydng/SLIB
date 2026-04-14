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
public class LayoutScheduleRequest {
    private LayoutSnapshotRequest snapshot;
    private LocalDateTime scheduledFor;
}
