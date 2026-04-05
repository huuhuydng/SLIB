package slib.com.example.dto.zone_config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LayoutPublishResponse {
    private Long publishedVersion;
    private String publishedByName;
    private LayoutSnapshotRequest snapshot;
}
