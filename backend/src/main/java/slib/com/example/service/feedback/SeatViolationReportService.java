package slib.com.example.service.feedback;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import slib.com.example.dto.feedback.CreateViolationReportRequest;
import slib.com.example.dto.feedback.ViolationReportResponse;
import slib.com.example.entity.activity.ActivityLogEntity;
import slib.com.example.entity.activity.PointTransactionEntity;
import slib.com.example.entity.booking.ReservationEntity;
import slib.com.example.entity.feedback.SeatViolationReportEntity;
import slib.com.example.entity.feedback.SeatViolationReportEntity.ReportStatus;
import slib.com.example.entity.feedback.SeatViolationReportEntity.ViolationType;
import slib.com.example.entity.notification.NotificationEntity.NotificationType;
import slib.com.example.entity.reputation.ReputationRuleEntity;
import slib.com.example.entity.users.User;
import slib.com.example.entity.users.StudentProfile;
import slib.com.example.entity.zone_config.SeatEntity;
import slib.com.example.exception.BadRequestException;
import slib.com.example.repository.activity.ActivityLogRepository;
import slib.com.example.repository.activity.PointTransactionRepository;
import slib.com.example.repository.booking.ReservationRepository;
import slib.com.example.repository.feedback.SeatViolationReportRepository;
import slib.com.example.repository.notification.NotificationRepository;
import slib.com.example.repository.reputation.ReputationRuleRepository;
import slib.com.example.repository.users.StudentProfileRepository;
import slib.com.example.repository.users.UserRepository;
import slib.com.example.repository.zone_config.SeatRepository;
import slib.com.example.service.chat.CloudinaryService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import slib.com.example.entity.complaint.ComplaintEntity;
import slib.com.example.repository.complaint.ComplaintRepository;
import slib.com.example.service.notification.PushNotificationService;
import slib.com.example.service.notification.LibrarianNotificationService;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SeatViolationReportService {

    private final SeatViolationReportRepository violationReportRepository;
    private final UserRepository userRepository;
    private final SeatRepository seatRepository;
    private final ReservationRepository reservationRepository;
    private final ReputationRuleRepository reputationRuleRepository;
    private final ActivityLogRepository activityLogRepository;
    private final PointTransactionRepository pointTransactionRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final CloudinaryService cloudinaryService;
    private final ComplaintRepository complaintRepository;
    private final PushNotificationService pushNotificationService;
    private final LibrarianNotificationService librarianNotificationService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Sinh vien tao bao cao vi pham
     */
    @Transactional
    public ViolationReportResponse createReport(UUID reporterId, CreateViolationReportRequest request,
            List<MultipartFile> images) {
        User reporter = userRepository.findById(reporterId)
                .orElseThrow(() -> new RuntimeException("Reporter not found: " + reporterId));
        ReservationEntity reporterReservation = findActiveConfirmedReservationByUser(reporterId)
                .orElseThrow(() -> new BadRequestException(
                        "Bạn chỉ có thể báo cáo vi phạm khi đang check-in trong thư viện."));

        SeatEntity seat = seatRepository.findById(request.getSeatId())
                .orElseThrow(() -> new RuntimeException("Seat not found: " + request.getSeatId()));

        ViolationType violationType;
        try {
            violationType = ViolationType.valueOf(request.getViolationType());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid violation type: " + request.getViolationType());
        }

        ReservationEntity violatorReservation = findActiveConfirmedReservationBySeat(seat.getSeatId())
                .orElseThrow(() -> new BadRequestException("Ghế này hiện không có người check-in để báo cáo."));
        if (reporterReservation.getSeat() != null
                && reporterReservation.getSeat().getSeatId().equals(seat.getSeatId())) {
            throw new BadRequestException("Ghế của bạn chỉ có thể gửi báo cáo tình trạng, không dùng báo cáo vi phạm.");
        }

        User violator = violatorReservation.getUser();
        UUID reservationId = violatorReservation.getReservationId();

        // Upload anh bang chung
        String evidenceUrl = null;
        if (images != null && !images.isEmpty()) {
            try {
                evidenceUrl = cloudinaryService.uploadImageChat(images.get(0));
                log.info("[ViolationReport] Uploaded evidence image: {}", evidenceUrl);
            } catch (Exception e) {
                log.error("[ViolationReport] Failed to upload evidence: {}", e.getMessage());
            }
        }

        SeatViolationReportEntity report = SeatViolationReportEntity.builder()
                .reporter(reporter)
                .violator(violator)
                .seat(seat)
                .reservationId(reservationId)
                .violationType(violationType)
                .description(request.getDescription())
                .evidenceUrl(evidenceUrl)
                .status(ReportStatus.PENDING)
                .build();

        SeatViolationReportEntity saved = violationReportRepository.save(report);
        log.info("[ViolationReport] Created report {} by reporter {} for seat {}",
                saved.getId(), reporterId, request.getSeatId());

        // Gui thong bao cho violator khi bi bao cao
        if (violator != null) {
            sendNewReportNotification(violator.getId(), saved);
        }

        broadcastDashboardUpdate("VIOLATION_UPDATE", "CREATED");
        librarianNotificationService.broadcastPendingCounts("VIOLATION", "CREATED");
        return ViolationReportResponse.fromEntity(saved);
    }

    /**
     * Sinh vien xem bao cao cua minh
     */
    public List<ViolationReportResponse> getMyReports(UUID reporterId) {
        return violationReportRepository.findByReporter_IdOrderByCreatedAtDesc(reporterId)
                .stream()
                .map(ViolationReportResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Sinh viên xem danh sách vi phạm bị gán cho mình (kèm trạng thái kháng cáo)
     */
    public List<ViolationReportResponse> getViolationsAgainstMe(UUID violatorId) {
        List<ViolationReportResponse> responses = violationReportRepository
                .findByViolator_IdOrderByCreatedAtDesc(violatorId)
                .stream()
                .map(ViolationReportResponse::fromEntity)
                .collect(Collectors.toList());

        for (ViolationReportResponse resp : responses) {
            List<ComplaintEntity> complaints = complaintRepository
                    .findByViolationReportIdOrderByCreatedAtDesc(resp.getId());
            if (!complaints.isEmpty()) {
                ComplaintEntity latest = complaints.get(0);
                resp.setAppealStatus(latest.getStatus().name());
                resp.setAppealResolutionNote(latest.getResolutionNote());
            }
        }

        return responses;
    }

    /**
     * Thu thu xem tat ca bao cao
     */
    public List<ViolationReportResponse> getAll() {
        return violationReportRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(ViolationReportResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Thu thu filter theo status
     */
    public List<ViolationReportResponse> getByStatus(ReportStatus status) {
        return violationReportRepository.findByStatusOrderByCreatedAtDesc(status)
                .stream()
                .map(ViolationReportResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Thu thu xac minh bao cao va tru diem
     */
    @Transactional
    public ViolationReportResponse verifyReport(UUID reportId, UUID librarianId) {
        SeatViolationReportEntity report = violationReportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Report not found: " + reportId));

        if (report.getStatus() != ReportStatus.PENDING) {
            throw new RuntimeException("Report has already been processed");
        }

        User librarian = userRepository.findById(librarianId)
                .orElseThrow(() -> new RuntimeException("Librarian not found: " + librarianId));

        // Tim reputation rule de lay diem phat
        String ruleCode = mapViolationTypeToRuleCode(report.getViolationType());
        ReputationRuleEntity rule = reputationRuleRepository.findByRuleCode(ruleCode)
                .orElse(null);

        int pointsToDeduct = rule != null ? Math.abs(rule.getPoints()) : 10;

        // Neu violator null, thu tim lai tu reservationId hoac reservation active
        User violator = report.getViolator();
        if (violator == null) {
            log.warn("[ViolationReport] Violator is null for report {}, attempting to find from reservation", reportId);

            // Thu tim tu reservationId da luu trong report
            if (report.getReservationId() != null) {
                violator = reservationRepository.findById(report.getReservationId())
                        .map(r -> r.getUser())
                        .orElse(null);
                if (violator != null) {
                    log.info("[ViolationReport] Found violator {} from reservationId {}",
                            violator.getId(), report.getReservationId());
                }
            }

            // Neu van null, thu tim tu reservation active tai ghe
            if (violator == null) {
                violator = findActiveConfirmedReservationBySeat(report.getSeat().getSeatId())
                        .map(ReservationEntity::getUser)
                        .orElse(null);
                if (violator != null) {
                    log.info("[ViolationReport] Found violator {} from active reservation at seat {}",
                            violator.getId(), report.getSeat().getSeatId());
                }
            }

            // Cap nhat violator vao report
            if (violator != null) {
                report.setViolator(violator);
            }
        }

        report.setStatus(ReportStatus.VERIFIED);
        report.setVerifiedBy(librarian);
        report.setVerifiedAt(LocalDateTime.now());
        report.setPointDeducted(pointsToDeduct);

        SeatViolationReportEntity saved = violationReportRepository.save(report);

        // Tru diem cho violator
        if (violator != null) {
            deductPoints(violator.getId(), pointsToDeduct, report.getViolationType(), rule, report);
            // Gui thong bao cho violator
            sendViolationNotification(violator.getId(), report, pointsToDeduct);
            log.info("[ViolationReport] Deducted {} points from violator {}", pointsToDeduct, violator.getId());
        } else {
            log.warn("[ViolationReport] No violator found for report {}, points not deducted", reportId);
        }

        // Gui thong bao cho reporter
        sendReporterNotification(report, true);

        log.info("[ViolationReport] Verified report {} by librarian {}, deducted {} points",
                reportId, librarianId, pointsToDeduct);

        broadcastDashboardUpdate("VIOLATION_UPDATE", "VERIFIED");
        librarianNotificationService.broadcastPendingCounts("VIOLATION", "VERIFIED");
        return ViolationReportResponse.fromEntity(saved);
    }

    /**
     * Thu thu tu choi bao cao
     */
    @Transactional
    public ViolationReportResponse rejectReport(UUID reportId, UUID librarianId) {
        SeatViolationReportEntity report = violationReportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Report not found: " + reportId));

        if (report.getStatus() != ReportStatus.PENDING) {
            throw new RuntimeException("Report has already been processed");
        }

        User librarian = userRepository.findById(librarianId)
                .orElseThrow(() -> new RuntimeException("Librarian not found: " + librarianId));

        report.setStatus(ReportStatus.REJECTED);
        report.setVerifiedBy(librarian);
        report.setVerifiedAt(LocalDateTime.now());

        SeatViolationReportEntity saved = violationReportRepository.save(report);

        // Gui thong bao cho reporter
        sendReporterNotification(report, false);

        log.info("[ViolationReport] Rejected report {} by librarian {}", reportId, librarianId);

        broadcastDashboardUpdate("VIOLATION_UPDATE", "REJECTED");
        librarianNotificationService.broadcastPendingCounts("VIOLATION", "REJECTED");
        return ViolationReportResponse.fromEntity(saved);
    }

    /**
     * Dem bao cao theo status
     */
    public long countByStatus(ReportStatus status) {
        return violationReportRepository.countByStatus(status);
    }

    /**
     * Xoa nhieu bao cao cung luc
     */
    @Transactional
    public void deleteBatch(List<UUID> ids) {
        violationReportRepository.deleteAllById(ids);
        log.info("[ViolationReport] Deleted {} reports", ids.size());
        broadcastDashboardUpdate("VIOLATION_UPDATE", "DELETED");
        librarianNotificationService.broadcastPendingCounts("VIOLATION", "DELETED");
    }

    // ===== PRIVATE HELPERS =====

    /**
     * Tim nguoi dang ngoi tai ghe (co reservation active)
     */
    private User findActiveViolator(Integer seatId) {
        return findActiveConfirmedReservationBySeat(seatId)
                .map(ReservationEntity::getUser)
                .orElse(null);
    }

    private java.util.Optional<ReservationEntity> findActiveConfirmedReservationBySeat(Integer seatId) {
        LocalDateTime now = LocalDateTime.now();
        List<ReservationEntity> activeReservations = reservationRepository.findOverlappingReservations(
                seatId, now, now.plusMinutes(1));
        return activeReservations.stream()
                .filter(r -> "CONFIRMED".equalsIgnoreCase(r.getStatus()))
                .findFirst();
    }

    private UUID findActiveReservationId(Integer seatId) {
        return findActiveConfirmedReservationBySeat(seatId)
                .map(ReservationEntity::getReservationId)
                .orElse(null);
    }

    private java.util.Optional<ReservationEntity> findActiveConfirmedReservationByUser(UUID userId) {
        LocalDateTime now = LocalDateTime.now();
        return reservationRepository.findByUserId(userId).stream()
                .filter(r -> "CONFIRMED".equalsIgnoreCase(r.getStatus()))
                .filter(r -> r.getStartTime() != null && r.getEndTime() != null)
                .filter(r -> !r.getStartTime().isAfter(now) && !r.getEndTime().isBefore(now))
                .findFirst();
    }

    /**
     * Map ViolationType sang rule_code trong reputation_rules
     */
    private String mapViolationTypeToRuleCode(ViolationType type) {
        return switch (type) {
            case NOISE -> "NOISE_VIOLATION";
            case UNAUTHORIZED_USE -> "UNAUTHORIZED_SEAT";
            case FEET_ON_SEAT -> "FEET_ON_SEAT";
            case FOOD_DRINK -> "FOOD_DRINK";
            case SLEEPING -> "SLEEPING";
            case LEFT_BELONGINGS -> "LEFT_BELONGINGS";
            case OTHER -> "OTHER_VIOLATION"; // vi phạm khác
        };
    }

    /**
     * Tru diem cho violator: cap nhat student_profile, ghi activity_log, roi ghi
     * point_transaction
     */
    private void deductPoints(UUID userId, int points, ViolationType violationType, ReputationRuleEntity rule,
            SeatViolationReportEntity report) {
        try {
            // 1. Cap nhat reputation_score va violation_count trong student_profiles
            StudentProfile profile = studentProfileRepository.findByUserId(userId).orElse(null);
            int currentScore = 100; // default
            if (profile != null) {
                currentScore = profile.getReputationScore();
                profile.setReputationScore(Math.max(0, currentScore - points));
                profile.setViolationCount(profile.getViolationCount() + 1);
                studentProfileRepository.save(profile);
                log.info("[ViolationReport] Updated student profile: reputation {} -> {}, violations: {}",
                        currentScore, profile.getReputationScore(), profile.getViolationCount());
            } else {
                log.warn("[ViolationReport] Student profile not found for user {}", userId);
            }

            int balanceAfter = profile != null ? profile.getReputationScore() : (currentScore - points);

            // 2. Ghi activity log de sinh vien co the thay
            String seatCode = report.getSeat() != null ? report.getSeat().getSeatCode() : null;
            String zoneName = report.getSeat() != null && report.getSeat().getZone() != null
                    ? report.getSeat().getZone().getZoneName()
                    : null;

            ActivityLogEntity activityLog = ActivityLogEntity.builder()
                    .userId(userId)
                    .activityType(ActivityLogEntity.TYPE_VIOLATION)
                    .title("Bạn đã bị báo cáo vi phạm: " + getViolationLabel(violationType))
                    .description("Bạn đã bị báo cáo vi phạm tại thư viện. Trừ " + points + " điểm uy tín. Còn lại: "
                            + balanceAfter + " điểm.")
                    .reservationId(report.getReservationId())
                    .seatCode(seatCode)
                    .zoneName(zoneName)
                    .build();

            ActivityLogEntity savedLog = activityLogRepository.save(activityLog);
            log.info("[ViolationReport] Created activity log {} for user {}", savedLog.getId(), userId);

            // 3. Ghi point transaction voi activity_log_id va balance_after
            PointTransactionEntity transaction = PointTransactionEntity.builder()
                    .userId(userId)
                    .points(-points)
                    .transactionType(PointTransactionEntity.TYPE_PENALTY)
                    .title("Vi phạm: " + getViolationLabel(violationType))
                    .description("Bị báo cáo vi phạm tại thư viện")
                    .balanceAfter(balanceAfter)
                    .activityLogId(savedLog.getId())
                    .rule(rule)
                    .build();

            pointTransactionRepository.save(transaction);
            log.info("[ViolationReport] Deducted {} points from user {}, reputation: {} -> {}",
                    points, userId, currentScore, balanceAfter);
        } catch (Exception e) {
            log.error("[ViolationReport] Failed to deduct points: {}", e.getMessage(), e);
        }
    }

    private String getViolationLabel(ViolationType type) {
        return switch (type) {
            case UNAUTHORIZED_USE -> "Sử dụng ghế không đúng";
            case LEFT_BELONGINGS -> "Để đồ giữ chỗ";
            case NOISE -> "Gây ồn ào";
            case FEET_ON_SEAT -> "Gác chân lên ghế/bàn";
            case FOOD_DRINK -> "Ăn uống trong thư viện";
            case SLEEPING -> "Ngủ tại chỗ ngồi";
            case OTHER -> "Vi phạm khác";
        };
    }

    private String getSeatDisplay(SeatViolationReportEntity report) {
        if (report.getSeat() == null) {
            return "ghế không xác định";
        }

        String seatCode = report.getSeat().getSeatCode();
        String zoneName = report.getSeat().getZone() != null
                ? report.getSeat().getZone().getZoneName()
                : null;

        if (zoneName != null && !zoneName.isBlank()) {
            return "ghế " + seatCode + " - " + zoneName;
        }

        return "ghế " + seatCode;
    }

    /**
     * Gui thong bao cho violator khi bi tru diem (xac minh)
     */
    private void sendViolationNotification(UUID violatorId, SeatViolationReportEntity report, int points) {
        try {
            final String title = "Thủ thư đã xác nhận vi phạm";
            final String body = "Thủ thư vừa xác nhận bạn vi phạm lỗi "
                    + getViolationLabel(report.getViolationType())
                    + " tại " + getSeatDisplay(report)
                    + ". Điểm uy tín của bạn đã bị trừ " + points + " điểm.";

            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            try {
                                pushNotificationService.sendToUser(
                                        violatorId, title, body,
                                        NotificationType.VIOLATION,
                                        report.getId());
                            } catch (Exception e) {
                                log.error("[ViolationReport] Failed to send violation notification: {}",
                                        e.getMessage());
                            }
                        }
                    });
        } catch (Exception e) {
            log.error("[ViolationReport] Failed to prepare violation notification: {}", e.getMessage());
        }
    }

    /**
     * Gui thong bao cho reporter khi bao cao duoc xu ly
     */
    private void sendReporterNotification(SeatViolationReportEntity report, boolean accepted) {
        try {
            UUID reporterId = report.getReporter().getId();
            String seatDisplay = getSeatDisplay(report);
            String violationLabel = getViolationLabel(report.getViolationType());
            String violatorLabel = report.getViolator() != null
                    ? "sinh viên " + report.getViolator().getFullName()
                    : "sinh viên vi phạm";
            final String title = accepted
                    ? "Báo cáo vi phạm đã được xác minh"
                    : "Báo cáo vi phạm bị từ chối";
            final String body = accepted
                    ? "Đơn tố cáo vi phạm của bạn về lỗi "
                            + violationLabel
                            + " tại "
                            + seatDisplay
                            + " đã được thủ thư xác minh. "
                            + violatorLabel
                            + " đã bị xử phạt"
                            + (report.getPointDeducted() != null && report.getPointDeducted() > 0
                                    ? " và trừ " + report.getPointDeducted() + " điểm uy tín."
                                    : ".")
                    : "Đơn tố cáo vi phạm của bạn về lỗi "
                            + violationLabel
                            + " tại "
                            + seatDisplay
                            + " đã bị thủ thư từ chối sau khi kiểm tra.";

            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            try {
                                pushNotificationService.sendToUser(
                                        reporterId, title, body,
                                        NotificationType.VIOLATION_REPORT,
                                        report.getId(),
                                        "VIOLATION_REPORT",
                                        "PROCESSING");
                            } catch (Exception e) {
                                log.error("[ViolationReport] Failed to send reporter notification: {}", e.getMessage());
                            }
                        }
                    });
        } catch (Exception e) {
            log.error("[ViolationReport] Failed to prepare reporter notification: {}", e.getMessage());
        }
    }

    /**
     * Gui thong bao cho violator khi co don bao cao moi
     */
    private void sendNewReportNotification(UUID violatorId, SeatViolationReportEntity report) {
        try {
            final String title = "Bạn bị báo cáo vi phạm";
            final String body = "Bạn bị báo cáo vi phạm: "
                    + getViolationLabel(report.getViolationType())
                    + " tại ghế " + report.getSeat().getSeatCode()
                    + ". Thủ thư sẽ xem xét và xử lý.";

            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            try {
                                pushNotificationService.sendToUser(
                                        violatorId, title, body,
                                        NotificationType.VIOLATION_REPORT,
                                        report.getId(),
                                        "VIOLATION_REPORT",
                                        "PROCESSING");
                            } catch (Exception e) {
                                log.error("[ViolationReport] Failed to send new report notification: {}",
                                        e.getMessage());
                            }
                        }
                    });
        } catch (Exception e) {
            log.error("[ViolationReport] Failed to prepare new report notification: {}", e.getMessage());
        }
    }

    private void broadcastDashboardUpdate(String type, String action) {
        try {
            messagingTemplate.convertAndSend("/topic/dashboard",
                    java.util.Map.of("type", type, "action", action, "timestamp", java.time.Instant.now().toString()));
        } catch (Exception e) {
            log.warn("Failed to broadcast dashboard update: {}", e.getMessage());
        }
    }
}
