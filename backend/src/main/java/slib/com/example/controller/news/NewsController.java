package slib.com.example.controller.news;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import slib.com.example.dto.common.LongBatchRequest;
import slib.com.example.dto.news.NewsUpsertRequest;
import slib.com.example.dto.news.NewsListDTO;
import slib.com.example.entity.news.News;
import slib.com.example.service.news.NewsService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/slib/news")
public class NewsController {

    @Autowired
    private NewsService newsService;

    // 1. Dành cho Sinh viên - Mobile App
    @GetMapping("/public")
    public ResponseEntity<List<NewsListDTO>> getPublicNews() {
        return ResponseEntity.ok(newsService.getPublicNews());
    }

    @GetMapping("/public/category/{categoryId}")
    public ResponseEntity<List<NewsListDTO>> getPublicNewsByCategory(@PathVariable Long categoryId) {
        return ResponseEntity.ok(newsService.getPublicNewsByCategory(categoryId));
    }

    @GetMapping("/public/detail/{id}")
    public ResponseEntity<NewsListDTO> getNewsDetail(@PathVariable Long id) {
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
    public ResponseEntity<NewsListDTO> createNews(@Valid @RequestBody NewsUpsertRequest news) {
        return ResponseEntity.ok(newsService.createNews(news));
    }

    @PutMapping("/admin/{id}")
    public ResponseEntity<NewsListDTO> updateNews(@PathVariable Long id, @Valid @RequestBody NewsUpsertRequest newsDetails) {
        return ResponseEntity.ok(newsService.updateNews(id, newsDetails));
    }

    @DeleteMapping("/admin/{id}")
    public ResponseEntity<String> deleteNews(@PathVariable Long id) {
        newsService.deleteNews(id);
        return ResponseEntity.ok("Đã xóa tin tức thành công!");
    }

    @DeleteMapping("/admin/batch")
    public ResponseEntity<?> deleteBatch(@Valid @RequestBody LongBatchRequest body) {
        try {
            List<Long> ids = body.getIds();
            newsService.deleteBatch(ids);
            return ResponseEntity.ok(Map.of("deleted", ids.size()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Toggle pin status của tin tức
     */
    @PatchMapping("/admin/{id}/pin")
    public ResponseEntity<?> togglePin(@PathVariable Long id) {
        try {
            News news = newsService.togglePin(id);
            return ResponseEntity.ok(java.util.Map.of(
                    "id", news.getId(),
                    "isPinned", news.getIsPinned(),
                    "message", news.getIsPinned() ? "Đã ghim tin tức" : "Đã bỏ ghim tin tức"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(java.util.Map.of("error", e.getMessage()));
        }
    }
}
