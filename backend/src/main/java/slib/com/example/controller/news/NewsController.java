package slib.com.example.controller.news;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import slib.com.example.dto.NewsListDTO;
import slib.com.example.entity.news.News;
import slib.com.example.service.NewsService;

import java.util.List;

@RestController
@RequestMapping("/slib/news")
@CrossOrigin(origins = "*", allowedHeaders = "*") 
public class NewsController {

    @Autowired
    private NewsService newsService;

    // 1. Dành cho Sinh viên - Mobile App
    @GetMapping("/public")
    public ResponseEntity<List<News>> getPublicNews() {
        List<News> newsList = newsService.getPublicNews();
        return ResponseEntity.ok(newsList);
    }

    @GetMapping("/public/category/{categoryId}")
    public ResponseEntity<List<News>> getPublicNewsByCategory(@PathVariable Long categoryId) {
        return ResponseEntity.ok(newsService.getPublicNewsByCategory(categoryId));
    }

    @GetMapping("/public/detail/{id}")
    public ResponseEntity<News> getNewsDetail(@PathVariable Long id) {
        return ResponseEntity.ok(newsService.getNewsDetailAndIncrementView(id));
    }

    // 2. ADMIN API (Dành cho Web Admin/Librarian)
    @GetMapping("/admin/all")
    public ResponseEntity<List<NewsListDTO>> getAllNewsForAdmin() {
        return ResponseEntity.ok(newsService.getAllNewsForAdmin());
    }

    @GetMapping("/admin/detail/{id}")
    public ResponseEntity<NewsListDTO> getNewsDetailForAdmin(@PathVariable Long id) {
        return ResponseEntity.ok(newsService.getNewsDetailForAdmin(id));
    }

    @GetMapping("/admin/image/{id}")
    public ResponseEntity<String> getNewsImage(@PathVariable Long id) {
        return ResponseEntity.ok(newsService.getNewsImage(id));
    }


    @PostMapping("/admin")
    public ResponseEntity<News> createNews(@RequestBody News news) {
        News createdNews = newsService.createNews(news);
        return ResponseEntity.ok(createdNews);
    }

    @PutMapping("/admin/{id}")
    public ResponseEntity<News> updateNews(@PathVariable Long id, @RequestBody News newsDetails) {
        News updatedNews = newsService.updateNews(id, newsDetails);
        return ResponseEntity.ok(updatedNews);
    }

    @DeleteMapping("/admin/{id}")
    public ResponseEntity<String> deleteNews(@PathVariable Long id) {
        newsService.deleteNews(id);
        return ResponseEntity.ok("Đã xóa tin tức thành công!");
    }
    
    @PatchMapping("/admin/{id}/toggle-pin")
    public ResponseEntity<News> togglePinNews(@PathVariable Long id) {
        News toggledNews = newsService.togglePinNews(id);
        return ResponseEntity.ok(toggledNews);
    }
}