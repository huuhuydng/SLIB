package slib.com.example.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import slib.com.example.dto.NewsListDTO;
import slib.com.example.entity.news.News;
import slib.com.example.repository.NewsRepository;
import slib.com.example.scheduler.NewsScheduler;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class NewsService {

    @Autowired
    private NewsRepository newsRepository;

    @Autowired
    private NewsScheduler newsScheduler;

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

        // Schedule nếu là tin lên lịch (có publishedAt nhưng chưa publish)
        if (!Boolean.TRUE.equals(savedNews.getIsPublished()) && savedNews.getPublishedAt() != null) {
            newsScheduler.scheduleNewsPublication(savedNews);
        }

        return savedNews;
    }

    public News updateNews(Long id, News newsDetails) {
        News existingNews = newsRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tin tức để sửa!"));

        existingNews.setTitle(newsDetails.getTitle());
        existingNews.setSummary(newsDetails.getSummary());
        existingNews.setContent(newsDetails.getContent());
        existingNews.setImageUrl(newsDetails.getImageUrl());
        existingNews.setCategory(newsDetails.getCategory());
        existingNews.setIsPinned(newsDetails.getIsPinned());
        existingNews.setPublishedAt(newsDetails.getPublishedAt());

        if (!existingNews.getIsPublished() && Boolean.TRUE.equals(newsDetails.getIsPublished())) {
            existingNews.setPublishedAt(LocalDateTime.now());
        }
        existingNews.setIsPublished(newsDetails.getIsPublished());

        News savedNews = newsRepository.save(existingNews);

        // Schedule/reschedule nếu là tin lên lịch
        if (!Boolean.TRUE.equals(savedNews.getIsPublished()) && savedNews.getPublishedAt() != null) {
            newsScheduler.scheduleNewsPublication(savedNews);
        } else {
            // Cancel nếu đã publish hoặc không còn lịch
            newsScheduler.cancelScheduledTask(id);
        }

        return savedNews;
    }

    public void deleteNews(Long id) {
        if (!newsRepository.existsById(id)) {
            throw new RuntimeException("Không tồn tại tin tức để xóa!");
        }
        // Cancel scheduled task nếu có
        newsScheduler.cancelScheduledTask(id);
        newsRepository.deleteById(id);
    }

    /**
     * Toggle pin status của tin tức
     */
    public News togglePin(Long id) {
        News news = newsRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tin tức: " + id));
        news.setIsPinned(!Boolean.TRUE.equals(news.getIsPinned()));
        return newsRepository.save(news);
    }
}