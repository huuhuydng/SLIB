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
public class LayoutDraftResponse {
    private boolean hasDraft;
    private Long basedOnPublishedVersion;
    private String updatedByName;
    private LocalDateTime updatedAt;
    private LayoutSnapshotRequest snapshot;
}
