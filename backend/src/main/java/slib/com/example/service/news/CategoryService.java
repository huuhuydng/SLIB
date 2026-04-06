package slib.com.example.service.news;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import slib.com.example.entity.news.Category;
import slib.com.example.exception.BadRequestException;
import slib.com.example.repository.news.CategoryRepository;
import slib.com.example.util.ContentValidationUtil;

import java.util.List;

@Service
public class CategoryService {

    @Autowired
    private CategoryRepository categoryRepository;

    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    public Category createCategory(String name, String colorCode) {
        String normalizedName = ContentValidationUtil.normalizeRequiredText(name, "Tên danh mục", 50);
        String normalizedColorCode = ContentValidationUtil.normalizeOptionalColorCode(colorCode);

        if (categoryRepository.findByNameIgnoreCase(normalizedName).isPresent()) {
            throw new BadRequestException("Danh mục đã tồn tại: " + normalizedName);
        }

        Category category = Category.builder()
                .name(normalizedName)
                .colorCode(normalizedColorCode)
                .build();

        return categoryRepository.save(category);
    }

    public void deleteCategory(Long id) {
        if (!categoryRepository.existsById(id)) {
            throw new RuntimeException("Category not found: " + id);
        }
        categoryRepository.deleteById(id);
    }

    public Category getCategoryById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found: " + id));
    }
}
