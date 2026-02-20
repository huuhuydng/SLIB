package slib.com.example.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import slib.com.example.entity.booking.ReservationEntity;
import slib.com.example.entity.complaint.ComplaintEntity;
import slib.com.example.entity.feedback.FeedbackEntity;
import slib.com.example.entity.feedback.SeatViolationReportEntity;
import slib.com.example.entity.hce.AccessLog;
import slib.com.example.entity.news.News;
import slib.com.example.entity.support.SupportRequest;
import slib.com.example.entity.support.SupportRequestStatus;
import slib.com.example.entity.users.User;
import slib.com.example.entity.zone_config.SeatEntity;
import slib.com.example.repository.*;
import slib.com.example.repository.support.SupportRequestRepository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

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
    private final SupportRequestRepository supportRequestRepository;
    private final AccessLogRepository accessLogRepository;
    private final ComplaintRepository complaintRepository;
    private final FeedbackRepository feedbackRepository;

    // Marker để nhận diện dữ liệu seed (dùng để xoá)
    private static final String SEED_MARKER = "[SEED]";

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
        String[] pastStatuses = { "EXPIRED", "CANCEL" };
        // Trạng thái cho booking tương lai (chưa kết thúc) - scheduler sẽ không đổi
        String[] futureStatuses = { "BOOKED", "CONFIRMED" };

        List<UUID> createdIds = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        Random rng = new Random();

        // Đảm bảo ít nhất 1 booking cho mỗi trạng thái
        String[] guaranteedStatuses = { "PROCESSING", "BOOKED", "CONFIRMED", "CANCEL", "EXPIRED" };
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
        result.put("status", "SUCCESS");
        result.put("message", String.format(
                "Đã tạo 50 access logs, %d bookings, %d violations, %d supports, 5 complaints, 8 feedbacks",
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

        // Xoá access logs có marker
        List<AccessLog> allLogs = accessLogRepository.findAllOrderByCheckInTimeDesc();
        for (AccessLog al : allLogs) {
            if (al.getDeviceId() != null && al.getDeviceId().startsWith(SEED_MARKER)) {
                accessLogRepository.delete(al);
                accessLogsDeleted++;
            }
        }

        log.info("[SeedData] Đã xoá {} violations, {} supports, {} complaints, {} feedbacks, {} access logs",
                violationsDeleted, supportDeleted, complaintsDeleted, feedbacksDeleted, accessLogsDeleted);
        return Map.of(
                "status", "SUCCESS",
                "message", String.format(
                        "Đã xoá %d violations, %d supports, %d complaints, %d feedbacks, %d access logs",
                        violationsDeleted, supportDeleted, complaintsDeleted, feedbacksDeleted, accessLogsDeleted));
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
}
