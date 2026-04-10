package slib.com.example.service.system;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import slib.com.example.entity.booking.ReservationEntity;
import slib.com.example.entity.complaint.ComplaintEntity;
import slib.com.example.entity.feedback.FeedbackEntity;
import slib.com.example.entity.feedback.SeatStatusReportEntity;
import slib.com.example.entity.feedback.SeatViolationReportEntity;
import slib.com.example.entity.activity.ActivityLogEntity;
import slib.com.example.entity.hce.AccessLog;
import slib.com.example.entity.news.Category;
import slib.com.example.entity.news.News;
import slib.com.example.entity.news.NewBookEntity;
import slib.com.example.entity.notification.NotificationEntity;
import slib.com.example.entity.activity.PointTransactionEntity;
import slib.com.example.entity.users.Role;
import slib.com.example.entity.support.SupportRequest;
import slib.com.example.entity.support.SupportRequestStatus;
import slib.com.example.entity.system.SeedRecordEntity;
import slib.com.example.entity.users.StudentProfile;
import slib.com.example.entity.users.User;
import slib.com.example.entity.zone_config.SeatEntity;
import slib.com.example.entity.zone_config.ZoneEntity;
import slib.com.example.repository.activity.PointTransactionRepository;
import slib.com.example.repository.activity.ActivityLogRepository;
import slib.com.example.repository.booking.ReservationRepository;
import slib.com.example.repository.complaint.ComplaintRepository;
import slib.com.example.repository.feedback.FeedbackRepository;
import slib.com.example.repository.feedback.SeatStatusReportRepository;
import slib.com.example.repository.feedback.SeatViolationReportRepository;
import slib.com.example.repository.hce.AccessLogRepository;
import slib.com.example.repository.news.CategoryRepository;
import slib.com.example.repository.news.NewBookRepository;
import slib.com.example.repository.news.NewsRepository;
import slib.com.example.repository.notification.NotificationRepository;
import slib.com.example.repository.support.SupportRequestRepository;
import slib.com.example.repository.system.SeedRecordRepository;
import slib.com.example.repository.users.StudentProfileRepository;
import slib.com.example.repository.users.UserRepository;
import slib.com.example.repository.zone_config.SeatRepository;
import slib.com.example.repository.zone_config.ZoneRepository;
import slib.com.example.service.zone_config.SeatStatusSyncService;

import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.Month;
import java.util.*;

