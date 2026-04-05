package slib.com.example.entity.zone_config;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "layout_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LayoutHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "history_id")
    private Long historyId;

    @Column(name = "action_type", nullable = false, length = 32)
    private String actionType;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @Column(name = "snapshot_json", nullable = false, columnDefinition = "TEXT")
    private String snapshotJson;

    @Column(name = "published_version")
    private Long publishedVersion;

    @Column(name = "created_by_user_id")
    private UUID createdByUserId;

    @Column(name = "created_by_name")
    private String createdByName;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
