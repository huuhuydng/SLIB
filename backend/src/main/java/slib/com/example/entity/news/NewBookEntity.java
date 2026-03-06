package slib.com.example.entity.news;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import slib.com.example.entity.users.User;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * New Book Entity
 * New book arrivals display for library
 */
@Entity
@Table(name = "new_books")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NewBookEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "title", length = 300, nullable = false)
    private String title;

    @Column(name = "author", length = 200)
    private String author;

    @Column(name = "isbn", length = 20)
    private String isbn;

    @Column(name = "cover_url", columnDefinition = "TEXT")
    private String coverUrl;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "category", length = 100)
    private String category;

    @Column(name = "publish_date")
    private LocalDate publishDate;

    @Builder.Default
    @Column(name = "arrival_date")
    private LocalDate arrivalDate = LocalDate.now();

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
