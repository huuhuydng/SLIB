package slib.com.example.entity;

import jakarta.persistence.*;
import lombok.*;

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
    private Integer id = 1; // Singleton - chỉ có 1 record cấu hình

    @Column(name = "open_time", nullable = false)
    private String openTime = "07:00"; // Giờ mở cửa

    @Column(name = "close_time", nullable = false)
    private String closeTime = "21:00"; // Giờ đóng cửa

    @Column(name = "slot_duration", nullable = false)
    private Integer slotDuration = 60; // Thời lượng mỗi ca (phút)

    @Column(name = "max_booking_days", nullable = false)
    private Integer maxBookingDays = 14; // Số ngày tối đa có thể đặt trước

    @Column(name = "working_days", nullable = false)
    private String workingDays = "2,3,4,5,6"; // Ngày làm việc (1=CN, 2=T2, ..., 7=T7)

    @Column(name = "max_bookings_per_day", nullable = false)
    private Integer maxBookingsPerDay = 3; // Số lần đặt tối đa mỗi ngày cho 1 user

    @Column(name = "max_hours_per_day", nullable = false)
    private Integer maxHoursPerDay = 4; // Số giờ tối đa được đặt trong 1 ngày
}
