package slib.com.example.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import slib.com.example.entity.news.News; 
import slib.com.example.repository.NewsRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NewsService {

    @Autowired
    private NewsRepository newsRepository;


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

    public List<News> getAllNewsForAdmin() {
        return newsRepository.findAll(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));
    }


    public News createNews(News news) {
        if (Boolean.TRUE.equals(news.getIsPublished())) {
            news.setPublishedAt(LocalDateTime.now());
        }
        if (news.getViewCount() == null) {
            news.setViewCount(0);
        }
        return newsRepository.save(news);
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


        if (!existingNews.getIsPublished() && Boolean.TRUE.equals(newsDetails.getIsPublished())) {
            existingNews.setPublishedAt(LocalDateTime.now());
        }
        existingNews.setIsPublished(newsDetails.getIsPublished());
        return newsRepository.save(existingNews);
    }


    public void deleteNews(Long id) {
        if (!newsRepository.existsById(id)) {
            throw new RuntimeException("Không tồn tại tin tức để xóa!");
        }
        newsRepository.deleteById(id);
    }
}