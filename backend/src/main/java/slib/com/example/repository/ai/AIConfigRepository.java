package slib.com.example.repository.ai;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import slib.com.example.entity.ai.AIConfigEntity;

import java.util.Optional;

@Repository
public interface AIConfigRepository extends JpaRepository<AIConfigEntity, Long> {

    // Get the singleton config (first record)
    default Optional<AIConfigEntity> getConfig() {
        return findAll().stream().findFirst();
    }
}
