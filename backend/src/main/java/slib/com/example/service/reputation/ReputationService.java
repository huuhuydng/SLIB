package slib.com.example.service.reputation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import slib.com.example.entity.activity.ActivityLogEntity;
import slib.com.example.entity.activity.PointTransactionEntity;
import slib.com.example.entity.reputation.ReputationRuleEntity;
import slib.com.example.entity.users.StudentProfile;
import slib.com.example.entity.users.User;
import slib.com.example.exception.BadRequestException;
import slib.com.example.repository.users.StudentProfileRepository;
import slib.com.example.repository.activity.ActivityLogRepository;
import slib.com.example.repository.activity.PointTransactionRepository;
import slib.com.example.repository.reputation.ReputationRuleRepository;
import slib.com.example.repository.users.UserRepository;
import slib.com.example.entity.notification.NotificationEntity.NotificationType;
import slib.com.example.service.notification.PushNotificationService;

import java.util.Optional;
import java.util.UUID;
import java.time.ZonedDateTime;

/**
 * Service for managing user reputation system.
 * Handles applying penalties and rewards based on reputation rules.
 * 
 * Điểm uy tín được lưu trong bảng student_profiles.reputation_score.
 */
@Service
@Slf4j
public class ReputationService {
    private static final int NO_SHOW_SECOND_OFFENSE_POINTS = 15;
    private static final int NO_SHOW_REPEAT_POINTS = 20;

    private final StudentProfileRepository studentProfileRepository;
    private final ReputationRuleRepository reputationRuleRepository;
    private final ActivityLogRepository activityLogRepository;
    private final PointTransactionRepository pointTransactionRepository;
    private final UserRepository userRepository;
    private final PushNotificationService pushNotificationService;

    public ReputationService(
            StudentProfileRepository studentProfileRepository,
            ReputationRuleRepository reputationRuleRepository,
            ActivityLogRepository activityLogRepository,
            PointTransactionRepository pointTransactionRepository,
            UserRepository userRepository,
            PushNotificationService pushNotificationService) {
        this.studentProfileRepository = studentProfileRepository;
        this.reputationRuleRepository = reputationRuleRepository;
        this.activityLogRepository = activityLogRepository;
        this.pointTransactionRepository = pointTransactionRepository;
        this.userRepository = userRepository;
        this.pushNotificationService = pushNotificationService;
    }

