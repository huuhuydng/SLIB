package slib.com.example.service.feedback;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import slib.com.example.dto.feedback.CreateSeatStatusReportRequest;
import slib.com.example.dto.feedback.SeatStatusReportResponse;
import slib.com.example.entity.feedback.SeatStatusReportEntity;
import slib.com.example.entity.feedback.SeatStatusReportEntity.IssueType;
import slib.com.example.entity.feedback.SeatStatusReportEntity.ReportStatus;
import slib.com.example.entity.users.User;
import slib.com.example.entity.zone_config.SeatEntity;
import slib.com.example.exception.BadRequestException;
import slib.com.example.exception.ResourceNotFoundException;
import slib.com.example.repository.zone_config.SeatRepository;
import slib.com.example.repository.feedback.SeatStatusReportRepository;
import slib.com.example.repository.users.UserRepository;
import slib.com.example.service.chat.CloudinaryService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import slib.com.example.service.notification.LibrarianNotificationService;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SeatStatusReportService {

    private final SeatStatusReportRepository seatStatusReportRepository;
    private final UserRepository userRepository;
    private final SeatRepository seatRepository;
    private final CloudinaryService cloudinaryService;
    private final LibrarianNotificationService librarianNotificationService;

    @Transactional
    public SeatStatusReportResponse createReport(UUID reporterId, CreateSeatStatusReportRequest request, MultipartFile image) {
        User reporter = userRepository.findById(reporterId)
                .orElseThrow(() -> new ResourceNotFoundException("Reporter", "id", reporterId));

        SeatEntity seat = seatRepository.findById(request.getSeatId())
                .orElseThrow(() -> new ResourceNotFoundException("Seat", request.getSeatId()));

        IssueType issueType;
        try {
            issueType = IssueType.valueOf(request.getIssueType().trim().toUpperCase());
        } catch (Exception e) {
            throw new BadRequestException("Invalid issueType: " + request.getIssueType());
        }

        String imageUrl = null;
        if (image != null && !image.isEmpty()) {
            imageUrl = cloudinaryService.uploadImageChat(image);
        }

        SeatStatusReportEntity report = SeatStatusReportEntity.builder()
                .user(reporter)
                .seat(seat)
                .issueType(issueType)
                .description(request.getDescription())
                .imageUrl(imageUrl)
                .status(ReportStatus.PENDING)
                .build();

        SeatStatusReportEntity saved = seatStatusReportRepository.save(report);
        log.info("[SeatStatusReport] Created report {} for seat {}", saved.getId(), seat.getSeatId());
        librarianNotificationService.broadcastPendingCounts("SEAT_STATUS_REPORT", "CREATED");
        return SeatStatusReportResponse.fromEntity(saved);
    }

    public List<SeatStatusReportResponse> getMyReports(UUID reporterId) {
        return seatStatusReportRepository.findByUser_IdOrderByCreatedAtDesc(reporterId).stream()
                .map(SeatStatusReportResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public List<SeatStatusReportResponse> getAll(String status) {
        List<SeatStatusReportEntity> reports;
        if (status == null || status.isBlank()) {
            reports = seatStatusReportRepository.findAllByOrderByCreatedAtDesc();
        } else {
            try {
                ReportStatus reportStatus = ReportStatus.valueOf(status.trim().toUpperCase());
                reports = seatStatusReportRepository.findByStatusOrderByCreatedAtDesc(reportStatus);
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid status: " + status);
            }
        }

        return reports.stream().map(SeatStatusReportResponse::fromEntity).collect(Collectors.toList());
    }

    public SeatStatusReportResponse getById(UUID reportId) {
        SeatStatusReportEntity report = seatStatusReportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Seat status report", "id", reportId));
        return SeatStatusReportResponse.fromEntity(report);
    }

    @Transactional
    public SeatStatusReportResponse verifyReport(UUID reportId, UUID librarianId) {
        SeatStatusReportEntity report = findPendingReport(reportId);
        User librarian = userRepository.findById(librarianId)
                .orElseThrow(() -> new ResourceNotFoundException("Librarian", "id", librarianId));

        report.setStatus(ReportStatus.VERIFIED);
        report.setVerifiedBy(librarian);
        report.setVerifiedAt(LocalDateTime.now());
        SeatStatusReportEntity saved = seatStatusReportRepository.save(report);
        librarianNotificationService.broadcastPendingCounts("SEAT_STATUS_REPORT", "VERIFIED");
        return SeatStatusReportResponse.fromEntity(saved);
    }

    @Transactional
    public SeatStatusReportResponse rejectReport(UUID reportId, UUID librarianId) {
        SeatStatusReportEntity report = findPendingReport(reportId);
        User librarian = userRepository.findById(librarianId)
                .orElseThrow(() -> new ResourceNotFoundException("Librarian", "id", librarianId));

        report.setStatus(ReportStatus.REJECTED);
        report.setVerifiedBy(librarian);
        report.setVerifiedAt(LocalDateTime.now());
        SeatStatusReportEntity saved = seatStatusReportRepository.save(report);
        librarianNotificationService.broadcastPendingCounts("SEAT_STATUS_REPORT", "REJECTED");
        return SeatStatusReportResponse.fromEntity(saved);
    }

    @Transactional
    public SeatStatusReportResponse resolveReport(UUID reportId, UUID librarianId) {
        SeatStatusReportEntity report = seatStatusReportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Seat status report", "id", reportId));
        User librarian = userRepository.findById(librarianId)
                .orElseThrow(() -> new ResourceNotFoundException("Librarian", "id", librarianId));

        if (report.getStatus() == ReportStatus.REJECTED) {
            throw new BadRequestException("Rejected report cannot be resolved");
        }

        if (report.getVerifiedBy() == null) {
            report.setVerifiedBy(librarian);
        }
        if (report.getVerifiedAt() == null) {
            report.setVerifiedAt(LocalDateTime.now());
        }
        report.setStatus(ReportStatus.RESOLVED);
        report.setResolvedAt(LocalDateTime.now());
        SeatStatusReportEntity saved = seatStatusReportRepository.save(report);
        librarianNotificationService.broadcastPendingCounts("SEAT_STATUS_REPORT", "RESOLVED");
        return SeatStatusReportResponse.fromEntity(saved);
    }

    @Transactional
    public void deleteBatch(List<UUID> ids) {
        seatStatusReportRepository.deleteAllById(ids);
        log.info("[SeatStatusReport] Deleted {} reports", ids.size());
        librarianNotificationService.broadcastPendingCounts("SEAT_STATUS_REPORT", "DELETED");
    }

    private SeatStatusReportEntity findPendingReport(UUID reportId) {
        SeatStatusReportEntity report = seatStatusReportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Seat status report", "id", reportId));
        if (report.getStatus() != ReportStatus.PENDING) {
            throw new BadRequestException("Report has already been processed");
        }
        return report;
    }
}
