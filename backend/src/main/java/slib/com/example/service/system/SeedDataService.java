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
import slib.com.example.entity.news.News;
import slib.com.example.entity.users.Role;
import slib.com.example.entity.support.SupportRequest;
import slib.com.example.entity.support.SupportRequestStatus;
import slib.com.example.entity.users.User;
import slib.com.example.entity.zone_config.SeatEntity;
import slib.com.example.repository.activity.ActivityLogRepository;
import slib.com.example.repository.booking.ReservationRepository;
import slib.com.example.repository.complaint.ComplaintRepository;
import slib.com.example.repository.feedback.FeedbackRepository;
import slib.com.example.repository.feedback.SeatStatusReportRepository;
import slib.com.example.repository.feedback.SeatViolationReportRepository;
import slib.com.example.repository.hce.AccessLogRepository;
import slib.com.example.repository.news.NewsRepository;
import slib.com.example.repository.support.SupportRequestRepository;
import slib.com.example.repository.users.UserRepository;
import slib.com.example.repository.zone_config.SeatRepository;

import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
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
    private final SimpMessagingTemplate messagingTemplate;

    // Marker để nhận diện dữ liệu seed (dùng để xoá)
    private static final String SEED_MARKER = "[SEED]";
    private static final int MIN_SEAT_STATUS_REPORTS = 8;
    private static final List<SeatStatusReportSeedTemplate> SEAT_STATUS_REPORT_SEED_TEMPLATES = List.of(
            new SeatStatusReportSeedTemplate(
                    SeatStatusReportEntity.IssueType.BROKEN,
                    SeatStatusReportEntity.ReportStatus.PENDING,
                    SEED_MARKER + " Ghế %s bị lỏng chân, sinh viên ngồi không vững và cần kiểm tra sớm",
                    "https://placehold.co/1200x800/png?text=SLIB+Broken+Seat"),
            new SeatStatusReportSeedTemplate(
                    SeatStatusReportEntity.IssueType.DIRTY,
                    SeatStatusReportEntity.ReportStatus.PENDING,
                    SEED_MARKER + " Ghế %s có nhiều vết bẩn và bụi trên mặt ngồi, cần vệ sinh trước ca tiếp theo",
                    null),
            new SeatStatusReportSeedTemplate(
                    SeatStatusReportEntity.IssueType.MISSING_EQUIPMENT,
                    SeatStatusReportEntity.ReportStatus.PENDING,
                    SEED_MARKER + " Vị trí %s thiếu ổ cắm điện bên cạnh, ảnh hưởng đến việc học nhóm",
                    "https://placehold.co/1200x800/png?text=SLIB+Missing+Equipment"),
            new SeatStatusReportSeedTemplate(
                    SeatStatusReportEntity.IssueType.OTHER,
                    SeatStatusReportEntity.ReportStatus.PENDING,
                    SEED_MARKER + " Khu vực quanh ghế %s có tiếng kêu lớn từ bàn đi kèm khi sử dụng",
                    null),
            new SeatStatusReportSeedTemplate(
                    SeatStatusReportEntity.IssueType.BROKEN,
                    SeatStatusReportEntity.ReportStatus.VERIFIED,
                    SEED_MARKER + " Ghế %s bị nứt tay vịn, thủ thư đã xác minh và chờ bộ phận kỹ thuật xử lý",
                    null),
            new SeatStatusReportSeedTemplate(
                    SeatStatusReportEntity.IssueType.DIRTY,
                    SeatStatusReportEntity.ReportStatus.VERIFIED,
                    SEED_MARKER + " Ghế %s bám mực và bụi lâu ngày, đã xác minh để lên lịch vệ sinh",
                    "https://placehold.co/1200x800/png?text=SLIB+Dirty+Seat"),
            new SeatStatusReportSeedTemplate(
                    SeatStatusReportEntity.IssueType.MISSING_EQUIPMENT,
                    SeatStatusReportEntity.ReportStatus.RESOLVED,
                    SEED_MARKER + " Ghế %s từng thiếu đèn đọc sách, thư viện đã bổ sung đầy đủ",
                    null),
            new SeatStatusReportSeedTemplate(
                    SeatStatusReportEntity.IssueType.OTHER,
                    SeatStatusReportEntity.ReportStatus.REJECTED,
                    SEED_MARKER + " Sinh viên phản ánh ghế %s khó ngồi, nhưng kiểm tra thực tế không phát hiện bất thường",
                    null));

    /**
     * Tạo dữ liệu mẫu booking (đặt chỗ) - trải đều 7 ngày
     */
    @Transactional
    public Map<String, Object> seedBookings(int count) {
        List<User> users = userRepository.findAll();
        List<SeatEntity> seats = seatRepository.findAll();

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
                    .status(status)
                    .build();

            reservationRepository.save(res);
            createdIds.add(res.getReservationId());
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
        List<User> users = userRepository.findAll();

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
                    .deviceId(SEED_MARKER + "-device-" + (rng.nextInt(5) + 1))
                    .build();

            accessLogRepository.save(accessLog);
            createdIds.add(accessLog.getLogId());
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
        List<User> users = userRepository.findAll();
        List<SeatEntity> seats = seatRepository.findAll();

        if (users.isEmpty() || seats.isEmpty()) {
            return Map.of("status", "ERROR", "message", "Cần có users và seats trước khi seed violations");
        }

        SeatViolationReportEntity.ViolationType[] types = SeatViolationReportEntity.ViolationType.values();
        SeatViolationReportEntity.ReportStatus[] reportStatuses = SeatViolationReportEntity.ReportStatus.values();
        String[] descriptions = {
                SEED_MARKER + " Sinh viên để đồ chiếm chỗ không sử dụng",
                SEED_MARKER + " Nói chuyện ồn ào trong khu yên tĩnh",
                SEED_MARKER + " Ăn uống tại bàn học",
                SEED_MARKER + " Ngủ trong thư viện quá 30 phút",
                SEED_MARKER + " Sử dụng chỗ ngồi mà không đặt trước",
                SEED_MARKER + " Để chân lên ghế"
        };
        Random rng = new Random();
        List<UUID> createdIds = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            User reporter = users.get(rng.nextInt(users.size()));
            User violator = users.get(rng.nextInt(users.size()));
            SeatEntity seat = seats.get(rng.nextInt(seats.size()));

            SeatViolationReportEntity report = SeatViolationReportEntity.builder()
                    .reporter(reporter)
                    .violator(violator)
                    .seat(seat)
                    .violationType(types[rng.nextInt(types.length)])
                    .description(descriptions[rng.nextInt(descriptions.length)])
                    .status(reportStatuses[rng.nextInt(reportStatuses.length)])
                    .build();

            violationReportRepository.save(report);
            createdIds.add(report.getId());
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
        List<User> users = userRepository.findAll();

        if (users.isEmpty()) {
            return Map.of("status", "ERROR", "message", "Cần có users trước khi seed support requests");
        }

        String[] descriptions = {
                SEED_MARKER + " WiFi khu vực tầng 2 bị chập chờn, không thể kết nối ổn định",
                SEED_MARKER + " Điều hoà khu B không hoạt động, rất nóng",
                SEED_MARKER + " Ổ cắm điện tại bàn A3-05 bị hỏng",
                SEED_MARKER + " Đèn bàn không sáng tại khu đọc sách",
                SEED_MARKER + " Ghế ngồi bị gãy chân tại vị trí B2-12",
                SEED_MARKER + " Máy tính công cộng số 3 bị treo liên tục",
                SEED_MARKER + " Cần thêm ổ cắm sạc tại khu vực tầng 3",
                SEED_MARKER + " Nhà vệ sinh tầng 1 cần vệ sinh gấp"
        };
        SupportRequestStatus[] statuses = SupportRequestStatus.values();
        Random rng = new Random();
        List<UUID> createdIds = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            User student = users.get(rng.nextInt(users.size()));

            SupportRequest req = SupportRequest.builder()
                    .student(student)
                    .description(descriptions[rng.nextInt(descriptions.length)])
                    .status(statuses[rng.nextInt(statuses.length)])
                    .build();

            supportRequestRepository.save(req);
            createdIds.add(req.getId());
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
        List<User> users = userRepository.findAll();

        if (users.isEmpty()) {
            return Map.of("status", "ERROR", "message", "Cần có users trước khi seed complaints");
        }

        String[] subjects = {
                SEED_MARKER + " Bị trừ điểm oan do hệ thống lỗi",
                SEED_MARKER + " Không đồng ý với báo cáo vi phạm",
                SEED_MARKER + " Điểm uy tín bị trừ sai ngày 15/02",
                SEED_MARKER + " Yêu cầu xem lại vi phạm ăn uống",
                SEED_MARKER + " Không có mặt tại ghế nhưng vẫn bị báo cáo"
        };
        String[] contents = {
                SEED_MARKER
                        + " Tôi đã check-in đúng giờ nhưng hệ thống ghi nhận muộn 5 phút, dẫn đến bị trừ điểm. Xin xem lại.",
                SEED_MARKER
                        + " Tôi chỉ uống nước lọc, không phải ăn uống tại bàn. Báo cáo vi phạm này là không chính xác.",
                SEED_MARKER + " Tôi bị trừ 10 điểm uy tín vào ngày 15/02 nhưng hôm đó tôi không có mặt tại thư viện.",
                SEED_MARKER + " Người báo cáo nhầm người vi phạm. Tôi ngồi bàn bên cạnh, không phải người gây ồn.",
                SEED_MARKER + " Tôi đã rời ghế để đi vệ sinh trong 5 phút, không phải bỏ đồ chiếm chỗ."
        };
        ComplaintEntity.ComplaintStatus[] statuses = ComplaintEntity.ComplaintStatus.values();
        Random rng = new Random();
        List<UUID> createdIds = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            User user = users.get(rng.nextInt(users.size()));

            ComplaintEntity complaint = ComplaintEntity.builder()
                    .user(user)
                    .subject(subjects[rng.nextInt(subjects.length)])
                    .content(contents[rng.nextInt(contents.length)])
                    .status(statuses[rng.nextInt(statuses.length)])
                    .build();

            complaintRepository.save(complaint);
            createdIds.add(complaint.getId());
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
        List<User> users = userRepository.findAll();

        if (users.isEmpty()) {
            return Map.of("status", "ERROR", "message", "Cần có users trước khi seed feedbacks");
        }

        String[] contents = {
                SEED_MARKER + " Thư viện rất yên tĩnh, phù hợp để học tập tập trung",
                SEED_MARKER + " WiFi nhanh và ổn định, rất hài lòng",
                SEED_MARKER + " Ghế ngồi khá cứng, nên đầu tư ghế mới",
                SEED_MARKER + " Điều hoà hơi lạnh quá, nên chỉnh nhiệt độ cao hơn",
                SEED_MARKER + " Nhân viên thư viện rất nhiệt tình hỗ trợ",
                SEED_MARKER + " Khu vực đọc sách rất đẹp, ánh sáng tốt",
                SEED_MARKER + " App đặt chỗ dễ dùng, nhanh chóng",
                SEED_MARKER + " Nên mở cửa thư viện sớm hơn vào cuối tuần"
        };
        String[] categories = { "FACILITY", "SERVICE", "GENERAL" };
        FeedbackEntity.FeedbackStatus[] statuses = FeedbackEntity.FeedbackStatus.values();
        Random rng = new Random();
        List<UUID> createdIds = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            User user = users.get(rng.nextInt(users.size()));

            FeedbackEntity feedback = FeedbackEntity.builder()
                    .user(user)
                    .rating(1 + rng.nextInt(5)) // 1-5 sao
                    .content(contents[rng.nextInt(contents.length)])
                    .category(categories[rng.nextInt(categories.length)])
                    .status(statuses[rng.nextInt(statuses.length)])
                    .build();

            feedbackRepository.save(feedback);
            createdIds.add(feedback.getId());
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

    /**
     * Tạo tất cả dữ liệu mẫu cùng lúc (bao gồm dashboard data)
     */
    @Transactional
    public Map<String, Object> seedAll(int bookingCount, int violationCount, int supportCount) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("accessLogs", seedAccessLogs(50)); // 50 access logs trải 7 ngày
        result.put("bookings", seedBookings(bookingCount));
        result.put("violations", seedViolations(violationCount));
        result.put("supportRequests", seedSupportRequests(supportCount));
        result.put("complaints", seedComplaints(5)); // 5 khiếu nại
        result.put("feedbacks", seedFeedbacks(8)); // 8 phản hồi
        result.put("seatStatusReports", seedSeatStatusReports(8));
        result.put("status", "SUCCESS");
        result.put("message", String.format(
                "Đã tạo 50 access logs, %d bookings, %d violations, %d supports, 5 complaints, 8 feedbacks, 8 seat status reports",
                bookingCount, violationCount, supportCount));
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

        // Xoá violations có marker
        List<SeatViolationReportEntity> violations = violationReportRepository.findAllByOrderByCreatedAtDesc();
        for (SeatViolationReportEntity v : violations) {
            if (v.getDescription() != null && v.getDescription().startsWith(SEED_MARKER)) {
                violationReportRepository.delete(v);
                violationsDeleted++;
            }
        }

        // Xoá support requests có marker
        List<SupportRequest> supports = supportRequestRepository.findAllByOrderByCreatedAtDesc();
        for (SupportRequest s : supports) {
            if (s.getDescription() != null && s.getDescription().startsWith(SEED_MARKER)) {
                supportRequestRepository.delete(s);
                supportDeleted++;
            }
        }

        // Xoá complaints có marker
        List<ComplaintEntity> complaints = complaintRepository.findAllByOrderByCreatedAtDesc();
        for (ComplaintEntity c : complaints) {
            if (c.getSubject() != null && c.getSubject().startsWith(SEED_MARKER)) {
                complaintRepository.delete(c);
                complaintsDeleted++;
            }
        }

        // Xoá feedbacks có marker
        List<FeedbackEntity> feedbacks = feedbackRepository.findAllByOrderByCreatedAtDesc();
        for (FeedbackEntity f : feedbacks) {
            if (f.getContent() != null && f.getContent().startsWith(SEED_MARKER)) {
                feedbackRepository.delete(f);
                feedbacksDeleted++;
            }
        }

        List<SeatStatusReportEntity> seatStatusReports = seatStatusReportRepository.findAllByOrderByCreatedAtDesc();
        for (SeatStatusReportEntity report : seatStatusReports) {
            if (report.getDescription() != null && report.getDescription().startsWith(SEED_MARKER)) {
                seatStatusReportRepository.delete(report);
                seatStatusReportsDeleted++;
            }
        }

        // Xoá access logs có marker
        List<AccessLog> allLogs = accessLogRepository.findAllOrderByCheckInTimeDesc();
        for (AccessLog al : allLogs) {
            if (al.getDeviceId() != null && al.getDeviceId().startsWith(SEED_MARKER)) {
                accessLogRepository.delete(al);
                accessLogsDeleted++;
            }
        }

        log.info("[SeedData] Đã xoá {} violations, {} supports, {} complaints, {} feedbacks, {} seat-status reports, {} access logs",
                violationsDeleted, supportDeleted, complaintsDeleted, feedbacksDeleted, seatStatusReportsDeleted, accessLogsDeleted);
        return Map.of(
                "status", "SUCCESS",
                "message", String.format(
                        "Đã xoá %d violations, %d supports, %d complaints, %d feedbacks, %d seat status reports, %d access logs",
                        violationsDeleted, supportDeleted, complaintsDeleted, feedbacksDeleted, seatStatusReportsDeleted, accessLogsDeleted));
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
                .status("CONFIRMED")
                .build();
        reservationRepository.save(mainBooking);

        // 4b. Tạo AccessLog check-in cho user chính (đã vào thư viện)
        AccessLog mainAccessLog = AccessLog.builder()
                .user(mainUser)
                .checkInTime(startTime)
                .checkOutTime(null) // Đang trong thư viện
                .deviceId(SEED_MARKER + "-violation-test")
                .reservationId(mainBooking.getReservationId())
                .build();
        accessLogRepository.save(mainAccessLog);

        // Ghi activity log
        activityLogRepository.save(ActivityLogEntity.builder()
                .userId(mainUser.getId())
                .activityType(ActivityLogEntity.TYPE_CHECK_IN)
                .title("Check-in thành công")
                .description("Bạn đã vào thư viện tại " + SEED_MARKER + "-violation-test")
                .build());

        // Broadcast websocket
        Map<String, Object> wsMsg = new HashMap<>();
        wsMsg.put("type", "CHECK_IN");
        wsMsg.put("userId", mainUser.getId().toString());
        wsMsg.put("fullName", mainUser.getFullName());
        wsMsg.put("userCode", mainUser.getUserCode());
        wsMsg.put("deviceId", SEED_MARKER + "-violation-test");
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
                    .status("CONFIRMED")
                    .build();
            reservationRepository.save(neighborBooking);

            // 5b. Tạo AccessLog check-in cho neighbor (đã vào thư viện)
            AccessLog neighborAccessLog = AccessLog.builder()
                    .user(neighbor)
                    .checkInTime(nStart)
                    .checkOutTime(null) // Đang trong thư viện
                    .deviceId(SEED_MARKER + "-violation-test")
                    .reservationId(neighborBooking.getReservationId())
                    .build();
            accessLogRepository.save(neighborAccessLog);

            // Ghi activity log
            activityLogRepository.save(ActivityLogEntity.builder()
                    .userId(neighbor.getId())
                    .activityType(ActivityLogEntity.TYPE_CHECK_IN)
                    .title("Check-in thành công")
                    .description("Bạn đã vào thư viện tại " + SEED_MARKER + "-violation-test")
                    .build());

            // Broadcast websocket
            Map<String, Object> neighWsMsg = new HashMap<>();
            neighWsMsg.put("type", "CHECK_IN");
            neighWsMsg.put("userId", neighbor.getId().toString());
            neighWsMsg.put("fullName", neighbor.getFullName());
            neighWsMsg.put("userCode", neighbor.getUserCode());
            neighWsMsg.put("deviceId", SEED_MARKER + "-violation-test");
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
                .status("CONFIRMED")
                .build();
        reservationRepository.save(booking);

        log.info("[SeedData] Tạo reminder test data: user {} tại ghế {}, startTime={}",
                userCode, seat.getSeatCode(), startTime);

        return Map.of(
                "status", "SUCCESS",
                "message", String.format("Đã tạo booking lúc %s cho user %s", startTime.toString(), userCode),
                "startTime", startTime.toString(),
                "seatCode", seat.getSeatCode());
    }
}
