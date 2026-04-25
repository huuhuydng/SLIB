package slib.com.example.service.news;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import slib.com.example.dto.news.NewsListDTO;
import slib.com.example.dto.news.NewsUpsertRequest;
import slib.com.example.entity.news.Category;
import slib.com.example.entity.news.News;
import slib.com.example.entity.notification.NotificationEntity.NotificationType;
import slib.com.example.repository.news.NewsRepository;
import slib.com.example.scheduler.NewsScheduler;
import slib.com.example.service.chat.CloudinaryService;
import slib.com.example.service.system.HtmlSanitizerService;
import slib.com.example.util.ContentValidationUtil;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import slib.com.example.service.notification.PushNotificationService;

@Service
@Slf4j
public class NewsService {

    @Autowired
    private NewsRepository newsRepository;

    @Autowired
    private NewsScheduler newsScheduler;

    @Autowired
    private CloudinaryService cloudinaryService;

    @Autowired(required = false)
    private PushNotificationService pushNotificationService;

    @Autowired
    private HtmlSanitizerService htmlSanitizerService;

    @Autowired
    private CategoryService categoryService;

    @Transactional(readOnly = true)
    public List<NewsListDTO> getPublicNews() {
        return newsRepository.getAllPublishedNews().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<NewsListDTO> getPublicNewsByCategory(Long categoryId) {
        return newsRepository.getPublishedNewsByCategory(categoryId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public NewsListDTO getNewsDetailAndIncrementView(Long newsId) {
        News news = newsRepository.findById(newsId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tin tức với ID: " + newsId));
        news.setViewCount(news.getViewCount() + 1);
        News saved = newsRepository.save(news);
        return toDTO(saved);
    }

    public NewsListDTO toDTO(News news) {
        return NewsListDTO.builder()
                .id(news.getId())
                .title(news.getTitle())
                .summary(news.getSummary())
                .content(htmlSanitizerService.sanitizeRichText(news.getContent()))
                .categoryId(news.getCategory() != null ? news.getCategory().getId() : null)
                .categoryName(news.getCategory() != null ? news.getCategory().getName() : null)
                .isPublished(news.getIsPublished())
                .isPinned(news.getIsPinned())
                .viewCount(news.getViewCount())
                .createdAt(news.getCreatedAt())
                .publishedAt(news.getPublishedAt())
                .imageUrl(news.getImageUrl())
                .pdfUrl(news.getPdfUrl())
                .pdfFileName(news.getPdfFileName())
                .pdfFileSize(news.getPdfFileSize())
                .build();
    }

    // --- ADMIN/LIBRARIAN (WEB PORTAL) ---

    @Transactional(readOnly = true)
    public List<NewsListDTO> getAllNewsForAdmin() {
        return newsRepository
                .findAll(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC,
                        "createdAt"))
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public NewsListDTO getNewsDetailForAdmin(Long id) {
        News news = newsRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tin tức"));
        return toDTO(news);
    }

    @Transactional(readOnly = true)
    public String getNewsImage(Long id) {
        News news = newsRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tin tức"));
        return news.getImageUrl();
    }

    public NewsListDTO createNews(NewsUpsertRequest request) {
        News news = News.builder().build();
        applyRequest(news, request, false);
        News savedNews = newsRepository.save(news);

        // Send push notification if news is published immediately
        if (Boolean.TRUE.equals(savedNews.getIsPublished())) {
            sendNewsNotification(savedNews);
        }

        // Schedule neu la tin len lich (co publishedAt nhung chua publish)
        if (!Boolean.TRUE.equals(savedNews.getIsPublished()) && savedNews.getPublishedAt() != null) {
            newsScheduler.scheduleNewsPublication(savedNews);
        }

        return toDTO(savedNews);
    }

    public NewsListDTO updateNews(Long id, NewsUpsertRequest newsDetails) {
        News existingNews = newsRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tin tức để sửa!"));

        boolean wasPublished = Boolean.TRUE.equals(existingNews.getIsPublished());

        // Xoa anh cu tren Cloudinary neu imageUrl thay doi
        String oldImageUrl = existingNews.getImageUrl();
        String newImageUrl = newsDetails.getImageUrl();
        if (oldImageUrl != null && !oldImageUrl.isEmpty()
                && (newImageUrl == null || !oldImageUrl.equals(newImageUrl))) {
            cloudinaryService.deleteImageByUrl(oldImageUrl);
        }

        String oldPdfUrl = existingNews.getPdfUrl();
        String newPdfUrl = newsDetails.getPdfUrl();
        if (oldPdfUrl != null && !oldPdfUrl.isEmpty()
                && (newPdfUrl == null || !oldPdfUrl.equals(newPdfUrl))) {
            cloudinaryService.deleteRawFileByUrl(oldPdfUrl);
        }

        applyRequest(existingNews, newsDetails, wasPublished);
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

        return toDTO(savedNews);
    }

    public void deleteNews(Long id) {
        News news = newsRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tồn tại tin tức để xóa!"));

        // Xoa anh tren Cloudinary neu co
        if (news.getImageUrl() != null && !news.getImageUrl().isEmpty()) {
            cloudinaryService.deleteImageByUrl(news.getImageUrl());
        }
        if (news.getPdfUrl() != null && !news.getPdfUrl().isEmpty()) {
            cloudinaryService.deleteRawFileByUrl(news.getPdfUrl());
        }

        // Cancel scheduled task nếu có
        newsScheduler.cancelScheduledTask(id);
        newsRepository.deleteById(id);
    }

    public void deleteBatch(List<Long> ids) {
        for (Long id : ids) {
            newsRepository.findById(id).ifPresent(news -> {
                if (news.getImageUrl() != null && !news.getImageUrl().isEmpty()) {
                    cloudinaryService.deleteImageByUrl(news.getImageUrl());
                }
                if (news.getPdfUrl() != null && !news.getPdfUrl().isEmpty()) {
                    cloudinaryService.deleteRawFileByUrl(news.getPdfUrl());
                }
                newsScheduler.cancelScheduledTask(id);
            });
        }
        newsRepository.deleteAllById(ids);
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

    private void applyRequest(News entity, NewsUpsertRequest request, boolean wasPublished) {
        String title = ContentValidationUtil.normalizeRequiredText(request.getTitle(), "Tiêu đề tin tức", 255);
        String summary = ContentValidationUtil.normalizeOptionalText(request.getSummary(), "Tóm tắt", 1000);
        String imageUrl = ContentValidationUtil.normalizeOptionalUrl(request.getImageUrl(), "Đường dẫn ảnh", 1000);
        String pdfUrl = ContentValidationUtil.normalizeOptionalUrl(request.getPdfUrl(), "Đường dẫn PDF", 1000);
        String pdfFileName = ContentValidationUtil.normalizeOptionalText(request.getPdfFileName(), "Tên file PDF", 255);
        Long pdfFileSize = request.getPdfFileSize();
        if (pdfUrl == null) {
            pdfFileName = null;
            pdfFileSize = null;
        } else if (pdfFileSize != null && pdfFileSize < 0) {
            throw new slib.com.example.exception.BadRequestException("Dung lượng PDF không hợp lệ");
        }
        String content = request.getContent() != null
                ? htmlSanitizerService.sanitizeRichText(request.getContent())
                : null;
        Category category = request.getCategoryId() != null ? categoryService.getCategoryById(request.getCategoryId()) : null;
        boolean isPublished = Boolean.TRUE.equals(request.getIsPublished());

        entity.setTitle(title);
        entity.setSummary(summary);
        entity.setContent(content);
        entity.setImageUrl(imageUrl);
        entity.setPdfUrl(pdfUrl);
        entity.setPdfFileName(pdfFileName);
        entity.setPdfFileSize(pdfFileSize);
        entity.setCategory(category);
        entity.setIsPinned(Boolean.TRUE.equals(request.getIsPinned()));
        entity.setViewCount(entity.getViewCount() == null ? 0 : entity.getViewCount());

        LocalDateTime publishedAt = request.getPublishedAt();
        if (!wasPublished && isPublished) {
            publishedAt = LocalDateTime.now();
        }
        entity.setPublishedAt(publishedAt);
        entity.setIsPublished(isPublished);
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

            pushNotificationService.sendToPatrons(title, body, NotificationType.NEWS, referenceId);
        } catch (Exception e) {
            log.warn("Failed to send news notification for newsId={}", news.getId(), e);
        }
    }
}
