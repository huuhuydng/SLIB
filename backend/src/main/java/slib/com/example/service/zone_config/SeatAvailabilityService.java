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

    private static final long PROCESSING_TIMEOUT_SECONDS = 120L;

    private final ReservationRepository reservationRepository;

    /**
     * Calculate dynamic seat status for a specific time range.
     * 
     * Rules:
     * - UNAVAILABLE: if seat.isActive = false (admin disabled)
     * - AVAILABLE: no overlapping active reservations
     * - CONFIRMED: overlapping CONFIRMED reservation
     * - BOOKED: overlapping BOOKED reservation
     * - HOLDING: overlapping PROCESSING reservation within timeout
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

        boolean hasConfirmed = overlapping.stream()
                .anyMatch(r -> "CONFIRMED".equalsIgnoreCase(r.getStatus()));
        if (hasConfirmed) {
            return SeatStatus.CONFIRMED;
        }

        boolean hasBooked = overlapping.stream()
                .anyMatch(r -> "BOOKED".equalsIgnoreCase(r.getStatus()));
        if (hasBooked) {
            return SeatStatus.BOOKED;
        }

        LocalDateTime processingCutoff = LocalDateTime.now().minusSeconds(PROCESSING_TIMEOUT_SECONDS);
        boolean hasActiveProcessing = overlapping.stream()
                .anyMatch(r -> "PROCESSING".equalsIgnoreCase(r.getStatus())
                        && r.getCreatedAt() != null
                        && r.getCreatedAt().isAfter(processingCutoff));

        return hasActiveProcessing ? SeatStatus.HOLDING : SeatStatus.AVAILABLE;
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
