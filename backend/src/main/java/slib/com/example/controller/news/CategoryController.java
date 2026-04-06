package slib.com.example.controller.news;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import slib.com.example.dto.news.CategoryCreateRequest;
import slib.com.example.entity.news.Category;
import slib.com.example.service.news.CategoryService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/slib/news-categories")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    /**
     * Lấy tất cả categories
     */
    @GetMapping
    public ResponseEntity<List<Category>> getAllCategories() {
        return ResponseEntity.ok(categoryService.getAllCategories());
    }

    /**
     * Tạo category mới
     */
    @PostMapping
    public ResponseEntity<?> createCategory(@Valid @RequestBody CategoryCreateRequest request) {
        try {
            Category category = categoryService.createCategory(request.getName(), request.getColorCode());
            return ResponseEntity.ok(category);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Xoá category
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCategory(@PathVariable Long id) {
        try {
            categoryService.deleteCategory(id);
            return ResponseEntity.ok(Map.of("message", "Đã xoá danh mục thành công"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
