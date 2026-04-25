package slib.com.example.service.booking;

import lombok.extern.slf4j.Slf4j;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import slib.com.example.exception.BadRequestException;
import slib.com.example.entity.activity.ActivityLogEntity;
import slib.com.example.entity.notification.NotificationEntity.NotificationType;
import slib.com.example.entity.users.User;
import slib.com.example.entity.zone_config.SeatEntity;
import slib.com.example.entity.zone_config.SeatStatus;
import slib.com.example.entity.zone_config.ZoneEntity;
import slib.com.example.dto.booking.BookingResponse;
import slib.com.example.dto.booking.SeatNfcActionStatusResponse;
import slib.com.example.dto.zone_config.SeatDTO;
import slib.com.example.entity.booking.ReservationEntity;
import slib.com.example.entity.library.LibrarySetting;
import slib.com.example.repository.booking.ReservationRepository;
import slib.com.example.repository.hce.AccessLogRepository;
import slib.com.example.repository.zone_config.SeatRepository;
import slib.com.example.repository.users.UserRepository;
import slib.com.example.repository.zone_config.ZoneRepository;
import slib.com.example.service.activity.ActivityService;
import slib.com.example.service.notification.PushNotificationService;
import slib.com.example.service.reputation.ReputationService;
import slib.com.example.service.system.LibrarySettingService;
import slib.com.example.service.users.StudentProfileService;
import slib.com.example.service.zone_config.SeatAvailabilityService;
import slib.com.example.service.zone_config.SeatService;
import slib.com.example.service.zone_config.SeatStatusSyncService;

@Service
@Slf4j
public class BookingService {
        private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
        private static final Set<String> ACTIVE_RESERVATION_STATUSES = Set.of("PROCESSING", "BOOKED", "CONFIRMED");
        private static final long PROCESSING_TIMEOUT_SECONDS = 120L;
        private static final DateTimeFormatter LAYOUT_CHANGE_TIME_FORMAT = DateTimeFormatter
                        .ofPattern("HH:mm 'ngày' dd/MM/yyyy");

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
        private final BookingPolicyService bookingPolicyService;
        private final ReputationService reputationService;
        private final SeatService seatService;
        private final StudentProfileService studentProfileService;
        private final AccessLogRepository accessLogRepository;

        public BookingService(ReservationRepository reservationRepository, UserRepository userRepository,
                        SeatRepository seatRepository, ZoneRepository zoneRepository,
                        SeatStatusSyncService seatStatusSyncService, LibrarySettingService librarySettingService,
                        SeatAvailabilityService seatAvailabilityService,
                        PushNotificationService pushNotificationService,
                        ActivityService activityService,
                        SimpMessagingTemplate messagingTemplate,
                        BookingPolicyService bookingPolicyService,
                        ReputationService reputationService,
                        SeatService seatService,
                        StudentProfileService studentProfileService,
                        AccessLogRepository accessLogRepository) {
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
                this.bookingPolicyService = bookingPolicyService;
                this.reputationService = reputationService;
                this.seatService = seatService;
                this.studentProfileService = studentProfileService;
                this.accessLogRepository = accessLogRepository;
        }

        @Transactional
        public ReservationEntity createBooking(UUID userId, Integer seatId,
                        LocalDateTime startTime, LocalDateTime endTime) {
                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new RuntimeException("User not found"));
                SeatEntity seat = seatRepository.findByIdForUpdate(seatId)
                                .orElseThrow(() -> new RuntimeException("Seat not found"));

                if (Boolean.FALSE.equals(seat.getIsVisible())) {
                        throw new RuntimeException("Ghế này không còn hiển thị trên sơ đồ và không thể đặt mới");
                }

                // Lấy cấu hình giới hạn đặt chỗ
                LibrarySetting settings = librarySettingService.getSettings();

                // Kiểm tra lịch tạm đóng theo đúng khung giờ đặt, không chặn nhầm các ngày khác.
                if (librarySettingService.isLibraryClosedFor(settings, startTime, endTime)) {
                        String reason = settings.getClosedReason() != null
                                        ? settings.getClosedReason()
                                        : "Thư viện tạm thời không nhận đặt chỗ";
                        throw new RuntimeException("Thư viện tạm đóng trong khung giờ bạn chọn. Lý do: " + reason);
                }

                int currentReputation = bookingPolicyService.resolveCurrentReputation(userId);
                bookingPolicyService.enforceBookingPolicies(userId, startTime, settings, currentReputation);

                LocalDate bookingDate = startTime.toLocalDate();

                // Đếm số lượt đặt của user trong ngày này (tính cả ghế đang giữ, đã đặt và đã check-in)
                List<ReservationEntity> userBookingsToday = reservationRepository.findByUserId(userId).stream()
                                .filter(r -> r.getStartTime().toLocalDate().equals(bookingDate))
                                .filter(this::countsTowardsBookingLimit)
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

