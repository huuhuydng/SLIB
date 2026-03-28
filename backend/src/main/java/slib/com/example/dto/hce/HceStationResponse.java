package slib.com.example.dto.hce;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HceStationResponse {
    private Integer id;
    private String deviceId;
    private String deviceName;
    private String location;
    private String deviceType;
    private String status;
    private LocalDateTime lastHeartbeat;
    private boolean online;
    private Long areaId;
    private String areaName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private long todayScanCount;
    private LocalDateTime lastAccessTime;
}