    /**
     * Apply a reputation rule to a user.
     * This method dynamically applies any active rule from the database.
     * 
     * @param userId              User ID to apply the rule to
     * @param ruleCode            Rule code (e.g., "NO_SHOW", "LATE_CHECKOUT", etc.)
     * @param activityTitle       Activity title for the log
     * @param activityDescription Activity description for the log
     * @param activityType        Activity type constant
     * @param transactionType     Transaction type constant
     * @param seatCode            Optional seat code for context
     * @param zoneName            Optional zone name for context
     * @param reservationId       Optional reservation ID
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
            log.warn("Rule not found or not active: {}", ruleCode);
            return false;
        }

        ReputationRuleEntity rule = ruleOpt.get();
        int appliedPoints = resolveAppliedPoints(userId, rule);
        String effectiveDescription = appendEscalationNote(activityDescription, rule.getPoints(), appliedPoints);

        // Get student profile (reputation score lives here)
        Optional<StudentProfile> profileOpt = studentProfileRepository.findByUserId(userId);
        if (profileOpt.isEmpty()) {
            log.warn("StudentProfile not found for user: {}", userId);
            return false;
        }

        StudentProfile profile = profileOpt.get();

        // Update reputation score
        int currentScore = profile.getReputationScore() != null ? profile.getReputationScore() : 100;
        int newScore = Math.min(200, Math.max(0, currentScore + appliedPoints));
        profile.setReputationScore(newScore);
        studentProfileRepository.save(profile);

        log.info("Applied rule '{}' to user {}: {} -> {} (change: {})",
                ruleCode, userId, currentScore, newScore, appliedPoints);

        // Log activity
        ActivityLogEntity activityLog = ActivityLogEntity.builder()
                .userId(userId)
                .activityType(activityType)
                .title(activityTitle)
                .description(effectiveDescription)
                .seatCode(seatCode)
                .zoneName(zoneName)
                .reservationId(reservationId)
                .build();

        ActivityLogEntity savedActivity = activityLogRepository.save(activityLog);

        // Create point transaction
        PointTransactionEntity transaction = PointTransactionEntity.builder()
                .userId(userId)
                .points(appliedPoints)
                .transactionType(transactionType)
                .title(activityTitle)
                .description(effectiveDescription)
                .balanceAfter(newScore)
                .activityLogId(savedActivity.getId())
                .rule(rule)
                .build();

        PointTransactionEntity savedTransaction = pointTransactionRepository.save(transaction);
        sendReputationNotification(userId, appliedPoints, newScore, effectiveDescription, savedTransaction.getId());

        return true;
    }

    public int resolveAppliedPoints(UUID userId, ReputationRuleEntity rule) {
        if (rule == null || rule.getPoints() == null || rule.getPoints() >= 0) {
            return rule != null && rule.getPoints() != null ? rule.getPoints() : 0;
        }

        int basePenalty = Math.abs(rule.getPoints());
        String ruleCode = rule.getRuleCode();
        ZonedDateTime now = ZonedDateTime.now();
        long sameRuleCount30Days = pointTransactionRepository.countPenaltyByUserAndRuleCodeSince(
                userId,
                ruleCode,
                now.minusDays(30));

        return switch (ruleCode) {
            case "NO_SHOW" -> {
                long sameRuleCount7Days = pointTransactionRepository.countPenaltyByUserAndRuleCodeSince(
                        userId,
                        ruleCode,
                        now.minusDays(7));
                if (sameRuleCount30Days >= 2) {
                    yield -NO_SHOW_REPEAT_POINTS;
                }
                if (sameRuleCount7Days >= 1) {
                    yield -NO_SHOW_SECOND_OFFENSE_POINTS;
                }
                yield -basePenalty;
            }
            case "NOISE_VIOLATION", "UNAUTHORIZED_SEAT", "LEFT_BELONGINGS" -> {
                if (sameRuleCount30Days >= 2) {
                    yield -(basePenalty * 2);
                }
                if (sameRuleCount30Days >= 1) {
                    yield -increaseByHalf(basePenalty);
                }
                yield -basePenalty;
            }
            case "FOOD_DRINK", "FEET_ON_SEAT", "SLEEPING", "OTHER_VIOLATION", "LATE_CHECKOUT" -> {
                if (sameRuleCount30Days >= 2) {
                    yield -increaseByHalf(basePenalty);
                }
                yield -basePenalty;
            }
            default -> -basePenalty;
        };
    }

    private String appendEscalationNote(String activityDescription, int basePoints, int appliedPoints) {
        if (basePoints >= 0 || appliedPoints >= 0 || Math.abs(appliedPoints) <= Math.abs(basePoints)) {
            return activityDescription;
        }

        return activityDescription + " Áp dụng mức phạt tăng dần do tái phạm: trừ "
                + Math.abs(appliedPoints) + " điểm uy tín.";
    }

    private int increaseByHalf(int basePenalty) {
        return (int) Math.ceil(basePenalty * 1.5);
    }

    private void sendReputationNotification(UUID userId, int pointsChanged, int currentScore, String reason,
            UUID referenceId) {
        try {
            String title = pointsChanged >= 0 ? "Điểm uy tín đã tăng" : "Điểm uy tín đã giảm";
            String body = pointsChanged >= 0
                    ? String.format("Bạn được cộng %d điểm. %s Điểm hiện tại: %d.", pointsChanged, reason, currentScore)
                    : String.format("Bạn bị trừ %d điểm. %s Điểm hiện tại: %d.", Math.abs(pointsChanged), reason,
                            currentScore);

            pushNotificationService.sendToUser(
                    userId,
                    title,
                    body,
                    NotificationType.REPUTATION,
                    referenceId);
        } catch (Exception e) {
            log.warn("Failed to send reputation notification for user {}", userId, e);
        }
    }

    @Transactional
    public StudentProfile applyManualAdjustment(UUID userId, int points, String reason, User performedBy) {
        if (performedBy == null) {
            throw new BadRequestException("Không xác định được quản trị viên thực hiện điều chỉnh");
        }
        if (points == 0) {
            throw new BadRequestException("Số điểm điều chỉnh phải khác 0");
        }
        if (Math.abs(points) > 20) {
            throw new BadRequestException("Chỉ được điều chỉnh tối đa 20 điểm mỗi lần");
        }

        String normalizedReason = reason != null ? reason.trim() : "";
        if (normalizedReason.isBlank()) {
            throw new BadRequestException("Lý do điều chỉnh điểm là bắt buộc");
        }

        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy người dùng cần điều chỉnh điểm"));

        if (targetUser.getRole() == null || !targetUser.getRole().isPatron()) {
            throw new BadRequestException("Chỉ được điều chỉnh điểm uy tín cho sinh viên hoặc giáo viên");
        }

        StudentProfile profile = studentProfileRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultProfile(targetUser));

        int currentScore = profile.getReputationScore() != null ? profile.getReputationScore() : 100;
        int newScore = Math.min(200, Math.max(0, currentScore + points));
        profile.setReputationScore(newScore);
        studentProfileRepository.save(profile);

        String actionLabel = points > 0 ? "cộng" : "trừ";
        String title = points > 0 ? "Điều chỉnh tăng điểm uy tín" : "Điều chỉnh giảm điểm uy tín";
        String description = String.format(
                "Quản trị viên %s đã %s %d điểm uy tín. Lý do: %s. Điểm trước: %d, điểm sau: %d.",
                performedBy.getFullName() != null ? performedBy.getFullName() : performedBy.getEmail(),
                actionLabel,
                Math.abs(points),
                normalizedReason,
                currentScore,
                newScore);

        ActivityLogEntity activityLog = activityLogRepository.save(ActivityLogEntity.builder()
                .userId(userId)
                .activityType(ActivityLogEntity.TYPE_MANUAL_REPUTATION_ADJUSTMENT)
                .title(title)
                .description(description)
                .build());

        PointTransactionEntity transaction = pointTransactionRepository.save(PointTransactionEntity.builder()
                .userId(userId)
                .points(points)
                .transactionType(PointTransactionEntity.TYPE_MANUAL_ADJUSTMENT)
                .title(title)
                .description(description)
                .balanceAfter(newScore)
                .activityLogId(activityLog.getId())
                .build());

        sendReputationNotification(
                userId,
                points,
                newScore,
                "Lý do điều chỉnh: " + normalizedReason,
                transaction.getId());

        log.info("Admin {} adjusted reputation for user {}: {} -> {} (delta: {})",
                performedBy.getId(), userId, currentScore, newScore, points);

        return profile;
    }

    private StudentProfile createDefaultProfile(User user) {
        StudentProfile profile = StudentProfile.builder()
                .userId(user.getId())
                .user(user)
                .reputationScore(100)
                .totalStudyHours(0.0)
                .violationCount(0)
                .build();
        return studentProfileRepository.save(profile);
    }

    /**
     * Phạt NO_SHOW: đặt chỗ nhưng không quét NFC trong thời gian quy định.
     */
    @Transactional
    public boolean applyNoShowPenalty(UUID userId, String seatCode, String zoneName, UUID reservationId) {
        String description = String.format(
                "Bạn đã đặt ghế %s tại %s nhưng không quét NFC trong vòng thời gian quy định.",
                seatCode, zoneName);

        return applyReputationRule(
                userId,
                "NO_SHOW",
                "Phạt: Không quét NFC đúng giờ",
                description,
                ActivityLogEntity.TYPE_NO_SHOW,
                PointTransactionEntity.TYPE_NO_SHOW_PENALTY,
                seatCode,
                zoneName,
                reservationId);
    }

