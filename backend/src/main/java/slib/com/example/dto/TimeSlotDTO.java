package slib.com.example.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimeSlotDTO {
    private String startTime; // "07:00"
    private String endTime; // "08:00"
    private String label; // "07:00 - 08:00"
}
