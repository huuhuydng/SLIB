package slib.com.example.scheduler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import slib.com.example.entity.news.News;
import slib.com.example.repository.NewsRepository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled task để tự động publish tin tức khi đến thời gian hẹn
 */
@Component
public class NewsScheduler {

    @Autowired
    private NewsRepository newsRepository;

    /**
     * Chạy mỗi phút để kiểm tra và publish các tin tức đã đến thời gian hẹn
     */
    @Scheduled(fixedRate = 60000) // 60 giây = 1 phút
    @Transactional
    public void publishScheduledNews() {
        LocalDateTime now = LocalDateTime.now();

        // Tìm các tin tức có publishedAt <= now và isPublished = false
        List<News> scheduledNews = newsRepository.findScheduledNewsToPublish(now);

        if (!scheduledNews.isEmpty()) {
            System.out.println("📰 [NewsScheduler] Found " + scheduledNews.size() + " scheduled news to publish");

            for (News news : scheduledNews) {
                news.setIsPublished(true);
                newsRepository.save(news);
                System.out.println("✅ [NewsScheduler] Published: " + news.getTitle());
            }
        }
    }
}
