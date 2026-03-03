package slib.com.example.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for Library Map
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KioskLibraryMapDTO {
    private Integer id;
    private String mapName;
    private String mapImageUrl;
    private String description;
    private Boolean isActive;
    private List<KioskZoneMapDTO> zones;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
