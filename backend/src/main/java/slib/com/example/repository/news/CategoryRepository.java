package slib.com.example.repository.news;

import org.springframework.data.jpa.repository.JpaRepository;
import slib.com.example.entity.news.Category;

import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    Optional<Category> findByName(String name);

    boolean existsByName(String name);
}
