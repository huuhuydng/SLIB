package slib.com.example.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import slib.com.example.entity.reputation.ReputationRuleEntity;

import java.util.Optional;

@Repository
public interface ReputationRuleRepository extends JpaRepository<ReputationRuleEntity, Integer> {

    Optional<ReputationRuleEntity> findByRuleCode(String ruleCode);
}
