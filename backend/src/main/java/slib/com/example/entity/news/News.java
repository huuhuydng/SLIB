package slib.com.example.entity.news;

import jakarta.persistence.*;
import lombok.*;
import slib.com.example.entity.users.User;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "news", indexes = {
    @Index(name = "idx_news_published", columnList = "is_published, published_at DESC")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class News {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String summary; 

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content; 

    @Column(name = "image_url")
    private String imageUrl; 

    @ManyToOne(fetch = FetchType.EAGER) 
    @JoinColumn(name = "category_id")
    @ToString.Exclude 
    @EqualsAndHashCode.Exclude 
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY) 
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude 
    @EqualsAndHashCode.Exclude 
    private User author;
    
    @Column(name = "user_id", insertable = false, updatable = false)
    private UUID authorId;

    @Column(name = "is_published", nullable = false)
    @Builder.Default
    private Boolean isPublished = false;

    @Column(name = "is_pinned", nullable = false)
    @Builder.Default
    private Boolean isPinned = false; 

    @Column(name = "view_count", nullable = false)
    @Builder.Default
    private Integer viewCount = 0;

    @Column(name = "published_at")
    private LocalDateTime publishedAt; 

    @CreationTimestamp 
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp 
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}