package slib.com.example.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Simple in-memory settings object (no JPA mapping).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LibrarySetting {

    private Integer id = 1; // Singleton - chỉ có 1 record cấu hình

    private String openTime = "07:00"; // Giờ mở cửa

    private String closeTime = "21:00"; // Giờ đóng cửa

    private Integer slotDuration = 60; // Thời lượng mỗi ca (phút)

    private Integer maxBookingDays = 14; // Số ngày tối đa có thể đặt trước

    private String workingDays = "2,3,4,5,6"; // Ngày làm việc (1=CN, 2=T2, ..., 7=T7)

    private Integer maxBookingsPerDay = 3; // Số lần đặt tối đa mỗi ngày cho 1 user

    private Integer maxHoursPerDay = 4; // Số giờ tối đa được đặt trong 1 ngày
}
