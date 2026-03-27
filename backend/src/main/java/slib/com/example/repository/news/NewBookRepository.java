package slib.com.example.repository.news;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import slib.com.example.entity.news.NewBookEntity;

import java.util.List;

@Repository
public interface NewBookRepository extends JpaRepository<NewBookEntity, Integer> {
    List<NewBookEntity> findByIsActiveTrue(Sort sort);
}
