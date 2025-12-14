package slib.com.example.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
@Data// Tự động tạo getter, setter, toString, equals, hashCode
@NoArgsConstructor // Constructor rỗng
@AllArgsConstructor // Constructor đầy đủ
@Builder // Design pattern Builder
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID) 
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "student_code", length = 10, unique = true)
    private String studentCode;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "role", nullable = false)
    private String role;


    @Column(name = "reputation_score")
    private Integer reputationScore;

    @Column(name = "noti_device", unique = true)
    private String notiDevice;

    @CreationTimestamp 
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

}