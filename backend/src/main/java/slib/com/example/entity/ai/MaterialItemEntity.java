package slib.com.example.entity.ai;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Material Item Entity - Individual file/text within a Material
 */
@Entity
@Table(name = "ai_material_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MaterialItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "material_id", nullable = false)
    private MaterialEntity material;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private ItemType type;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content; // For TEXT type

    @Column(name = "file_name", length = 255)
    private String fileName; // For FILE type

    @Column(name = "file_path", length = 500)
    private String filePath; // For FILE type

    @Column(name = "file_size")
    private Long fileSize;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public enum ItemType {
        FILE, TEXT
    }
}