                // Kiểm tra overlap với reservation đang hoạt động tại ghế
                boolean isConflict = seat.getReservation().stream()
                                .anyMatch(r -> isActiveReservation(r) &&
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
                        log.warn("Failed to log booking activity for reservation {}", saved.getReservationId(), e);
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
                                                .noneMatch(r -> isActiveReservation(r)
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
                                .actualEndTime(reservation.getActualEndTime())
                                .createdAt(reservation.getCreatedAt())
                                .cancellationReason(reservation.getCancellationReason())
                                .cancelledByStaff(isCancelledByStaff(reservation))
                                .layoutChanged(Boolean.TRUE.equals(reservation.getLayoutChanged()))
                                .layoutChangeTitle(reservation.getLayoutChangeTitle())
                                .layoutChangeMessage(reservation.getLayoutChangeMessage())
                                .layoutChangedAt(reservation.getLayoutChangedAt())
                                .canCancel(canUserCancelReservationNow(reservation))
                                .canChangeSeat(canUserChangeSeatNow(reservation))
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
                                .layoutChanged(Boolean.TRUE.equals(reservation.getLayoutChanged()))
                                .layoutChangeTitle(reservation.getLayoutChangeTitle())
                                .layoutChangeMessage(reservation.getLayoutChangeMessage())
                                .layoutChangedAt(reservation.getLayoutChangedAt())
                                .canCancel(canUserCancelReservationNow(reservation))
                                .canChangeSeat(canUserChangeSeatNow(reservation))
                                .build();
        }

        /**
         * Cancel booking.
         * Patrons must respect the 12-hour rule, while staff can cancel future bookings
         * with a mandatory reason.
         */
        @Transactional
        public ReservationEntity cancelBooking(UUID reservationId, UUID cancelledByUserId, boolean cancelledByStaff,
                        String cancellationReason) {
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

                LocalDateTime now = LocalDateTime.now(VIETNAM_ZONE);
                String normalizedCancellationReason = cancellationReason != null ? cancellationReason.trim() : null;
                boolean flexibleCancellation = canBypassStandardCancellationWindow(reservation, now);

                if (cancelledByStaff) {
                        if (normalizedCancellationReason == null || normalizedCancellationReason.isBlank()) {
                                throw new BadRequestException("Vui lòng nhập lý do hủy đặt chỗ.");
                        }
                        if (!reservation.getStartTime().isAfter(now)) {
                                throw new BadRequestException(
                                                "Thủ thư chỉ có thể hủy đặt chỗ trước giờ bắt đầu. Nếu sinh viên đã vào chỗ, hãy dùng chức năng trả chỗ.");
                        }
                }

                // PROCESSING reservations can be cancelled immediately (user is still
                // confirming)
                // Only apply 12-hour rule for BOOKED/CONFIRMED reservations
                if (!cancelledByStaff && !"PROCESSING".equalsIgnoreCase(reservation.getStatus()) && !flexibleCancellation) {
                        int cancellationLeadHours = resolveBookingCancellationLeadHours();
                        LocalDateTime cancelDeadline = reservation.getStartTime().minusHours(cancellationLeadHours);

                        if (now.isAfter(cancelDeadline)) {
                                long hoursUntilStart = java.time.Duration.between(now, reservation.getStartTime())
                                                .toHours();
                                throw new RuntimeException(
                                                "Không thể hủy đặt chỗ. Bạn chỉ có thể hủy trước "
                                                                + cancellationLeadHours + " tiếng. " +
                                                                "Còn " + hoursUntilStart
                                                                + " tiếng nữa là đến giờ đặt.");
                        }
                }

                if (!cancelledByStaff && flexibleCancellation
                                && (normalizedCancellationReason == null || normalizedCancellationReason.isBlank())) {
                        normalizedCancellationReason = "Sinh viên hủy chỗ do sơ đồ thư viện thay đổi";
                }

                reservation.setStatus("CANCEL");
                reservation.setCancellationReason(normalizedCancellationReason);
                reservation.setCancelledByUserId(cancelledByUserId);
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
                        log.warn("Failed to log cancellation activity for reservation {}", saved.getReservationId(), e);
                }

                // Broadcast to WebSocket clients for real-time updates
                // (status is calculated dynamically, no DB update needed)

                // Broadcast cho tất cả clients kể cả đang xem future time slots
                seatStatusSyncService.broadcastSeatUpdateWithTimeSlot(reservation.getSeat(), "AVAILABLE",
                                reservation.getStartTime(), reservation.getEndTime());

                // Broadcast dashboard update
                broadcastDashboardUpdate("BOOKING_UPDATE", "CANCELLED");

                if (cancelledByStaff) {
                        sendStaffCancellationNotification(saved, normalizedCancellationReason);
                }

                return saved;
        }

