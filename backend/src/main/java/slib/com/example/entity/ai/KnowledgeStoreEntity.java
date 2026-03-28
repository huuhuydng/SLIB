package slib.com.example.entity.ai;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Knowledge Store Entity - Curated selection of MaterialItems for RAG
 * Status tracks sync state with vector database
 */
@Entity
@Table(name = "ai_knowledge_stores")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KnowledgeStoreEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "status", nullable = false, length = 20)
    private SyncStatus status = SyncStatus.CHANGED;

    @Builder.Default
    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @Column(name = "last_synced_at")
    private LocalDateTime lastSyncedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Builder.Default
    @ManyToMany
    @JoinTable(name = "ai_knowledge_store_items", joinColumns = @JoinColumn(name = "knowledge_store_id"), inverseJoinColumns = @JoinColumn(name = "material_item_id"))
    private Set<MaterialItemEntity> items = new HashSet<>();

    public enum SyncStatus {
        CHANGED, // Needs sync
        SYNCING, // Currently syncing
        SYNCED, // Up to date
        ERROR // Sync failed
    }
}