/**
 * Service tạo dữ liệu mẫu cho hệ thống SLIB.
 * Sử dụng qua Postman để seed/clear data nhanh chóng.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SeedDataService {

    private final UserRepository userRepository;
    private final ReservationRepository reservationRepository;
    private final SeatRepository seatRepository;
    private final SeatViolationReportRepository violationReportRepository;
    private final SeatStatusReportRepository seatStatusReportRepository;
    private final SupportRequestRepository supportRequestRepository;
    private final AccessLogRepository accessLogRepository;
    private final ComplaintRepository complaintRepository;
    private final FeedbackRepository feedbackRepository;
    private final ActivityLogRepository activityLogRepository;
    private final PointTransactionRepository pointTransactionRepository;
    private final NewsRepository newsRepository;
    private final NotificationRepository notificationRepository;
    private final CategoryRepository categoryRepository;
    private final NewBookRepository newBookRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final SeedRecordRepository seedRecordRepository;
    private final ZoneRepository zoneRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final SeatStatusSyncService seatStatusSyncService;

    // Legacy marker chỉ dùng để dọn dữ liệu cũ đã seed từ phiên bản trước
    private static final String LEGACY_SEED_MARKER = "[SEED]";
    private static final String SYSTEM_SHOWCASE_SCOPE = "system-showcase";
    private static final String STUDENT_JOURNEY_SCOPE_PREFIX = "student-journey:";
    private static final String DEFAULT_SEED_GATE_DEVICE_ID = "gate-main-01";
    private static final List<String> SAMPLE_DEVICE_IDS = List.of(
            "gate-main-01",
            "gate-east-02",
            "kiosk-floor-2",
            "kiosk-floor-3");
    private static final List<String> SAMPLE_AVATAR_BACKGROUNDS = List.of("0f766e", "1d4ed8", "7c3aed", "c2410c");
    private static final int MIN_SEAT_STATUS_REPORTS = 8;
    private static final List<SeatStatusReportSeedTemplate> SEAT_STATUS_REPORT_SEED_TEMPLATES = List.of(
            new SeatStatusReportSeedTemplate(
                    SeatStatusReportEntity.IssueType.BROKEN,
                    SeatStatusReportEntity.ReportStatus.PENDING,
                    "Ghế %s bị lỏng chân, sinh viên ngồi không vững và cần kiểm tra sớm",
                    "https://placehold.co/1200x800/png?text=SLIB+Broken+Seat"),
            new SeatStatusReportSeedTemplate(
                    SeatStatusReportEntity.IssueType.DIRTY,
                    SeatStatusReportEntity.ReportStatus.PENDING,
                    "Ghế %s có nhiều vết bẩn và bụi trên mặt ngồi, cần vệ sinh trước ca tiếp theo",
                    null),
            new SeatStatusReportSeedTemplate(
                    SeatStatusReportEntity.IssueType.MISSING_EQUIPMENT,
                    SeatStatusReportEntity.ReportStatus.PENDING,
                    "Vị trí %s thiếu ổ cắm điện bên cạnh, ảnh hưởng đến việc học nhóm",
                    "https://placehold.co/1200x800/png?text=SLIB+Missing+Equipment"),
            new SeatStatusReportSeedTemplate(
                    SeatStatusReportEntity.IssueType.OTHER,
                    SeatStatusReportEntity.ReportStatus.PENDING,
                    "Khu vực quanh ghế %s có tiếng kêu lớn từ bàn đi kèm khi sử dụng",
                    null),
            new SeatStatusReportSeedTemplate(
                    SeatStatusReportEntity.IssueType.BROKEN,
                    SeatStatusReportEntity.ReportStatus.VERIFIED,
                    "Ghế %s bị nứt tay vịn, thủ thư đã xác minh và chờ bộ phận kỹ thuật xử lý",
                    null),
            new SeatStatusReportSeedTemplate(
                    SeatStatusReportEntity.IssueType.DIRTY,
                    SeatStatusReportEntity.ReportStatus.VERIFIED,
                    "Ghế %s bám mực và bụi lâu ngày, đã xác minh để lên lịch vệ sinh",
                    "https://placehold.co/1200x800/png?text=SLIB+Dirty+Seat"),
            new SeatStatusReportSeedTemplate(
                    SeatStatusReportEntity.IssueType.MISSING_EQUIPMENT,
                    SeatStatusReportEntity.ReportStatus.RESOLVED,
                    "Ghế %s từng thiếu đèn đọc sách, thư viện đã bổ sung đầy đủ",
                    null),
            new SeatStatusReportSeedTemplate(
                    SeatStatusReportEntity.IssueType.OTHER,
                    SeatStatusReportEntity.ReportStatus.REJECTED,
                    "Sinh viên phản ánh ghế %s khó ngồi, nhưng kiểm tra thực tế không phát hiện bất thường",
                    null));

    /**
     * Tạo dữ liệu mẫu booking (đặt chỗ) - trải đều 7 ngày
     */
    @Transactional
    public Map<String, Object> seedBookings(int count) {
        return seedBookings(count, SYSTEM_SHOWCASE_SCOPE);
    }

    @Transactional
    public Map<String, Object> seedBookings(int count, String seedScope) {
        List<User> users = ensureStudentCohort(Math.max(8, Math.min(count, 12)), seedScope);
        List<SeatEntity> seats = getActiveSeats();

        if (users.isEmpty() || seats.isEmpty()) {
            return Map.of("status", "ERROR", "message", "Cần có users và seats trước khi seed bookings");
        }

        // Trạng thái cho booking quá khứ (đã kết thúc) - scheduler sẽ không đổi
        String[] pastStatuses = { "COMPLETED", "EXPIRED", "CANCEL" };
        // Trạng thái cho booking tương lai (chưa kết thúc) - scheduler sẽ không đổi
        String[] futureStatuses = { "BOOKED", "CONFIRMED" };

        List<UUID> createdIds = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        Random rng = new Random();

        // Đảm bảo ít nhất 1 booking cho mỗi trạng thái
        String[] guaranteedStatuses = { "PROCESSING", "BOOKED", "CONFIRMED", "CANCEL", "EXPIRED", "COMPLETED" };
        int guaranteed = Math.min(guaranteedStatuses.length, count);

        for (int i = 0; i < count; i++) {
            User user = users.get(rng.nextInt(users.size()));
            SeatEntity seat = seats.get(rng.nextInt(seats.size()));

            String status;
            LocalDateTime startTime;
            LocalDateTime endTime;

            if (i < guaranteed) {
                // Đảm bảo mỗi trạng thái xuất hiện ít nhất 1 lần
                status = guaranteedStatuses[i];
            } else {
                // Phần còn lại random
                boolean isFuture = rng.nextBoolean();
                status = isFuture
                        ? futureStatuses[rng.nextInt(futureStatuses.length)]
                        : pastStatuses[rng.nextInt(pastStatuses.length)];
            }

            // Gán thời gian phù hợp với status để scheduler không override
            switch (status) {
                case "PROCESSING":
                    // PROCESSING phải mới tạo (< 2 phút) và thời gian tương lai
                    // để scheduler không auto-cancel
                    int futureHour1 = now.getHour() + 1 + rng.nextInt(3);
                    if (futureHour1 > 22)
                        futureHour1 = 22;
                    startTime = now.toLocalDate().atTime(futureHour1, rng.nextInt(60));
                    endTime = startTime.plusHours(1 + rng.nextInt(2));
                    break;

                case "BOOKED":
                case "CONFIRMED":
                    // Phải có endTime trong tương lai để scheduler không expire
                    int futureHour2 = now.getHour() + rng.nextInt(4);
                    if (futureHour2 > 22)
                        futureHour2 = 20 + rng.nextInt(2);
                    startTime = now.toLocalDate().atTime(futureHour2, rng.nextInt(60));
                    endTime = startTime.plusHours(1 + rng.nextInt(3));
                    // Đảm bảo endTime luôn sau now
                    if (endTime.isBefore(now)) {
                        startTime = now.plusMinutes(10 + rng.nextInt(60));
                        endTime = startTime.plusHours(1 + rng.nextInt(2));
                    }
                    break;

                case "CANCEL":
                case "EXPIRED":
                case "COMPLETED":
                default:
                    // Booking quá khứ - an toàn, scheduler không đổi
                    int daysAgo = 1 + rng.nextInt(6);
                    int pastHour = 7 + rng.nextInt(13);
                    startTime = now.minusDays(daysAgo).toLocalDate().atTime(pastHour, rng.nextInt(60));
                    endTime = startTime.plusHours(1 + rng.nextInt(3));
                    break;
            }

            ReservationEntity res = ReservationEntity.builder()
                    .user(user)
                    .seat(seat)
                    .startTime(startTime)
                    .endTime(endTime)
                    .confirmedAt(("CONFIRMED".equals(status) || "COMPLETED".equals(status)) ? startTime : null)
                    .status(status)
                    .build();

            reservationRepository.save(res);
            createdIds.add(res.getReservationId());
            recordSeed("RESERVATION", res.getReservationId(), seedScope);
        }

        log.info("[SeedData] Đã tạo {} booking mẫu (đủ 5 trạng thái)", count);
        return Map.of(
                "status", "SUCCESS",
                "message", "Đã tạo " + count + " booking mẫu (đủ 5 trạng thái)",
                "count", count,
                "ids", createdIds);
    }

    /**
     * Tạo dữ liệu mẫu access logs (check-in/out) - trải đều 7 ngày
     */
    @Transactional
    public Map<String, Object> seedAccessLogs(int count) {
        return seedAccessLogs(count, SYSTEM_SHOWCASE_SCOPE);
    }

    @Transactional
    public Map<String, Object> seedAccessLogs(int count, String seedScope) {
        List<User> users = ensureStudentCohort(Math.max(8, Math.min(count, 12)), seedScope);

        if (users.isEmpty()) {
            return Map.of("status", "ERROR", "message", "Cần có users trước khi seed access logs");
        }

        Random rng = new Random();
        List<UUID> createdIds = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (int i = 0; i < count; i++) {
            User user = users.get(rng.nextInt(users.size()));

            // Trải đều 7 ngày gần nhất
            int daysAgo = rng.nextInt(7);
            int checkInHour = 7 + rng.nextInt(13); // 7h - 20h
            int durationMinutes = 30 + rng.nextInt(300); // 30 phút - 5.5 giờ

            LocalDateTime checkInTime = now.minusDays(daysAgo).toLocalDate()
                    .atTime(checkInHour, rng.nextInt(60));
            LocalDateTime checkOutTime = checkInTime.plusMinutes(durationMinutes);

            // Nếu ngày hiện tại, có thể chưa check-out
            boolean hasCheckout = daysAgo > 0 || rng.nextBoolean();

            AccessLog accessLog = AccessLog.builder()
                    .user(user)
                    .checkInTime(checkInTime)
                    .checkOutTime(hasCheckout ? checkOutTime : null)
                    .deviceId(SAMPLE_DEVICE_IDS.get(rng.nextInt(SAMPLE_DEVICE_IDS.size())))
                    .build();

            accessLogRepository.save(accessLog);
            createdIds.add(accessLog.getLogId());
            recordSeed("ACCESS_LOG", accessLog.getLogId(), seedScope);
        }

        log.info("[SeedData] Đã tạo {} access logs mẫu (7 ngày)", count);
        return Map.of(
                "status", "SUCCESS",
                "message", "Đã tạo " + count + " access logs mẫu (trải 7 ngày)",
                "count", count,
                "ids", createdIds);
    }

    /**
     * Tạo dữ liệu mẫu vi phạm
     */
    @Transactional
    public Map<String, Object> seedViolations(int count) {
        return seedViolations(count, SYSTEM_SHOWCASE_SCOPE);
    }

    @Transactional
    public Map<String, Object> seedViolations(int count, String seedScope) {
        List<User> students = ensureStudentCohort(Math.max(8, Math.min(count + 2, 12)), seedScope);
        List<User> staff = getActiveStaff();
        List<SeatEntity> seats = getActiveSeats();

        if (students.isEmpty() || seats.isEmpty()) {
            return Map.of("status", "ERROR", "message", "Cần có users và seats trước khi seed violations");
        }

        SeatViolationReportEntity.ViolationType[] types = SeatViolationReportEntity.ViolationType.values();
        SeatViolationReportEntity.ReportStatus[] reportStatuses = SeatViolationReportEntity.ReportStatus.values();
        String[] descriptions = {
                "Sinh viên để balo chiếm ghế nhưng rời khỏi khu vực hơn 20 phút",
                "Nhóm sinh viên trao đổi lớn tiếng trong khu tự học yên tĩnh",
                "Sinh viên mang đồ uống có mùi vào bàn học nhóm",
                "Sinh viên ngủ gục tại ghế quá lâu, ảnh hưởng người xung quanh",
                "Sinh viên ngồi sai ghế đã đặt của bạn khác trong cùng khung giờ",
                "Sinh viên đặt chân lên ghế bên cạnh khi thư viện đang đông"
        };
        Random rng = new Random();
        List<UUID> createdIds = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            User reporter = students.get(rng.nextInt(students.size()));
            User violator = students.get(rng.nextInt(students.size()));
            if (students.size() > 1) {
                while (Objects.equals(reporter.getId(), violator.getId())) {
                    violator = students.get(rng.nextInt(students.size()));
                }
            }
            SeatEntity seat = seats.get(rng.nextInt(seats.size()));
            SeatViolationReportEntity.ReportStatus status = reportStatuses[rng.nextInt(reportStatuses.length)];
            User verifier = (status == SeatViolationReportEntity.ReportStatus.PENDING || staff.isEmpty())
                    ? null
                    : staff.get(rng.nextInt(staff.size()));
            Integer pointDeducted = status == SeatViolationReportEntity.ReportStatus.VERIFIED ? 5 + rng.nextInt(11) : null;
            LocalDateTime verifiedAt = verifier != null ? LocalDateTime.now().minusHours(rng.nextInt(72) + 1) : null;

            SeatViolationReportEntity report = SeatViolationReportEntity.builder()
                    .reporter(reporter)
                    .violator(violator)
                    .seat(seat)
                    .violationType(types[rng.nextInt(types.length)])
                    .description(descriptions[rng.nextInt(descriptions.length)])
                    .status(status)
                    .verifiedBy(verifier)
                    .pointDeducted(pointDeducted)
                    .verifiedAt(verifiedAt)
                    .build();

            violationReportRepository.save(report);
            createdIds.add(report.getId());
            recordSeed("VIOLATION_REPORT", report.getId(), seedScope);
        }

        log.info("[SeedData] Đã tạo {} vi phạm mẫu", count);
        return Map.of(
                "status", "SUCCESS",
                "message", "Đã tạo " + count + " vi phạm mẫu",
                "count", count,
                "ids", createdIds);
    }

    /**
     * Tạo dữ liệu mẫu yêu cầu hỗ trợ
     */
    @Transactional
    public Map<String, Object> seedSupportRequests(int count) {
        return seedSupportRequests(count, SYSTEM_SHOWCASE_SCOPE);
    }

    @Transactional
    public Map<String, Object> seedSupportRequests(int count, String seedScope) {
        List<User> users = ensureStudentCohort(Math.max(8, Math.min(count + 2, 12)), seedScope);
        List<User> staff = getActiveStaff();

        if (users.isEmpty()) {
            return Map.of("status", "ERROR", "message", "Cần có users trước khi seed support requests");
        }

        String[] descriptions = {
                "WiFi khu tự học tầng 2 bị chập chờn, đăng nhập FAP rất khó ổn định",
                "Điều hoà khu B hoạt động yếu từ đầu giờ chiều, không gian khá nóng",
                "Ổ cắm điện gần bàn A3-05 lỏng, sạc laptop lúc vào lúc không",
                "Đèn bàn tại góc đọc sách phía cửa sổ không sáng từ sáng nay",
                "Ghế ngồi ở khu máy tính bị nghiêng về bên phải, cần kiểm tra lại chân ghế",
                "Máy tính công cộng số 3 treo liên tục khi mở trình duyệt",
                "Khu học nhóm tầng 3 thiếu ổ cắm cho nhóm 4 người",
                "Nhà vệ sinh tầng 1 cần được dọn dẹp trước giờ cao điểm"
        };
        SupportRequestStatus[] statuses = SupportRequestStatus.values();
        Random rng = new Random();
        List<UUID> createdIds = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            User student = users.get(rng.nextInt(users.size()));
            SupportRequestStatus status = statuses[rng.nextInt(statuses.length)];
            User resolver = status == SupportRequestStatus.PENDING || staff.isEmpty()
                    ? null
                    : staff.get(rng.nextInt(staff.size()));
            String response = switch (status) {
                case IN_PROGRESS -> "Thủ thư đã tiếp nhận và chuyển bộ phận kỹ thuật kiểm tra trong ca hiện tại.";
                case RESOLVED -> "Yêu cầu đã được xử lý xong, sinh viên có thể sử dụng lại khu vực này bình thường.";
                case REJECTED -> "Đã kiểm tra thực tế nhưng chưa ghi nhận lỗi lặp lại tại thời điểm xử lý.";
                default -> null;
            };

            SupportRequest req = SupportRequest.builder()
                    .student(student)
                    .description(descriptions[rng.nextInt(descriptions.length)])
                    .status(status)
                    .adminResponse(response)
                    .resolvedBy(resolver)
                    .resolvedAt(status == SupportRequestStatus.PENDING ? null : LocalDateTime.now().minusHours(rng.nextInt(48) + 1))
                    .build();

            supportRequestRepository.save(req);
            createdIds.add(req.getId());
            recordSeed("SUPPORT_REQUEST", req.getId(), seedScope);
        }

        log.info("[SeedData] Đã tạo {} yêu cầu hỗ trợ mẫu", count);
        return Map.of(
                "status", "SUCCESS",
                "message", "Đã tạo " + count + " yêu cầu hỗ trợ mẫu",
                "count", count,
                "ids", createdIds);
    }

    /**
     * Tạo dữ liệu mẫu khiếu nại
     */
    @Transactional
    public Map<String, Object> seedComplaints(int count) {
        return seedComplaints(count, SYSTEM_SHOWCASE_SCOPE);
    }

    @Transactional
    public Map<String, Object> seedComplaints(int count, String seedScope) {
        List<User> users = ensureStudentCohort(Math.max(8, Math.min(count + 2, 12)), seedScope);
        List<User> staff = getActiveStaff();

        if (users.isEmpty()) {
            return Map.of("status", "ERROR", "message", "Cần có users trước khi seed complaints");
        }

        String[] subjects = {
                "Đề nghị xem lại việc trừ điểm uy tín sau buổi tối hôm qua",
                "Không đồng ý với báo cáo vi phạm do nhầm người",
                "Yêu cầu kiểm tra lại lịch sử điểm uy tín ngày 15/02",
                "Mong thư viện xem lại phản ánh ăn uống tại khu học nhóm",
                "Tôi không có mặt tại ghế vào thời điểm bị lập báo cáo"
        };
        String[] contents = {
                "Tôi đã check-in đúng giờ nhưng ứng dụng vẫn ghi nhận muộn 5 phút, dẫn đến bị trừ điểm. Nhờ thư viện đối soát lại giúp tôi.",
                "Tôi chỉ mang nước lọc có nắp và không ăn uống tại bàn. Báo cáo hiện tại không phản ánh đúng tình huống.",
                "Ngày 15/02 tôi không vào thư viện nhưng hệ thống vẫn ghi nhận bị trừ 10 điểm. Mong được kiểm tra lại nhật ký vào ra.",
                "Bạn báo cáo có thể nhầm người vì tôi ngồi bàn bên cạnh, không tham gia nói chuyện lớn tiếng.",
                "Tôi rời ghế khoảng 5 phút để lấy tài liệu, không phải bỏ đồ chiếm chỗ trong thời gian dài."
        };
        ComplaintEntity.ComplaintStatus[] statuses = ComplaintEntity.ComplaintStatus.values();
        Random rng = new Random();
        List<UUID> createdIds = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            User user = users.get(rng.nextInt(users.size()));
            ComplaintEntity.ComplaintStatus status = statuses[rng.nextInt(statuses.length)];
            User resolver = status == ComplaintEntity.ComplaintStatus.PENDING || staff.isEmpty()
                    ? null
                    : staff.get(rng.nextInt(staff.size()));

            ComplaintEntity complaint = ComplaintEntity.builder()
                    .user(user)
                    .subject(subjects[rng.nextInt(subjects.length)])
                    .content(contents[rng.nextInt(contents.length)])
                    .status(status)
                    .resolvedBy(resolver)
                    .resolvedAt(status == ComplaintEntity.ComplaintStatus.PENDING ? null
                            : LocalDateTime.now().minusHours(rng.nextInt(72) + 2))
                    .resolutionNote(status == ComplaintEntity.ComplaintStatus.PENDING
                            ? null
                            : status == ComplaintEntity.ComplaintStatus.ACCEPTED
                                    ? "Thư viện đã đối soát và điều chỉnh lại điểm uy tín cho sinh viên."
                                    : "Thư viện đã kiểm tra camera và nhật ký hệ thống, báo cáo ban đầu vẫn hợp lệ.")
                    .build();

            complaintRepository.save(complaint);
            createdIds.add(complaint.getId());
            recordSeed("COMPLAINT", complaint.getId(), seedScope);
        }

        log.info("[SeedData] Đã tạo {} khiếu nại mẫu", count);
        return Map.of(
                "status", "SUCCESS",
                "message", "Đã tạo " + count + " khiếu nại mẫu",
                "count", count,
                "ids", createdIds);
    }

    /**
     * Tạo dữ liệu mẫu phản hồi
     */
    @Transactional
    public Map<String, Object> seedFeedbacks(int count) {
        return seedFeedbacks(count, SYSTEM_SHOWCASE_SCOPE);
    }

    @Transactional
    public Map<String, Object> seedFeedbacks(int count, String seedScope) {
        List<User> users = ensureStudentCohort(Math.max(8, Math.min(count + 2, 12)), seedScope);
        List<User> staff = getActiveStaff();

        if (users.isEmpty()) {
            return Map.of("status", "ERROR", "message", "Cần có users trước khi seed feedbacks");
        }

        String[] contents = {
                "Thư viện rất yên tĩnh vào buổi sáng, phù hợp để học tập trung trước giờ lên lớp.",
                "WiFi ổn định hơn nhiều so với đầu học kỳ, tải tài liệu khá nhanh.",
                "Ghế ngồi ở khu tự học hơi cứng, nếu có đệm lưng sẽ thoải mái hơn.",
                "Điều hoà khu B buổi tối khá lạnh, mong thư viện cân đối lại nhiệt độ.",
                "Thủ thư hỗ trợ sinh viên nhiệt tình khi cần đổi chỗ hoặc xác minh check-in.",
                "Khu vực đọc sách bên cửa sổ rất đẹp, ánh sáng tự nhiên đủ dùng cả buổi sáng.",
                "App đặt chỗ rõ ràng và thao tác nhanh, nhất là khi xem sơ đồ ghế theo khu.",
                "Nếu thư viện mở sớm hơn cuối tuần khoảng 30 phút thì sẽ tiện cho sinh viên hơn."
        };
        String[] categories = { "FACILITY", "SERVICE", "GENERAL" };
        FeedbackEntity.FeedbackStatus[] statuses = FeedbackEntity.FeedbackStatus.values();
        Random rng = new Random();
        List<UUID> createdIds = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            User user = users.get(rng.nextInt(users.size()));
            FeedbackEntity.FeedbackStatus status = statuses[rng.nextInt(statuses.length)];
            User reviewer = status == FeedbackEntity.FeedbackStatus.NEW || staff.isEmpty()
                    ? null
                    : staff.get(rng.nextInt(staff.size()));

            FeedbackEntity feedback = FeedbackEntity.builder()
                    .user(user)
                    .rating(1 + rng.nextInt(5)) // 1-5 sao
                    .content(contents[rng.nextInt(contents.length)])
                    .category(categories[rng.nextInt(categories.length)])
                    .status(status)
                    .reviewedBy(reviewer)
                    .reviewedAt(status == FeedbackEntity.FeedbackStatus.NEW ? null
                            : LocalDateTime.now().minusHours(rng.nextInt(96) + 2))
                    .build();

            feedbackRepository.save(feedback);
            createdIds.add(feedback.getId());
            recordSeed("FEEDBACK", feedback.getId(), seedScope);
        }

        log.info("[SeedData] Đã tạo {} phản hồi mẫu", count);
        return Map.of(
                "status", "SUCCESS",
                "message", "Đã tạo " + count + " phản hồi mẫu",
                "count", count,
                "ids", createdIds);
    }

    @Transactional
    public Map<String, Object> seedSeatStatusReports(int count) {
        return seedSeatStatusReports(count, SYSTEM_SHOWCASE_SCOPE);
    }

    @Transactional
    public Map<String, Object> seedSeatStatusReports(int count, String seedScope) {
        List<User> users = userRepository.findAll().stream()
                .sorted(Comparator.comparing(User::getUserCode, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();
        List<SeatEntity> seats = seatRepository.findAll().stream()
                .sorted(Comparator.comparing(SeatEntity::getSeatCode, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();

        if (users.isEmpty() || seats.isEmpty()) {
            return Map.of("status", "ERROR", "message", "Cần có users và seats trước khi seed seat status reports");
        }

        List<User> reporters = users.stream()
                .filter(this::isActiveUser)
                .filter(user -> user.getRole() == Role.STUDENT)
                .toList();
        if (reporters.isEmpty()) {
            reporters = users.stream().filter(this::isActiveUser).toList();
        }

        List<User> librarians = users.stream()
                .filter(this::isActiveUser)
                .filter(user -> user.getRole() == Role.LIBRARIAN || user.getRole() == Role.ADMIN)
                .toList();
        if (librarians.isEmpty()) {
            return Map.of(
                    "status", "ERROR",
                    "message", "Cần có ít nhất 1 tài khoản LIBRARIAN hoặc ADMIN để seed seat status reports đã xử lý");
        }

        int actualCount = Math.max(count, MIN_SEAT_STATUS_REPORTS);
        List<UUID> createdIds = new ArrayList<>();
        EnumSet<SeatStatusReportEntity.IssueType> seededIssueTypes = EnumSet.noneOf(SeatStatusReportEntity.IssueType.class);
        EnumSet<SeatStatusReportEntity.ReportStatus> seededStatuses = EnumSet.noneOf(SeatStatusReportEntity.ReportStatus.class);

        for (int i = 0; i < actualCount; i++) {
            User reporter = reporters.get(i % reporters.size());
            User librarian = librarians.get(i % librarians.size());
            SeatEntity seat = seats.get(i % seats.size());
            SeatStatusReportSeedTemplate template = SEAT_STATUS_REPORT_SEED_TEMPLATES
                    .get(i % SEAT_STATUS_REPORT_SEED_TEMPLATES.size());

            SeatStatusReportEntity report = SeatStatusReportEntity.builder()
                    .user(reporter)
                    .seat(seat)
                    .issueType(template.issueType())
                    .description(buildSeatStatusReportDescription(template.descriptionTemplate(), seat.getSeatCode(), i))
                    .imageUrl(template.imageUrl())
                    .status(SeatStatusReportEntity.ReportStatus.PENDING)
                    .build();

            SeatStatusReportEntity savedReport = seatStatusReportRepository.save(report);
            applySeatStatusReportState(savedReport, template.status(), librarian);
            recordSeed("SEAT_STATUS_REPORT", savedReport.getId(), seedScope);

            seededIssueTypes.add(savedReport.getIssueType());
            seededStatuses.add(savedReport.getStatus());
            createdIds.add(savedReport.getId());
        }

        log.info(
                "[SeedData] Đã tạo {} seat status reports mẫu (issueTypes={}, statuses={})",
                actualCount,
                seededIssueTypes,
                seededStatuses);

        return Map.of(
                "status", "SUCCESS",
                "message", "Đã tạo " + actualCount + " báo cáo tình trạng ghế mẫu với đủ trạng thái xử lý",
                "count", actualCount,
                "ids", createdIds);
    }

    private boolean isActiveUser(User user) {
        return user.getIsActive() == null || Boolean.TRUE.equals(user.getIsActive());
    }

    private String buildSeatStatusReportDescription(String descriptionTemplate, String seatCode, int index) {
        String description = String.format(descriptionTemplate, seatCode);
        int batchNumber = index / SEAT_STATUS_REPORT_SEED_TEMPLATES.size();
        if (batchNumber == 0) {
            return description;
        }
        return description + " (đợt mẫu " + (batchNumber + 1) + ")";
    }

    private void applySeatStatusReportState(
            SeatStatusReportEntity report,
            SeatStatusReportEntity.ReportStatus targetStatus,
            User librarian) {
        if (targetStatus == SeatStatusReportEntity.ReportStatus.PENDING) {
            return;
        }

        LocalDateTime baseTime = report.getCreatedAt() != null ? report.getCreatedAt() : LocalDateTime.now();
        LocalDateTime verifiedAt = baseTime.plusMinutes(15);
        report.setStatus(targetStatus);
        report.setVerifiedBy(librarian);
        report.setVerifiedAt(verifiedAt);

        if (targetStatus == SeatStatusReportEntity.ReportStatus.RESOLVED) {
            report.setResolvedAt(verifiedAt.plusHours(2));
        } else {
            report.setResolvedAt(null);
        }

        seatStatusReportRepository.save(report);
    }

    private record SeatStatusReportSeedTemplate(
            SeatStatusReportEntity.IssueType issueType,
            SeatStatusReportEntity.ReportStatus status,
            String descriptionTemplate,
            String imageUrl) {
    }

    private void recordSeed(String entityType, Object entityId, String seedScope) {
        if (entityId == null || seedRecordRepository.existsByEntityTypeAndEntityId(entityType, entityId.toString())) {
            return;
        }
        seedRecordRepository.save(SeedRecordEntity.builder()
                .entityType(entityType)
                .entityId(entityId.toString())
                .seedScope(seedScope)
                .build());
    }

    private List<User> getActiveStudents() {
        return userRepository.findAll().stream()
                .filter(this::isActiveUser)
                .filter(user -> user.getRole() == Role.STUDENT)
                .sorted(Comparator.comparing(User::getUserCode, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();
    }

    private List<User> getActiveStaff() {
        return userRepository.findAll().stream()
                .filter(this::isActiveUser)
                .filter(user -> user.getRole() == Role.LIBRARIAN || user.getRole() == Role.ADMIN)
                .sorted(Comparator.comparing(User::getFullName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();
    }

    private List<SeatEntity> getActiveSeats() {
        return seatRepository.findAll().stream()
                .filter(seat -> seat.getIsActive() == null || Boolean.TRUE.equals(seat.getIsActive()))
                .sorted(Comparator.comparing(SeatEntity::getSeatCode, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();
    }

    private boolean isActiveReservationStatus(String status) {
        if (status == null || status.isBlank()) {
            return false;
        }
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        return "PROCESSING".equals(normalized) || "BOOKED".equals(normalized) || "CONFIRMED".equals(normalized);
    }

    private boolean overlapsNow(ReservationEntity reservation, LocalDateTime now) {
        return reservation.getStartTime() != null
                && reservation.getEndTime() != null
                && !reservation.getStartTime().isAfter(now)
                && reservation.getEndTime().isAfter(now);
    }

    private String buildAvatarUrl(String fullName, int colorIndex) {
        String normalizedName = Arrays.stream(fullName.trim().split("\\s+"))
                .filter(part -> !part.isBlank())
                .reduce((left, right) -> left + "+" + right)
                .orElse("SLIB");
        String bg = SAMPLE_AVATAR_BACKGROUNDS.get(Math.floorMod(colorIndex, SAMPLE_AVATAR_BACKGROUNDS.size()));
        return "https://ui-avatars.com/api/?name=" + normalizedName + "&background=" + bg + "&color=ffffff&bold=true";
    }

    private User ensureStudentUser(String userCode, String seedScope, int fallbackIndex) {
        return userRepository.findByUserCode(userCode)
                .orElseGet(() -> {
                    String normalizedUserCode = userCode.trim().toUpperCase(Locale.ROOT);
                    String fullName = sampleStudentNames().get(Math.floorMod(fallbackIndex, sampleStudentNames().size()));
                    User created = userRepository.save(User.builder()
                            .userCode(normalizedUserCode)
                            .username(normalizedUserCode.toLowerCase(Locale.ROOT))
                            .email(normalizedUserCode.toLowerCase(Locale.ROOT) + "@fpt.edu.vn")
                            .fullName(fullName)
                            .role(Role.STUDENT)
                            .isActive(true)
                            .passwordChanged(true)
                            .notifyBooking(true)
                            .notifyReminder(true)
                            .notifyNews(true)
                            .reputationScore(100)
                            .avtUrl(buildAvatarUrl(fullName, fallbackIndex))
                            .build());
                    recordSeed("USER", created.getId(), seedScope);

                    StudentProfile profile = studentProfileRepository.findByUserId(created.getId())
                            .orElseGet(() -> studentProfileRepository.save(StudentProfile.builder()
                                    .user(created)
                                    .userId(created.getId())
                                    .reputationScore(100)
                                    .totalStudyHours(0.0)
                                    .violationCount(0)
                                    .build()));
                    recordSeed("STUDENT_PROFILE", profile.getUserId(), seedScope);
                    return created;
                });
    }

    private List<User> ensureStudentCohort(int minimumStudents, String seedScope) {
        List<User> students = new ArrayList<>(getActiveStudents());
        List<String> sampleCodes = sampleStudentCodes();
        int index = 0;
        while (students.size() < minimumStudents && index < sampleCodes.size()) {
            String code = sampleCodes.get(index);
            if (students.stream().noneMatch(student -> code.equalsIgnoreCase(student.getUserCode()))) {
                students.add(ensureStudentUser(code, seedScope, index));
            }
            index++;
        }
        students.sort(Comparator.comparing(User::getUserCode, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
        return students;
    }

    private List<String> sampleStudentCodes() {
        return List.of(
                "SE171001",
                "SE171024",
                "SE171035",
                "SE171078",
                "SE172003",
                "SE172017",
                "SE172031",
                "SE172056",
                "SE173009",
                "SE173044");
    }

    private List<String> sampleStudentNames() {
        return List.of(
                "Nguyễn Minh Anh",
                "Trần Gia Huy",
                "Lê Khánh Linh",
                "Phạm Đức Long",
                "Võ Bảo Ngọc",
                "Đặng Hoàng Nam",
                "Bùi Quỳnh Chi",
                "Phan Nhật Minh",
                "Ngô Hải Yến",
                "Hoàng Phúc An");
    }

    private List<Category> ensureNewsCategories() {
        return List.of(
                ensureCategory("Thông báo", "#F97316"),
                ensureCategory("Tin tức", "#2563EB"),
                ensureCategory("Sự kiện", "#16A34A"),
                ensureCategory("Hướng dẫn", "#7C3AED"));
    }

    private Category ensureCategory(String name, String colorCode) {
        return categoryRepository.findByName(name)
                .orElseGet(() -> categoryRepository.save(Category.builder()
                        .name(name)
                        .colorCode(colorCode)
                        .build()));
    }

    private List<NewsSeedTemplate> newsTemplates() {
        return List.of(
                new NewsSeedTemplate(
                        "Thư viện mở rộng khu tự học yên tĩnh trong giai đoạn giữa kỳ",
                        "Từ tuần này, thư viện bổ sung thêm chỗ ngồi yên tĩnh tại tầng 3 để phục vụ sinh viên ôn thi giữa kỳ.",
                        "<p>Nhằm đáp ứng nhu cầu học tập tăng cao trong giai đoạn giữa kỳ, thư viện đã mở rộng thêm <strong>khu tự học yên tĩnh</strong> tại tầng 3.</p><p>Khu vực mới được bố trí thêm ổ cắm, đèn bàn và biển hướng dẫn rõ ràng để sinh viên dễ tìm chỗ trống.</p><p>Thư viện khuyến khích sinh viên đặt chỗ trước trên ứng dụng để hạn chế tình trạng chờ đợi vào giờ cao điểm.</p>",
                        "https://images.unsplash.com/photo-1521587760476-6c12a4b040da?auto=format&fit=crop&w=1200&q=80",
                        true,
                        284),
                new NewsSeedTemplate(
                        "Lịch vệ sinh và bảo trì khu máy tính tuần này",
                        "Khu máy tính tầng 2 sẽ được vệ sinh và kiểm tra thiết bị vào sáng thứ Bảy.",
                        "<p>Thư viện thông báo <strong>khu máy tính tầng 2</strong> sẽ được vệ sinh và bảo trì định kỳ vào sáng thứ Bảy.</p><p>Trong thời gian từ 07:00 đến 10:30, một số dãy ghế sẽ tạm ngưng phục vụ để kỹ thuật kiểm tra nguồn điện, bàn phím và thiết bị mạng.</p><p>Sinh viên nên theo dõi sơ đồ ghế trong ứng dụng để chọn khu vực thay thế phù hợp.</p>",
                        "https://images.unsplash.com/photo-1497633762265-9d179a990aa6?auto=format&fit=crop&w=1200&q=80",
                        false,
                        156),
                new NewsSeedTemplate(
                        "Workshop kỹ năng tìm tài liệu học thuật vào tối thứ Năm",
                        "Buổi workshop dành cho sinh viên năm nhất và năm hai sẽ diễn ra tại không gian học nhóm tầng 1.",
                        "<p>Thư viện phối hợp cùng bộ môn tổ chức workshop <strong>Kỹ năng tìm tài liệu học thuật hiệu quả</strong> vào 18:30 tối thứ Năm.</p><p>Nội dung tập trung vào cách tìm bài báo, lọc nguồn đáng tin cậy và quản lý trích dẫn cho bài tập nhóm.</p><p>Sinh viên có thể đăng ký nhanh qua quầy thủ thư hoặc biểu mẫu trong ứng dụng.</p>",
                        "https://images.unsplash.com/photo-1513258496099-48168024aec0?auto=format&fit=crop&w=1200&q=80",
                        false,
                        119),
                new NewsSeedTemplate(
                        "Hướng dẫn check-in ghế bằng NFC trên ứng dụng sinh viên",
                        "Sinh viên mới có thể xem nhanh 3 bước xác nhận chỗ ngồi bằng NFC ngay trên điện thoại.",
                        "<p>Để tránh mất lượt đặt, sinh viên nên hoàn tất <strong>check-in ghế bằng NFC</strong> trong thời gian quy định.</p><ol><li>Mở ứng dụng SLIB.</li><li>Chọn lượt đặt đang hoạt động.</li><li>Chạm điện thoại vào thẻ NFC tại ghế để xác nhận.</li></ol><p>Nếu gặp lỗi, vui lòng báo thủ thư để được hỗ trợ trực tiếp.</p>",
                        "https://images.unsplash.com/photo-1516321318423-f06f85e504b3?auto=format&fit=crop&w=1200&q=80",
                        false,
                        203),
                new NewsSeedTemplate(
                        "Thống kê sử dụng thư viện tuần qua ghi nhận khung giờ cao điểm mới",
                        "Khung 13:00 - 15:00 hiện là thời gian có tỷ lệ lấp đầy cao nhất trong tuần gần đây.",
                        "<p>Dữ liệu vận hành tuần qua cho thấy khung giờ <strong>13:00 - 15:00</strong> đang trở thành thời điểm có tỷ lệ lấp đầy cao nhất.</p><p>Thư viện khuyến nghị sinh viên đặt chỗ sớm hoặc chuyển sang khung 09:00 - 11:00 để dễ chọn chỗ hơn.</p><p>Đội ngũ thủ thư cũng đang theo dõi để điều chỉnh phân bổ khu vực phù hợp.</p>",
                        "https://images.unsplash.com/photo-1503676260728-1c00da094a0b?auto=format&fit=crop&w=1200&q=80",
                        false,
                        88),
                new NewsSeedTemplate(
                        "Thông báo lịch mở cửa dịp cao điểm đồ án",
                        "Thư viện dự kiến kéo dài thời gian phục vụ đến 21:30 trong hai tuần cao điểm đồ án cuối kỳ.",
                        "<p>Trong giai đoạn cao điểm đồ án cuối kỳ, thư viện dự kiến mở cửa đến <strong>21:30</strong> từ thứ Hai đến thứ Sáu.</p><p>Lịch áp dụng sẽ được xác nhận sau khi rà soát nhu cầu thực tế và nhân sự trực ca tối.</p><p>Thông báo chính thức sẽ được phát hành vào sáng mai nếu không có thay đổi.</p>",
                        "https://images.unsplash.com/photo-1524995997946-a1c2e315a42f?auto=format&fit=crop&w=1200&q=80",
                        true,
                        64));
    }

    private List<NewBookSeedTemplate> newBookTemplates() {
        return List.of(
                new NewBookSeedTemplate(
                        "Clean Code",
                        "Robert C. Martin",
                        "9780132350884",
                        "https://images.unsplash.com/photo-1512820790803-83ca734da794?auto=format&fit=crop&w=900&q=80",
                        "Cuốn sách kinh điển về tư duy viết mã dễ đọc, dễ bảo trì và cách tổ chức codebase bền vững.",
                        "Công nghệ phần mềm",
                        "https://library.fpt.edu.vn",
                        "Prentice Hall",
                        LocalDate.of(2008, Month.AUGUST, 1)),
                new NewBookSeedTemplate(
                        "Designing Data-Intensive Applications",
                        "Martin Kleppmann",
                        "9781449373320",
                        "https://images.unsplash.com/photo-1495446815901-a7297e633e8d?auto=format&fit=crop&w=900&q=80",
                        "Tổng quan sâu về kiến trúc dữ liệu hiện đại, xử lý stream, consistency và khả năng mở rộng hệ thống.",
                        "Kiến trúc hệ thống",
                        "https://library.fpt.edu.vn",
                        "O'Reilly Media",
                        LocalDate.of(2017, Month.MARCH, 16)),
                new NewBookSeedTemplate(
                        "Atomic Habits",
                        "James Clear",
                        "9780735211292",
                        "https://images.unsplash.com/photo-1516979187457-637abb4f9353?auto=format&fit=crop&w=900&q=80",
                        "Tài liệu hữu ích cho sinh viên muốn cải thiện thói quen học tập, quản lý thời gian và duy trì nhịp học đều.",
                        "Kỹ năng học tập",
                        "https://library.fpt.edu.vn",
                        "Avery",
                        LocalDate.of(2018, Month.OCTOBER, 16)),
                new NewBookSeedTemplate(
                        "Hooked: How to Build Habit-Forming Products",
                        "Nir Eyal",
                        "9780241184837",
                        "https://images.unsplash.com/photo-1519682337058-a94d519337bc?auto=format&fit=crop&w=900&q=80",
                        "Phù hợp cho sinh viên quan tâm đến UX, sản phẩm số và hành vi người dùng trong thiết kế ứng dụng.",
                        "Thiết kế sản phẩm",
                        "https://library.fpt.edu.vn",
                        "Portfolio",
                        LocalDate.of(2014, Month.NOVEMBER, 4)),
                new NewBookSeedTemplate(
                        "The Pragmatic Programmer",
                        "David Thomas, Andrew Hunt",
                        "9780135957059",
                        "https://images.unsplash.com/photo-1511108690759-009324a90311?auto=format&fit=crop&w=900&q=80",
                        "Tập hợp những nguyên tắc thực chiến dành cho lập trình viên muốn phát triển tư duy làm nghề bài bản.",
                        "Lập trình thực chiến",
                        "https://library.fpt.edu.vn",
                        "Addison-Wesley",
                        LocalDate.of(2019, Month.SEPTEMBER, 13)),
                new NewBookSeedTemplate(
                        "Sprint",
                        "Jake Knapp",
                        "9781501121746",
                        "https://images.unsplash.com/photo-1481627834876-b7833e8f5570?auto=format&fit=crop&w=900&q=80",
                        "Sách phù hợp cho các nhóm đồ án muốn học quy trình giải bài toán, thử nghiệm ý tưởng và ra quyết định nhanh.",
                        "Làm việc nhóm",
                        "https://library.fpt.edu.vn",
                        "Simon & Schuster",
                        LocalDate.of(2016, Month.MARCH, 8)));
    }

    private List<NotificationSeedTemplate> notificationTemplates(User mainUser, UUID bookingReferenceId) {
        return List.of(
                new NotificationSeedTemplate(
                        "Nhắc nhở check-in",
                        NotificationEntity.NotificationType.REMINDER,
                        bookingReferenceId != null ? "REMINDER" : "REMINDER",
                        bookingReferenceId,
                        false,
                        user -> "Bạn còn khoảng 15 phút để xác nhận lịch đặt chỗ của mình. Hãy đến đúng giờ để tránh bị huỷ lượt đặt."),
                new NotificationSeedTemplate(
                        "Sắp hết giờ sử dụng",
                        NotificationEntity.NotificationType.REMINDER,
                        bookingReferenceId != null ? "REMINDER" : "REMINDER",
                        bookingReferenceId,
                        false,
                        user -> "Phiên sử dụng ghế của bạn sắp kết thúc. Hãy chuẩn bị lưu tài liệu và rời chỗ đúng giờ."),
                new NotificationSeedTemplate(
                        "Điểm uy tín vừa được cập nhật",
                        NotificationEntity.NotificationType.REPUTATION,
                        "REPUTATION",
                        null,
                        false,
                        user -> "Điểm uy tín của " + (user.getFullName() != null ? user.getFullName() : "bạn")
                                + " vừa được hệ thống cập nhật theo hoạt động gần đây."),
                new NotificationSeedTemplate(
                        "Thư viện vừa có tin mới",
                        NotificationEntity.NotificationType.NEWS,
                        "NEWS",
                        null,
                        false,
                        user -> "Đã có thông báo mới về lịch mở cửa và khu vực học nhóm trong tuần này."),
                new NotificationSeedTemplate(
                        "Yêu cầu hỗ trợ đang được xử lý",
                        NotificationEntity.NotificationType.SUPPORT_REQUEST,
                        "SUPPORT_REQUEST",
                        null,
                        true,
                        user -> "Thủ thư đã tiếp nhận yêu cầu hỗ trợ của bạn và đang cập nhật tiến độ xử lý."),
                new NotificationSeedTemplate(
                        "Khiếu nại đã được ghi nhận",
                        NotificationEntity.NotificationType.COMPLAINT,
                        "COMPLAINT",
                        null,
                        true,
                        user -> "Khiếu nại gần đây của bạn đã được chuyển đến thủ thư để xem xét."),
                new NotificationSeedTemplate(
                        "Báo cáo vi phạm mới",
                        NotificationEntity.NotificationType.VIOLATION_REPORT,
                        "VIOLATION_REPORT",
                        null,
                        false,
                        user -> "Hệ thống vừa ghi nhận một báo cáo vi phạm cần được kiểm tra thêm."),
                new NotificationSeedTemplate(
                        "Thông báo đặt chỗ",
                        NotificationEntity.NotificationType.BOOKING,
                        bookingReferenceId != null ? "RESERVATION" : "BOOKING",
                        bookingReferenceId,
                        false,
                        user -> "Lịch đặt chỗ của bạn vừa được cập nhật trạng thái mới trong hệ thống."));
    }

    private record NewsSeedTemplate(
            String title,
            String summary,
            String content,
            String imageUrl,
            boolean pinned,
            int viewCount) {
    }

    private record NewBookSeedTemplate(
            String title,
            String author,
            String isbn,
            String coverUrl,
            String description,
            String category,
            String sourceUrl,
            String publisher,
            LocalDate publishDate) {
    }

    private record NotificationSeedTemplate(
            String title,
            NotificationEntity.NotificationType type,
            String referenceType,
            UUID referenceId,
            boolean read,
            java.util.function.Function<User, String> contentSupplier) {

        String content(User user) {
            return contentSupplier.apply(user);
        }
    }

    /**
     * Tạo tất cả dữ liệu mẫu cùng lúc (bao gồm dashboard data)
     */
    @Transactional
    public Map<String, Object> seedAll(int bookingCount, int violationCount, int supportCount) {
        return seedAll(bookingCount, 24, violationCount, supportCount, 5, 8, 8, 6, 8, 8, null);
    }

    @Transactional
    public Map<String, Object> seedAll(int bookingCount, int violationCount, int supportCount, String studentCode) {
        return seedAll(bookingCount, 24, violationCount, supportCount, 5, 8, 8, 6, 8, 8, studentCode);
    }

    @Transactional
    public Map<String, Object> seedAll(
            int bookingCount,
            int accessLogCount,
            int violationCount,
            int supportCount,
            int complaintCount,
            int feedbackCount,
            int seatStatusReportCount,
            int newsCount,
            int newBookCount,
            int notificationCount,
            String studentCode) {
        ensureStudentCohort(Math.max(8, bookingCount / 2), SYSTEM_SHOWCASE_SCOPE);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("accessLogs", seedAccessLogs(accessLogCount, SYSTEM_SHOWCASE_SCOPE));
        result.put("bookings", seedBookings(bookingCount, SYSTEM_SHOWCASE_SCOPE));
        result.put("violations", seedViolations(violationCount, SYSTEM_SHOWCASE_SCOPE));
        result.put("supportRequests", seedSupportRequests(supportCount, SYSTEM_SHOWCASE_SCOPE));
        result.put("complaints", seedComplaints(complaintCount, SYSTEM_SHOWCASE_SCOPE));
        result.put("feedbacks", seedFeedbacks(feedbackCount, SYSTEM_SHOWCASE_SCOPE));
        result.put("seatStatusReports", seedSeatStatusReports(seatStatusReportCount, SYSTEM_SHOWCASE_SCOPE));
        result.put("news", seedNews(newsCount, SYSTEM_SHOWCASE_SCOPE));
        result.put("newBooks", seedNewBooks(newBookCount, SYSTEM_SHOWCASE_SCOPE));
        result.put("notifications", seedNotifications(notificationCount, studentCode, SYSTEM_SHOWCASE_SCOPE));

        if (studentCode != null && !studentCode.isBlank()) {
            result.put("studentJourney", seedStudentJourney(studentCode.trim().toUpperCase(Locale.ROOT)));
        }

        result.put("status", "SUCCESS");
        result.put("message", String.format(
                "Đã tạo bộ dữ liệu showcase gồm access logs, bookings, violations, supports, complaints, feedbacks, seat status reports, news, new books, notifications%s",
                studentCode != null && !studentCode.isBlank() ? " và dữ liệu hành trình cho sinh viên " + studentCode : ""));
        return result;
    }

    @Transactional
    public Map<String, Object> seedNotifications(int count, String userCode, String seedScope) {
        List<User> students = ensureStudentCohort(Math.max(6, Math.min(count, 10)), seedScope);
        if (students.isEmpty()) {
            return Map.of("status", "ERROR", "message", "Cần có users trước khi seed notifications");
        }

        User preferredUser = null;
        if (userCode != null && !userCode.isBlank()) {
            preferredUser = userRepository.findByUserCode(userCode.trim().toUpperCase(Locale.ROOT)).orElse(null);
        }

        User mainUser = preferredUser != null ? preferredUser : students.get(0);
        List<ReservationEntity> existingBookings = reservationRepository.findByUserId(mainUser.getId());
        UUID bookingReferenceId = existingBookings.isEmpty() ? null : existingBookings.get(0).getReservationId();

        List<NotificationSeedTemplate> templates = notificationTemplates(mainUser, bookingReferenceId);
        List<UUID> createdIds = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            NotificationSeedTemplate template = templates.get(i % templates.size());
            User targetUser = i % 3 == 0 ? mainUser : students.get(i % students.size());

            NotificationEntity notification = notificationRepository.save(NotificationEntity.builder()
                    .user(targetUser)
                    .title(template.title())
                    .content(template.content(targetUser))
                    .notificationType(template.type())
                    .referenceType(template.referenceType())
                    .referenceId(template.referenceId())
                    .isRead(template.read())
                    .build());

            createdIds.add(notification.getId());
            recordSeed("NOTIFICATION", notification.getId(), seedScope);
        }

        return Map.of(
                "status", "SUCCESS",
                "message", "Đã tạo " + count + " thông báo mẫu",
                "count", count,
                "ids", createdIds,
                "targetUser", mainUser.getUserCode());
    }

    @Transactional
    public Map<String, Object> seedNews(int count, String seedScope) {
        List<Category> categories = ensureNewsCategories();
        List<Long> createdIds = new ArrayList<>();
        List<NewsSeedTemplate> templates = newsTemplates();
        LocalDateTime now = LocalDateTime.now();

        for (int i = 0; i < count; i++) {
            NewsSeedTemplate template = templates.get(i % templates.size());
            boolean published = i != count - 1 || count == 1;
            LocalDateTime publishedAt = published ? now.minusHours(3L * (i + 1)) : now.plusHours(8);

            News news = newsRepository.save(News.builder()
                    .title(template.title())
                    .summary(template.summary())
                    .content(template.content())
                    .imageUrl(template.imageUrl())
                    .category(categories.get(i % categories.size()))
                    .isPublished(published)
                    .isPinned(template.pinned() && published)
                    .viewCount(template.viewCount())
                    .publishedAt(publishedAt)
                    .build());

            createdIds.add(news.getId());
            recordSeed("NEWS", news.getId(), seedScope);
        }

        return Map.of(
                "status", "SUCCESS",
                "message", "Đã tạo " + count + " tin tức mẫu",
                "count", count,
                "ids", createdIds);
    }

    @Transactional
    public Map<String, Object> seedNewBooks(int count, String seedScope) {
        List<User> staff = getActiveStaff();
        User creator = staff.isEmpty() ? null : staff.get(0);
        List<Integer> createdIds = new ArrayList<>();
        List<NewBookSeedTemplate> templates = newBookTemplates();

        for (int i = 0; i < count; i++) {
            NewBookSeedTemplate template = templates.get(i % templates.size());
            NewBookEntity book = newBookRepository.save(NewBookEntity.builder()
                    .title(template.title())
                    .author(template.author())
                    .isbn(template.isbn())
                    .coverUrl(template.coverUrl())
                    .description(template.description())
                    .category(template.category())
                    .sourceUrl(template.sourceUrl())
                    .publisher(template.publisher())
                    .publishDate(template.publishDate())
                    .arrivalDate(LocalDate.now().minusDays(i))
                    .isActive(true)
                    .isPinned(i < 2)
                    .createdBy(creator)
                    .build());

            createdIds.add(book.getId());
            recordSeed("NEW_BOOK", book.getId(), seedScope);
        }

        return Map.of(
                "status", "SUCCESS",
                "message", "Đã tạo " + count + " sách mới mẫu",
                "count", count,
                "ids", createdIds);
    }

    @Transactional
    public Map<String, Object> seedStudentJourney(String userCode) {
        String seedScope = STUDENT_JOURNEY_SCOPE_PREFIX + userCode;
        User student = ensureStudentUser(userCode, seedScope, Math.abs(userCode.hashCode()));
        StudentProfile profile = studentProfileRepository.findByUserId(student.getId())
                .orElseGet(() -> studentProfileRepository.save(StudentProfile.builder()
                        .user(student)
                        .userId(student.getId())
                        .reputationScore(92)
                        .totalStudyHours(26.5)
                        .violationCount(1)
                        .build()));

        List<User> staff = getActiveStaff();
        User librarian = staff.isEmpty() ? null : staff.get(0);
        User peerStudent = ensureStudentCohort(6, seedScope).stream()
                .filter(candidate -> !candidate.getId().equals(student.getId()))
                .findFirst()
                .orElseGet(() -> ensureStudentUser("SE173099", seedScope, 99));
        List<SeatEntity> seats = getActiveSeats();
        if (seats.size() < 5) {
            return Map.of("status", "ERROR", "message", "Cần có tối thiểu 5 ghế active để tạo dữ liệu demo mobile cho sinh viên");
        }

        SeatEntity currentSeat = seats.get(0);
        SeatEntity pastSeat1 = seats.get(1);
        SeatEntity pastSeat2 = seats.get(2);
        SeatEntity futureSeat = seats.get(3);
        SeatEntity cancelledSeat = seats.get(4);
        LocalDateTime now = LocalDateTime.now();

        ReservationEntity currentReservation = reservationRepository.save(ReservationEntity.builder()
                .user(student)
                .seat(currentSeat)
                .startTime(now.minusMinutes(25))
                .endTime(now.plusHours(2))
                .confirmedAt(now.minusMinutes(23))
                .status("CONFIRMED")
                .build());
        recordSeed("RESERVATION", currentReservation.getReservationId(), seedScope);

        ReservationEntity completedReservation = reservationRepository.save(ReservationEntity.builder()
                .user(student)
                .seat(pastSeat1)
                .startTime(now.minusDays(1).withHour(9).withMinute(0))
                .endTime(now.minusDays(1).withHour(11).withMinute(30))
                .confirmedAt(now.minusDays(1).withHour(9).withMinute(2))
                .status("COMPLETED")
                .build());
        recordSeed("RESERVATION", completedReservation.getReservationId(), seedScope);

        ReservationEntity expiredReservation = reservationRepository.save(ReservationEntity.builder()
                .user(student)
                .seat(pastSeat2)
                .startTime(now.minusDays(3).withHour(13).withMinute(0))
                .endTime(now.minusDays(3).withHour(15).withMinute(0))
                .status("EXPIRED")
                .build());
        recordSeed("RESERVATION", expiredReservation.getReservationId(), seedScope);

        ReservationEntity upcomingReservation = reservationRepository.save(ReservationEntity.builder()
                .user(student)
                .seat(futureSeat)
                .startTime(now.plusDays(1).withHour(14).withMinute(0))
                .endTime(now.plusDays(1).withHour(16).withMinute(0))
                .status("BOOKED")
                .build());
        recordSeed("RESERVATION", upcomingReservation.getReservationId(), seedScope);

        ReservationEntity cancelledReservation = reservationRepository.save(ReservationEntity.builder()
                .user(student)
                .seat(cancelledSeat)
                .startTime(now.plusDays(2).withHour(8).withMinute(30))
                .endTime(now.plusDays(2).withHour(10).withMinute(30))
                .status("CANCELLED")
                .cancellationReason("Thủ thư hủy lịch này để bảo trì khu vực và sắp xếp lại ghế trước ca cao điểm.")
                .cancelledByUserId(librarian != null ? librarian.getId() : student.getId())
                .build());
        recordSeed("RESERVATION", cancelledReservation.getReservationId(), seedScope);

        AccessLog currentAccess = accessLogRepository.save(AccessLog.builder()
                .user(student)
                .reservationId(currentReservation.getReservationId())
                .checkInTime(now.minusMinutes(24))
                .deviceId("gate-main-01")
                .build());
        recordSeed("ACCESS_LOG", currentAccess.getLogId(), seedScope);

        AccessLog completedAccess = accessLogRepository.save(AccessLog.builder()
                .user(student)
                .reservationId(completedReservation.getReservationId())
                .checkInTime(completedReservation.getStartTime())
                .checkOutTime(completedReservation.getEndTime())
                .deviceId("gate-main-01")
                .build());
        recordSeed("ACCESS_LOG", completedAccess.getLogId(), seedScope);

        ActivityLogEntity bookingActivity = activityLogRepository.save(ActivityLogEntity.builder()
                .userId(student.getId())
                .activityType(ActivityLogEntity.TYPE_BOOKING_SUCCESS)
                .title("Đặt chỗ thành công")
                .description("Đã đặt ghế " + futureSeat.getSeatCode() + " cho buổi học nhóm chiều mai")
                .reservationId(upcomingReservation.getReservationId())
                .seatCode(futureSeat.getSeatCode())
                .zoneName(futureSeat.getZone().getZoneName())
                .createdAt(now.minusHours(2).atZone(java.time.ZoneId.of("Asia/Ho_Chi_Minh")))
                .build());
        recordSeed("ACTIVITY_LOG", bookingActivity.getId(), seedScope);

        ActivityLogEntity checkInActivity = activityLogRepository.save(ActivityLogEntity.builder()
                .userId(student.getId())
                .activityType(ActivityLogEntity.TYPE_CHECK_IN)
                .title("Check-in thành công")
                .description("Đã vào thư viện và xác nhận chỗ ngồi tại ghế " + currentSeat.getSeatCode())
                .reservationId(currentReservation.getReservationId())
                .seatCode(currentSeat.getSeatCode())
                .zoneName(currentSeat.getZone().getZoneName())
                .createdAt(now.minusMinutes(24).atZone(java.time.ZoneId.of("Asia/Ho_Chi_Minh")))
                .build());
        recordSeed("ACTIVITY_LOG", checkInActivity.getId(), seedScope);

        ActivityLogEntity checkOutActivity = activityLogRepository.save(ActivityLogEntity.builder()
                .userId(student.getId())
                .activityType(ActivityLogEntity.TYPE_CHECK_OUT)
                .title("Check-out thành công")
                .description("Hoàn tất buổi học 2 giờ 30 phút tại ghế " + pastSeat1.getSeatCode())
                .reservationId(completedReservation.getReservationId())
                .seatCode(pastSeat1.getSeatCode())
                .zoneName(pastSeat1.getZone().getZoneName())
                .durationMinutes(150)
                .createdAt(completedReservation.getEndTime().atZone(java.time.ZoneId.of("Asia/Ho_Chi_Minh")))
                .build());
        recordSeed("ACTIVITY_LOG", checkOutActivity.getId(), seedScope);

        ActivityLogEntity cancellationActivity = activityLogRepository.save(ActivityLogEntity.builder()
                .userId(student.getId())
                .activityType(ActivityLogEntity.TYPE_BOOKING_CANCEL)
                .title("Lịch đặt đã bị hủy")
                .description("Thủ thư đã hủy lịch tại ghế " + cancelledSeat.getSeatCode()
                        + " do khu vực đang được bảo trì trước giờ mở cửa.")
                .reservationId(cancelledReservation.getReservationId())
                .seatCode(cancelledSeat.getSeatCode())
                .zoneName(cancelledSeat.getZone().getZoneName())
                .createdAt(now.minusHours(6).atZone(java.time.ZoneId.of("Asia/Ho_Chi_Minh")))
                .build());
        recordSeed("ACTIVITY_LOG", cancellationActivity.getId(), seedScope);

        PointTransactionEntity rewardTransaction = pointTransactionRepository.save(PointTransactionEntity.builder()
                .userId(student.getId())
                .points(5)
                .transactionType(PointTransactionEntity.TYPE_WEEKLY_BONUS)
                .title("Thưởng học tập đều")
                .description("Hoàn thành đủ 4 buổi học đúng giờ trong tuần này.")
                .balanceAfter(97)
                .createdAt(now.minusDays(2).atZone(java.time.ZoneId.of("Asia/Ho_Chi_Minh")))
                .build());
        recordSeed("POINT_TRANSACTION", rewardTransaction.getId(), seedScope);

        PointTransactionEntity penaltyTransaction = pointTransactionRepository.save(PointTransactionEntity.builder()
                .userId(student.getId())
                .points(-5)
                .transactionType(PointTransactionEntity.TYPE_NO_SHOW_PENALTY)
                .title("No-show một lượt đặt chỗ")
                .description("Sinh viên không đến xác nhận chỗ ngồi trong khung giờ đã đặt.")
                .balanceAfter(92)
                .createdAt(now.minusDays(3).atZone(java.time.ZoneId.of("Asia/Ho_Chi_Minh")))
                .build());
        recordSeed("POINT_TRANSACTION", penaltyTransaction.getId(), seedScope);

        SeatViolationReportEntity receivedViolation = violationReportRepository.save(SeatViolationReportEntity.builder()
                .reporter(librarian != null ? librarian : peerStudent)
                .violator(student)
                .seat(pastSeat2)
                .violationType(SeatViolationReportEntity.ViolationType.LEFT_BELONGINGS)
                .description("Sinh viên để balo và giáo trình giữ chỗ gần 30 phút nhưng rời khỏi khu vực.")
                .status(SeatViolationReportEntity.ReportStatus.VERIFIED)
                .verifiedBy(librarian)
                .pointDeducted(5)
                .verifiedAt(now.minusDays(3).plusHours(2))
                .build());
        recordSeed("VIOLATION_REPORT", receivedViolation.getId(), seedScope);

        SeatViolationReportEntity reportedViolation = violationReportRepository.save(SeatViolationReportEntity.builder()
                .reporter(student)
                .violator(peerStudent)
                .seat(cancelledSeat)
                .violationType(SeatViolationReportEntity.ViolationType.NOISE)
                .description("Bạn sinh viên ngồi ghế bên cạnh nói chuyện nhóm quá lớn trong giờ tự học và chưa giảm âm lượng.")
                .status(SeatViolationReportEntity.ReportStatus.PENDING)
                .reservationId(currentReservation.getReservationId())
                .build());
        recordSeed("VIOLATION_REPORT", reportedViolation.getId(), seedScope);

        ComplaintEntity complaint = complaintRepository.save(ComplaintEntity.builder()
                .user(student)
                .pointTransactionId(penaltyTransaction.getId())
                .violationReportId(receivedViolation.getId())
                .subject("Mong thư viện xem lại mức trừ điểm của lượt no-show")
                .content("Em có việc gấp nên đến trễ khoảng 15 phút và không cố ý bỏ chỗ. Mong thư viện cân nhắc giảm mức trừ điểm.")
                .status(ComplaintEntity.ComplaintStatus.PENDING)
                .build());
        recordSeed("COMPLAINT", complaint.getId(), seedScope);

        FeedbackEntity feedback = feedbackRepository.save(FeedbackEntity.builder()
                .user(student)
                .reservationId(completedReservation.getReservationId())
                .rating(5)
                .category("SERVICE")
                .content("Buổi học tối qua khá ổn, khu tự học yên tĩnh và thủ thư hỗ trợ nhanh khi em cần đổi ghế.")
                .status(FeedbackEntity.FeedbackStatus.NEW)
                .build());
        recordSeed("FEEDBACK", feedback.getId(), seedScope);

        SupportRequest supportRequest = supportRequestRepository.save(SupportRequest.builder()
                .student(student)
                .description("Ổ cắm gần ghế " + currentSeat.getSeatCode() + " lúc cắm sạc bị chập chờn, em đã thử 2 adapter khác nhau.")
                .status(SupportRequestStatus.IN_PROGRESS)
                .adminResponse("Đã ghi nhận và chuyển kỹ thuật kiểm tra cuối ca hôm nay.")
                .resolvedBy(librarian)
                .resolvedAt(now.minusHours(1))
                .build());
        recordSeed("SUPPORT_REQUEST", supportRequest.getId(), seedScope);

        SeatStatusReportEntity seatStatusReport = seatStatusReportRepository.save(SeatStatusReportEntity.builder()
                .user(student)
                .seat(currentSeat)
                .issueType(SeatStatusReportEntity.IssueType.MISSING_EQUIPMENT)
                .description("Ghế " + currentSeat.getSeatCode() + " thiếu móc treo balo, khá bất tiện khi học lâu.")
                .status(SeatStatusReportEntity.ReportStatus.PENDING)
                .build());
        recordSeed("SEAT_STATUS_REPORT", seatStatusReport.getId(), seedScope);

        List<NotificationEntity> notifications = List.of(
                NotificationEntity.builder()
                        .user(student)
                        .title("Nhắc nhở check-in")
                        .content("Bạn còn 15 phút để xác nhận chỗ ngồi tại ghế " + futureSeat.getSeatCode() + " vào ngày mai.")
                        .notificationType(NotificationEntity.NotificationType.REMINDER)
                        .referenceType("RESERVATION")
                        .referenceId(upcomingReservation.getReservationId())
                        .isRead(false)
                        .build(),
                NotificationEntity.builder()
                        .user(student)
                        .title("Yêu cầu hỗ trợ đang được xử lý")
                        .content("Thủ thư đã tiếp nhận phản ánh về ổ cắm gần ghế " + currentSeat.getSeatCode() + ".")
                        .notificationType(NotificationEntity.NotificationType.SUPPORT_REQUEST)
                        .referenceType("SUPPORT_REQUEST")
                        .referenceId(supportRequest.getId())
                        .isRead(true)
                        .build(),
                NotificationEntity.builder()
                        .user(student)
                        .title("Lịch đặt đã bị thủ thư hủy")
                        .content("Lượt đặt ghế " + cancelledSeat.getSeatCode()
                                + " đã bị hủy. Lý do: Thủ thư hủy để bảo trì khu vực trước giờ cao điểm.")
                        .notificationType(NotificationEntity.NotificationType.BOOKING)
                        .referenceType("RESERVATION")
                        .referenceId(cancelledReservation.getReservationId())
                        .isRead(false)
                        .build(),
                NotificationEntity.builder()
                        .user(student)
                        .title("Bạn đã bị ghi nhận vi phạm")
                        .content("Thư viện đã xác minh vi phạm giữ chỗ sai quy định tại ghế " + pastSeat2.getSeatCode()
                                + " và trừ 5 điểm uy tín.")
                        .notificationType(NotificationEntity.NotificationType.VIOLATION)
                        .referenceType("VIOLATION_REPORT")
                        .referenceId(receivedViolation.getId())
                        .isRead(false)
                        .build(),
                NotificationEntity.builder()
                        .user(student)
                        .title("Khiếu nại đang chờ xử lý")
                        .content("Khiếu nại về lượt no-show của bạn đã được ghi nhận và đang chờ thủ thư phản hồi.")
                        .notificationType(NotificationEntity.NotificationType.COMPLAINT)
                        .referenceType("COMPLAINT")
                        .referenceId(complaint.getId())
                        .isRead(false)
                        .build(),
                NotificationEntity.builder()
                        .user(student)
                        .title("Có tin mới từ thư viện")
                        .content("Thư viện vừa cập nhật lịch mở cửa dịp cao điểm giữa kỳ.")
                        .notificationType(NotificationEntity.NotificationType.NEWS)
                        .referenceType("NEWS")
                        .isRead(false)
                        .build());
        notifications.forEach(notification -> {
            NotificationEntity saved = notificationRepository.save(notification);
            recordSeed("NOTIFICATION", saved.getId(), seedScope);
        });

        profile.setReputationScore(97);
        profile.setTotalStudyHours(26.5);
        profile.setViolationCount(1);
        studentProfileRepository.save(profile);
        student.setReputationScore(97);
        userRepository.save(student);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "SUCCESS");
        result.put("message", "Đã tạo preset mobile đầy đủ cho sinh viên " + userCode);
        result.put("studentId", student.getId());
        result.put("userCode", student.getUserCode());
        result.put("studentName", student.getFullName());
        result.put("reputationScore", student.getReputationScore());
        result.put("currentSeat", currentSeat.getSeatCode());
        result.put("upcomingSeat", futureSeat.getSeatCode());
        result.put("cancelledSeat", cancelledSeat.getSeatCode());
        result.put("bookingStatuses", List.of("CONFIRMED", "COMPLETED", "EXPIRED", "BOOKED", "CANCELLED"));
        result.put("coverage", List.of(
                "Trang chủ và điểm uy tín",
                "Lịch sử đặt chỗ đủ 5 trạng thái",
                "Lịch sử hoạt động",
                "Thông báo mobile",
                "Khiếu nại và phản hồi",
                "Lịch sử vi phạm",
                "Lịch sử báo cáo tình trạng ghế",
                "Yêu cầu hỗ trợ"));
        result.put("notificationCount", notifications.size());
        result.put("reportedViolationId", reportedViolation.getId());
        result.put("receivedViolationId", receivedViolation.getId());
        return result;
    }

    /**
     * Xoá dữ liệu seed (nhận diện qua SEED_MARKER trong
     * description/content/deviceId)
     */
    @Transactional
    public Map<String, Object> clearSeedData() {
        long violationsDeleted = 0;
        long supportDeleted = 0;
        long complaintsDeleted = 0;
        long feedbacksDeleted = 0;
        long seatStatusReportsDeleted = 0;
        long accessLogsDeleted = 0;
        long reservationsDeleted = 0;
        long activityLogsDeleted = 0;
        long pointTransactionsDeleted = 0;
        long notificationsDeleted = 0;
        long newsDeleted = 0;
        long newBooksDeleted = 0;
        long profilesDeleted = 0;
        long usersDeleted = 0;

        List<SeedRecordEntity> seedRecords = seedRecordRepository.findAllByOrderByIdDesc();
        for (SeedRecordEntity record : seedRecords) {
            switch (record.getEntityType()) {
                case "NOTIFICATION" -> {
                    notificationRepository.findById(UUID.fromString(record.getEntityId())).ifPresent(notification -> {
                        notificationRepository.delete(notification);
                    });
                    notificationsDeleted++;
                }
                case "COMPLAINT" -> {
                    complaintRepository.findById(UUID.fromString(record.getEntityId())).ifPresent(complaintRepository::delete);
                    complaintsDeleted++;
                }
                case "FEEDBACK" -> {
                    feedbackRepository.findById(UUID.fromString(record.getEntityId())).ifPresent(feedbackRepository::delete);
                    feedbacksDeleted++;
                }
                case "SUPPORT_REQUEST" -> {
                    supportRequestRepository.findById(UUID.fromString(record.getEntityId())).ifPresent(supportRequestRepository::delete);
                    supportDeleted++;
                }
                case "SEAT_STATUS_REPORT" -> {
                    seatStatusReportRepository.findById(UUID.fromString(record.getEntityId()))
                            .ifPresent(seatStatusReportRepository::delete);
                    seatStatusReportsDeleted++;
                }
                case "VIOLATION_REPORT" -> {
                    violationReportRepository.findById(UUID.fromString(record.getEntityId()))
                            .ifPresent(violationReportRepository::delete);
                    violationsDeleted++;
                }
                case "POINT_TRANSACTION" -> {
                    pointTransactionRepository.findById(UUID.fromString(record.getEntityId()))
                            .ifPresent(pointTransactionRepository::delete);
                    pointTransactionsDeleted++;
                }
                case "ACTIVITY_LOG" -> {
                    activityLogRepository.findById(UUID.fromString(record.getEntityId()))
                            .ifPresent(activityLogRepository::delete);
                    activityLogsDeleted++;
                }
                case "ACCESS_LOG" -> {
                    accessLogRepository.findById(UUID.fromString(record.getEntityId()))
                            .ifPresent(accessLogRepository::delete);
                    accessLogsDeleted++;
                }
                case "RESERVATION" -> {
                    reservationRepository.findById(UUID.fromString(record.getEntityId()))
                            .ifPresent(reservationRepository::delete);
                    reservationsDeleted++;
                }
                case "NEWS" -> {
                    newsRepository.findById(Long.parseLong(record.getEntityId())).ifPresent(newsRepository::delete);
                    newsDeleted++;
                }
                case "NEW_BOOK" -> {
                    newBookRepository.findById(Integer.parseInt(record.getEntityId())).ifPresent(newBookRepository::delete);
                    newBooksDeleted++;
                }
                case "STUDENT_PROFILE" -> {
                    studentProfileRepository.findById(UUID.fromString(record.getEntityId()))
                            .ifPresent(studentProfileRepository::delete);
                    profilesDeleted++;
                }
                case "USER" -> {
                    userRepository.findById(UUID.fromString(record.getEntityId())).ifPresent(userRepository::delete);
                    usersDeleted++;
                }
                default -> {
                }
            }
        }
        seedRecordRepository.deleteAll();

        // Xoá dữ liệu seed cũ còn dùng marker từ phiên bản trước
        List<SeatViolationReportEntity> violations = violationReportRepository.findAllByOrderByCreatedAtDesc();
        for (SeatViolationReportEntity v : violations) {
            if (v.getDescription() != null && v.getDescription().startsWith(LEGACY_SEED_MARKER)) {
                violationReportRepository.delete(v);
                violationsDeleted++;
            }
        }

        List<SupportRequest> supports = supportRequestRepository.findAllByOrderByCreatedAtDesc();
        for (SupportRequest s : supports) {
            if (s.getDescription() != null && s.getDescription().startsWith(LEGACY_SEED_MARKER)) {
                supportRequestRepository.delete(s);
                supportDeleted++;
            }
        }

        List<ComplaintEntity> complaints = complaintRepository.findAllByOrderByCreatedAtDesc();
        for (ComplaintEntity c : complaints) {
            if (c.getSubject() != null && c.getSubject().startsWith(LEGACY_SEED_MARKER)) {
                complaintRepository.delete(c);
                complaintsDeleted++;
            }
        }

        List<FeedbackEntity> feedbacks = feedbackRepository.findAllByOrderByCreatedAtDesc();
        for (FeedbackEntity f : feedbacks) {
            if (f.getContent() != null && f.getContent().startsWith(LEGACY_SEED_MARKER)) {
                feedbackRepository.delete(f);
                feedbacksDeleted++;
            }
        }

        List<SeatStatusReportEntity> seatStatusReports = seatStatusReportRepository.findAllByOrderByCreatedAtDesc();
        for (SeatStatusReportEntity report : seatStatusReports) {
            if (report.getDescription() != null && report.getDescription().startsWith(LEGACY_SEED_MARKER)) {
                seatStatusReportRepository.delete(report);
                seatStatusReportsDeleted++;
            }
        }

        List<AccessLog> allLogs = accessLogRepository.findAllOrderByCheckInTimeDesc();
        for (AccessLog al : allLogs) {
            if (al.getDeviceId() != null && al.getDeviceId().startsWith(LEGACY_SEED_MARKER)) {
                accessLogRepository.delete(al);
                accessLogsDeleted++;
            }
        }

        log.info(
                "[SeedData] Đã xoá {} reservations, {} access logs, {} activity logs, {} point transactions, {} violations, {} supports, {} complaints, {} feedbacks, {} seat-status reports, {} notifications, {} news, {} new books, {} profiles, {} users",
                reservationsDeleted,
                accessLogsDeleted,
                activityLogsDeleted,
                pointTransactionsDeleted,
                violationsDeleted,
                supportDeleted,
                complaintsDeleted,
                feedbacksDeleted,
                seatStatusReportsDeleted,
                notificationsDeleted,
                newsDeleted,
                newBooksDeleted,
                profilesDeleted,
                usersDeleted);
        return Map.of(
                "status", "SUCCESS",
                "message", String.format(
                        "Đã xoá %d reservations, %d access logs, %d activity logs, %d point transactions, %d violations, %d supports, %d complaints, %d feedbacks, %d seat status reports, %d notifications, %d news, %d sách mới, %d hồ sơ sinh viên, %d tài khoản mẫu",
                        reservationsDeleted,
                        accessLogsDeleted,
                        activityLogsDeleted,
                        pointTransactionsDeleted,
                        violationsDeleted,
                        supportDeleted,
                        complaintsDeleted,
                        feedbacksDeleted,
                        seatStatusReportsDeleted,
                        notificationsDeleted,
                        newsDeleted,
                        newBooksDeleted,
                        profilesDeleted,
                        usersDeleted));
    }

    /**
     * Xoá tất cả bookings (không phân biệt seed hay thật)
     */
    @Transactional
    public Map<String, Object> clearAllBookings() {
        long count = reservationRepository.count();
        reservationRepository.deleteAll();
        log.info("[SeedData] Đã xoá tất cả {} bookings", count);
        return Map.of(
                "status", "SUCCESS",
                "message", "Đã xoá tất cả " + count + " bookings",
                "count", count);
    }

    /**
     * Tạo dữ liệu test cho tính năng báo cáo vi phạm.
     * - Tạo booking CONFIRMED cho user chính (đang ngồi)
     * - Tạo booking CONFIRMED cho nhiều user khác ở cùng zone (ghế xung quanh đỏ)
     * 
     * @param userCode      MSSV của user chính (ví dụ: SL000001)
     * @param neighborCount số user ngồi xung quanh (mặc định 4)
     */
    @Transactional
    public Map<String, Object> seedViolationTestData(String userCode, int neighborCount, boolean sameZone) {
        String seedScope = STUDENT_JOURNEY_SCOPE_PREFIX + userCode + ":violation";
        // 1. Tìm user chính
        Optional<User> mainUserOpt = userRepository.findByUserCode(userCode);
        if (mainUserOpt.isEmpty()) {
            return Map.of("status", "ERROR", "message", "Không tìm thấy user với mã: " + userCode);
        }
        User mainUser = mainUserOpt.get();

        // 2. Lấy tất cả seats, lọc ra các ghế active
        List<SeatEntity> allSeats = seatRepository.findAll();
        if (allSeats.isEmpty()) {
            return Map.of("status", "ERROR", "message", "Không có ghế nào trong hệ thống");
        }

        List<SeatEntity> activeSeats = new ArrayList<>();
        for (SeatEntity seat : allSeats) {
            if (seat.getIsActive()) {
                activeSeats.add(seat);
            }
        }

        List<SeatEntity> zoneSeats;
        String zoneName;

        if (sameZone) {
            // Nhóm seats theo zoneId
            Map<Integer, List<SeatEntity>> seatsByZone = new LinkedHashMap<>();
            for (SeatEntity seat : activeSeats) {
                seatsByZone.computeIfAbsent(seat.getZone().getZoneId(), k -> new ArrayList<>()).add(seat);
            }

            // Tìm zone có đủ ghế (ít nhất neighborCount + 1)
            Map.Entry<Integer, List<SeatEntity>> selectedZone = null;
            for (Map.Entry<Integer, List<SeatEntity>> entry : seatsByZone.entrySet()) {
                if (entry.getValue().size() >= neighborCount + 1) {
                    selectedZone = entry;
                    break;
                }
            }

            if (selectedZone == null) {
                return Map.of("status", "ERROR", "message",
                        "Không tìm thấy zone nào có đủ " + (neighborCount + 1) + " ghế active");
            }

            zoneSeats = selectedZone.getValue();
            zoneName = zoneSeats.get(0).getZone().getZoneName();
        } else {
            if (activeSeats.size() < neighborCount + 1) {
                return Map.of("status", "ERROR", "message",
                        "Hệ thống không có đủ " + (neighborCount + 1) + " ghế active");
            }
            Collections.shuffle(activeSeats);
            zoneSeats = activeSeats;
            zoneName = "Nhiều khu vực khác nhau";
        }

        // 3. Xoá booking cũ của user chính (nếu có) để tránh conflict
        // Không xoá tất cả, chỉ xoá BOOKED/CONFIRMED/PROCESSING chưa kết thúc
        LocalDateTime now = LocalDateTime.now();

        // 4. Tạo booking CONFIRMED cho user chính tại ghế đầu tiên
        SeatEntity mainSeat = zoneSeats.get(0);
        LocalDateTime startTime = now.minusMinutes(30); // Đã bắt đầu 30 phút trước
        LocalDateTime endTime = now.plusHours(2); // Kết thúc sau 2 giờ nữa

        ReservationEntity mainBooking = ReservationEntity.builder()
                .user(mainUser)
                .seat(mainSeat)
                .startTime(startTime)
                .endTime(endTime)
                .confirmedAt(startTime)
                .status("CONFIRMED")
                .build();
        reservationRepository.save(mainBooking);
        recordSeed("RESERVATION", mainBooking.getReservationId(), seedScope);

        // 4b. Tạo AccessLog check-in cho user chính (đã vào thư viện)
        AccessLog mainAccessLog = AccessLog.builder()
                .user(mainUser)
                .checkInTime(startTime)
                .checkOutTime(null) // Đang trong thư viện
                .deviceId(DEFAULT_SEED_GATE_DEVICE_ID)
                .reservationId(mainBooking.getReservationId())
                .build();
        accessLogRepository.save(mainAccessLog);
        recordSeed("ACCESS_LOG", mainAccessLog.getLogId(), seedScope);

        // Ghi activity log
        activityLogRepository.save(ActivityLogEntity.builder()
                .userId(mainUser.getId())
                .activityType(ActivityLogEntity.TYPE_CHECK_IN)
                .title("Check-in thành công")
                .description("Bạn đã vào thư viện qua cổng chính.")
                .build());

        // Broadcast websocket
        Map<String, Object> wsMsg = new HashMap<>();
        wsMsg.put("type", "CHECK_IN");
        wsMsg.put("userId", mainUser.getId().toString());
        wsMsg.put("fullName", mainUser.getFullName());
        wsMsg.put("userCode", mainUser.getUserCode());
        wsMsg.put("deviceId", DEFAULT_SEED_GATE_DEVICE_ID);
        wsMsg.put("time", startTime.toString());
        wsMsg.put("checkInTime", startTime.toString());
        messagingTemplate.convertAndSend("/topic/access-logs", wsMsg);
        messagingTemplate.convertAndSend("/topic/dashboard",
                Map.of("type", "CHECKIN_UPDATE", "action", "CHECK_IN", "timestamp",
                        java.time.Instant.now().toString()));

        // 5. Tạo bookings CONFIRMED cho các user khác ở ghế xung quanh
        List<User> allUsers = userRepository.findAll();
        // Loại bỏ user chính
        allUsers.removeIf(u -> u.getId().equals(mainUser.getId()));

        if (allUsers.isEmpty()) {
            return Map.of("status", "ERROR", "message", "Cần thêm users khác ngoài user chính");
        }

        Random rng = new Random();
        int actualNeighbors = Math.min(neighborCount, Math.min(zoneSeats.size() - 1, allUsers.size()));
        List<Map<String, String>> neighborInfo = new ArrayList<>();
        int accessLogCount = 1; // Đã tạo 1 cho user chính

        for (int i = 0; i < actualNeighbors; i++) {
            User neighbor = allUsers.get(i % allUsers.size());
            SeatEntity neighborSeat = zoneSeats.get(i + 1); // Bỏ qua ghế 0 (của user chính)

            // Thời gian hơi khác nhau cho mỗi neighbor
            LocalDateTime nStart = now.minusMinutes(10 + rng.nextInt(60));
            LocalDateTime nEnd = now.plusHours(1 + rng.nextInt(3));

            ReservationEntity neighborBooking = ReservationEntity.builder()
                    .user(neighbor)
                    .seat(neighborSeat)
                    .startTime(nStart)
                    .endTime(nEnd)
                    .confirmedAt(nStart)
                    .status("CONFIRMED")
                    .build();
            reservationRepository.save(neighborBooking);
            recordSeed("RESERVATION", neighborBooking.getReservationId(), seedScope);

            // 5b. Tạo AccessLog check-in cho neighbor (đã vào thư viện)
            AccessLog neighborAccessLog = AccessLog.builder()
                    .user(neighbor)
                    .checkInTime(nStart)
                    .checkOutTime(null) // Đang trong thư viện
                    .deviceId(DEFAULT_SEED_GATE_DEVICE_ID)
                    .reservationId(neighborBooking.getReservationId())
                    .build();
            accessLogRepository.save(neighborAccessLog);
            recordSeed("ACCESS_LOG", neighborAccessLog.getLogId(), seedScope);

            // Ghi activity log
            activityLogRepository.save(ActivityLogEntity.builder()
                    .userId(neighbor.getId())
                    .activityType(ActivityLogEntity.TYPE_CHECK_IN)
                    .title("Check-in thành công")
                    .description("Bạn đã vào thư viện qua cổng chính.")
                    .build());

            // Broadcast websocket
            Map<String, Object> neighWsMsg = new HashMap<>();
            neighWsMsg.put("type", "CHECK_IN");
            neighWsMsg.put("userId", neighbor.getId().toString());
            neighWsMsg.put("fullName", neighbor.getFullName());
            neighWsMsg.put("userCode", neighbor.getUserCode());
            neighWsMsg.put("deviceId", DEFAULT_SEED_GATE_DEVICE_ID);
            neighWsMsg.put("time", nStart.toString());
            neighWsMsg.put("checkInTime", nStart.toString());
            messagingTemplate.convertAndSend("/topic/access-logs", neighWsMsg);
            messagingTemplate.convertAndSend("/topic/dashboard",
                    Map.of("type", "CHECKIN_UPDATE", "action", "CHECK_IN", "timestamp",
                            java.time.Instant.now().toString()));

            accessLogCount++;

            neighborInfo.add(Map.of(
                    "user", neighbor.getFullName() + " (" + neighbor.getUserCode() + ")",
                    "seat",
                    neighborSeat.getSeatCode() + (sameZone ? "" : " - " + neighborSeat.getZone().getZoneName())));
        }

        log.info("[SeedData] Tạo violation test data: user {} tại ghế {}, {} neighbors tại zone {}, {} access logs",
                userCode, mainSeat.getSeatCode(), actualNeighbors, zoneName, accessLogCount);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "SUCCESS");
        result.put("message", String.format(
                "User %s ngồi ghế %s, %d người khác ngồi xung quanh tại zone '%s'. Đã tạo %d access logs (check-in).",
                userCode, mainSeat.getSeatCode(), actualNeighbors, zoneName, accessLogCount));
        result.put("mainUser", Map.of(
                "userCode", userCode,
                "seatCode", mainSeat.getSeatCode(),
                "zone", zoneName,
                "status", "CONFIRMED",
                "startTime", startTime.toString(),
                "endTime", endTime.toString()));
        result.put("neighbors", neighborInfo);
        result.put("accessLogCount", accessLogCount);

        return result;
    }

    /**
     * Tạo dữ liệu test cho tính năng reminder (sau 15 phút)
     */
    @Transactional
    public Map<String, Object> seedReminderTestData(String userCode) {
        String seedScope = STUDENT_JOURNEY_SCOPE_PREFIX + userCode + ":reminder";
        Optional<User> mainUserOpt = userRepository.findByUserCode(userCode);
        if (mainUserOpt.isEmpty()) {
            return Map.of("status", "ERROR", "message", "Không tìm thấy user với mã: " + userCode);
        }
        User mainUser = mainUserOpt.get();

        List<SeatEntity> allSeats = seatRepository.findAll();
        if (allSeats.isEmpty()) {
            return Map.of("status", "ERROR", "message", "Không có ghế nào trong hệ thống");
        }
        SeatEntity seat = allSeats.get(0);

        LocalDateTime now = LocalDateTime.now();
        // Set startTime to 15.5 minutes from now to ensure the scheduler picks it up in
        // the next run
        LocalDateTime startTime = now.plusMinutes(15).plusSeconds(30);
        LocalDateTime endTime = startTime.plusHours(2);

        ReservationEntity booking = ReservationEntity.builder()
                .user(mainUser)
                .seat(seat)
                .startTime(startTime)
                .endTime(endTime)
                .confirmedAt(startTime)
                .status("CONFIRMED")
                .build();
        reservationRepository.save(booking);
        recordSeed("RESERVATION", booking.getReservationId(), seedScope);

        log.info("[SeedData] Tạo reminder test data: user {} tại ghế {}, startTime={}",
                userCode, seat.getSeatCode(), startTime);

        return Map.of(
                "status", "SUCCESS",
                "message", String.format("Đã tạo booking lúc %s cho user %s", startTime.toString(), userCode),
                "startTime", startTime.toString(),
                "seatCode", seat.getSeatCode());
    }

    /**
     * Tạo hoặc ép một booking hiện tại sang trạng thái đang ngồi (CONFIRMED)
     * để demo mobile/web realtime ngay lập tức.
     */
    @Transactional
    public Map<String, Object> seedActiveBookingTestData(String userCode) {
        String normalizedUserCode = userCode.trim().toUpperCase(Locale.ROOT);
        String seedScope = STUDENT_JOURNEY_SCOPE_PREFIX + normalizedUserCode + ":active-booking";

        User mainUser = ensureStudentUser(normalizedUserCode, seedScope, Math.abs(normalizedUserCode.hashCode()));
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startTime = now.minusMinutes(18);
        LocalDateTime endTime = now.plusHours(1).plusMinutes(42);

        ReservationEntity activeReservation = reservationRepository.findByUserId(mainUser.getId()).stream()
                .filter(reservation -> isActiveReservationStatus(reservation.getStatus()))
                .filter(reservation -> overlapsNow(reservation, now) || "BOOKED".equalsIgnoreCase(reservation.getStatus())
                        || "PROCESSING".equalsIgnoreCase(reservation.getStatus()))
                .sorted(Comparator.comparing(ReservationEntity::getCreatedAt, Comparator.nullsLast(LocalDateTime::compareTo)).reversed())
                .findFirst()
                .orElse(null);

        if (activeReservation == null) {
            SeatEntity seat = getActiveSeats().stream()
                    .filter(candidate -> candidate.getReservation().stream()
                            .noneMatch(existing -> isActiveReservationStatus(existing.getStatus()) && overlapsNow(existing, now)))
                    .findFirst()
                    .orElse(null);

            if (seat == null) {
                return Map.of("status", "ERROR", "message", "Không còn ghế trống để tạo đặt chỗ đang ngồi.");
            }

            activeReservation = ReservationEntity.builder()
                    .user(mainUser)
                    .seat(seat)
                    .startTime(startTime)
                    .endTime(endTime)
                    .confirmedAt(startTime)
                    .status("CONFIRMED")
                    .build();
        } else {
            activeReservation.setStartTime(startTime);
            activeReservation.setEndTime(endTime);
            activeReservation.setConfirmedAt(startTime);
            activeReservation.setStatus("CONFIRMED");
            activeReservation.setActualEndTime(null);
            activeReservation.setCancellationReason(null);
            activeReservation.setCancelledByUserId(null);
        }

        ReservationEntity savedReservation = reservationRepository.save(activeReservation);
        recordSeed("RESERVATION", savedReservation.getReservationId(), seedScope);

        AccessLog accessLog = AccessLog.builder()
                .user(mainUser)
                .checkInTime(startTime)
                .checkOutTime(null)
                .deviceId(DEFAULT_SEED_GATE_DEVICE_ID)
                .reservationId(savedReservation.getReservationId())
                .build();
        accessLogRepository.save(accessLog);
        recordSeed("ACCESS_LOG", accessLog.getLogId(), seedScope);

        ActivityLogEntity activity = activityLogRepository.save(ActivityLogEntity.builder()
                .userId(mainUser.getId())
                .activityType(ActivityLogEntity.TYPE_CHECK_IN)
                .title("Check-in thành công")
                .description("Bạn đang ngồi học tại ghế " + savedReservation.getSeat().getSeatCode() + ".")
                .seatCode(savedReservation.getSeat().getSeatCode())
                .zoneName(savedReservation.getSeat().getZone() != null ? savedReservation.getSeat().getZone().getZoneName() : "")
                .reservationId(savedReservation.getReservationId())
                .build());
        recordSeed("ACTIVITY_LOG", activity.getId(), seedScope);

        seatStatusSyncService.broadcastSeatUpdateWithTimeSlot(
                savedReservation.getSeat(),
                "CONFIRMED",
                savedReservation.getStartTime(),
                savedReservation.getEndTime());

        Map<String, Object> wsMsg = new HashMap<>();
        wsMsg.put("type", "CHECK_IN");
        wsMsg.put("userId", mainUser.getId().toString());
        wsMsg.put("fullName", mainUser.getFullName());
        wsMsg.put("userCode", mainUser.getUserCode());
        wsMsg.put("deviceId", DEFAULT_SEED_GATE_DEVICE_ID);
        wsMsg.put("time", startTime.toString());
        wsMsg.put("checkInTime", startTime.toString());
        messagingTemplate.convertAndSend("/topic/access-logs", wsMsg);
        messagingTemplate.convertAndSend("/topic/dashboard",
                Map.of("type", "CHECKIN_UPDATE", "action", "CHECK_IN", "timestamp",
                        java.time.Instant.now().toString()));

        return Map.of(
                "status", "SUCCESS",
                "message", "Đã tạo đặt chỗ đang ngồi cho user " + normalizedUserCode,
                "userCode", normalizedUserCode,
                "seatCode", savedReservation.getSeat().getSeatCode(),
                "zoneName", savedReservation.getSeat().getZone() != null ? savedReservation.getSeat().getZone().getZoneName() : "",
                "bookingStatus", savedReservation.getStatus(),
                "startTime", savedReservation.getStartTime().toString(),
                "endTime", savedReservation.getEndTime().toString());
    }
}
