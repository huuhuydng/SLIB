package slib.com.example.service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import slib.com.example.entity.LibrarySetting;
import slib.com.example.entity.users.User;
import slib.com.example.entity.zone_config.SeatEntity;
import slib.com.example.entity.zone_config.SeatStatus;
import slib.com.example.entity.zone_config.ZoneEntity;
import slib.com.example.dto.zone_config.SeatDTO;
import slib.com.example.entity.booking.ReservationEntity;
import slib.com.example.repository.ReservationRepository;
import slib.com.example.repository.SeatRepository;
import slib.com.example.repository.UserRepository;
import slib.com.example.repository.ZoneRepository;

@Service
public class BookingService {
    private final ReservationRepository reservationRepository;
    private final UserRepository userRepository;
    private final SeatRepository seatRepository;
    private final ZoneRepository zoneRepository;
    private final SeatStatusSyncService seatStatusSyncService;
    private final LibrarySettingService librarySettingService;

    public BookingService(ReservationRepository reservationRepository, UserRepository userRepository,
            SeatRepository seatRepository, ZoneRepository zoneRepository,
            SeatStatusSyncService seatStatusSyncService, LibrarySettingService librarySettingService) {
        this.reservationRepository = reservationRepository;
        this.userRepository = userRepository;
        this.seatRepository = seatRepository;
        this.zoneRepository = zoneRepository;
        this.seatStatusSyncService = seatStatusSyncService;
        this.librarySettingService = librarySettingService;
    }

