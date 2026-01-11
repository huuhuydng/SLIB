package slib.com.example.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import slib.com.example.dto.SeatDTO;
import slib.com.example.entity.ReservationEntity;
import slib.com.example.entity.SeatEntity;
import slib.com.example.entity.SeatStatus;
import slib.com.example.entity.users.User;
import slib.com.example.entity.ZoneEntity;
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

    public BookingService(ReservationRepository reservationRepository, UserRepository userRepository,
            SeatRepository seatRepository, ZoneRepository zoneRepository) {
        this.reservationRepository = reservationRepository;
        this.userRepository = userRepository;
        this.seatRepository = seatRepository;
        this.zoneRepository = zoneRepository;
    }

    public ReservationEntity createBooking(UUID userId, Integer seatId,
            LocalDateTime startTime, LocalDateTime endTime) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        SeatEntity seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new RuntimeException("Seat not found"));

        // kiểm tra overlap với reservation cùng ngày
        boolean isBooked = seat.getReservation().stream()
                .anyMatch(r -> r.getStatus().equalsIgnoreCase("BOOKED") &&
                        r.getStartTime().toLocalDate().equals(startTime.toLocalDate()) &&
                        r.getStartTime().isBefore(endTime) &&
                        r.getEndTime().isAfter(startTime));

        if (isBooked) {
            throw new RuntimeException("Ghế đã được đặt");
        }

        ReservationEntity reservation = ReservationEntity.builder()
                .user(user)
                .seat(seat)
                .startTime(startTime)
                .endTime(endTime)
                .status("PROCESSING")
                .build();

        // 👉 cập nhật seat thành BOOKED ngay khi có PROCESSING
        seat.setSeatStatus(SeatStatus.BOOKED);
        seatRepository.save(seat);

        return reservationRepository.save(reservation);
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
        reservationRepository.save(reservation);

        // 👉 trả ghế về AVAILABLE
        SeatEntity seat = reservation.getSeat();
        seat.setSeatStatus(SeatStatus.AVAILABLE);
        seatRepository.save(seat);

        return reservation;
    }

    public ReservationEntity updateStatus(UUID reservationId, String status) {
        ReservationEntity reserv = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new RuntimeException("Reservation not found"));
        reserv.setStatus(status);
        reservationRepository.save(reserv);

        SeatEntity seat = reserv.getSeat();
        if ("CANCEL".equalsIgnoreCase(status)) {
            seat.setSeatStatus(SeatStatus.AVAILABLE);
        } else if ("BOOKED".equalsIgnoreCase(status) || "PROCESSING".equalsIgnoreCase(status)) {
            seat.setSeatStatus(SeatStatus.BOOKED);
        }
        seatRepository.save(seat);

        return reserv;
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
                seat.getPositionX(),
                seat.getPositionY(),
                seat.getZone().getZoneId());
    }

    public List<SeatDTO> getSeatsByTime(Integer zoneId, LocalDate date, LocalTime start, LocalTime end) {
        List<SeatEntity> seats = seatRepository.findByZone_ZoneId(zoneId);

        LocalDateTime startDateTime = LocalDateTime.of(date, start);
        LocalDateTime endDateTime = LocalDateTime.of(date, end);

        return seats.stream().map(seat -> {
            boolean isBooked = seat.getReservation().stream()
                    .anyMatch(r -> (r.getStatus().equalsIgnoreCase("BOOKED")
                            || r.getStatus().equalsIgnoreCase("PROCESSING")) &&
                            r.getStartTime().toLocalDate().equals(date) &&
                            r.getStartTime().isBefore(endDateTime) &&
                            r.getEndTime().isAfter(startDateTime));

            SeatDTO dto = new SeatDTO(
                    seat.getSeatId(),
                    seat.getSeatCode(),
                    isBooked ? SeatStatus.BOOKED : SeatStatus.AVAILABLE,
                    seat.getPositionX(),
                    seat.getPositionY(),
                    seat.getZone().getZoneId());
            return dto;
        }).toList();
    }

    public List<SeatDTO> getSeatsByDate(Integer zoneId, LocalDate date) {
        List<SeatEntity> seats = seatRepository.findByZone_ZoneId(zoneId);

        return seats.stream().map(seat -> {
            boolean isBooked = seat.getReservation().stream()
                    .anyMatch(r -> (r.getStatus().equalsIgnoreCase("BOOKED") ||
                            r.getStatus().equalsIgnoreCase("PROCESSING")) &&
                            r.getStartTime().toLocalDate().equals(date));

            return new SeatDTO(
                    seat.getSeatId(),
                    seat.getSeatCode(),
                    isBooked ? SeatStatus.BOOKED : SeatStatus.AVAILABLE,
                    seat.getPositionX(),
                    seat.getPositionY(),
                    seat.getZone().getZoneId());
        }).toList();
    }

}
