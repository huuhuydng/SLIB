package slib.com.example.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import slib.com.example.entity.booking.ReservationEntity;
import slib.com.example.entity.zone_config.SeatEntity;
import slib.com.example.entity.zone_config.SeatStatus;
import slib.com.example.repository.ReservationRepository;
import slib.com.example.repository.SeatRepository;

@Service
public class ReservationScheduler {
    private final ReservationRepository reservationRepository;
    private final SeatRepository seatRepository;

    public ReservationScheduler(ReservationRepository reservationRepository,
            SeatRepository seatRepository) {
        this.reservationRepository = reservationRepository;
        this.seatRepository = seatRepository;
    }

    @Scheduled(fixedRate = 60000) // check mỗi 60s
    public void releaseExpiredSeats() {
        LocalDateTime now = LocalDateTime.now();

        // 1. Hủy các reservation BOOKED đã hết hạn
        List<ReservationEntity> expired = reservationRepository.findByEndTimeBeforeAndStatus(now, "BOOKED");
        for (ReservationEntity r : expired) {
            r.setStatus("EXPIRED");
            reservationRepository.save(r);

            SeatEntity seat = r.getSeat();
            seat.setSeatStatus(SeatStatus.AVAILABLE);
            seatRepository.save(seat);
        }

        // 2. Hủy các reservation PROCESSING quá 2 phút (120s)
        LocalDateTime cutoff = now.minusSeconds(120);
        List<ReservationEntity> processingExpired = reservationRepository.findByCreatedAtBeforeAndStatus(cutoff,
                "PROCESSING");

        for (ReservationEntity r : processingExpired) {
            r.setStatus("CANCEL");
            reservationRepository.save(r);

            SeatEntity seat = r.getSeat();
            seat.setSeatStatus(SeatStatus.AVAILABLE);
            seatRepository.save(seat);
        }

        // 3. Kích hoạt seat_status = BOOKED cho reservations đang trong khung giờ
        activateBookedSeats(now);
    }

    /**
     * Update seat_status thành BOOKED khi reservation BOOKED đến giờ bắt đầu
     */
    private void activateBookedSeats(LocalDateTime now) {
        // Tìm tất cả reservations BOOKED đang trong khung giờ active
        List<ReservationEntity> activeBookings = reservationRepository.findAll().stream()
                .filter(r -> "BOOKED".equalsIgnoreCase(r.getStatus()))
                .filter(r -> !now.isBefore(r.getStartTime()) && now.isBefore(r.getEndTime()))
                .toList();

        for (ReservationEntity r : activeBookings) {
            SeatEntity seat = r.getSeat();
            if (seat.getSeatStatus() != SeatStatus.BOOKED) {
                seat.setSeatStatus(SeatStatus.BOOKED);
                seatRepository.save(seat);
            }
        }
    }
}
