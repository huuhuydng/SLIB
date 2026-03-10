package slib.com.example.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import slib.com.example.entity.activity.ActivityLogEntity;
import slib.com.example.entity.activity.PointTransactionEntity;
import slib.com.example.entity.reputation.ReputationRuleEntity;
import slib.com.example.entity.users.User;
import slib.com.example.repository.UserRepository;
import slib.com.example.repository.activity.ActivityLogRepository;
import slib.com.example.repository.activity.PointTransactionRepository;
import slib.com.example.repository.reputation.ReputationRuleRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing user reputation system.
 * Handles applying penalties and rewards based on reputation rules.
 */
@Service
public class ReputationService {
    
    private final UserRepository userRepository;
    private final ReputationRuleRepository reputationRuleRepository;
    private final ActivityLogRepository activityLogRepository;
    private final PointTransactionRepository pointTransactionRepository;

    public ReputationService(
            UserRepository userRepository,
            ReputationRuleRepository reputationRuleRepository,
            ActivityLogRepository activityLogRepository,
            PointTransactionRepository pointTransactionRepository) {
        this.userRepository = userRepository;
        this.reputationRuleRepository = reputationRuleRepository;
        this.activityLogRepository = activityLogRepository;
        this.pointTransactionRepository = pointTransactionRepository;
    }

    /**
     * Apply a reputation rule to a user.
     * This method dynamically applies any active rule from the database.
     * 
     * @param userId User ID to apply the rule to
     * @param ruleCode Rule code (e.g., "NO_SHOW", "LATE_CHECKOUT", etc.)
     * @param activityTitle Activity title for the log
     * @param activityDescription Activity description for the log
     * @param activityType Activity type constant
     * @param transactionType Transaction type constant
     * @param seatCode Optional seat code for context
     * @param zoneName Optional zone name for context
     * @param reservationId Optional reservation ID
     * @return true if rule was successfully applied, false otherwise
     */
    @Transactional
    public boolean applyReputationRule(
            UUID userId,
            String ruleCode,
            String activityTitle,
            String activityDescription,
            String activityType,
            String transactionType,
            String seatCode,
            String zoneName,
            UUID reservationId) {
        
        // Get the reputation rule from database
        Optional<ReputationRuleEntity> ruleOpt = reputationRuleRepository
            .findByRuleCodeAndIsActive(ruleCode, true);
        
        if (ruleOpt.isEmpty()) {
            System.err.println("Rule not found or not active: " + ruleCode);
            return false;
        }
        
        ReputationRuleEntity rule = ruleOpt.get();
        
        // Get user
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            System.err.println("User not found: " + userId);
            return false;
        }
        
        User user = userOpt.get();
        
        // Update user reputation score
        int currentScore = user.getReputationScore() != null ? user.getReputationScore() : 100;
        int newScore = Math.max(0, currentScore + rule.getPoints()); // Points can be negative (penalty) or positive (reward)
        user.setReputationScore(newScore);
        userRepository.save(user);
        
        System.out.println(String.format(
            "Applied rule '%s' to user %s: %d -> %d (change: %d)",
            ruleCode, userId, currentScore, newScore, rule.getPoints()));
        
        // Log activity
        ActivityLogEntity activityLog = ActivityLogEntity.builder()
            .userId(userId)
            .activityType(activityType)
            .title(activityTitle)
            .description(activityDescription)
            .seatCode(seatCode)
            .zoneName(zoneName)
            .reservationId(reservationId)
            .build();
        
        ActivityLogEntity savedActivity = activityLogRepository.save(activityLog);
        
        // Create point transaction
        PointTransactionEntity transaction = PointTransactionEntity.builder()
            .userId(userId)
            .points(rule.getPoints())
            .transactionType(transactionType)
            .title(activityTitle)
            .description(activityDescription)
            .balanceAfter(newScore)
            .activityLogId(savedActivity.getId())
            .rule(rule)
            .build();
        
        pointTransactionRepository.save(transaction);
        
        return true;
    }

    /**
     * Simplified method for applying NO_SHOW penalty
     */
    @Transactional
    public boolean applyNoShowPenalty(UUID userId, String seatCode, String zoneName, UUID reservationId) {
        String description = String.format(
            "Bạn đã đặt ghế %s tại %s nhưng không check-in trong vòng 15 phút theo quy định.",
            seatCode, zoneName);
        
        return applyReputationRule(
            userId,
            "NO_SHOW",
            "Phạt: Không check-in đúng giờ",
            description,
            ActivityLogEntity.TYPE_LATE_CHECKIN_PENALTY,
            PointTransactionEntity.TYPE_LATE_CHECKIN_PENALTY,
            seatCode,
            zoneName,
            reservationId
        );
    }

    /**
     * Apply LATE_CHECKOUT penalty
     */
    @Transactional
    public boolean applyLateCheckoutPenalty(UUID userId, String seatCode, String zoneName, UUID reservationId) {
        String description = String.format(
            "Bạn đã trả ghế %s tại %s muộn hơn thời gian quy định.",
            seatCode, zoneName);
        
        return applyReputationRule(
            userId,
            "LATE_CHECKOUT",
            "Phạt: Trả chỗ muộn",
            description,
            ActivityLogEntity.TYPE_LATE_CHECKIN_PENALTY,
            PointTransactionEntity.TYPE_CHECK_OUT_LATE_PENALTY,
            seatCode,
            zoneName,
            reservationId
        );
    }

    /**
     * Apply CHECK_IN_BONUS reward
     */
    @Transactional
    public boolean applyCheckInBonus(UUID userId, String seatCode, String zoneName, UUID reservationId) {
        String description = String.format(
            "Bạn đã check-in đúng giờ tại ghế %s, khu %s.",
            seatCode, zoneName);
        
        return applyReputationRule(
            userId,
            "CHECK_IN_BONUS",
            "Thưởng: Check-in đúng giờ",
            description,
            ActivityLogEntity.TYPE_NFC_CONFIRM,
            PointTransactionEntity.TYPE_REWARD,
            seatCode,
            zoneName,
            reservationId
        );
    }

    /**
     * Get user's current reputation score
     */
    public int getUserReputationScore(UUID userId) {
        return userRepository.findById(userId)
            .map(user -> user.getReputationScore() != null ? user.getReputationScore() : 100)
            .orElse(0);
    }
}
