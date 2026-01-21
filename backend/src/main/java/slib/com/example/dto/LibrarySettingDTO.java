package slib.com.example.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LibrarySettingDTO {
    private String openTime; // "07:00"
    private String closeTime; // "21:00"
    private Integer slotDuration; // 60 (phút)
    private Integer maxBookingDays; // 14 (ngày)
    private String workingDays; // "2,3,4,5,6" (1=CN, 2=T2, ..., 7=T7)
    private Integer maxBookingsPerDay; // 3 (số lần đặt tối đa mỗi ngày)
    private Integer maxHoursPerDay; // 4 (số giờ tối đa được đặt mỗi ngày)
}
