package slib.com.example.dto.hce;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccessLogStatsDTO {
    private long totalCheckInsToday;
    private long totalCheckOutsToday;
    private long currentlyInLibrary;
}
