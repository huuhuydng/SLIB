package slib.com.example.dto.kiosk;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for Zone Map
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KioskZoneMapDTO {
    private Integer id;
    private String zoneName;
    private String zoneType;
    private Integer xPosition;
    private Integer yPosition;
    private Integer width;
    private Integer height;
    private String colorCode;
    private Boolean isInteractive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
