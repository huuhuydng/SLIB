package slib.com.example.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import slib.com.example.entity.booking.ReservationEntity;
import slib.com.example.repository.ReservationRepository;

/**
 * Scheduler for handling reservation expirations.
 * 
 * With the new dynamic status calculation, this scheduler only updates
 * reservation statuses. Seat availability is calculated dynamically from
 * the reservations table.
 */
@Service
public class ReservationScheduler {
    private final ReservationRepository reservationRepository;

    public ReservationScheduler(ReservationRepository reservationRepository) {
        this.reservationRepository = reservationRepository;
    }

    @Scheduled(fixedRate = 60000) // check every 60s
    public void releaseExpiredReservations() {
        LocalDateTime now = LocalDateTime.now();

        // 1. Expire BOOKED reservations that have ended
        List<ReservationEntity> expiredBooked = reservationRepository.findByEndTimeBeforeAndStatus(now, "BOOKED");
        for (ReservationEntity r : expiredBooked) {
            r.setStatus("EXPIRED");
            reservationRepository.save(r);
        }

        // 2. Expire CONFIRMED reservations that have ended
        List<ReservationEntity> expiredConfirmed = reservationRepository.findByEndTimeBeforeAndStatus(now, "CONFIRMED");
        for (ReservationEntity r : expiredConfirmed) {
            r.setStatus("EXPIRED");
            reservationRepository.save(r);
        }

        // 3. Cancel PROCESSING reservations older than 2 minutes (120s)
        LocalDateTime cutoff = now.minusSeconds(120);
        List<ReservationEntity> processingExpired = reservationRepository.findByCreatedAtBeforeAndStatus(cutoff,
                "PROCESSING");

        for (ReservationEntity r : processingExpired) {
            r.setStatus("CANCELLED");
            reservationRepository.save(r);
        }

        // Note: No need to update seat_status anymore - availability is calculated
        // dynamically from reservations when queried.
    }
}