    public ReservationEntity createBooking(UUID userId, Integer seatId,
            LocalDateTime startTime, LocalDateTime endTime) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        SeatEntity seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new RuntimeException("Seat not found"));

        // Lấy cấu hình giới hạn đặt chỗ
        LibrarySetting settings = librarySettingService.getSettings();
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
                    "Bạn đã đạt giới hạn " + maxBookingsPerDay + " lần đặt trong ngày " + bookingDate);
        }

        // Tính tổng số giờ đã đặt trong ngày
        long totalMinutesBooked = userBookingsToday.stream()
                .mapToLong(r -> Duration.between(r.getStartTime(), r.getEndTime()).toMinutes())
                .sum();
        long newBookingMinutes = Duration.between(startTime, endTime).toMinutes();
        int maxHoursPerDay = settings.getMaxHoursPerDay() != null ? settings.getMaxHoursPerDay() : 4;

        if ((totalMinutesBooked + newBookingMinutes) > maxHoursPerDay * 60) {
            throw new RuntimeException("Bạn đã đạt giới hạn " + maxHoursPerDay + " giờ đặt trong ngày " + bookingDate);
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

        // Sync seat status ngay lập tức (nếu trong khung giờ hiện tại)
        seatStatusSyncService.updateSeatStatus(seat, startTime, endTime, "PROCESSING");

        return saved;
    }

    public List<ZoneEntity> getAllZones() {
        return zoneRepository.findAll();
    }

    public List<SeatEntity> getAllSeats(Integer zoneId) {
        return seatRepository.findByZone_ZoneId(zoneId);
    }

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

    public List<ReservationEntity> getBookingsByUser(UUID userId) {
        return reservationRepository.findByUserId(userId);
    }

    public ReservationEntity cancelBooking(UUID reservationId) {
        ReservationEntity reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new RuntimeException("Reservation not found"));
        reservation.setStatus("CANCEL");
        ReservationEntity saved = reservationRepository.save(reservation);

        // Sync seat status ngay - trả về AVAILABLE nếu đang trong khung giờ
        seatStatusSyncService.updateSeatStatus(reservation.getSeat(),
                reservation.getStartTime(), reservation.getEndTime(), "CANCEL");

        return saved;
    }

    public ReservationEntity updateStatus(UUID reservationId, String status) {
        ReservationEntity reserv = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new RuntimeException("Reservation not found"));
        reserv.setStatus(status);
        ReservationEntity saved = reservationRepository.save(reserv);

        // Sync seat status ngay lập tức
        seatStatusSyncService.updateSeatStatus(reserv.getSeat(),
                reserv.getStartTime(), reserv.getEndTime(), status);

        return saved;
    }

    public List<ReservationEntity> getAllBookings() {
        return reservationRepository.findAll();
    }

    public List<SeatDTO> getAllSeatsDTO(Integer zoneId) {
        List<SeatEntity> seats = seatRepository.findByZone_ZoneId(zoneId);
        return seats.stream().map(this::mapToDTO).toList();
    }

    private SeatDTO mapToDTO(SeatEntity seat) {
        return new SeatDTO(
                seat.getSeatId(),
                seat.getSeatCode(),
                seat.getSeatStatus(),
                seat.getRowNumber(),
                seat.getColumnNumber(),
                seat.getZone().getZoneId());
    }

    public List<SeatDTO> getSeatsByTime(Integer zoneId, LocalDate date, LocalTime start, LocalTime end) {
        List<SeatEntity> seats = seatRepository.findByZone_ZoneId(zoneId);

        LocalDateTime startDateTime = LocalDateTime.of(date, start);
        LocalDateTime endDateTime = LocalDateTime.of(date, end);

        return seats.stream().map(seat -> {
            // Tìm reservation trùng time slot
            var matchingReservation = seat.getReservation().stream()
                    .filter(r -> (r.getStatus().equalsIgnoreCase("BOOKED") ||
                            r.getStatus().equalsIgnoreCase("PROCESSING")) &&
                            r.getStartTime().toLocalDate().equals(date) &&
                            r.getStartTime().isBefore(endDateTime) &&
                            r.getEndTime().isAfter(startDateTime))
                    .findFirst();

            // Xác định seat status dựa trên reservation status
            SeatStatus status = SeatStatus.AVAILABLE;
            if (matchingReservation.isPresent()) {
                String reservStatus = matchingReservation.get().getStatus();
                if ("BOOKED".equalsIgnoreCase(reservStatus)) {
                    status = SeatStatus.BOOKED;
                } else if ("PROCESSING".equalsIgnoreCase(reservStatus)) {
                    status = SeatStatus.HOLDING;
                }
            }

            return new SeatDTO(
                    seat.getSeatId(),
                    seat.getSeatCode(),
                    status,
                    seat.getRowNumber(),
                    seat.getColumnNumber(),
                    seat.getZone().getZoneId());
        }).toList();
    }

    public List<SeatDTO> getSeatsByDate(Integer zoneId, LocalDate date) {
        List<SeatEntity> seats = seatRepository.findByZone_ZoneId(zoneId);

        return seats.stream().map(seat -> {
            // Tìm reservation trong ngày
            var matchingReservation = seat.getReservation().stream()
                    .filter(r -> (r.getStatus().equalsIgnoreCase("BOOKED") ||
                            r.getStatus().equalsIgnoreCase("PROCESSING")) &&
                            r.getStartTime().toLocalDate().equals(date))
                    .findFirst();

            // Xác định seat status dựa trên reservation status
            SeatStatus status = SeatStatus.AVAILABLE;
            if (matchingReservation.isPresent()) {
                String reservStatus = matchingReservation.get().getStatus();
                if ("BOOKED".equalsIgnoreCase(reservStatus)) {
                    status = SeatStatus.BOOKED;
                } else if ("PROCESSING".equalsIgnoreCase(reservStatus)) {
                    status = SeatStatus.HOLDING;
                }
            }

            return new SeatDTO(
                    seat.getSeatId(),
                    seat.getSeatCode(),
                    status,
                    seat.getRowNumber(),
                    seat.getColumnNumber(),
                    seat.getZone().getZoneId());
        }).toList();
    }

    /**
     * Lấy tất cả seats của 1 area theo time slot
     * Trả về Map<zoneId, List<SeatDTO>> - tối ưu từ N query thành 1 batch
     */
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

}
