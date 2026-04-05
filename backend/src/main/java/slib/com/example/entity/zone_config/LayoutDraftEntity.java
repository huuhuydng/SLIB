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
@Table(name = "layout_drafts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LayoutDraftEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "draft_id")
    private Long draftId;

    @Lob
    @Column(name = "snapshot_json", nullable = false)
    private String snapshotJson;

    @Column(name = "based_on_published_version")
    private Long basedOnPublishedVersion;

    @Column(name = "updated_by_user_id")
    private UUID updatedByUserId;

    @Column(name = "updated_by_name")
    private String updatedByName;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
