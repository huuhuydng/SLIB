package slib.com.example.service.zone_config;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import slib.com.example.entity.booking.ReservationEntity;
import slib.com.example.entity.zone_config.SeatEntity;
import slib.com.example.entity.zone_config.SeatStatus;
import slib.com.example.repository.booking.ReservationRepository;

/**
 * Service for calculating seat availability dynamically from reservations.
 * 
 * This replaces the old approach of storing seat_status in the seats table.
 * Status is now calculated based on overlapping reservations for a given time
 * range.
 */
@Service
@RequiredArgsConstructor
public class SeatAvailabilityService {

    private static final int PROCESSING_TIMEOUT_MINUTES = 10;

    private final ReservationRepository reservationRepository;

    /**
     * Calculate dynamic seat status for a specific time range.
     * 
     * Rules:
     * - UNAVAILABLE: if seat.isActive = false (admin disabled)
     * - AVAILABLE: no overlapping active reservations
     * - HOLDING: overlapping PROCESSING reservation within timeout (10 minutes)
     * - BOOKED: overlapping BOOKED or CONFIRMED reservation
     * 
     * @param seat       The seat entity to check
     * @param queryStart Start of the time range to check
     * @param queryEnd   End of the time range to check
     * @return Calculated SeatStatus for the time range
     */
    public SeatStatus calculateStatus(SeatEntity seat, LocalDateTime queryStart, LocalDateTime queryEnd) {
        // Check if seat is administratively disabled
        if (seat.getIsActive() == null || !seat.getIsActive()) {
            return SeatStatus.UNAVAILABLE;
        }

        // Find overlapping reservations with active statuses
        List<ReservationEntity> overlapping = reservationRepository
                .findOverlappingReservations(seat.getSeatId(), queryStart, queryEnd);

        if (overlapping.isEmpty()) {
            return SeatStatus.AVAILABLE;
        }

        // Check for PROCESSING (HOLDING) - must be within timeout
        LocalDateTime processingCutoff = LocalDateTime.now().minusMinutes(PROCESSING_TIMEOUT_MINUTES);
        boolean hasActiveProcessing = overlapping.stream()
                .anyMatch(r -> "PROCESSING".equalsIgnoreCase(r.getStatus())
                        && r.getCreatedAt() != null
                        && r.getCreatedAt().isAfter(processingCutoff));

        if (hasActiveProcessing) {
            return SeatStatus.HOLDING;
        }

        // Check for BOOKED/CONFIRMED
        boolean hasBooked = overlapping.stream()
                .anyMatch(r -> "BOOKED".equalsIgnoreCase(r.getStatus())
                        || "CONFIRMED".equalsIgnoreCase(r.getStatus()));

        return hasBooked ? SeatStatus.BOOKED : SeatStatus.AVAILABLE;
    }

    /**
     * Calculate status for current moment (now to now + 1 minute).
     * Useful for real-time seat status display.
     */
    public SeatStatus calculateCurrentStatus(SeatEntity seat) {
        LocalDateTime now = LocalDateTime.now();
        return calculateStatus(seat, now, now.plusMinutes(1));
    }

    /**
     * Check if a seat is available for a specific time range.
     * 
     * @param seatId    The seat ID to check
     * @param startTime Start of the time range
     * @param endTime   End of the time range
     * @return true if the seat is available, false otherwise
     */
    public boolean isAvailable(Integer seatId, LocalDateTime startTime, LocalDateTime endTime) {
        List<ReservationEntity> overlapping = reservationRepository
                .findOverlappingReservations(seatId, startTime, endTime);
        return overlapping.isEmpty();
    }
}
