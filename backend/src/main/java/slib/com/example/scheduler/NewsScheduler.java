package slib.com.example.scheduler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import slib.com.example.entity.news.News;
import slib.com.example.repository.news.NewsRepository;

import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Real-time scheduler để publish tin tức chính xác theo thời gian đã hẹn.
 * Sử dụng ScheduledExecutorService thay vì polling mỗi phút.
 */
@Component
@Slf4j
public class NewsScheduler {

    @Autowired
    private NewsRepository newsRepository;

    @Autowired(required = false)
    private SimpMessagingTemplate messagingTemplate;

    // Executor để schedule các task publish
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    // Map lưu các scheduled tasks để có thể cancel nếu cần
    private final Map<Long, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    /**
     * Khi application khởi động, load tất cả tin scheduled và đặt timer
     */
    @EventListener(ContextRefreshedEvent.class)
    public void onApplicationStart() {
        log.info("[NewsScheduler] Initializing real-time news scheduler");
        loadAndScheduleAllPendingNews();
    }

    /**
     * Load tất cả tin chưa publish có publishedAt trong tương lai và schedule
     */
    public void loadAndScheduleAllPendingNews() {
        LocalDateTime now = LocalDateTime.now();

        // Tìm tin đã đến giờ nhưng chưa publish -> publish ngay
        List<News> overdueNews = newsRepository.findScheduledNewsToPublish(now);
        for (News news : overdueNews) {
            publishNewsImmediately(news);
        }

        // Tìm tin chưa đến giờ -> schedule
        List<News> futureNews = newsRepository.findFutureScheduledNews(now);
        log.info("[NewsScheduler] Found {} future scheduled news", futureNews.size());

        for (News news : futureNews) {
            scheduleNewsPublication(news);
        }
    }

    /**
     * Schedule một tin để publish vào đúng thời điểm publishedAt
     */
    public void scheduleNewsPublication(News news) {
        if (news.getPublishedAt() == null || news.getIsPublished()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime publishTime = news.getPublishedAt();

        // Nếu đã quá giờ, publish ngay
        if (!publishTime.isAfter(now)) {
            publishNewsImmediately(news);
            return;
        }

        // Tính delay đến thời điểm publish
        long delayMillis = Duration.between(now, publishTime).toMillis();

        log.info("[NewsScheduler] Scheduling news ID {} '{}' to publish at {} (in {} seconds)",
                news.getId(), news.getTitle(), publishTime, delayMillis / 1000);

        // Cancel task cũ nếu có (trường hợp update lịch)
        cancelScheduledTask(news.getId());

        // Schedule task mới
        ScheduledFuture<?> future = scheduler.schedule(
                () -> publishNewsById(news.getId()),
                delayMillis,
                TimeUnit.MILLISECONDS);

        scheduledTasks.put(news.getId(), future);
    }

    /**
     * Cancel scheduled task cho một tin
     */
    public void cancelScheduledTask(Long newsId) {
        ScheduledFuture<?> existingTask = scheduledTasks.remove(newsId);
        if (existingTask != null && !existingTask.isDone()) {
            existingTask.cancel(false);
            log.info("[NewsScheduler] Cancelled scheduled task for news ID {}", newsId);
        }
    }

    /**
     * Publish tin ngay lập tức (dùng cho tin đã quá giờ)
     */
    @Transactional
    public void publishNewsImmediately(News news) {
        publishNewsById(news.getId());
    }

    /**
     * Publish tin theo ID (được gọi bởi scheduled task)
     */
    @Transactional
    public void publishNewsById(Long newsId) {
        try {
            int updated = newsRepository.markPublishedIfPending(newsId);
            if (updated == 0) {
                return;
            }

            News news = newsRepository.findById(newsId).orElse(null);
            if (news == null) {
                return;
            }

            log.info("[NewsScheduler] Published on schedule: {}", news.getTitle());

            // Thông báo qua WebSocket
            notifyNewsPublished(news);
        } catch (Exception e) {
            log.error("[NewsScheduler] Error publishing news ID {}", newsId, e);
        } finally {
            scheduledTasks.remove(newsId);
        }
    }

    /**
     * Gửi thông báo WebSocket khi tin được publish
     */
    private void notifyNewsPublished(News news) {
        if (messagingTemplate != null) {
            try {
                Map<String, Object> payload = Map.of(
                        "type", "NEWS_PUBLISHED",
                        "newsId", news.getId(),
                        "title", news.getTitle(),
                        "publishedAt", news.getPublishedAt().toString());
                messagingTemplate.convertAndSend("/topic/news", payload);
                log.info("[NewsScheduler] WebSocket notification sent for: {}", news.getTitle());
            } catch (Exception e) {
                log.warn("[NewsScheduler] WebSocket notification failed for newsId={}", news.getId(), e);
            }
        }
    }

    /**
     * Cleanup khi application shutdown
     */
    @PreDestroy
    public void shutdown() {
        log.info("[NewsScheduler] Shutting down scheduler");
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
        }
    }
}
