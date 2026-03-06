package slib.com.example.entity.analytics;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity lưu trữ hành vi của sinh viên
 * Dùng để phân tích và AI predict
 */
@Entity
@Table(name = "student_behaviors")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentBehaviorEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "behavior_type", nullable = false)
    private BehaviorType behaviorType;

    @Column(name = "description")
    private String description;

    @Column(name = "related_booking_id")
    private UUID relatedBookingId;

    @Column(name = "related_seat_id")
    private Integer relatedSeatId;

    @Column(name = "related_zone_id")
    private Integer relatedZoneId;

    @Column(name = "points_impact")
    private Integer pointsImpact; // Điểm uy tín thay đổi (âm hoặc dương)

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata; // JSON string chứa extra data

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum BehaviorType {
        // Booking behaviors
        BOOKING_CREATED,        // Tạo booking mới
        BOOKING_CONFIRMED,     // Xác nhận (check-in)
        BOOKING_CANCELLED,    // Hủy booking
        BOOKING_NO_SHOW,      // Không đến (bỏ chỗ)
        BOOKING_EXPIRED,      // Hết hạn

        // Check-in behaviors
        CHECKIN_ON_TIME,      // Check-in đúng giờ
        CHECKIN_LATE,         // Check-in muộn
        CHECKIN_EARLY,         // Check-in sớm
        CHECKOUT_ON_TIME,     // Check-out đúng giờ
        CHECKOUT_LATE,        // Check-out muộn

        // Seat holding behavior
        SEAT_HOLDING,         // Giữ chỗ bất thường (check-in rồi đi quá lâu)

        // Violation behaviors
        VIOLATION_REPORTED,   // Bị báo vi phạm
        VIOLATION_CONFIRMED,  // Vi phạm được xác nhận
        VIOLATION_DISPUTED,   // Khiếu nại vi phạm

        // Reputation behaviors
        POINTS_EARNED,        // Được cộng điểm
        POINTS_DEDUCTED,      // Bị trừ điểm

        // General
        LOGIN,                // Đăng nhập
        LOGOUT               // Đăng xuất
    }
}
