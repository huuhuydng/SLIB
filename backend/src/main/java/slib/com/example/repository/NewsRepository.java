package slib.com.example.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import slib.com.example.entity.news.News;

@Repository
public interface NewsRepository extends JpaRepository<News, Long> {
    @Query("SELECT n FROM News n WHERE n.isPublished = true ORDER BY n.publishedAt DESC")
    List<News> getAllPublishedNews();

    @Query("SELECT n FROM News n WHERE n.category.id = :catId AND n.isPublished = true ORDER BY n.publishedAt DESC")
    List<News> getPublishedNewsByCategory(@Param("catId") Long categoryId);

}