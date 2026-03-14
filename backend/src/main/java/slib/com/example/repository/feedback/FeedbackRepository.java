package slib.com.example.repository.feedback;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import slib.com.example.entity.feedback.FeedbackEntity;
import slib.com.example.entity.feedback.FeedbackEntity.FeedbackStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface FeedbackRepository extends JpaRepository<FeedbackEntity, UUID> {

    List<FeedbackEntity> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<FeedbackEntity> findByStatusOrderByCreatedAtDesc(FeedbackStatus status);

    List<FeedbackEntity> findAllByOrderByCreatedAtDesc();

    long countByStatus(FeedbackStatus status);

    // Dashboard: lấy 5 phản hồi gần đây nhất
    List<FeedbackEntity> findTop5ByOrderByCreatedAtDesc();

    // Statistic: rating trung bình trong range
    @Query(value = "SELECT COALESCE(AVG(rating), 0) FROM feedbacks WHERE created_at >= :startDate AND rating IS NOT NULL", nativeQuery = true)
    double getAverageRatingAfter(@Param("startDate") LocalDateTime startDate);

    // Statistic: phân bổ rating trong range
    @Query(value = "SELECT rating, COUNT(*) as cnt FROM feedbacks WHERE created_at >= :startDate AND rating IS NOT NULL "
            +
            "GROUP BY rating ORDER BY rating", nativeQuery = true)
    List<Object[]> countByRatingAfter(@Param("startDate") LocalDateTime startDate);

    // Statistic: tổng feedback trong range
    long countByCreatedAtAfter(LocalDateTime startDate);

    // Statistic: feedback gần đây trong range (top 10)
    List<FeedbackEntity> findTop10ByCreatedAtAfterOrderByCreatedAtDesc(LocalDateTime startDate);
}
