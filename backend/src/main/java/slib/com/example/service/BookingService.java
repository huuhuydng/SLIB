package slib.com.example.service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import slib.com.example.exception.BadRequestException;
import slib.com.example.entity.LibrarySetting;
import slib.com.example.entity.activity.ActivityLogEntity;
import slib.com.example.entity.notification.NotificationEntity.NotificationType;
import slib.com.example.entity.users.StudentProfile;
import slib.com.example.entity.users.User;
import slib.com.example.entity.zone_config.SeatEntity;
import slib.com.example.entity.zone_config.SeatStatus;
import slib.com.example.entity.zone_config.ZoneEntity;
import slib.com.example.dto.booking.BookingResponse;
import slib.com.example.dto.zone_config.SeatDTO;
import slib.com.example.entity.booking.ReservationEntity;
import slib.com.example.repository.ReservationRepository;
import slib.com.example.repository.SeatRepository;
import slib.com.example.repository.StudentProfileRepository;
import slib.com.example.repository.UserRepository;
import slib.com.example.repository.ZoneRepository;
import slib.com.example.service.ReputationService;

@Service
public class BookingService {
        private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

        private final ReservationRepository reservationRepository;
        private final UserRepository userRepository;
        private final SeatRepository seatRepository;
        private final ZoneRepository zoneRepository;
        private final SeatStatusSyncService seatStatusSyncService;
        private final LibrarySettingService librarySettingService;
        private final SeatAvailabilityService seatAvailabilityService;
        private final PushNotificationService pushNotificationService;
        private final ActivityService activityService;
        private final SimpMessagingTemplate messagingTemplate;
        private final StudentProfileRepository studentProfileRepository;
        private final ReputationService reputationService;
        private final SeatService seatService;

        public BookingService(ReservationRepository reservationRepository, UserRepository userRepository,
                        SeatRepository seatRepository, ZoneRepository zoneRepository,
                        SeatStatusSyncService seatStatusSyncService, LibrarySettingService librarySettingService,
                        SeatAvailabilityService seatAvailabilityService,
                        PushNotificationService pushNotificationService,
                        ActivityService activityService,
                        SimpMessagingTemplate messagingTemplate,
                        StudentProfileRepository studentProfileRepository,
                        ReputationService reputationService,
                        SeatService seatService) {
                this.reservationRepository = reservationRepository;
                this.userRepository = userRepository;
                this.seatRepository = seatRepository;
                this.zoneRepository = zoneRepository;
                this.seatStatusSyncService = seatStatusSyncService;
                this.librarySettingService = librarySettingService;
                this.seatAvailabilityService = seatAvailabilityService;
                this.pushNotificationService = pushNotificationService;
                this.activityService = activityService;
                this.messagingTemplate = messagingTemplate;
                this.studentProfileRepository = studentProfileRepository;
                this.reputationService = reputationService;
                this.seatService = seatService;
        }

        @Transactional
        public ReservationEntity createBooking(UUID userId, Integer seatId,
                        LocalDateTime startTime, LocalDateTime endTime) {
                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new RuntimeException("User not found"));
                SeatEntity seat = seatRepository.findById(seatId)
                                .orElseThrow(() -> new RuntimeException("Seat not found"));

                // Lấy cấu hình giới hạn đặt chỗ
                LibrarySetting settings = librarySettingService.getSettings();

                // Kiểm tra thư viện có đang tạm đóng không
                if (Boolean.TRUE.equals(settings.getLibraryClosed())) {
                        String reason = settings.getClosedReason() != null
                                        ? settings.getClosedReason()
                                        : "Thư viện hiện đang tạm đóng";
                        throw new RuntimeException("Thư viện hiện đang tạm đóng. Lý do: " + reason);
                }

                // Kiểm tra điểm uy tín tối thiểu
                int minReputation = settings.getMinReputation() != null ? settings.getMinReputation() : 0;
                if (minReputation > 0) {
                        StudentProfile profile = studentProfileRepository.findByUserId(userId).orElse(null);
                        int currentReputation = (profile != null && profile.getReputationScore() != null)
                                        ? profile.getReputationScore()
                                        : 100;
                        if (currentReputation < minReputation) {
                                throw new RuntimeException(
                                                "Điểm uy tín của bạn (" + currentReputation
                                                                + ") thấp hơn mức tối thiểu ("
                                                                + minReputation + ") để đặt chỗ.");
                        }
                }

                LocalDate bookingDate = startTime.toLocalDate();

                // Đếm số lượt đặt của user trong ngày này (chỉ tính BOOKED và PROCESSING)
                List<ReservationEntity> userBookingsToday = reservationRepository.findByUserId(userId).stream()
                                .filter(r -> r.getStartTime().toLocalDate().equals(bookingDate))
                                .filter(r -> r.getStatus().equalsIgnoreCase("BOOKED") ||
                                                r.getStatus().equalsIgnoreCase("PROCESSING"))
                                .toList();