        @Transactional
        public ReservationEntity changeSeatForLayoutAffectedReservation(UUID reservationId, UUID actorUserId,
                        Integer newSeatId) {
                if (newSeatId == null) {
                        throw new BadRequestException("Vui lòng chọn ghế mới.");
                }

                ReservationEntity reservation = reservationRepository.findDetailById(reservationId)
                                .orElseThrow(() -> new RuntimeException("Reservation not found"));

                if (!reservation.getUser().getId().equals(actorUserId)) {
                        throw new BadRequestException("Bạn không có quyền đổi chỗ cho đặt chỗ này.");
                }

                if (!Boolean.TRUE.equals(reservation.getLayoutChanged())) {
                        throw new BadRequestException(
                                        "Chỉ các đặt chỗ bị ảnh hưởng bởi chỉnh sửa sơ đồ mới được đổi chỗ trực tiếp.");
                }

                LocalDateTime now = LocalDateTime.now(VIETNAM_ZONE);
                if (!reservation.getStartTime().isAfter(now)) {
                        throw new BadRequestException("Chỉ có thể đổi chỗ trước giờ bắt đầu.");
                }

                String normalizedStatus = normalizeStatus(reservation.getStatus());
                if (!ACTIVE_RESERVATION_STATUSES.contains(normalizedStatus)) {
                        throw new BadRequestException("Đặt chỗ này không còn đủ điều kiện để đổi chỗ.");
                }

                if (reservation.getSeat() != null && newSeatId.equals(reservation.getSeat().getSeatId())) {
                        throw new BadRequestException("Bạn đang chọn lại đúng ghế hiện tại.");
                }

                SeatEntity currentSeat = reservation.getSeat();
                SeatEntity targetSeat = seatRepository.findByIdForUpdate(newSeatId)
                                .orElseThrow(() -> new RuntimeException("Seat not found"));

                if (!Boolean.TRUE.equals(targetSeat.getIsActive())) {
                        throw new BadRequestException("Ghế mới hiện không hoạt động.");
                }

                if (!seatAvailabilityService.isAvailable(targetSeat.getSeatId(),
                                reservation.getStartTime(), reservation.getEndTime())) {
                        throw new BadRequestException("Ghế mới không còn trống trong khung giờ này.");
                }

                reservation.setSeat(targetSeat);
                clearLayoutChangeState(reservation);
                ReservationEntity saved = reservationRepository.save(reservation);

                seatStatusSyncService.broadcastSeatUpdateWithTimeSlot(
                                currentSeat,
                                "AVAILABLE",
                                reservation.getStartTime(),
                                reservation.getEndTime());
                seatStatusSyncService.broadcastSeatUpdateWithTimeSlot(
                                targetSeat,
                                mapSeatBroadcastStatus(saved),
                                reservation.getStartTime(),
                                reservation.getEndTime());

                try {
                        activityService.logActivity(ActivityLogEntity.builder()
                                        .userId(actorUserId)
                                        .activityType(ActivityLogEntity.TYPE_BOOKING_SUCCESS)
                                        .title("Đổi ghế do sơ đồ thư viện thay đổi")
                                        .description("Đã đổi từ ghế " + currentSeat.getSeatCode() + " sang ghế "
                                                        + targetSeat.getSeatCode())
                                        .seatCode(targetSeat.getSeatCode())
                                        .zoneName(targetSeat.getZone() != null ? targetSeat.getZone().getZoneName() : "")
                                        .reservationId(saved.getReservationId())
                                        .build());
                } catch (Exception e) {
                        log.warn("Failed to log layout-change seat swap for reservation {}", saved.getReservationId(), e);
                }

                pushNotificationService.sendToUser(
                                actorUserId,
                                "Đã đổi ghế thành công",
                                "SLIB đã cập nhật ghế mới cho suất đặt chỗ của bạn: "
                                                + targetSeat.getSeatCode() + " (" + buildTimeRange(saved) + ").",
                                NotificationType.BOOKING,
                                saved.getReservationId(),
                                "RESERVATION",
                                "BOOKING");

                broadcastDashboardUpdate("BOOKING_UPDATE", saved.getStatus());
                return saved;
        }

        @Transactional
        public void markReservationsAffectedByLayoutChange(List<ReservationEntity> reservations, LocalDateTime changedAt) {
                if (reservations == null || reservations.isEmpty()) {
                        return;
                }

                String title = "Lịch đặt chỗ của bạn vừa bị ảnh hưởng";
                for (ReservationEntity reservation : reservations) {
                        reservation.setLayoutChanged(true);
                        reservation.setLayoutChangeTitle(title);
                        reservation.setLayoutChangeMessage(buildLayoutChangeMessage(reservation, changedAt));
                        reservation.setLayoutChangedAt(changedAt);
                }
                reservationRepository.saveAll(reservations);

                for (ReservationEntity reservation : reservations) {
                        pushNotificationService.sendToUser(
                                        reservation.getUser().getId(),
                                        reservation.getLayoutChangeTitle(),
                                        reservation.getLayoutChangeMessage(),
                                        NotificationType.BOOKING,
                                        reservation.getReservationId(),
                                        "RESERVATION",
                                        "BOOKING");
                }
        }

        @Transactional
        public void clearLayoutChangeWarnings(List<ReservationEntity> reservations) {
                if (reservations == null || reservations.isEmpty()) {
                        return;
                }

                for (ReservationEntity reservation : reservations) {
                        clearLayoutChangeState(reservation);
                }
                reservationRepository.saveAll(reservations);
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

                return confirmReservationCheckIn(
                                reservation,
                                "Xác nhận ghế thành công",
                                "Đã xác nhận ghế " + reservation.getSeat().getSeatCode() + " tại "
                                                + reservation.getSeat().getZone().getZoneName() + " bằng NFC",
                                ActivityLogEntity.TYPE_NFC_CONFIRM,
                                "bằng NFC",
                                true);
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
                assertUserHasActiveLibraryCheckIn(
                                reservation.getUser().getId(),
                                "xác nhận ghế bằng NFC");

                // Step 1: Resolve seat from NFC UID (backend hashes internally)
                SeatEntity nfcSeat = seatService.findSeatEntityByNfcUid(rawNfcUid);

                // Step 2: Validate that NFC tag seat matches reservation seat
                SeatEntity expectedSeat = reservation.getSeat();
                if (!nfcSeat.getSeatId().equals(expectedSeat.getSeatId())) {
                        throw new RuntimeException(
                                        "Ghế không khớp! Bạn đang đặt ghế " + expectedSeat.getSeatCode() +
                                                        " nhưng thẻ NFC là ghế " + nfcSeat.getSeatCode());
                }

                return confirmReservationCheckIn(
                                reservation,
                                "Xác nhận ghế thành công",
                                "Đã xác nhận ghế " + reservation.getSeat().getSeatCode() + " tại "
                                                + reservation.getSeat().getZone().getZoneName() + " bằng NFC UID",
                                ActivityLogEntity.TYPE_NFC_CONFIRM,
                                "bằng NFC",
                                true);
        }

