package slib.com.example.entity.hce; 

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import slib.com.example.entity.users.User;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "access_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccessLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID) 
    @Column(name = "log_id")
    private UUID logId;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude      
    @EqualsAndHashCode.Exclude
    private User user;

    @Column(name = "user_id", insertable = false, updatable = false)
    private UUID userId; 

    @Column(name = "reservation_id")
    private UUID reservationId;


    @Column(name = "check_in_time", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime checkInTime;

    @Column(name = "check_out_time")
    private LocalDateTime checkOutTime; 

    @Column(name = "device_id", nullable = false, length = 50)
    private String deviceId; 
}