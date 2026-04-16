package slib.com.example.service.booking;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import slib.com.example.dto.booking.BookingRestrictionStatus;
import slib.com.example.entity.library.LibrarySetting;
import slib.com.example.entity.users.StudentProfile;
import slib.com.example.repository.activity.PointTransactionRepository;
import slib.com.example.repository.booking.ReservationRepository;
import slib.com.example.repository.users.StudentProfileRepository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BookingPolicyService {

    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final Set<String> ACTIVE_RESERVATION_STATUSES = Set.of("PROCESSING", "BOOKED", "CONFIRMED");
    private static final int REPUTATION_ACTIVE_BOOKING_LIMIT_THRESHOLD = 70;
    private static final int REPUTATION_ADVANCE_BOOKING_THRESHOLD = 50;
    private static final int REPUTATION_TEMP_BLOCK_THRESHOLD = 30;
    private static final int REPUTATION_TEMP_BLOCK_DAYS = 3;
    private static final int REPUTATION_SEVERE_BLOCK_THRESHOLD = 20;
    private static final int REPUTATION_SEVERE_BLOCK_DAYS = 7;
    private static final int LOW_REPUTATION_ADVANCE_HOURS = 24;

    private final StudentProfileRepository studentProfileRepository;
    private final ReservationRepository reservationRepository;
    private final PointTransactionRepository pointTransactionRepository;

    public int resolveCurrentReputation(UUID userId) {
        StudentProfile profile = studentProfileRepository.findByUserId(userId).orElse(null);
        return (profile != null && profile.getReputationScore() != null)
                ? profile.getReputationScore()
                : 100;
    }

    public void enforceBookingPolicies(UUID userId, LocalDateTime startTime, LibrarySetting settings, int currentReputation) {
        BookingRestrictionStatus status = evaluateStatus(userId, startTime, settings, currentReputation);
        if (Boolean.FALSE.equals(status.getAllowedNow()) && status.getRestrictionReason() != null) {
            throw new RuntimeException(status.getRestrictionReason());
        }
    }

    public BookingRestrictionStatus getCurrentRestrictionStatus(UUID userId, int currentReputation, LibrarySetting settings) {
        return evaluateStatus(userId, null, settings, currentReputation);
    }

    private BookingRestrictionStatus evaluateStatus(
            UUID userId,
            LocalDateTime requestedStartTime,
            LibrarySetting settings,
            int currentReputation) {
        int minReputation = settings.getMinReputation() != null ? settings.getMinReputation() : 0;
        if (minReputation > 0 && currentReputation < minReputation) {
            return BookingRestrictionStatus.builder()
                    .allowedNow(false)
                    .restrictionReason(
                            "Điểm uy tín của bạn (" + currentReputation + ") thấp hơn mức tối thiểu (" + minReputation
                                    + ") để đặt chỗ.")
                    .build();
        }

        LocalDateTime now = LocalDateTime.now(VIETNAM_ZONE);
        var latestPenalty = pointTransactionRepository
                .findTopByUserIdAndPointsLessThanOrderByCreatedAtDesc(userId, 0);

        if (latestPenalty.isPresent() && latestPenalty.get().getCreatedAt() != null) {
            LocalDateTime severeBlockedUntil = latestPenalty.get().getCreatedAt()
                    .withZoneSameInstant(VIETNAM_ZONE)
                    .toLocalDateTime()
                    .plusDays(REPUTATION_SEVERE_BLOCK_DAYS);
            if (currentReputation < REPUTATION_SEVERE_BLOCK_THRESHOLD && severeBlockedUntil.isAfter(now)) {
                return buildBlockedStatus(
                        "Điểm uy tín của bạn đang quá thấp. Bạn tạm thời bị khóa đặt chỗ kể từ lần vi phạm gần nhất.",
                        now,
                        severeBlockedUntil);
            }

            LocalDateTime blockedUntil = latestPenalty.get().getCreatedAt()
                    .withZoneSameInstant(VIETNAM_ZONE)
                    .toLocalDateTime()
                    .plusDays(REPUTATION_TEMP_BLOCK_DAYS);
            if (currentReputation < REPUTATION_TEMP_BLOCK_THRESHOLD && blockedUntil.isAfter(now)) {
                return buildBlockedStatus(
                        "Điểm uy tín của bạn đang thấp. Bạn tạm thời không thể đặt chỗ kể từ lần vi phạm gần nhất.",
                        now,
                        blockedUntil);
            }
        }

        if (currentReputation < REPUTATION_ACTIVE_BOOKING_LIMIT_THRESHOLD) {
            long activeBookings = reservationRepository.countByUser_IdAndStatusInAndEndTimeAfter(
                    userId,
                    ACTIVE_RESERVATION_STATUSES.stream().toList(),
                    now);
            if (activeBookings >= 1) {
                return BookingRestrictionStatus.builder()
                        .allowedNow(false)
                        .restrictionReason("Điểm uy tín hiện tại chỉ cho phép bạn giữ tối đa 1 đặt chỗ đang hoạt động.")
                        .policyHint(buildPolicyHint(currentReputation))
                        .build();
            }
        }

        int maxActiveBookings = settings.getMaxActiveBookings() != null ? settings.getMaxActiveBookings() : 2;
        long activeBookings = reservationRepository.countByUser_IdAndStatusInAndEndTimeAfter(
                userId,
                ACTIVE_RESERVATION_STATUSES.stream().toList(),
                now);
        if (activeBookings >= maxActiveBookings) {
            return BookingRestrictionStatus.builder()
                    .allowedNow(false)
                    .restrictionReason(
                            "Bạn đang giữ tối đa " + maxActiveBookings
                                    + " booking sắp tới. Vui lòng hủy hoặc hoàn tất bớt booking trước khi đặt thêm.")
                    .policyHint("Giới hạn này dùng để tránh giữ chỗ liên tiếp trên quá nhiều ngày.")
                    .build();
        }

        if (requestedStartTime != null
                && currentReputation < REPUTATION_ADVANCE_BOOKING_THRESHOLD
                && requestedStartTime.isAfter(now.plusHours(LOW_REPUTATION_ADVANCE_HOURS))) {
            return BookingRestrictionStatus.builder()
                    .allowedNow(false)
                    .restrictionReason(
                            "Điểm uy tín hiện tại chỉ cho phép bạn đặt chỗ trước tối đa "
                                    + LOW_REPUTATION_ADVANCE_HOURS + " giờ.")
                    .policyHint(buildPolicyHint(currentReputation))
                    .build();
        }

        return BookingRestrictionStatus.builder()
                .allowedNow(true)
                .policyHint(buildPolicyHint(currentReputation))
                .build();
    }

    private BookingRestrictionStatus buildBlockedStatus(String reason, LocalDateTime now, LocalDateTime blockedUntil) {
        long totalHours = Math.max(1, Math.ceilDiv(Duration.between(now, blockedUntil).toMinutes(), 60));
        return BookingRestrictionStatus.builder()
                .allowedNow(false)
                .restrictionReason(reason)
                .blockedUntil(blockedUntil)
                .remainingDays((int) (totalHours / 24))
                .remainingHours((int) (totalHours % 24))
                .policyHint("Hãy giữ đúng quy định để điểm uy tín phục hồi và mở lại quyền đặt chỗ.")
                .build();
    }

    private String buildPolicyHint(int currentReputation) {
        List<String> hints = new ArrayList<>();
        if (currentReputation < REPUTATION_ACTIVE_BOOKING_LIMIT_THRESHOLD) {
            hints.add("Bạn chỉ được giữ tối đa 1 đặt chỗ đang hoạt động");
        }
        if (currentReputation < REPUTATION_ADVANCE_BOOKING_THRESHOLD) {
            hints.add("Bạn chỉ được đặt trước tối đa " + LOW_REPUTATION_ADVANCE_HOURS + " giờ");
        }
        if (hints.isEmpty()) {
            return null;
        }
        return String.join(" và ", hints) + ".";
    }
}
