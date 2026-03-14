package slib.com.example.repository.news;

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

    /**
     * Tìm các tin tức đã đến thời gian hẹn nhưng chưa được publish
     */
    @Query("SELECT n FROM News n WHERE n.publishedAt <= :now AND n.isPublished = false")
    List<News> findScheduledNewsToPublish(@Param("now") java.time.LocalDateTime now);

    /**
     * Tìm các tin tức có lịch publish trong tương lai (chưa đến giờ)
     */
    @Query("SELECT n FROM News n WHERE n.publishedAt > :now AND n.isPublished = false")
    List<News> findFutureScheduledNews(@Param("now") java.time.LocalDateTime now);

}