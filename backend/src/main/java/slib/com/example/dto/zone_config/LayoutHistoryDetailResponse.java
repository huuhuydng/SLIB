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
public class LayoutHistoryDetailResponse {
    private Long historyId;
    private String actionType;
    private String summary;
    private Long publishedVersion;
    private String createdByName;
    private LocalDateTime createdAt;
    private LayoutSnapshotRequest snapshot;
}
