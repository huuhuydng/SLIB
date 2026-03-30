package slib.com.example.service.complaint;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import slib.com.example.entity.activity.ActivityLogEntity;
import slib.com.example.entity.activity.PointTransactionEntity;
import slib.com.example.service.notification.LibrarianNotificationService;
import slib.com.example.dto.complaint.ComplaintDTO;
import slib.com.example.entity.complaint.ComplaintEntity;
import slib.com.example.entity.complaint.ComplaintEntity.ComplaintStatus;
import slib.com.example.entity.feedback.SeatViolationReportEntity;
import slib.com.example.entity.feedback.SeatViolationReportEntity.ReportStatus;
import slib.com.example.entity.notification.NotificationEntity.NotificationType;
import slib.com.example.entity.users.StudentProfile;
import slib.com.example.entity.users.User;
import slib.com.example.exception.BadRequestException;
import slib.com.example.repository.activity.ActivityLogRepository;
import slib.com.example.repository.activity.PointTransactionRepository;
import slib.com.example.repository.complaint.ComplaintRepository;
import slib.com.example.repository.feedback.SeatViolationReportRepository;
import slib.com.example.repository.users.StudentProfileRepository;
import slib.com.example.repository.users.UserRepository;
import slib.com.example.service.notification.PushNotificationService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ComplaintService {

        private final ComplaintRepository complaintRepository;
        private final UserRepository userRepository;
        private final PointTransactionRepository pointTransactionRepository;
        private final SeatViolationReportRepository seatViolationReportRepository;
        private final StudentProfileRepository studentProfileRepository;
        private final ActivityLogRepository activityLogRepository;
        private final LibrarianNotificationService librarianNotificationService;
        private final PushNotificationService pushNotificationService;
        private final SimpMessagingTemplate messagingTemplate;

        /**
         * Lấy tất cả khiếu nại (cho thủ thư)
         */
        @Transactional(readOnly = true)
        public List<ComplaintDTO> getAll() {
                return complaintRepository.findAllByOrderByCreatedAtDesc()
                                .stream()
                                .map(ComplaintDTO::fromEntity)
                                .collect(Collectors.toList());
        }

        /**
         * Lấy khiếu nại theo trạng thái
         */
        @Transactional(readOnly = true)
        public List<ComplaintDTO> getByStatus(ComplaintStatus status) {
                return complaintRepository.findByStatusOrderByCreatedAtDesc(status)
                                .stream()
                                .map(ComplaintDTO::fromEntity)
                                .collect(Collectors.toList());
        }

        /**
         * Lấy khiếu nại của sinh viên
         */
        @Transactional(readOnly = true)
        public List<ComplaintDTO> getByStudent(UUID studentId) {
                return complaintRepository.findByUserIdOrderByCreatedAtDesc(studentId)
                                .stream()
                                .map(ComplaintDTO::fromEntity)
                                .collect(Collectors.toList());
        }

        /**
         * Sinh viên tạo khiếu nại
         */
        @Transactional
        public ComplaintDTO create(UUID studentId, String subject, String content, String evidenceUrl,
                        UUID pointTransactionId) {
                return create(studentId, subject, content, evidenceUrl, pointTransactionId, null);
        }

        /**
         * Sinh viên tạo khiếu nại
         */
        @Transactional
        public ComplaintDTO create(UUID studentId, String subject, String content, String evidenceUrl,
                        UUID pointTransactionId, UUID violationReportId) {
                User student = userRepository.findById(studentId)
                                .orElseThrow(() -> new RuntimeException("Không tìm thấy sinh viên"));

                validateComplaintTarget(studentId, pointTransactionId, violationReportId);

                ComplaintEntity complaint = ComplaintEntity.builder()
                                .user(student)
                                .subject(subject)
                                .content(content)
                                .evidenceUrl(evidenceUrl)
                                .pointTransactionId(pointTransactionId)
                                .violationReportId(violationReportId)
                                .status(ComplaintStatus.PENDING)
                                .build();

                ComplaintEntity saved = complaintRepository.save(complaint);
                log.info("[Complaint] Sinh viên {} đã gửi khiếu nại: {}", student.getFullName(), subject);
                broadcastDashboardUpdate("COMPLAINT_UPDATE", "CREATED");
                librarianNotificationService.broadcastPendingCounts("COMPLAINT", "CREATED");
                return ComplaintDTO.fromEntity(saved);
        }

        /**
         * Thủ thư chấp nhận khiếu nại (hoàn điểm)
         */
        @Transactional
        public ComplaintDTO accept(UUID complaintId, UUID librarianId, String note) {
                ComplaintEntity complaint = complaintRepository.findById(complaintId)
                                .orElseThrow(() -> new RuntimeException("Không tìm thấy khiếu nại"));
                ensurePendingComplaint(complaint);

                User librarian = userRepository.findById(librarianId)
                                .orElseThrow(() -> new RuntimeException("Không tìm thấy thủ thư"));

                if (complaint.getPointTransactionId() != null) {
                        PointTransactionEntity transaction = pointTransactionRepository.findById(complaint.getPointTransactionId())
                                        .orElseThrow(() -> new RuntimeException("Không tìm thấy giao dịch điểm bị khiếu nại"));
                        refundPenaltyTransaction(complaint.getUser(), transaction, complaint);
                }

                if (complaint.getViolationReportId() != null) {
                        SeatViolationReportEntity report = seatViolationReportRepository.findById(complaint.getViolationReportId())
                                        .orElseThrow(() -> new RuntimeException("Không tìm thấy báo cáo vi phạm bị khiếu nại"));
                        reverseViolationReportPenalty(complaint.getUser(), report, complaint);
                }

                complaint.setStatus(ComplaintStatus.ACCEPTED);
                complaint.setResolvedBy(librarian);
                complaint.setResolvedAt(LocalDateTime.now());
                complaint.setResolutionNote(note);

                ComplaintEntity saved = complaintRepository.save(complaint);
                log.info("[Complaint] Khiếu nại {} được CHẤP NHẬN bởi {}", complaintId, librarian.getFullName());
                sendComplaintStatusNotification(saved, true);

                broadcastDashboardUpdate("COMPLAINT_UPDATE", "ACCEPTED");
                librarianNotificationService.broadcastPendingCounts("COMPLAINT", "ACCEPTED");
                return ComplaintDTO.fromEntity(saved);
        }

        /**
         * Thủ thư từ chối khiếu nại
         */
        @Transactional
        public ComplaintDTO deny(UUID complaintId, UUID librarianId, String note) {
                ComplaintEntity complaint = complaintRepository.findById(complaintId)
                                .orElseThrow(() -> new RuntimeException("Không tìm thấy khiếu nại"));
                ensurePendingComplaint(complaint);

                User librarian = userRepository.findById(librarianId)
                                .orElseThrow(() -> new RuntimeException("Không tìm thấy thủ thư"));

                complaint.setStatus(ComplaintStatus.DENIED);
                complaint.setResolvedBy(librarian);
                complaint.setResolvedAt(LocalDateTime.now());
                complaint.setResolutionNote(note);

                ComplaintEntity saved = complaintRepository.save(complaint);
                log.info("[Complaint] Khiếu nại {} bị TỪ CHỐI bởi {}", complaintId, librarian.getFullName());
                sendComplaintStatusNotification(saved, false);
                broadcastDashboardUpdate("COMPLAINT_UPDATE", "DENIED");
                librarianNotificationService.broadcastPendingCounts("COMPLAINT", "DENIED");
                return ComplaintDTO.fromEntity(saved);
        }

        /**
         * Đếm khiếu nại theo trạng thái
         */
        public long countByStatus(ComplaintStatus status) {
                return complaintRepository.countByStatus(status);
        }

        /**
         * Xoá nhiều khiếu nại cùng lúc
         */
        @Transactional
        public void deleteBatch(List<UUID> ids) {
                complaintRepository.deleteAllById(ids);
                log.info("[Complaint] Deleted {} complaints", ids.size());
                broadcastDashboardUpdate("COMPLAINT_UPDATE", "DELETED");
                librarianNotificationService.broadcastPendingCounts("COMPLAINT", "DELETED");
        }

        private void broadcastDashboardUpdate(String type, String action) {
                try {
                        messagingTemplate.convertAndSend("/topic/dashboard",
                                        java.util.Map.of("type", type, "action", action, "timestamp",
                                                        java.time.Instant.now().toString()));
                } catch (Exception e) {
                        log.warn("Failed to broadcast dashboard update: {}", e.getMessage());
                }
        }

        private void validateComplaintTarget(UUID studentId, UUID pointTransactionId, UUID violationReportId) {
                boolean hasPointTransaction = pointTransactionId != null;
                boolean hasViolationReport = violationReportId != null;

                if (hasPointTransaction == hasViolationReport) {
                        throw new BadRequestException("Khiếu nại phải gắn với đúng một giao dịch điểm hoặc một báo cáo vi phạm.");
                }

                if (hasPointTransaction) {
                        PointTransactionEntity transaction = pointTransactionRepository.findById(pointTransactionId)
                                        .orElseThrow(() -> new RuntimeException("Không tìm thấy giao dịch điểm"));

                        if (!studentId.equals(transaction.getUserId())) {
                                throw new BadRequestException("Bạn không thể khiếu nại giao dịch điểm của người khác.");
                        }
                        if (transaction.getPoints() >= 0) {
                                throw new BadRequestException("Chỉ có thể khiếu nại giao dịch bị trừ điểm.");
                        }
                        if (complaintRepository.existsByPointTransactionIdAndStatus(pointTransactionId, ComplaintStatus.PENDING)) {
                                throw new BadRequestException("Giao dịch điểm này đang có một khiếu nại chờ xử lý.");
                        }
                        return;
                }

                SeatViolationReportEntity report = seatViolationReportRepository.findById(violationReportId)
                                .orElseThrow(() -> new RuntimeException("Không tìm thấy báo cáo vi phạm"));

                if (report.getViolator() == null || !studentId.equals(report.getViolator().getId())) {
                        throw new BadRequestException("Bạn không thể khiếu nại báo cáo vi phạm không thuộc về mình.");
                }
                if (report.getStatus() == ReportStatus.PENDING || report.getStatus() == ReportStatus.REJECTED) {
                        throw new BadRequestException("Báo cáo vi phạm này chưa ở trạng thái có thể khiếu nại.");
                }
                if (complaintRepository.existsByViolationReportIdAndStatus(violationReportId, ComplaintStatus.PENDING)) {
                        throw new BadRequestException("Báo cáo vi phạm này đang có một khiếu nại chờ xử lý.");
                }
        }

        private void ensurePendingComplaint(ComplaintEntity complaint) {
                if (complaint.getStatus() != ComplaintStatus.PENDING) {
                        throw new BadRequestException("Khiếu nại này đã được xử lý trước đó.");
                }
        }

        private void refundPenaltyTransaction(User student, PointTransactionEntity originalTransaction,
                        ComplaintEntity complaint) {
                int pointsToRefund = Math.abs(originalTransaction.getPoints());
                if (pointsToRefund == 0) {
                        throw new BadRequestException("Giao dịch điểm này không còn điểm để hoàn lại.");
                }

                StudentProfile profile = studentProfileRepository.findByUserId(student.getId())
                                .orElseThrow(() -> new RuntimeException("Không tìm thấy hồ sơ sinh viên"));
                int currentScore = profile.getReputationScore() != null ? profile.getReputationScore() : 100;
                int newScore = Math.min(200, currentScore + pointsToRefund);
                profile.setReputationScore(newScore);
                studentProfileRepository.save(profile);

                ActivityLogEntity activityLog = activityLogRepository.save(ActivityLogEntity.builder()
                                .userId(student.getId())
                                .activityType(ActivityLogEntity.TYPE_APPEAL_REFUND)
                                .title("Hoàn điểm do khiếu nại được chấp nhận")
                                .description("Đã hoàn " + pointsToRefund
                                                + " điểm uy tín từ giao dịch bị khiếu nại.")
                                .build());

                pointTransactionRepository.save(PointTransactionEntity.builder()
                                .userId(student.getId())
                                .points(pointsToRefund)
                                .transactionType(PointTransactionEntity.TYPE_APPEAL_REFUND)
                                .title("Hoàn điểm khiếu nại")
                                .description("Hoàn điểm do khiếu nại được chấp nhận")
                                .balanceAfter(newScore)
                                .activityLogId(activityLog.getId())
                                .build());

                try {
                        pushNotificationService.sendToUser(
                                        student.getId(),
                                        "Điểm uy tín đã được hoàn lại",
                                        "Khiếu nại của bạn đã được chấp nhận. Hệ thống đã hoàn " + pointsToRefund
                                                        + " điểm uy tín.",
                                        NotificationType.REPUTATION,
                                        complaint.getId());
                } catch (Exception e) {
                        log.warn("Failed to send refund notification for complaint {}", complaint.getId(), e);
                }
        }

        private void reverseViolationReportPenalty(User student, SeatViolationReportEntity report,
                        ComplaintEntity complaint) {
                if (report.getViolator() == null || !student.getId().equals(report.getViolator().getId())) {
                        throw new BadRequestException("Báo cáo vi phạm này không thuộc về sinh viên đang khiếu nại.");
                }

                int pointsToRefund = report.getPointDeducted() != null ? report.getPointDeducted() : 0;
                if (pointsToRefund <= 0) {
                        throw new BadRequestException("Báo cáo vi phạm này không còn điểm để hoàn lại.");
                }

                StudentProfile profile = studentProfileRepository.findByUserId(student.getId())
                                .orElseThrow(() -> new RuntimeException("Không tìm thấy hồ sơ sinh viên"));
                int currentScore = profile.getReputationScore() != null ? profile.getReputationScore() : 100;
                int newScore = Math.min(200, currentScore + pointsToRefund);
                profile.setReputationScore(newScore);
                int currentViolationCount = profile.getViolationCount() != null ? profile.getViolationCount() : 0;
                profile.setViolationCount(Math.max(0, currentViolationCount - 1));
                studentProfileRepository.save(profile);

                String seatCode = report.getSeat() != null ? report.getSeat().getSeatCode() : null;
                String zoneName = report.getSeat() != null && report.getSeat().getZone() != null
                                ? report.getSeat().getZone().getZoneName()
                                : null;

                ActivityLogEntity activityLog = activityLogRepository.save(ActivityLogEntity.builder()
                                .userId(student.getId())
                                .activityType(ActivityLogEntity.TYPE_APPEAL_REFUND)
                                .title("Hoàn điểm do khiếu nại vi phạm được chấp nhận")
                                .description("Đã hoàn " + pointsToRefund
                                                + " điểm uy tín cho báo cáo vi phạm tại ghế "
                                                + (seatCode != null ? seatCode : "không xác định") + ".")
                                .reservationId(report.getReservationId())
                                .seatCode(seatCode)
                                .zoneName(zoneName)
                                .build());

                pointTransactionRepository.save(PointTransactionEntity.builder()
                                .userId(student.getId())
                                .points(pointsToRefund)
                                .transactionType(PointTransactionEntity.TYPE_APPEAL_REFUND)
                                .title("Hoàn điểm khiếu nại vi phạm")
                                .description("Hoàn điểm do báo cáo vi phạm bị đảo kết quả sau khiếu nại")
                                .balanceAfter(newScore)
                                .activityLogId(activityLog.getId())
                                .build());

                try {
                        pushNotificationService.sendToUser(
                                        student.getId(),
                                        "Điểm uy tín đã được hoàn lại",
                                        "Khiếu nại vi phạm của bạn đã được chấp nhận. Hệ thống đã hoàn "
                                                        + pointsToRefund
                                                        + " điểm uy tín.",
                                        NotificationType.REPUTATION,
                                        complaint.getId());
                } catch (Exception e) {
                        log.warn("Failed to send violation refund notification for complaint {}", complaint.getId(),
                                        e);
                }

                report.setStatus(ReportStatus.RESOLVED);
                report.setPointDeducted(0);
                seatViolationReportRepository.save(report);
        }

        private void sendComplaintStatusNotification(ComplaintEntity complaint, boolean accepted) {
                try {
                        String title = accepted ? "Khiếu nại đã được chấp nhận" : "Khiếu nại đã bị từ chối";
                        String body = accepted
                                        ? "Khiếu nại của bạn đã được thủ thư chấp nhận. Mở ứng dụng để xem chi tiết xử lý."
                                        : "Khiếu nại của bạn đã bị từ chối. Mở ứng dụng để xem phản hồi của thủ thư.";

                        pushNotificationService.sendToUser(
                                        complaint.getUser().getId(),
                                        title,
                                        body,
                                        NotificationType.COMPLAINT,
                                        complaint.getId(),
                                        "COMPLAINT",
                                        "PROCESSING");
                } catch (Exception e) {
                        log.warn("Failed to send complaint notification for {}", complaint.getId(), e);
                }
        }
}
