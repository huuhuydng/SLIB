package slib.com.example.dto.zone_config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LayoutConflictResponse {
    private String code;
    private String severity;
    private String entityType;
    private String entityKey;
    private String title;
    private String message;
}
