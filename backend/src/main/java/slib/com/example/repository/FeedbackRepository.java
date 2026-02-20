package slib.com.example.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import slib.com.example.entity.feedback.FeedbackEntity;
import slib.com.example.entity.feedback.FeedbackEntity.FeedbackStatus;

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
}