                int bookingsCount = userBookingsToday.size();
                int maxBookingsPerDay = settings.getMaxBookingsPerDay() != null ? settings.getMaxBookingsPerDay() : 3;

                if (bookingsCount >= maxBookingsPerDay) {
                        throw new RuntimeException(
                                        "Bạn đã đạt giới hạn " + maxBookingsPerDay + " lần đặt trong ngày "
                                                        + bookingDate);
                }

                // Tính tổng số giờ đã đặt trong ngày
                long totalMinutesBooked = userBookingsToday.stream()
                                .mapToLong(r -> Duration.between(r.getStartTime(), r.getEndTime()).toMinutes())
                                .sum();
                long newBookingMinutes = Duration.between(startTime, endTime).toMinutes();
                int maxHoursPerDay = settings.getMaxHoursPerDay() != null ? settings.getMaxHoursPerDay() : 4;

                if ((totalMinutesBooked + newBookingMinutes) > maxHoursPerDay * 60) {
                        throw new RuntimeException(
                                        "Bạn đã đạt giới hạn " + maxHoursPerDay + " giờ đặt trong ngày " + bookingDate);
                }

                // Kiểm tra user đã đặt ghế nào trong cùng time slot chưa (chỉ được đặt 1
                // ghế/slot)
                boolean hasBookingInSlot = userBookingsToday.stream()
                                .anyMatch(r -> r.getStartTime().equals(startTime) && r.getEndTime().equals(endTime));

                if (hasBookingInSlot) {
                        throw new RuntimeException(
                                        "Bạn đã đặt ghế trong khung giờ này rồi. Mỗi người chỉ được đặt 1 ghế/khung giờ.");
                }

                // kiểm tra overlap với reservation cùng time slot (BOOKED hoặc PROCESSING)
                boolean isConflict = seat.getReservation().stream()
                                .anyMatch(r -> (r.getStatus().equalsIgnoreCase("BOOKED") ||
                                                r.getStatus().equalsIgnoreCase("PROCESSING")) &&
                                                r.getStartTime().toLocalDate().equals(startTime.toLocalDate()) &&
                                                r.getStartTime().isBefore(endTime) &&
                                                r.getEndTime().isAfter(startTime));

                if (isConflict) {
                        throw new RuntimeException("Ghế đã được đặt hoặc đang chờ xác nhận");
                }

                ReservationEntity reservation = ReservationEntity.builder()
                                .user(user)
                                .seat(seat)
                                .startTime(startTime)
                                .endTime(endTime)
                                .status("PROCESSING")
                                .build();

                // KHÔNG thay đổi seat_status trực tiếp - status được tính động từ reservations
                ReservationEntity saved = reservationRepository.save(reservation);

                // Broadcast to WebSocket clients for real-time updates

                // LUÔN broadcast WebSocket cho tất cả bookings (kể cả future) để clients sync
                // được
                seatStatusSyncService.broadcastSeatUpdateWithTimeSlot(seat, "HOLDING", startTime, endTime);

                // Log activity for BOOKING_SUCCESS
                String zoneName = seat.getZone() != null ? seat.getZone().getZoneName() : "";
                String timeStr = String.format("%02d:%02d - %02d:%02d",
                                startTime.getHour(), startTime.getMinute(),
                                endTime.getHour(), endTime.getMinute());
                try {
                        activityService.logActivity(ActivityLogEntity.builder()
                                        .userId(userId)
                                        .activityType(ActivityLogEntity.TYPE_BOOKING_SUCCESS)
                                        .title("Đặt chỗ thành công")
                                        .description("Đã đặt ghế " + seat.getSeatCode() + " tại " + zoneName + " ("
                                                        + timeStr + ")")
                                        .seatCode(seat.getSeatCode())
                                        .zoneName(zoneName)
                                        .reservationId(saved.getReservationId())
                                        .build());
                } catch (Exception e) {
                        System.err.println("Failed to log activity: " + e.getMessage());
                }

                // Send push notification ngay khi tạo booking (PROCESSING)
                try {
                        String notifTitle = "Đặt chỗ đang chờ xác nhận";
                        String notifBody = String.format(
                                        "Ghế %s tại %s (%s) đang chờ bạn xác nhận. Vui lòng xác nhận trong 5 phút!",
                                        seat.getSeatCode(), zoneName, timeStr);
                        pushNotificationService.sendToUser(userId, notifTitle, notifBody,
                                        NotificationType.BOOKING, saved.getReservationId());
                } catch (Exception e) {
                        System.err.println("Failed to send booking notification: " + e.getMessage());
                }

                // Broadcast dashboard update
                broadcastDashboardUpdate("BOOKING_UPDATE", "CREATED");

