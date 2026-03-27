package slib.com.example.repository.reputation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import slib.com.example.entity.reputation.ReputationRuleEntity;

import java.util.Optional;

@Repository
public interface ReputationRuleRepository extends JpaRepository<ReputationRuleEntity, Integer> {
    
    /**
     * Find a reputation rule by its code
     * @param ruleCode the unique rule code
     * @return Optional containing the rule if found
     */
    Optional<ReputationRuleEntity> findByRuleCode(String ruleCode);
    
    /**
     * Find an active reputation rule by its code
     * @param ruleCode the unique rule code
     * @param isActive whether the rule is active
     * @return Optional containing the rule if found
     */
    Optional<ReputationRuleEntity> findByRuleCodeAndIsActive(String ruleCode, Boolean isActive);
}
