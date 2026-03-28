package slib.com.example.repository.ai;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import slib.com.example.entity.ai.ChatSessionEntity;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSessionEntity, Long> {

    @Query("SELECT s FROM ChatSessionEntity s WHERE s.user.id = :userId ORDER BY s.createdAt DESC")
    List<ChatSessionEntity> findByUserIdOrderByCreatedAtDesc(@Param("userId") UUID userId);

    List<ChatSessionEntity> findByStatusOrderByCreatedAtDesc(ChatSessionEntity.SessionStatus status);

    // Find escalated sessions for librarian
    @Query("SELECT s FROM ChatSessionEntity s WHERE s.status = 'ESCALATED' ORDER BY s.createdAt ASC")
    List<ChatSessionEntity> findEscalatedSessions();

    // Find active session for user (if any)
    @Query("SELECT s FROM ChatSessionEntity s WHERE s.user.id = :userId AND s.status = 'ACTIVE' ORDER BY s.createdAt DESC")
    List<ChatSessionEntity> findActiveSessionsByUser(@Param("userId") UUID userId);

    // Delete all chat sessions by user ID (cascade delete messages via entity
    // config)
    void deleteByUser_Id(UUID userId);
}
