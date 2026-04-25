package slib.com.example.dto.hce;

import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccessLogDTO {
    private UUID logId;
    private UUID userId;
    private String userName;
    private String userCode;
    private String deviceId;
    private String deviceName;
    private LocalDateTime checkInTime;
    private LocalDateTime checkOutTime;
    private String action; // "CHECK_IN" or "CHECK_OUT"
}
