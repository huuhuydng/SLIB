package slib.com.example.entity.activity;

import jakarta.persistence.*;
import lombok.*;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "activity_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActivityLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "activity_type", nullable = false, length = 50)
    private String activityType;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "reservation_id")
    private UUID reservationId;

    @Column(name = "seat_code", length = 20)
    private String seatCode;

    @Column(name = "zone_name", length = 100)
    private String zoneName;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(name = "created_at")
    private ZonedDateTime createdAt;

    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = ZonedDateTime.now(VIETNAM_ZONE);
        }
    }

    // Activity types constants
    public static final String TYPE_CHECK_IN = "CHECK_IN";
    public static final String TYPE_CHECK_OUT = "CHECK_OUT";
    public static final String TYPE_BOOKING_SUCCESS = "BOOKING_SUCCESS";
    public static final String TYPE_BOOKING_CANCEL = "BOOKING_CANCEL";
    public static final String TYPE_NFC_CONFIRM = "NFC_CONFIRM";
    public static final String TYPE_GATE_ENTRY = "GATE_ENTRY";
    public static final String TYPE_NO_SHOW = "NO_SHOW";
}
