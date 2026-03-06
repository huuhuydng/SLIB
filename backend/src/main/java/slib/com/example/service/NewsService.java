package slib.com.example.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import slib.com.example.dto.NewsListDTO;
import slib.com.example.entity.news.News;
import slib.com.example.entity.notification.NotificationEntity.NotificationType;
import slib.com.example.repository.NewsRepository;
import slib.com.example.scheduler.NewsScheduler;
import slib.com.example.service.chat.CloudinaryService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class NewsService {

    @Autowired
    private NewsRepository newsRepository;

    @Autowired
    private NewsScheduler newsScheduler;

    @Autowired
    private CloudinaryService cloudinaryService;

    @Autowired(required = false)
    private PushNotificationService pushNotificationService;

    public List<News> getPublicNews() {
        return newsRepository.getAllPublishedNews();
    }

    public List<News> getPublicNewsByCategory(Long categoryId) {
        return newsRepository.getPublishedNewsByCategory(categoryId);
    }

    @Transactional
    public News getNewsDetailAndIncrementView(Long newsId) {
        News news = newsRepository.findById(newsId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tin tức với ID: " + newsId));
        news.setViewCount(news.getViewCount() + 1);
        return newsRepository.save(news);
    }

    // --- ADMIN/LIBRARIAN (WEB PORTAL) ---

    public List<NewsListDTO> getAllNewsForAdmin() {
        return newsRepository
                .findAll(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC,
                        "createdAt"))
                .stream()
                .map(news -> NewsListDTO.builder()
                        .id(news.getId())
                        .title(news.getTitle())
                        .summary(news.getSummary())
                        .categoryId(news.getCategory() != null ? news.getCategory().getId() : null)
                        .categoryName(news.getCategory() != null ? news.getCategory().getName() : null)
                        .isPublished(news.getIsPublished())
                        .isPinned(news.getIsPinned())
                        .viewCount(news.getViewCount())
                        .createdAt(news.getCreatedAt())
                        .publishedAt(news.getPublishedAt())
                        .imageUrl(news.getImageUrl())
                        .build())
                .collect(Collectors.toList());
    }

    public NewsListDTO getNewsDetailForAdmin(Long id) {
        News news = newsRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tin tức"));
        return NewsListDTO.builder()
                .id(news.getId())
                .title(news.getTitle())
                .summary(news.getSummary())
                .content(news.getContent())
                .categoryId(news.getCategory() != null ? news.getCategory().getId() : null)
                .categoryName(news.getCategory() != null ? news.getCategory().getName() : null)
                .isPublished(news.getIsPublished())
                .isPinned(news.getIsPinned())
                .viewCount(news.getViewCount())
                .createdAt(news.getCreatedAt())
                .publishedAt(news.getPublishedAt())
                .imageUrl(news.getImageUrl())
                .build();
    }

    public String getNewsImage(Long id) {
        News news = newsRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tin tức"));
        return news.getImageUrl();
    }

    public News createNews(News news) {
        if (Boolean.TRUE.equals(news.getIsPublished())) {
            news.setPublishedAt(LocalDateTime.now());
        }
        if (news.getViewCount() == null) {
            news.setViewCount(0);
        }
        News savedNews = newsRepository.save(news);

        // Send push notification if news is published immediately
        if (Boolean.TRUE.equals(savedNews.getIsPublished())) {
            sendNewsNotification(savedNews);
        }

        // Schedule neu la tin len lich (co publishedAt nhung chua publish)
        if (!Boolean.TRUE.equals(savedNews.getIsPublished()) && savedNews.getPublishedAt() != null) {
            newsScheduler.scheduleNewsPublication(savedNews);
        }

        return savedNews;
    }

    public News updateNews(Long id, News newsDetails) {
        News existingNews = newsRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tin tức để sửa!"));

        boolean wasPublished = Boolean.TRUE.equals(existingNews.getIsPublished());

        existingNews.setTitle(newsDetails.getTitle());
        existingNews.setSummary(newsDetails.getSummary());
        existingNews.setContent(newsDetails.getContent());

        // Xoa anh cu tren Cloudinary neu imageUrl thay doi
        String oldImageUrl = existingNews.getImageUrl();
        String newImageUrl = newsDetails.getImageUrl();
        if (oldImageUrl != null && !oldImageUrl.isEmpty()
                && (newImageUrl == null || !oldImageUrl.equals(newImageUrl))) {
            cloudinaryService.deleteImageByUrl(oldImageUrl);
        }

        existingNews.setImageUrl(newsDetails.getImageUrl());
        existingNews.setCategory(newsDetails.getCategory());
        existingNews.setIsPinned(newsDetails.getIsPinned());
        existingNews.setPublishedAt(newsDetails.getPublishedAt());

        if (!existingNews.getIsPublished() && Boolean.TRUE.equals(newsDetails.getIsPublished())) {
            existingNews.setPublishedAt(LocalDateTime.now());
        }
        existingNews.setIsPublished(newsDetails.getIsPublished());

        News savedNews = newsRepository.save(existingNews);

        // Send notification if news was just published
        if (!wasPublished && Boolean.TRUE.equals(savedNews.getIsPublished())) {
            sendNewsNotification(savedNews);
        }

        // Schedule/reschedule neu la tin len lich
        if (!Boolean.TRUE.equals(savedNews.getIsPublished()) && savedNews.getPublishedAt() != null) {
            newsScheduler.scheduleNewsPublication(savedNews);
        } else {
            // Cancel neu da publish hoac khong con lich
            newsScheduler.cancelScheduledTask(id);
        }

        return savedNews;
    }

    public void deleteNews(Long id) {
        News news = newsRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tồn tại tin tức để xóa!"));

        // Xoa anh tren Cloudinary neu co
        if (news.getImageUrl() != null && !news.getImageUrl().isEmpty()) {
            cloudinaryService.deleteImageByUrl(news.getImageUrl());
        }

        // Cancel scheduled task nếu có
        newsScheduler.cancelScheduledTask(id);
        newsRepository.deleteById(id);
    }

    /**
     * Toggle pin status cua tin tuc
     */
    public News togglePin(Long id) {
        News news = newsRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tin tức: " + id));
        news.setIsPinned(!Boolean.TRUE.equals(news.getIsPinned()));
        return newsRepository.save(news);
    }

    /**
     * Send push notification to all users when news is published
     */
    private void sendNewsNotification(News news) {
        if (pushNotificationService == null) {
            return;
        }

        try {
            String title = "Tin tức mới";
            String body = news.getTitle();
            if (news.getSummary() != null && !news.getSummary().isEmpty()) {
                body = news.getSummary();
            }

            // Convert Long id to UUID for referenceId (or use null if not compatible)
            UUID referenceId = null; // News uses Long id, notifications use UUID

            pushNotificationService.sendToRole("USER", title, body, NotificationType.NEWS, referenceId);
        } catch (Exception e) {
            System.err.println("Failed to send news notification: " + e.getMessage());
        }
    }
}