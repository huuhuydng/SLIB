package slib.com.example.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import slib.com.example.entity.booking.ReservationEntity;
import slib.com.example.entity.zone_config.SeatEntity;
import slib.com.example.entity.zone_config.ZoneEntity;
import slib.com.example.repository.ReservationRepository;
import slib.com.example.repository.activity.ActivityLogRepository;
import slib.com.example.service.ReputationService;
import slib.com.example.service.SeatStatusSyncService;

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
    private final ActivityLogRepository activityLogRepository;
    private final ReputationService reputationService;
    private final SeatStatusSyncService seatStatusSyncService;

    public ReservationScheduler(
            ReservationRepository reservationRepository,
            ActivityLogRepository activityLogRepository,
            ReputationService reputationService,
            SeatStatusSyncService seatStatusSyncService) {
        this.reservationRepository = reservationRepository;
        this.activityLogRepository = activityLogRepository;
        this.reputationService = reputationService;
        this.seatStatusSyncService = seatStatusSyncService;
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

    /**
     * Check for reservations that haven't been checked in within 15 minutes and apply penalty.
     * 
     * Case 1: If user books BEFORE the time slot starts, they must check in within 15 minutes 
     *         AFTER the time slot starts.
     * Case 2: If user books DURING the time slot, they must check in within 15 minutes 
     *         AFTER the booking creation time.
     */
    @Scheduled(fixedRate = 60000) // Check every 60 seconds
    @Transactional
    public void checkLateCheckInsAndApplyPenalty() {
        LocalDateTime now = LocalDateTime.now();
        
        // Find all BOOKED reservations that have started
        List<ReservationEntity> activeReservations = reservationRepository.findBookedReservationsStarted(now);
        
        for (ReservationEntity reservation : activeReservations) {
            try {
                // Skip if already penalized
                if (activityLogRepository.hasLateCheckinPenalty(reservation.getReservationId())) {
                    continue;
                }
                
                // Skip if already checked in (NFC confirmed)
                if (activityLogRepository.hasNfcConfirmation(reservation.getReservationId())) {
                    continue;
                }
                
                // Calculate check-in deadline based on booking time
                LocalDateTime checkInDeadline;
                
                // Case 1: Booked BEFORE time slot started (createdAt < startTime)
                // Deadline = startTime + 15 minutes
                if (reservation.getCreatedAt().isBefore(reservation.getStartTime())) {
                    checkInDeadline = reservation.getStartTime().plusMinutes(15);
                }
                // Case 2: Booked DURING time slot (createdAt >= startTime)
                // Deadline = createdAt + 15 minutes
                else {
                    checkInDeadline = reservation.getCreatedAt().plusMinutes(15);
                }
                
                // Check if deadline has passed
                if (now.isAfter(checkInDeadline)) {
                    // Apply penalty
                    applyLateCheckInPenalty(reservation);
                }
            } catch (Exception e) {
                System.err.println("Error processing late check-in for reservation " + 
                    reservation.getReservationId() + ": " + e.getMessage());
            }
        }
    }
    
    /**
     * Apply late check-in penalty to a user using NO_SHOW rule
     * This now uses ReputationService for dynamic rule application
     * AND cancels the reservation to release the seat for others
     */
    private void applyLateCheckInPenalty(ReservationEntity reservation) {
        SeatEntity seat = reservation.getSeat();
        ZoneEntity zone = seat.getZone();
        String seatCode = seat.getSeatCode();
        String zoneName = zone != null ? zone.getZoneName() : "";
        
        // Step 1: Apply NO_SHOW penalty (deduct reputation points)
        boolean penaltySuccess = reputationService.applyNoShowPenalty(
            reservation.getUser().getId(),
            seatCode,
            zoneName,
            reservation.getReservationId()
        );
        
        if (penaltySuccess) {
            System.out.println(String.format(
                "Successfully applied NO_SHOW penalty to user %s for reservation %s",
                reservation.getUser().getId(), 
                reservation.getReservationId()));
        } else {
            System.err.println(String.format(
                "Failed to apply NO_SHOW penalty to user %s for reservation %s",
                reservation.getUser().getId(), 
                reservation.getReservationId()));
        }
        
        // Step 2: Cancel reservation and release seat for others
        try {
            reservation.setStatus("CANCELLED");
            reservationRepository.save(reservation);
            
            // Step 3: Broadcast seat status change via WebSocket
            // This allows other users to book the seat immediately
            seatStatusSyncService.broadcastSeatUpdateWithTimeSlot(
                seat, 
                "AVAILABLE",
                reservation.getStartTime(), 
                reservation.getEndTime()
            );
            
            System.out.println(String.format(
                "Cancelled reservation %s and released seat %s for rebooking",
                reservation.getReservationId(),
                seatCode));
        } catch (Exception e) {
            System.err.println(String.format(
                "Failed to cancel reservation %s: %s",
                reservation.getReservationId(),
                e.getMessage()));
        }
    }
}
