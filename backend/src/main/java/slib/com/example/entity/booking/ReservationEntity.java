package slib.com.example.entity.booking;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import jakarta.persistence.FetchType;
import slib.com.example.entity.users.User;
import slib.com.example.entity.zone_config.SeatEntity;

@Entity
@Table(name = "reservations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "reservation_id", nullable = false, updatable = false)
    private UUID reservationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seat_id", nullable = false)
    private SeatEntity seat;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    // Actual end time when student physically left the seat (via NFC checkout or staff confirmation)
    // Null = left via automatic completion (scheduler). Non-null = left early via manual checkout
    @Column(name = "actual_end_time")
    private LocalDateTime actualEndTime;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "cancellation_reason")
    private String cancellationReason;

    @Column(name = "cancelled_by_user_id")
    private UUID cancelledByUserId;

    @Builder.Default
    @Column(name = "layout_changed", nullable = false)
    private Boolean layoutChanged = false;

    @Column(name = "layout_change_title", length = 200)
    private String layoutChangeTitle;

    @Column(name = "layout_change_message", columnDefinition = "TEXT")
    private String layoutChangeMessage;

    @Column(name = "layout_changed_at")
    private LocalDateTime layoutChangedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
