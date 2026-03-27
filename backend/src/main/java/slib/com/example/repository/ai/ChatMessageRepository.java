package slib.com.example.repository.ai;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import slib.com.example.entity.ai.ChatMessageEntity;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessageEntity, Long> {

    @Query("SELECT m FROM ChatMessageEntity m WHERE m.session.id = :sessionId ORDER BY m.createdAt ASC")
    List<ChatMessageEntity> findBySessionIdOrderByCreatedAtAsc(@Param("sessionId") Long sessionId);

    // Find messages needing librarian review
    @Query("SELECT m FROM ChatMessageEntity m WHERE m.needsReview = true ORDER BY m.createdAt ASC")
    List<ChatMessageEntity> findMessagesNeedingReview();

    // Get recent messages for context (last N messages)
    @Query("SELECT m FROM ChatMessageEntity m WHERE m.session.id = :sessionId ORDER BY m.createdAt DESC LIMIT :limit")
    List<ChatMessageEntity> findRecentMessages(@Param("sessionId") Long sessionId, @Param("limit") int limit);
}
