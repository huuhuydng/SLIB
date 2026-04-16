package slib.com.example.entity.library;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "library_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LibrarySetting {

    @Id
    @Column(name = "id")
    @Builder.Default
    private Integer id = 1; // Singleton - chỉ có 1 record cấu hình

    @Column(name = "open_time", nullable = false)
    @Builder.Default
    private String openTime = "07:00"; // Giờ mở cửa

    @Column(name = "close_time", nullable = false)
    @Builder.Default
    private String closeTime = "21:00"; // Giờ đóng cửa

    @Column(name = "slot_duration", nullable = false)
    @Builder.Default
    private Integer slotDuration = 60; // Thời lượng mỗi ca (phút)

    @Column(name = "max_booking_days", nullable = false)
    @Builder.Default
    private Integer maxBookingDays = 14; // Số ngày tối đa có thể đặt trước

    @Column(name = "working_days", nullable = false)
    @Builder.Default
    private String workingDays = "2,3,4,5,6"; // Ngày làm việc (1=CN, 2=T2, ..., 7=T7)

    @Column(name = "max_bookings_per_day", nullable = false)
    @Builder.Default
    private Integer maxBookingsPerDay = 3; // Số lần đặt tối đa mỗi ngày cho 1 user

    @Column(name = "max_active_bookings", nullable = false)
    @Builder.Default
    private Integer maxActiveBookings = 2; // Số booking sắp tới tối đa được giữ cùng lúc

    @Column(name = "max_hours_per_day", nullable = false)
    @Builder.Default
    private Integer maxHoursPerDay = 4; // Số giờ tối đa được đặt trong 1 ngày

    @Column(name = "auto_cancel_minutes", nullable = false)
    @Builder.Default
    private Integer autoCancelMinutes = 15; // Phút tự hủy booking nếu không xác nhận ghế

    @Column(name = "auto_cancel_on_leave_minutes", nullable = false)
    @Builder.Default
    private Integer autoCancelOnLeaveMinutes = 30; // Phút tự hủy sau khi rời chỗ

    @Column(name = "seat_confirmation_lead_minutes", nullable = false)
    @Builder.Default
    private Integer seatConfirmationLeadMinutes = 15; // Số phút trước giờ bắt đầu được phép xác nhận ghế

    @Column(name = "booking_reminder_lead_minutes", nullable = false)
    @Builder.Default
    private Integer bookingReminderLeadMinutes = 15; // Số phút trước giờ bắt đầu gửi nhắc lịch

    @Column(name = "expiry_warning_lead_minutes", nullable = false)
    @Builder.Default
    private Integer expiryWarningLeadMinutes = 10; // Số phút trước giờ kết thúc gửi cảnh báo sắp hết giờ

    @Column(name = "booking_cancel_deadline_hours", nullable = false)
    @Builder.Default
    private Integer bookingCancelDeadlineHours = 12; // Số giờ tối thiểu trước giờ bắt đầu để được hủy chỗ

    @Column(name = "min_reputation", nullable = false)
    @Builder.Default
    private Integer minReputation = 0; // Điểm uy tín tối thiểu để đặt chỗ (0 = không giới hạn)

    @Column(name = "library_closed", nullable = false)
    @Builder.Default
    private Boolean libraryClosed = false; // true = thư viện đang tạm đóng, sinh viên không thể đặt chỗ

    @Column(name = "closed_reason", length = 500)
    private String closedReason; // Lý do đóng thư viện (VD: "Sự kiện đặc biệt", "Bảo trì")

    @Column(name = "closed_from_at")
    private LocalDateTime closedFromAt; // Thời điểm bắt đầu tạm đóng

    @Column(name = "closed_until_at")
    private LocalDateTime closedUntilAt; // Thời điểm tự mở lại

    // === Notification Settings (FE-51) ===

    @Builder.Default
    @Column(name = "notify_booking_success", nullable = false)
    private Boolean notifyBookingSuccess = true; // Thông báo đặt chỗ thành công

    @Builder.Default
    @Column(name = "notify_checkin_reminder", nullable = false)
    private Boolean notifyCheckinReminder = true; // Nhắc nhở đến giờ xác nhận ghế

    @Builder.Default
    @Column(name = "notify_time_expiry", nullable = false)
    private Boolean notifyTimeExpiry = true; // Cảnh báo hết giờ

    @Builder.Default
    @Column(name = "notify_violation", nullable = false)
    private Boolean notifyViolation = true; // Thông báo vi phạm

    @Builder.Default
    @Column(name = "notify_weekly_report", nullable = false)
    private Boolean notifyWeeklyReport = false; // Báo cáo tuần cho admin

    @Builder.Default
    @Column(name = "notify_device_alert", nullable = false)
    private Boolean notifyDeviceAlert = true; // Cảnh báo thiết bị NFC
}