                return saved;
        }

        @Transactional(readOnly = true)
        public List<ZoneEntity> getAllZones() {
                return zoneRepository.findAll();
        }

        @Transactional(readOnly = true)
        public List<SeatEntity> getAllSeats(Integer zoneId) {
                return seatRepository.findByZone_ZoneId(zoneId);
        }

        @Transactional(readOnly = true)
        public long countAvailableSeats(Integer zoneId) {
                LocalDate today = LocalDate.now();
                List<SeatEntity> seats = seatRepository.findByZone_ZoneId(zoneId);

                return seats.stream()
                                .filter(seat -> seat.getReservation().stream()
                                                .noneMatch(r -> (r.getStatus().equalsIgnoreCase("BOOKED")
                                                                || r.getStatus().equalsIgnoreCase("PROCESSING"))
                                                                && r.getStartTime().toLocalDate().equals(today)))
                                .count();
        }

        @Transactional(readOnly = true)
        public List<ReservationEntity> getBookingsByUser(UUID userId) {
                return reservationRepository.findByUserId(userId);
        }

        /**
         * Get booking history with full zone/area info for mobile display
         */
        @Transactional(readOnly = true)
        public List<slib.com.example.dto.booking.BookingHistoryResponse> getBookingHistory(UUID userId) {
                return reservationRepository.findByUserId(userId).stream()
                                .map(this::mapToBookingHistoryResponse)
                                .toList();
        }

        private slib.com.example.dto.booking.BookingHistoryResponse mapToBookingHistoryResponse(
                        ReservationEntity reservation) {
                SeatEntity seat = reservation.getSeat();
                ZoneEntity zone = seat.getZone();

                return slib.com.example.dto.booking.BookingHistoryResponse.builder()
                                .reservationId(reservation.getReservationId())
                                .status(reservation.getStatus())
                                .seatId(seat.getSeatId())
                                .seatCode(seat.getSeatCode())
                                .zoneId(zone.getZoneId())
                                .zoneName(zone.getZoneName())
                                .areaId(zone.getArea().getAreaId().intValue())
                                .areaName(zone.getArea().getAreaName())
                                .startTime(reservation.getStartTime())
                                .endTime(reservation.getEndTime())
                                .createdAt(reservation.getCreatedAt())
                                .build();
        }

        /**
         * Get the upcoming or current active booking for a user
         * Returns the first BOOKED or PROCESSING reservation that hasn't ended yet
         */
        @Transactional(readOnly = true)
        public java.util.Optional<slib.com.example.dto.booking.UpcomingBookingResponse> getUpcomingBooking(
                        UUID userId) {
                // Use Vietnam timezone explicitly to ensure consistent time comparison
                LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));

                return reservationRepository.findByUserId(userId).stream()
                                // Only BOOKED, PROCESSING, or CONFIRMED status
                                .filter(r -> r.getStatus().equalsIgnoreCase("BOOKED") ||
                                                r.getStatus().equalsIgnoreCase("PROCESSING") ||
                                                r.getStatus().equalsIgnoreCase("CONFIRMED"))
                                // End time hasn't passed yet (booking still valid)
                                .filter(r -> r.getEndTime().isAfter(now))
                                // Sort by start time (closest first)
                                .sorted((a, b) -> a.getStartTime().compareTo(b.getStartTime()))
                                .findFirst()
                                .map(this::mapToUpcomingBookingResponse);
        }

        private slib.com.example.dto.booking.UpcomingBookingResponse mapToUpcomingBookingResponse(
                        ReservationEntity reservation) {
                SeatEntity seat = reservation.getSeat();
                ZoneEntity zone = seat.getZone();

                // Get Vietnamese day of week
                String[] vietnameseDays = { "CN", "TH 2", "TH 3", "TH 4", "TH 5", "TH 6", "TH 7" };
                int dayIndex = reservation.getStartTime().getDayOfWeek().getValue() % 7;
                String dayOfWeek = vietnameseDays[dayIndex];

                // Format time range
                String timeRange = String.format("%02d:%02d - %02d:%02d",
                                reservation.getStartTime().getHour(),
                                reservation.getStartTime().getMinute(),
                                reservation.getEndTime().getHour(),
                                reservation.getEndTime().getMinute());

                return slib.com.example.dto.booking.UpcomingBookingResponse.builder()
                                .reservationId(reservation.getReservationId())
                                .status(reservation.getStatus())
                                .seatId(seat.getSeatId())
                                .seatCode(seat.getSeatCode())
                                .zoneId(zone.getZoneId())
                                .zoneName(zone.getZoneName())
                                .areaId(zone.getArea().getAreaId().intValue())
                                .areaName(zone.getArea().getAreaName())
                                .floorNumber(null) // AreaEntity doesn't have floorNumber
                                .startTime(reservation.getStartTime())
                                .endTime(reservation.getEndTime())
                                .dayOfWeek(dayOfWeek)
                                .dayOfMonth(reservation.getStartTime().getDayOfMonth())
                                .timeRange(timeRange)
                                .build();
        }

        /**
         * Cancel booking with 12-hour rule validation
         * User must cancel at least 12 hours before start time
         */
        @Transactional
        public ReservationEntity cancelBooking(UUID reservationId) {
                ReservationEntity reservation = reservationRepository.findById(reservationId)
                                .orElseThrow(() -> new RuntimeException("Reservation not found"));

                // Check if reservation is already cancelled or completed
                if ("CANCEL".equalsIgnoreCase(reservation.getStatus()) ||
                                "CANCELLED".equalsIgnoreCase(reservation.getStatus())) {
                        throw new RuntimeException("Đặt chỗ này đã được hủy");
                }
                if ("COMPLETED".equalsIgnoreCase(reservation.getStatus())) {
                        throw new RuntimeException("Không thể hủy đặt chỗ đã hoàn thành");
                }

                // PROCESSING reservations can be cancelled immediately (user is still
                // confirming)
                // Only apply 12-hour rule for BOOKED/CONFIRMED reservations
                if (!"PROCESSING".equalsIgnoreCase(reservation.getStatus())) {
                        // Check 12-hour rule for confirmed bookings
                        LocalDateTime now = LocalDateTime.now(VIETNAM_ZONE);
                        LocalDateTime cancelDeadline = reservation.getStartTime().minusHours(12);

                        if (now.isAfter(cancelDeadline)) {
                                long hoursUntilStart = java.time.Duration.between(now, reservation.getStartTime())
                                                .toHours();
                                throw new RuntimeException(
                                                "Không thể hủy đặt chỗ. Bạn chỉ có thể hủy trước 12 tiếng. " +
                                                                "Còn " + hoursUntilStart
                                                                + " tiếng nữa là đến giờ đặt.");
                        }
                }

                reservation.setStatus("CANCEL");
                ReservationEntity saved = reservationRepository.save(reservation);

                // Log activity for BOOKING_CANCEL
                SeatEntity seat = reservation.getSeat();
                String zoneName = seat.getZone() != null ? seat.getZone().getZoneName() : "";
                try {
                        activityService.logActivity(ActivityLogEntity.builder()
                                        .userId(reservation.getUser().getId())
                                        .activityType(ActivityLogEntity.TYPE_BOOKING_CANCEL)
                                        .title("Hủy đặt chỗ thành công")
                                        .description("Đã hủy ghế " + seat.getSeatCode() + " tại " + zoneName)
                                        .seatCode(seat.getSeatCode())
                                        .zoneName(zoneName)
                                        .reservationId(saved.getReservationId())
                                        .build());
                } catch (Exception e) {
                        System.err.println("Failed to log activity: " + e.getMessage());
                }

                // Broadcast to WebSocket clients for real-time updates
                // (status is calculated dynamically, no DB update needed)

                // Broadcast cho tất cả clients kể cả đang xem future time slots
                seatStatusSyncService.broadcastSeatUpdateWithTimeSlot(reservation.getSeat(), "AVAILABLE",
                                reservation.getStartTime(), reservation.getEndTime());

                // Broadcast dashboard update
                broadcastDashboardUpdate("BOOKING_UPDATE", "CANCELLED");

                return saved;
        }

        /**
         * @deprecated Use {@link #confirmSeatWithNfcUid} instead.
         *             Confirm seat with NFC tag data (legacy format:
         *             SEAT_ID:A01_ZONE:B)
         */
        @Deprecated
        @Transactional
        public ReservationEntity confirmSeatWithNfc(UUID reservationId, String nfcData) {
                ReservationEntity reservation = reservationRepository.findById(reservationId)
                                .orElseThrow(() -> new RuntimeException("Đặt chỗ không tồn tại"));

                // Parse NFC data
                // Expected format: SEAT_ID:A01_ZONE:B
                String[] parts = nfcData.split("_");
                if (parts.length < 2) {
                        throw new RuntimeException("Dữ liệu NFC không hợp lệ");
                }

                String seatCodeFromNfc = parts[0].replace("SEAT_ID:", "");
                String zoneFromNfc = parts[1].replace("ZONE:", "");

                // Validate NFC matches reservation
                String expectedSeatCode = reservation.getSeat().getSeatCode();
                String expectedZoneName = reservation.getSeat().getZone().getZoneName();

                if (!seatCodeFromNfc.equalsIgnoreCase(expectedSeatCode)) {
                        throw new RuntimeException(
                                        "Ghế không khớp! Bạn đang đặt ghế " + expectedSeatCode +
                                                        " nhưng thẻ NFC là ghế " + seatCodeFromNfc);
                }

                // Check if within valid time window (start_time - 15 mins to end_time)
                LocalDateTime now = LocalDateTime.now(VIETNAM_ZONE);
                LocalDateTime checkInStart = reservation.getStartTime().minusMinutes(15);
                LocalDateTime checkInEnd = reservation.getEndTime();

                if (now.isBefore(checkInStart)) {
                        throw new RuntimeException("Chưa đến giờ check-in. Bạn có thể check-in từ " +
                                        checkInStart.toLocalTime().toString());
                }
                if (now.isAfter(checkInEnd)) {
                        throw new RuntimeException("Đã hết thời gian check-in");
                }

                // Update status to CONFIRMED (NFC check-in thực tế)
                reservation.setStatus("CONFIRMED");
                ReservationEntity saved = reservationRepository.save(reservation);

                // Log activity for NFC_CONFIRM
                SeatEntity seat = reservation.getSeat();
                String zoneName = seat.getZone() != null ? seat.getZone().getZoneName() : "";
                try {
                        activityService.logActivity(ActivityLogEntity.builder()
                                        .userId(reservation.getUser().getId())
                                        .activityType(ActivityLogEntity.TYPE_NFC_CONFIRM)
                                        .title("Xác nhận ghế thành công")
                                        .description("Đã xác nhận ghế " + seat.getSeatCode() + " tại " + zoneName
                                                        + " bằng NFC")
                                        .seatCode(seat.getSeatCode())
                                        .zoneName(zoneName)
                                        .reservationId(saved.getReservationId())
                                        .build());
                } catch (Exception e) {
                        System.err.println("Failed to log activity: " + e.getMessage());
                }

                // Thưởng điểm uy tín khi check-in đúng giờ
                try {
                        reputationService.applyCheckInBonus(
                                        reservation.getUser().getId(),
                                        seat.getSeatCode(),
                                        zoneName,
                                        saved.getReservationId());
                } catch (Exception e) {
                        System.err.println("Failed to apply check-in bonus: " + e.getMessage());
                }

                // Broadcast status to WebSocket clients
                seatStatusSyncService.broadcastSeatUpdateWithTimeSlot(reservation.getSeat(), "CONFIRMED",
                                reservation.getStartTime(), reservation.getEndTime());

                // Send push notification khi check-in NFC thành công
                try {
                        String timeStr = String.format("%02d:%02d - %02d:%02d",
                                        reservation.getStartTime().getHour(), reservation.getStartTime().getMinute(),
                                        reservation.getEndTime().getHour(), reservation.getEndTime().getMinute());
                        String notiTitle = "Check-in thành công";
                        String notiBody = String.format(
                                        "Ghế %s tại %s (%s) đã check-in bằng NFC. Chúc bạn học tập hiệu quả!",
                                        seat.getSeatCode(), zoneName, timeStr);
                        pushNotificationService.sendToUser(reservation.getUser().getId(), notiTitle, notiBody,
                                        NotificationType.BOOKING, saved.getReservationId());
                } catch (Exception e) {
                        System.err.println("Failed to send NFC confirmation notification: " + e.getMessage());
                }

                // Broadcast dashboard update
                broadcastDashboardUpdate("BOOKING_UPDATE", "CONFIRMED");

                return saved;
        }

        /**
         * Confirm seat check-in using NFC tag UID (UID Mapping Strategy).
         * Flow: raw UID → backend hashes → resolves seat → validates against
         * reservation → confirms.
         *
         * @param reservationId The booking reservation ID
         * @param rawNfcUid     Raw NFC tag UID from mobile app (uppercase HEX)
         * @return Updated reservation with CONFIRMED status
         */
        @Transactional
        public ReservationEntity confirmSeatWithNfcUid(UUID reservationId, String rawNfcUid) {
                ReservationEntity reservation = reservationRepository.findById(reservationId)
                                .orElseThrow(() -> new RuntimeException("Đặt chỗ không tồn tại"));

                // Step 1: Resolve seat from NFC UID (backend hashes internally)
                SeatEntity nfcSeat = seatService.findSeatEntityByNfcUid(rawNfcUid);

                // Step 2: Validate that NFC tag seat matches reservation seat
                SeatEntity expectedSeat = reservation.getSeat();
                if (!nfcSeat.getSeatId().equals(expectedSeat.getSeatId())) {
                        throw new RuntimeException(
                                        "Ghế không khớp! Bạn đang đặt ghế " + expectedSeat.getSeatCode() +
                                                        " nhưng thẻ NFC là ghế " + nfcSeat.getSeatCode());
                }

                // Step 3: Check time window (start_time - 15 mins to end_time)
                LocalDateTime now = LocalDateTime.now(VIETNAM_ZONE);
                LocalDateTime checkInStart = reservation.getStartTime().minusMinutes(15);
                LocalDateTime checkInEnd = reservation.getEndTime();

                if (now.isBefore(checkInStart)) {
                        throw new RuntimeException("Chưa đến giờ check-in. Bạn có thể check-in từ " +
                                        checkInStart.toLocalTime().toString());
                }
                if (now.isAfter(checkInEnd)) {
                        throw new RuntimeException("Đã hết thời gian check-in");
                }

                // Step 4: Update status to CONFIRMED
                reservation.setStatus("CONFIRMED");
                ReservationEntity saved = reservationRepository.save(reservation);

                // Step 5: Log activity
                SeatEntity seat = reservation.getSeat();
                String zoneName = seat.getZone() != null ? seat.getZone().getZoneName() : "";
                try {
                        activityService.logActivity(ActivityLogEntity.builder()
                                        .userId(reservation.getUser().getId())
                                        .activityType(ActivityLogEntity.TYPE_NFC_CONFIRM)
                                        .title("Xác nhận ghế thành công")
                                        .description("Đã xác nhận ghế " + seat.getSeatCode() + " tại " + zoneName
                                                        + " bằng NFC UID")
                                        .seatCode(seat.getSeatCode())
                                        .zoneName(zoneName)
                                        .reservationId(saved.getReservationId())
                                        .build());
                } catch (Exception e) {
                        System.err.println("Failed to log activity: " + e.getMessage());
                }

                // Step 6: Apply reputation bonus
                try {
                        reputationService.applyCheckInBonus(
                                        reservation.getUser().getId(),
                                        seat.getSeatCode(),
                                        zoneName,
                                        saved.getReservationId());
                } catch (Exception e) {
                        System.err.println("Failed to apply check-in bonus: " + e.getMessage());
                }

                // Step 7: Broadcast WebSocket
                seatStatusSyncService.broadcastSeatUpdateWithTimeSlot(reservation.getSeat(), "CONFIRMED",
                                reservation.getStartTime(), reservation.getEndTime());

                // Step 8: Send push notification
                try {
                        String timeStr = String.format("%02d:%02d - %02d:%02d",
                                        reservation.getStartTime().getHour(), reservation.getStartTime().getMinute(),
                                        reservation.getEndTime().getHour(), reservation.getEndTime().getMinute());
                        String notiTitle = "Check-in thành công";
                        String notiBody = String.format(
                                        "Ghế %s tại %s (%s) đã check-in bằng NFC. Chúc bạn học tập hiệu quả!",
                                        seat.getSeatCode(), zoneName, timeStr);
                        pushNotificationService.sendToUser(reservation.getUser().getId(), notiTitle, notiBody,
                                        NotificationType.BOOKING, saved.getReservationId());
                } catch (Exception e) {
                        System.err.println("Failed to send NFC confirmation notification: " + e.getMessage());
                }

                // Step 9: Broadcast dashboard update
                broadcastDashboardUpdate("BOOKING_UPDATE", "CONFIRMED");

                return saved;
        }

        private static final Map<String, Set<String>> VALID_STATUS_TRANSITIONS = Map.of(
                "PROCESSING", Set.of("BOOKED", "CANCELLED"),
                "BOOKED", Set.of("CONFIRMED", "CANCELLED", "EXPIRED"),
                "CONFIRMED", Set.of("COMPLETED", "CANCELLED"),
                "COMPLETED", Set.of(),
                "CANCELLED", Set.of(),
                "EXPIRED", Set.of()
        );

        @Transactional
        public ReservationEntity updateStatus(UUID reservationId, String status) {
                ReservationEntity reserv = reservationRepository.findById(reservationId)
                                .orElseThrow(() -> new RuntimeException("Reservation not found"));

                String currentStatus = reserv.getStatus().toUpperCase();
                String newStatus = status.toUpperCase();
                Set<String> allowedTransitions = VALID_STATUS_TRANSITIONS.getOrDefault(currentStatus, Set.of());
                if (!allowedTransitions.contains(newStatus)) {
                        throw new BadRequestException(
                                "Không thể chuyển trạng thái từ " + currentStatus + " sang " + newStatus);
                }

                reserv.setStatus(status);
                ReservationEntity saved = reservationRepository.save(reserv);

                // Broadcast status to WebSocket clients
                String wsStatus = "BOOKED".equalsIgnoreCase(status) ? "BOOKED"
                                : "CANCEL".equalsIgnoreCase(status) ? "AVAILABLE" : "HOLDING";
                seatStatusSyncService.broadcastSeatUpdateWithTimeSlot(reserv.getSeat(), wsStatus,
                                reserv.getStartTime(), reserv.getEndTime());

                // Send push notification khi xác nhận đặt chỗ thành công (BOOKED)
                if ("BOOKED".equalsIgnoreCase(status)) {
                        try {
                                SeatEntity seat = reserv.getSeat();
                                String zoneName = seat.getZone() != null ? seat.getZone().getZoneName() : "";
                                String timeStr = String.format("%02d:%02d - %02d:%02d",
                                                reserv.getStartTime().getHour(), reserv.getStartTime().getMinute(),
                                                reserv.getEndTime().getHour(), reserv.getEndTime().getMinute());
                                String title = "Đặt chỗ thành công";
                                String body = String.format(
                                                "Ghế %s tại %s (%s) đã được xác nhận. Hãy đến sớm để check-in!",
                                                seat.getSeatCode(), zoneName, timeStr);
                                System.out.println("[DEBUG-BOOKING] Sending notification to user="
                                                + reserv.getUser().getId());
                                pushNotificationService.sendToUser(reserv.getUser().getId(), title, body,
                                                NotificationType.BOOKING, saved.getReservationId());
                                System.out.println("[DEBUG-BOOKING] Notification sent OK");
                        } catch (Exception e) {
                                System.out.println("[DEBUG-BOOKING] ERROR: " + e.getMessage());
                                e.printStackTrace();
                        }
                }

                // Broadcast dashboard update
                broadcastDashboardUpdate("BOOKING_UPDATE", "STATUS_CHANGED");

                return saved;
        }

        @Transactional(readOnly = true)
        public List<BookingResponse> getAllBookings() {
                return reservationRepository.findAll().stream()
                                .map(this::toBookingResponse)
                                .toList();
        }

        private BookingResponse toBookingResponse(ReservationEntity reservation) {
                var user = reservation.getUser();
                var seat = reservation.getSeat();
                var zone = seat.getZone();
                var area = zone != null ? zone.getArea() : null;

                return BookingResponse.builder()
                                .reservationId(reservation.getReservationId())
                                .user(BookingResponse.UserInfo.builder()
                                                .id(user.getId())
                                                .userCode(user.getUserCode())
                                                .fullName(user.getFullName())
                                                .email(user.getEmail())
                                                .avtUrl(user.getAvtUrl())
                                                .build())
                                .seat(BookingResponse.SeatInfo.builder()
                                                .id(seat.getSeatId())
                                                .seatCode(seat.getSeatCode())
                                                .zone(zone != null ? BookingResponse.ZoneInfo.builder()
                                                                .id(zone.getZoneId())
                                                                .zoneName(zone.getZoneName())
                                                                .area(area != null ? BookingResponse.AreaInfo.builder()
                                                                                .id(area.getAreaId())
                                                                                .areaName(area.getAreaName())
                                                                                .build() : null)
                                                                .build() : null)
                                                .build())
                                .startTime(reservation.getStartTime())
                                .endTime(reservation.getEndTime())
                                .status(reservation.getStatus())
                                .createdAt(reservation.getCreatedAt())
                                .build();
        }

        @Transactional(readOnly = true)
        public List<SeatDTO> getAllSeatsDTO(Integer zoneId) {
                List<SeatEntity> seats = seatRepository.findByZone_ZoneId(zoneId);
                return seats.stream().map(this::mapToDTO).toList();
        }

        private SeatDTO mapToDTO(SeatEntity seat) {
                // Calculate current status dynamically
                SeatStatus status = seatAvailabilityService.calculateCurrentStatus(seat);
                return new SeatDTO(
                                seat.getSeatId(),
                                seat.getSeatCode(),
                                status,
                                seat.getRowNumber(),
                                seat.getColumnNumber(),
                                seat.getZone().getZoneId(),
                                seat.getNfcTagUid(),
                                null);
        }

        @Transactional(readOnly = true)
        public List<SeatDTO> getSeatsByTime(Integer zoneId, LocalDate date, LocalTime start, LocalTime end) {
                List<SeatEntity> seats = seatRepository.findByZone_ZoneId(zoneId);

                LocalDateTime startDateTime = LocalDateTime.of(date, start);
                LocalDateTime endDateTime = LocalDateTime.of(date, end);

                return seats.stream().map(seat -> {
                        // Check if seat is restricted (isActive = false)
                        if (seat.getIsActive() == null || !seat.getIsActive()) {
                                return new SeatDTO(
                                                seat.getSeatId(),
                                                seat.getSeatCode(),
                                                SeatStatus.UNAVAILABLE,
                                                seat.getRowNumber(),
                                                seat.getColumnNumber(),
                                                seat.getZone().getZoneId(),
                                                seat.getNfcTagUid(),
                                                null);
                        }

                        // Tìm reservation trùng time slot
                        var matchingReservation = seat.getReservation().stream()
                                        .filter(r -> (r.getStatus().equalsIgnoreCase("BOOKED") ||
                                                        r.getStatus().equalsIgnoreCase("PROCESSING") ||
                                                        r.getStatus().equalsIgnoreCase("CONFIRMED")) &&
                                                        r.getStartTime().toLocalDate().equals(date) &&
                                                        r.getStartTime().isBefore(endDateTime) &&
                                                        r.getEndTime().isAfter(startDateTime))
                                        .findFirst();

                        // Xác định seat status dựa trên reservation status
                        SeatStatus status = SeatStatus.AVAILABLE;
                        String endTimeStr = null;
                        if (matchingReservation.isPresent()) {
                                String reservStatus = matchingReservation.get().getStatus();
                                if ("BOOKED".equalsIgnoreCase(reservStatus) ||
                                                "CONFIRMED".equalsIgnoreCase(reservStatus)) {
                                        status = SeatStatus.BOOKED;
                                        endTimeStr = matchingReservation.get().getEndTime().toString();
                                } else if ("PROCESSING".equalsIgnoreCase(reservStatus)) {
                                        status = SeatStatus.HOLDING;
                                        endTimeStr = matchingReservation.get().getEndTime().toString();
                                }
                        }

                        return new SeatDTO(
                                        seat.getSeatId(),
                                        seat.getSeatCode(),
                                        status,
                                        seat.getRowNumber(),
                                        seat.getColumnNumber(),
                                        seat.getZone().getZoneId(),
                                        seat.getNfcTagUid(),
                                        endTimeStr);
                }).toList();
        }

        @Transactional(readOnly = true)
        public List<SeatDTO> getSeatsByDate(Integer zoneId, LocalDate date) {
                List<SeatEntity> seats = seatRepository.findByZone_ZoneId(zoneId);

                return seats.stream().map(seat -> {
                        // Check if seat is restricted (isActive = false)
                        if (seat.getIsActive() == null || !seat.getIsActive()) {
                                return new SeatDTO(
                                                seat.getSeatId(),
                                                seat.getSeatCode(),
                                                SeatStatus.UNAVAILABLE,
                                                seat.getRowNumber(),
                                                seat.getColumnNumber(),
                                                seat.getZone().getZoneId(),
                                                seat.getNfcTagUid(),
                                                null);
                        }

                        // Tìm reservation trong ngày
                        var matchingReservation = seat.getReservation().stream()
                                        .filter(r -> (r.getStatus().equalsIgnoreCase("BOOKED") ||
                                                        r.getStatus().equalsIgnoreCase("PROCESSING") ||
                                                        r.getStatus().equalsIgnoreCase("CONFIRMED")) &&
                                                        r.getStartTime().toLocalDate().equals(date))
                                        .findFirst();

                        // Xác định seat status dựa trên reservation status
                        SeatStatus status = SeatStatus.AVAILABLE;
                        String endTimeStr = null;
                        if (matchingReservation.isPresent()) {
                                String reservStatus = matchingReservation.get().getStatus();
                                if ("BOOKED".equalsIgnoreCase(reservStatus) ||
                                                "CONFIRMED".equalsIgnoreCase(reservStatus)) {
                                        status = SeatStatus.BOOKED;
                                        endTimeStr = matchingReservation.get().getEndTime().toString();
                                } else if ("PROCESSING".equalsIgnoreCase(reservStatus)) {
                                        status = SeatStatus.HOLDING;
                                        endTimeStr = matchingReservation.get().getEndTime().toString();
                                }
                        }

                        return new SeatDTO(
                                        seat.getSeatId(),
                                        seat.getSeatCode(),
                                        status,
                                        seat.getRowNumber(),
                                        seat.getColumnNumber(),
                                        seat.getZone().getZoneId(),
                                        seat.getNfcTagUid(),
                                        endTimeStr);
                }).toList();
        }

        /**
         * Lấy tất cả seats của 1 area theo time slot
         * Trả về Map<zoneId, List<SeatDTO>> - tối ưu từ N query thành 1 batch
         */
        @Transactional(readOnly = true)
        public java.util.Map<Integer, List<SeatDTO>> getAllSeatsByArea(Integer areaId, LocalDate date, LocalTime start,
                        LocalTime end) {
                // Lấy tất cả zones thuộc area
                List<ZoneEntity> zones = zoneRepository.findByArea_AreaId(Long.valueOf(areaId));

                java.util.Map<Integer, List<SeatDTO>> result = new java.util.HashMap<>();

                for (ZoneEntity zone : zones) {
                        List<SeatDTO> seats = getSeatsByTime(zone.getZoneId(), date, start, end);
                        result.put(zone.getZoneId(), seats);
                }

                return result;
        }

        private void broadcastDashboardUpdate(String type, String action) {
                try {
                        messagingTemplate.convertAndSend("/topic/dashboard",
                                        java.util.Map.of("type", type, "action", action, "timestamp",
                                                        java.time.Instant.now().toString()));
                } catch (Exception e) {
                        System.err.println("Failed to broadcast dashboard update: " + e.getMessage());
                }
        }
}
