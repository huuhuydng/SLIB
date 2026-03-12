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
    private Integer autoCancelMinutes; // 15 (phút tự hủy booking không check-in)
    private Integer autoCancelOnLeaveMinutes; // 30 (phút tự hủy sau khi rời chỗ)
    private Integer minReputation; // 0 (điểm uy tín tối thiểu, 0 = không giới hạn)
    private Boolean libraryClosed; // true = thư viện đang tạm đóng
    private String closedReason; // Lý do đóng thư viện

    // Notification Settings (FE-51)
    private Boolean notifyBookingSuccess;
    private Boolean notifyCheckinReminder;
    private Boolean notifyTimeExpiry;
    private Boolean notifyViolation;
    private Boolean notifyWeeklyReport;
    private Boolean notifyDeviceAlert;
}
