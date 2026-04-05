package slib.com.example.dto.system;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LibrarySettingDTO {
    @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "Giờ mở cửa phải đúng định dạng HH:mm")
    private String openTime; // "07:00"

    @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "Giờ đóng cửa phải đúng định dạng HH:mm")
    private String closeTime; // "21:00"

    @Min(value = 15, message = "Thời lượng mỗi slot phải từ 15 phút trở lên")
    @Max(value = 240, message = "Thời lượng mỗi slot không được vượt quá 240 phút")
    private Integer slotDuration; // 60 (phút)

    @Min(value = 1, message = "Số ngày đặt trước tối đa phải lớn hơn 0")
    @Max(value = 60, message = "Số ngày đặt trước tối đa không được vượt quá 60")
    private Integer maxBookingDays; // 14 (ngày)

    @Pattern(
            regexp = "^[1-7](,[1-7])*$",
            message = "Ngày làm việc phải có dạng 2,3,4,5,6 và chỉ chứa giá trị từ 1 đến 7")
    private String workingDays; // "2,3,4,5,6" (1=CN, 2=T2, ..., 7=T7)

    @Min(value = 1, message = "Số lượt đặt tối đa mỗi ngày phải lớn hơn 0")
    @Max(value = 20, message = "Số lượt đặt tối đa mỗi ngày không được vượt quá 20")
    private Integer maxBookingsPerDay; // 3 (số lần đặt tối đa mỗi ngày)

    @Min(value = 1, message = "Số giờ đặt tối đa mỗi ngày phải lớn hơn 0")
    @Max(value = 24, message = "Số giờ đặt tối đa mỗi ngày không được vượt quá 24")
    private Integer maxHoursPerDay; // 4 (số giờ tối đa được đặt mỗi ngày)

    @Min(value = 1, message = "Thời gian tự hủy nếu không check-in phải lớn hơn 0")
    @Max(value = 180, message = "Thời gian tự hủy nếu không check-in không được vượt quá 180 phút")
    private Integer autoCancelMinutes; // 15 (phút tự hủy booking không check-in)

    @Min(value = 1, message = "Thời gian tự hủy khi rời chỗ phải lớn hơn 0")
    @Max(value = 360, message = "Thời gian tự hủy khi rời chỗ không được vượt quá 360 phút")
    private Integer autoCancelOnLeaveMinutes; // 30 (phút tự hủy sau khi rời chỗ)

    @Min(value = 0, message = "Điểm uy tín tối thiểu không được nhỏ hơn 0")
    @Max(value = 100, message = "Điểm uy tín tối thiểu không được vượt quá 100")
    private Integer minReputation; // 0 (điểm uy tín tối thiểu, 0 = không giới hạn)
    private Boolean libraryClosed; // true = thư viện đang tạm đóng

    @Size(max = 500, message = "Lý do đóng thư viện không được vượt quá 500 ký tự")
    private String closedReason; // Lý do đóng thư viện

    // Notification Settings (FE-51)
    private Boolean notifyBookingSuccess;
    private Boolean notifyCheckinReminder;
    private Boolean notifyTimeExpiry;
    private Boolean notifyViolation;
    private Boolean notifyWeeklyReport;
    private Boolean notifyDeviceAlert;
}
