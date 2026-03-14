package slib.com.example.service.news;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import slib.com.example.entity.news.Category;
import slib.com.example.repository.news.CategoryRepository;

import java.util.List;

@Service
public class CategoryService {

    @Autowired
    private CategoryRepository categoryRepository;

    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    public Category createCategory(String name, String colorCode) {
        if (categoryRepository.existsByName(name)) {
            throw new RuntimeException("Category already exists: " + name);
        }

        Category category = Category.builder()
                .name(name)
                .colorCode(colorCode != null ? colorCode : "#6366F1")
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
