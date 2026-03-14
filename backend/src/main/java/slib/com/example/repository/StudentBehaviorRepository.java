package slib.com.example.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import slib.com.example.entity.analytics.StudentBehaviorEntity;

import java.util.List;
import java.util.UUID;

@Repository
public interface StudentBehaviorRepository extends JpaRepository<StudentBehaviorEntity, Integer> {

    List<StudentBehaviorEntity> findByUserId(UUID userId);

    List<StudentBehaviorEntity> findByUserIdAndBehaviorType(UUID userId, StudentBehaviorEntity.BehaviorType behaviorType);
}