        private static final Map<String, Set<String>> VALID_STATUS_TRANSITIONS = Map.of(
                "PROCESSING", Set.of("BOOKED", "CANCEL"),
                "BOOKED", Set.of("CANCEL", "EXPIRED"),
                "CONFIRMED", Set.of("COMPLETED", "CANCEL"),
                "COMPLETED", Set.of(),
                "CANCEL", Set.of(),
                "EXPIRED", Set.of()
        );

        @Transactional
        public ReservationEntity confirmSeatByStaff(UUID reservationId, UUID librarianId) {
                ReservationEntity reservation = reservationRepository.findById(reservationId)
                                .orElseThrow(() -> new RuntimeException("Đặt chỗ không tồn tại"));
                User librarian = userRepository.findById(librarianId)
                                .orElseThrow(() -> new RuntimeException("Không tìm thấy thủ thư"));

                return confirmReservationCheckIn(
                                reservation,
                                "Thủ thư xác nhận chỗ ngồi",
                                "Đã được thủ thư " + librarian.getFullName() + " xác nhận đang ngồi tại ghế "
                                                + reservation.getSeat().getSeatCode(),
                                ActivityLogEntity.TYPE_CHECK_IN,
                                "với xác nhận của thủ thư",
                                false);
        }

        @Transactional
        public ReservationEntity updateStatus(UUID reservationId, String status) {
                ReservationEntity reserv = reservationRepository.findById(reservationId)
                                .orElseThrow(() -> new RuntimeException("Reservation not found"));

                String currentStatus = normalizeStatus(reserv.getStatus());
                String newStatus = normalizeStatus(status);
                if (currentStatus.equals(newStatus)) {
                        return reserv;
                }
                if ("CONFIRMED".equals(newStatus)) {
                        throw new BadRequestException(
                                        "Trạng thái CONFIRMED chỉ được tạo qua check-in NFC hoặc xác nhận thủ thư.");
                }
                Set<String> allowedTransitions = VALID_STATUS_TRANSITIONS.getOrDefault(currentStatus, Set.of());
                if (!allowedTransitions.contains(newStatus)) {
                        throw new BadRequestException(
                                "Không thể chuyển trạng thái từ " + currentStatus + " sang " + newStatus);
                }

                reserv.setStatus(newStatus);
                ReservationEntity saved = reservationRepository.save(reserv);

                // Broadcast status to WebSocket clients
                seatStatusSyncService.updateSeatStatus(
                                reserv.getSeat(),
                                reserv.getStartTime(),
                                reserv.getEndTime(),
                                newStatus);

                // Gửi push notification khi xác nhận đặt chỗ thành công (BOOKED)
                // Quan trọng cho kiosk: user đặt từ kiosk cần nhận thông báo về điện thoại
                if ("BOOKED".equalsIgnoreCase(newStatus)) {
                        try {
                                SeatEntity seat = reserv.getSeat();
                                String zoneName = seat.getZone() != null ? seat.getZone().getZoneName() : "";
                                String timeStr = String.format("%02d:%02d - %02d:%02d",
                                                reserv.getStartTime().getHour(), reserv.getStartTime().getMinute(),
                                                reserv.getEndTime().getHour(), reserv.getEndTime().getMinute());
                                String title = "Đặt chỗ thành công";
                                String body = String.format(
                                                "Ghế %s tại %s (%s) đã được xác nhận. Hãy đến đúng giờ, check-in thư viện và xác nhận chỗ ngồi.",
                                                seat.getSeatCode(), zoneName, timeStr);
                                pushNotificationService.sendToUser(reserv.getUser().getId(), title, body,
                                                NotificationType.BOOKING, saved.getReservationId());
                        } catch (Exception e) {
                                log.warn("Failed to send booking confirmation notification for reservation {}",
                                                saved.getReservationId(), e);
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
                                .confirmedAt(reservation.getConfirmedAt())
                                .actualEndTime(reservation.getActualEndTime())
                                .status(reservation.getStatus())
                                .createdAt(reservation.getCreatedAt())
                                .cancellationReason(reservation.getCancellationReason())
                                .cancelledByStaff(isCancelledByStaff(reservation))
                                .build();
        }

        private boolean isCancelledByStaff(ReservationEntity reservation) {
                return reservation.getCancelledByUserId() != null
                                && reservation.getUser() != null
                                && !reservation.getCancelledByUserId().equals(reservation.getUser().getId());
        }

        private boolean canUserCancelReservationNow(ReservationEntity reservation) {
                if (reservation == null) {
                        return false;
                }

                String status = normalizeStatus(reservation.getStatus());
                if ("CANCEL".equals(status) || "COMPLETED".equals(status) || "EXPIRED".equals(status)) {
                        return false;
                }

                LocalDateTime now = LocalDateTime.now(VIETNAM_ZONE);
                if (!reservation.getStartTime().isAfter(now)) {
                        return "PROCESSING".equals(status);
                }

                if ("PROCESSING".equals(status)) {
                        return true;
                }

                return canBypassStandardCancellationWindow(reservation, now)
                                || now.isBefore(reservation.getStartTime().minusHours(resolveBookingCancellationLeadHours()));
        }

        private boolean canUserChangeSeatNow(ReservationEntity reservation) {
                if (reservation == null || !Boolean.TRUE.equals(reservation.getLayoutChanged())) {
                        return false;
                }

                String status = normalizeStatus(reservation.getStatus());
                if (!ACTIVE_RESERVATION_STATUSES.contains(status)) {
                        return false;
                }

                LocalDateTime now = LocalDateTime.now(VIETNAM_ZONE);
                return reservation.getStartTime().isAfter(now);
        }

        private boolean canBypassStandardCancellationWindow(ReservationEntity reservation, LocalDateTime now) {
                return Boolean.TRUE.equals(reservation.getLayoutChanged())
                                && reservation.getStartTime() != null
                                && reservation.getStartTime().isAfter(now)
                                && ACTIVE_RESERVATION_STATUSES.contains(normalizeStatus(reservation.getStatus()));
        }

        private void clearLayoutChangeState(ReservationEntity reservation) {
                reservation.setLayoutChanged(false);
                reservation.setLayoutChangeTitle(null);
                reservation.setLayoutChangeMessage(null);
                reservation.setLayoutChangedAt(null);
        }

        private String buildLayoutChangeMessage(ReservationEntity reservation, LocalDateTime changedAt) {
                String timeText = changedAt != null
                                ? changedAt.format(LAYOUT_CHANGE_TIME_FORMAT)
                                : LocalDateTime.now(VIETNAM_ZONE).format(LAYOUT_CHANGE_TIME_FORMAT);
                return "Thư viện vừa cập nhật sơ đồ vào " + timeText
                                + ". Suất ghế của bạn ở khung giờ " + buildTimeRange(reservation)
                                + " có thể đã thay đổi vị trí hiển thị. Bạn có thể mở ứng dụng để kiểm tra lại, đổi ghế khác hoặc hủy chỗ này mà không bị giới hạn thời hạn hủy tiêu chuẩn.";
        }

        private int resolveBookingCancellationLeadHours() {
                LibrarySetting settings = librarySettingService.getSettings();
                return settings.getBookingCancelDeadlineHours() != null
                                ? settings.getBookingCancelDeadlineHours()
                                : 12;
        }

        private String buildTimeRange(ReservationEntity reservation) {
                return String.format("%02d:%02d - %02d:%02d ngày %02d/%02d",
                                reservation.getStartTime().getHour(),
                                reservation.getStartTime().getMinute(),
                                reservation.getEndTime().getHour(),
                                reservation.getEndTime().getMinute(),
                                reservation.getStartTime().getDayOfMonth(),
                                reservation.getStartTime().getMonthValue());
        }

        private String mapSeatBroadcastStatus(ReservationEntity reservation) {
                return switch (normalizeStatus(reservation.getStatus())) {
                        case "PROCESSING" -> "HOLDING";
                        case "CONFIRMED" -> "CONFIRMED";
                        default -> "BOOKED";
                };
        }

        private void sendStaffCancellationNotification(ReservationEntity reservation, String cancellationReason) {
                try {
                        SeatEntity seat = reservation.getSeat();
                        String zoneName = seat.getZone() != null ? seat.getZone().getZoneName() : "";
                        String timeStr = String.format("%02d:%02d - %02d:%02d",
                                        reservation.getStartTime().getHour(), reservation.getStartTime().getMinute(),
                                        reservation.getEndTime().getHour(), reservation.getEndTime().getMinute());
                        String title = "Đặt chỗ đã bị thủ thư hủy";
                        String body = String.format(
                                        "Ghế %s tại %s (%s) đã bị thủ thư hủy. Lý do: %s",
                                        seat.getSeatCode(),
                                        zoneName,
                                        timeStr,
                                        cancellationReason);

                        pushNotificationService.sendToUser(
                                        reservation.getUser().getId(),
                                        title,
                                        body,
                                        NotificationType.BOOKING,
                                        reservation.getReservationId(),
                                        "RESERVATION",
                                        "BOOKING");
                } catch (Exception e) {
                        log.warn("Failed to send staff cancellation notification for reservation {}",
                                        reservation.getReservationId(), e);
                }
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
                                .filter(r -> isActiveReservation(r) &&
                                                        r.getStartTime().toLocalDate().equals(date) &&
                                                        r.getStartTime().isBefore(endDateTime) &&
                                                        r.getEndTime().isAfter(startDateTime))
                                        .findFirst();

                        // Xác định seat status dựa trên reservation status
                        SeatStatus status = SeatStatus.AVAILABLE;
                        String endTimeStr = null;
                        if (matchingReservation.isPresent()) {
                                String reservStatus = matchingReservation.get().getStatus();
                                if ("CONFIRMED".equalsIgnoreCase(reservStatus)) {
                                        status = SeatStatus.CONFIRMED;
                                        endTimeStr = matchingReservation.get().getEndTime().toString();
                                } else if ("BOOKED".equalsIgnoreCase(reservStatus)) {
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
                                        .filter(this::isActiveReservation)
                                        .filter(r ->
                                                        r.getStartTime().toLocalDate().equals(date))
                                        .findFirst();

                        // Xác định seat status dựa trên reservation status
                        SeatStatus status = SeatStatus.AVAILABLE;
                        String endTimeStr = null;
                        if (matchingReservation.isPresent()) {
                                String reservStatus = matchingReservation.get().getStatus();
                                if ("CONFIRMED".equalsIgnoreCase(reservStatus)) {
                                        status = SeatStatus.CONFIRMED;
                                        endTimeStr = matchingReservation.get().getEndTime().toString();
                                } else if ("BOOKED".equalsIgnoreCase(reservStatus)) {
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
                        log.warn("Failed to broadcast dashboard update", e);
                }
        }

        private boolean countsTowardsBookingLimit(ReservationEntity reservation) {
                return ACTIVE_RESERVATION_STATUSES.contains(normalizeStatus(reservation.getStatus()));
        }

        private boolean isActiveReservation(ReservationEntity reservation) {
                return ACTIVE_RESERVATION_STATUSES.contains(normalizeStatus(reservation.getStatus()));
        }

        private String normalizeStatus(String status) {
                if (status == null || status.isBlank()) {
                        return "";
                }

                String normalized = status.trim().toUpperCase();
                return "CANCELLED".equals(normalized) ? "CANCEL" : normalized;
        }

        private void validateSeatConfirmationEligibility(ReservationEntity reservation, boolean enforceTimeWindow) {
                String currentStatus = normalizeStatus(reservation.getStatus());
                if (!"BOOKED".equals(currentStatus)) {
                        throw new BadRequestException("Chỉ lượt đặt đang ở trạng thái BOOKED mới có thể xác nhận ghế.");
                }

                if (!enforceTimeWindow) {
                        return;
                }

                if (isBeforeCheckInWindow(reservation)) {
                        LibrarySetting settings = librarySettingService.getSettings();
                        int leadMinutes = settings.getSeatConfirmationLeadMinutes() != null
                                        ? settings.getSeatConfirmationLeadMinutes()
                                        : 15;
                        LocalDateTime checkInStart = reservation.getStartTime().minusMinutes(leadMinutes);
                        throw new BadRequestException(
                                        "Chưa đến giờ xác nhận ghế. Bạn có thể xác nhận từ "
                                                        + checkInStart.toLocalTime());
                }
                if (isAfterCheckInWindow(reservation)) {
                        throw new BadRequestException("Lượt đặt này đã hết thời gian xác nhận ghế.");
                }
        }

        private boolean isUserCheckedIntoLibrary(UUID userId) {
                return accessLogRepository.checkInUser(userId).isPresent();
        }

        private void assertUserHasActiveLibraryCheckIn(UUID userId, String actionLabel) {
                if (!isUserCheckedIntoLibrary(userId)) {
                        throw new BadRequestException(
                                        "Bạn cần check-in vào thư viện trước khi " + actionLabel + ".");
                }
        }

        private boolean isBeforeCheckInWindow(ReservationEntity reservation) {
                LocalDateTime now = LocalDateTime.now(VIETNAM_ZONE);
                LibrarySetting settings = librarySettingService.getSettings();
                int leadMinutes = settings.getSeatConfirmationLeadMinutes() != null
                                ? settings.getSeatConfirmationLeadMinutes()
                                : 15;
                LocalDateTime checkInStart = reservation.getStartTime().minusMinutes(leadMinutes);
                return now.isBefore(checkInStart);
        }

        private boolean isAfterCheckInWindow(ReservationEntity reservation) {
                LocalDateTime now = LocalDateTime.now(VIETNAM_ZONE);
                return now.isAfter(reservation.getEndTime());
        }

        private boolean isWithinCheckInWindow(ReservationEntity reservation) {
                return !isBeforeCheckInWindow(reservation) && !isAfterCheckInWindow(reservation);
        }

        private String buildSeatNfcActionMessage(
                        ReservationEntity reservation,
                        String status,
                        boolean checkedIntoLibrary) {
                if (!checkedIntoLibrary) {
                        if ("CONFIRMED".equals(status)) {
                                return "Bạn cần check-in vào thư viện trước khi xác nhận rời ghế bằng NFC.";
                        }
                        return "Bạn cần check-in vào thư viện trước khi xác nhận ghế bằng NFC.";
                }

                if ("CONFIRMED".equals(status)) {
                        return "Bạn có thể quét NFC đúng ghế để xác nhận rời ghế.";
                }

                if (!"BOOKED".equals(status)) {
                                return "Lượt đặt này hiện không hỗ trợ xác nhận ghế bằng NFC.";
                }

                if (isBeforeCheckInWindow(reservation)) {
                        LibrarySetting settings = librarySettingService.getSettings();
                        int leadMinutes = settings.getSeatConfirmationLeadMinutes() != null
                                        ? settings.getSeatConfirmationLeadMinutes()
                                        : 15;
                        LocalDateTime checkInStart = reservation.getStartTime().minusMinutes(leadMinutes);
                        return "Chưa đến giờ xác nhận ghế. Bạn có thể quét NFC từ "
                                        + checkInStart.toLocalTime()
                                        + ".";
                }

                if (isAfterCheckInWindow(reservation)) {
                        return "Lượt đặt này đã hết thời gian xác nhận ghế.";
                }

                return "Bạn có thể quét NFC để xác nhận ghế.";
        }

        private ReservationEntity confirmReservationCheckIn(
                        ReservationEntity reservation,
                        String activityTitle,
                        String activityDescription,
                        String activityType,
                        String confirmationLabel,
                        boolean enforceTimeWindow) {
                validateSeatConfirmationEligibility(reservation, enforceTimeWindow);

                LocalDateTime now = LocalDateTime.now(VIETNAM_ZONE);
                reservation.setStatus("CONFIRMED");
                reservation.setConfirmedAt(now);
                ReservationEntity saved = reservationRepository.save(reservation);

                SeatEntity seat = reservation.getSeat();
                String zoneName = seat.getZone() != null ? seat.getZone().getZoneName() : "";

                try {
                        activityService.logActivity(ActivityLogEntity.builder()
                                        .userId(reservation.getUser().getId())
                                        .activityType(activityType)
                                        .title(activityTitle)
                                        .description(activityDescription)
                                        .seatCode(seat.getSeatCode())
                                        .zoneName(zoneName)
                                        .reservationId(saved.getReservationId())
                                        .build());
                } catch (Exception e) {
                        log.warn("Failed to log check-in activity for reservation {}", saved.getReservationId(), e);
                }

                try {
                        reputationService.applyCheckInBonus(
                                        reservation.getUser().getId(),
                                        seat.getSeatCode(),
                                        zoneName,
                                        saved.getReservationId());
                } catch (Exception e) {
                        log.warn("Failed to apply check-in bonus for reservation {}", saved.getReservationId(), e);
                }

                seatStatusSyncService.updateSeatStatus(
                                reservation.getSeat(),
                                reservation.getStartTime(),
                                reservation.getEndTime(),
                                "CONFIRMED");

                try {
                        String timeStr = String.format("%02d:%02d - %02d:%02d",
                                        reservation.getStartTime().getHour(), reservation.getStartTime().getMinute(),
                                        reservation.getEndTime().getHour(), reservation.getEndTime().getMinute());
                        String notiTitle = "Ghế của bạn đã được xác nhận";
                        String notiBody = String.format(
                                        "Ghế %s tại %s (%s) đã được xác nhận %s. Bạn đã xác nhận chỗ ngồi thành công.",
                                        seat.getSeatCode(), zoneName, timeStr, confirmationLabel);
                        pushNotificationService.sendToUser(reservation.getUser().getId(), notiTitle, notiBody,
                                        NotificationType.BOOKING, saved.getReservationId());
                } catch (Exception e) {
                        log.warn("Failed to send check-in notification for reservation {}", saved.getReservationId(), e);
                }

                broadcastDashboardUpdate("BOOKING_UPDATE", "CONFIRMED");
                return saved;
        }

        /**
         * Student leaves their seat before reservation end time.
         * Triggered by: NFC scan at seat (2nd tap) OR librarian confirmation.
         * Sets reservation to COMPLETED with actualEndTime recorded.
         *
         * This allows distinguishing "left early via checkout" vs "completed on time".
         *
         * @param reservationId The CONFIRMED reservation to complete
         * @param seatCode Seat code for activity log
         * @param zoneName Zone name for activity log
         */
        @Transactional
        public ReservationEntity leaveSeat(UUID reservationId) {
                ReservationEntity reservation = reservationRepository.findById(reservationId)
                        .orElseThrow(() -> new RuntimeException("Đặt chỗ không tồn tại"));

                if (!"CONFIRMED".equalsIgnoreCase(reservation.getStatus())) {
                        throw new RuntimeException("Chỉ đặt chỗ đã xác nhận mới có thể thực hiện rời ghế");
                }

                SeatEntity seat = reservation.getSeat();
                String seatCode = seat != null ? seat.getSeatCode() : "";
                String zoneName = seat != null && seat.getZone() != null ? seat.getZone().getZoneName() : "";

                LocalDateTime now = LocalDateTime.now(VIETNAM_ZONE);
                LocalDateTime actualEnd = now;
                reservation.setActualEndTime(actualEnd);
                reservation.setStatus("COMPLETED");
                ReservationEntity saved = reservationRepository.save(reservation);

                // Calculate actual study duration
                LocalDateTime effectiveStart = reservation.getConfirmedAt() != null
                                ? reservation.getConfirmedAt()
                                : reservation.getStartTime();
                int durationMinutes = Math.max(
                                0,
                                (int) java.time.temporal.ChronoUnit.MINUTES.between(effectiveStart, actualEnd));

                // Log activity
                try {
                        String actualEndStr = String.format("%02d:%02d", actualEnd.getHour(), actualEnd.getMinute());
                        String endTimeStr = String.format("%02d:%02d", reservation.getEndTime().getHour(),
                                reservation.getEndTime().getMinute());
                        String timeSpent = formatDuration(durationMinutes);

                        String desc;
                        if (actualEnd.isBefore(reservation.getEndTime())) {
                                desc = String.format("Rời ghế sớm lúc %s (kết thúc dự kiến: %s). Thời gian học: %s.",
                                        actualEndStr, endTimeStr, timeSpent);
                        } else if (actualEnd.isAfter(reservation.getEndTime())) {
                                desc = String.format("Rời ghế lúc %s (quá giờ dự kiến: %s). Thời gian học: %s.",
                                        actualEndStr, endTimeStr, timeSpent);
                        } else {
                                desc = String.format("Rời ghế đúng giờ lúc %s. Thời gian học: %s.",
                                        actualEndStr, endTimeStr, timeSpent);
                        }

                        activityService.logActivity(ActivityLogEntity.builder()
                                .userId(reservation.getUser().getId())
                                .activityType(ActivityLogEntity.TYPE_SEAT_CHECKOUT)
                                .title("Rời ghế thành công")
                                .description(desc)
                                .seatCode(seatCode)
                                .zoneName(zoneName)
                                .durationMinutes(durationMinutes)
                                .reservationId(saved.getReservationId())
                                .build());
                } catch (Exception e) {
                        log.warn("Failed to log seat checkout activity for reservation {}", saved.getReservationId(), e);
                }

                // Add study hours to profile
                double studyHours = durationMinutes / 60.0;
                studentProfileService.addStudyHours(reservation.getUser().getId(), studyHours);

                // Broadcast seat status update
                seatStatusSyncService.broadcastSeatUpdateWithTimeSlot(
                                seat,
                                "AVAILABLE",
                                reservation.getStartTime(),
                                reservation.getEndTime());

                // Send push notification
                try {
                        String timeStr = String.format("%02d:%02d - %02d:%02d",
                                reservation.getStartTime().getHour(), reservation.getStartTime().getMinute(),
                                reservation.getEndTime().getHour(), reservation.getEndTime().getMinute());
                        String body = String.format(
                                "Bạn đã rời ghế %s tại %s. Thời gian học: %d phút. Tạm biệt!",
                                seatCode, zoneName, durationMinutes);
                        pushNotificationService.sendToUser(
                                reservation.getUser().getId(),
                                "Rời ghế thành công",
                                body,
                                NotificationType.BOOKING,
                                saved.getReservationId());
                } catch (Exception e) {
                        log.warn("Failed to send seat checkout notification for reservation {}",
                                saved.getReservationId(), e);
                }

                broadcastDashboardUpdate("BOOKING_UPDATE", "SEAT_CHECKOUT");
                return saved;
        }

        @Transactional
        public ReservationEntity leaveSeatWithNfcUid(UUID reservationId, UUID userId, String rawNfcUid) {
                ReservationEntity reservation = reservationRepository.findById(reservationId)
                                .orElseThrow(() -> new RuntimeException("Đặt chỗ không tồn tại"));

                if (reservation.getUser() == null || !reservation.getUser().getId().equals(userId)) {
                        throw new BadRequestException("Bạn không có quyền trả chỗ cho lượt đặt này.");
                }

                assertUserHasActiveLibraryCheckIn(
                                reservation.getUser().getId(),
                                "xác nhận rời ghế bằng NFC");

                if (!"CONFIRMED".equalsIgnoreCase(reservation.getStatus())) {
                        throw new BadRequestException("Chỉ ghế đang được xác nhận mới có thể trả chỗ.");
                }

                SeatEntity nfcSeat = seatService.findSeatEntityByNfcUid(rawNfcUid);
                SeatEntity expectedSeat = reservation.getSeat();
                if (expectedSeat == null || !nfcSeat.getSeatId().equals(expectedSeat.getSeatId())) {
                        throw new BadRequestException(
                                        "Ghế không khớp! Bạn đang dùng ghế " + expectedSeat.getSeatCode()
                                                        + " nhưng thẻ NFC là ghế " + nfcSeat.getSeatCode());
                }

                return leaveSeat(reservationId);
        }

        @Transactional(readOnly = true)
        public SeatNfcActionStatusResponse getSeatNfcActionStatus(UUID reservationId) {
                ReservationEntity reservation = reservationRepository.findById(reservationId)
                                .orElseThrow(() -> new RuntimeException("Đặt chỗ không tồn tại"));

                String status = normalizeStatus(reservation.getStatus());
                boolean checkedIntoLibrary = isUserCheckedIntoLibrary(reservation.getUser().getId());
                boolean canConfirmSeatWithNfc = checkedIntoLibrary
                                && "BOOKED".equals(status)
                                && isWithinCheckInWindow(reservation);
                boolean canLeaveSeatWithNfc = checkedIntoLibrary
                                && "CONFIRMED".equals(status);

                return SeatNfcActionStatusResponse.builder()
                                .reservationId(reservation.getReservationId())
                                .reservationStatus(status)
                                .checkedIntoLibrary(checkedIntoLibrary)
                                .canConfirmSeatWithNfc(canConfirmSeatWithNfc)
                                .canLeaveSeatWithNfc(canLeaveSeatWithNfc)
                                .message(buildSeatNfcActionMessage(reservation, status, checkedIntoLibrary))
                                .build();
        }

        private String formatDuration(int minutes) {
                int hours = minutes / 60;
                int mins = minutes % 60;
                if (hours > 0) {
                        return hours + " giờ " + mins + " phút";
                }
                return mins + " phút";
        }

        /**
         * Find active CONFIRMED reservation for a user at a specific seat.
         * Used when student scans NFC at seat to check if they should checkout or check-in.
         */
        @Transactional(readOnly = true)
        public java.util.Optional<ReservationEntity> findActiveConfirmedReservation(UUID userId, Integer seatId) {
                LocalDateTime now = LocalDateTime.now(VIETNAM_ZONE);
                return reservationRepository.findByUserId(userId).stream()
                        .filter(r -> "CONFIRMED".equalsIgnoreCase(r.getStatus()))
                        .filter(r -> r.getSeat() != null && seatId.equals(r.getSeat().getSeatId()))
                        .filter(r -> r.getEndTime().isAfter(now))
                        .findFirst();
        }

        /**
         * Find active CONFIRMED reservation for a user (any seat).
         */
        @Transactional(readOnly = true)
        public java.util.Optional<ReservationEntity> findActiveConfirmedReservationByUser(UUID userId) {
                LocalDateTime now = LocalDateTime.now(VIETNAM_ZONE);
                return reservationRepository.findByUserId(userId).stream()
                        .filter(r -> "CONFIRMED".equalsIgnoreCase(r.getStatus()))
                        .filter(r -> r.getEndTime().isAfter(now))
                        .findFirst();
        }
}
