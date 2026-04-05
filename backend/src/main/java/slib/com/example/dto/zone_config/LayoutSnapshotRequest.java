package slib.com.example.dto.zone_config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LayoutSnapshotRequest {
    private Long basedOnPublishedVersion;
    private List<AreaResponse> areas = new ArrayList<>();
    private List<ZoneResponse> zones = new ArrayList<>();
    private List<SeatResponse> seats = new ArrayList<>();
    private List<AreaFactoryResponse> factories = new ArrayList<>();
}
