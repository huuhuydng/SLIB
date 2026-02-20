package slib.com.example.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import slib.com.example.entity.booking.ReservationEntity;
import slib.com.example.entity.zone_config.SeatEntity;
import slib.com.example.entity.zone_config.SeatStatus;
import slib.com.example.repository.ReservationRepository;
import slib.com.example.repository.SeatRepository;

@Service
public class ReservationScheduler {
    private final ReservationRepository reservationRepository;
    private final SeatRepository seatRepository;
    private final SeatStatusSyncService seatStatusSyncService;

    public ReservationScheduler(ReservationRepository reservationRepository,
            SeatRepository seatRepository,
            SeatStatusSyncService seatStatusSyncService) {
        this.reservationRepository = reservationRepository;
        this.seatRepository = seatRepository;
        this.seatStatusSyncService = seatStatusSyncService;
    }

    @Scheduled(fixedRate = 10000) // check mỗi 10s cho real-time
    @Transactional
    public void releaseExpiredSeats() {
        try {
            LocalDateTime now = LocalDateTime.now();

            // 1. Hủy các reservation BOOKED đã hết hạn (endTime < now)
            List<ReservationEntity> expiredBooked = reservationRepository.findByEndTimeBeforeAndStatus(now, "BOOKED");
            for (ReservationEntity r : expiredBooked) {
                r.setStatus("EXPIRED");
                reservationRepository.save(r);
                // Broadcast WebSocket để clients cập nhật real-time
                seatStatusSyncService.broadcastSeatUpdateWithTimeSlot(
                        r.getSeat(), "AVAILABLE", r.getStartTime(), r.getEndTime());
            }

            // 2. Hủy các reservation CONFIRMED đã hết hạn (endTime < now)
            List<ReservationEntity> expiredConfirmed = reservationRepository.findByEndTimeBeforeAndStatus(now,
                    "CONFIRMED");
            for (ReservationEntity r : expiredConfirmed) {
                r.setStatus("EXPIRED");
                reservationRepository.save(r);
                // Broadcast WebSocket để clients cập nhật real-time
                seatStatusSyncService.broadcastSeatUpdateWithTimeSlot(
                        r.getSeat(), "AVAILABLE", r.getStartTime(), r.getEndTime());
            }

            // 3. Hủy các reservation PROCESSING quá 2 phút (120s)
            LocalDateTime cutoff = now.minusSeconds(120);
            List<ReservationEntity> processingExpired = reservationRepository.findByCreatedAtBeforeAndStatus(cutoff,
                    "PROCESSING");

            for (ReservationEntity r : processingExpired) {
                r.setStatus("CANCEL");
                reservationRepository.save(r);
                seatStatusSyncService.broadcastSeatUpdateWithTimeSlot(
                        r.getSeat(), "AVAILABLE", r.getStartTime(), r.getEndTime());
            }
        } catch (Exception e) {
            // Log error nhưng không để crash application
            System.err.println("Error in releaseExpiredSeats: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