    /**
     * Phạt LATE_CHECKOUT: trả chỗ muộn hơn thời gian quy định.
     */
    @Transactional
    public boolean applyLateCheckoutPenalty(UUID userId, String seatCode, String zoneName, UUID reservationId) {
        String description = (seatCode != null && !seatCode.isBlank())
                ? String.format("Bạn đã không check-out đúng giờ tại ghế %s%s.",
                        seatCode,
                        zoneName != null && !zoneName.isBlank() ? " ở " + zoneName : "")
                : "Bạn chưa check-out khỏi thư viện trước khi hệ thống tự động check-out cuối ngày.";
        String title = (seatCode != null && !seatCode.isBlank())
                ? "Phạt: Không check-out đúng giờ"
                : "Phạt: Chưa check-out trong ngày";

        return applyReputationRule(
                userId,
                "LATE_CHECKOUT",
                title,
                description,
                ActivityLogEntity.TYPE_LATE_CHECKOUT_PENALTY,
                PointTransactionEntity.TYPE_CHECK_OUT_LATE_PENALTY,
                seatCode,
                zoneName,
                reservationId);
    }

    /**
     * Thưởng CHECK_IN_BONUS: check-in đúng giờ bằng NFC.
     */
    @Transactional
    public boolean applyCheckInBonus(UUID userId, String seatCode, String zoneName, UUID reservationId) {
        String description = String.format(
                "Bạn đã quét NFC đúng giờ tại ghế %s, khu %s.",
                seatCode, zoneName);

        return applyReputationRule(
                userId,
                "CHECK_IN_BONUS",
                "Thưởng: Quét NFC đúng giờ",
                description,
                ActivityLogEntity.TYPE_NFC_CONFIRM,
                PointTransactionEntity.TYPE_REWARD,
                seatCode,
                zoneName,
                reservationId);
    }

    /**
     * Thưởng WEEKLY_PERFECT: không có vi phạm trong tuần.
     */
    @Transactional
    public boolean applyWeeklyPerfectBonus(UUID userId) {
        return applyReputationRule(
                userId,
                "WEEKLY_PERFECT",
                "Thưởng: Tuần hoàn hảo",
                "Bạn không có vi phạm nào trong tuần. Tuyệt vời!",
                ActivityLogEntity.TYPE_WEEKLY_BONUS,
                PointTransactionEntity.TYPE_WEEKLY_BONUS,
                null,
                null,
                null);
    }

    /**
     * Get user's current reputation score from student_profiles.
     */
    public int getUserReputationScore(UUID userId) {
        return studentProfileRepository.findByUserId(userId)
                .map(profile -> profile.getReputationScore() != null ? profile.getReputationScore() : 100)
                .orElse(100);
    }
}
