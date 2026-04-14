package slib.com.example.service.analytics;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import slib.com.example.entity.analytics.StudentBehaviorEntity;
import slib.com.example.repository.analytics.StudentBehaviorRepository;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StudentBehaviorService {

    private final StudentBehaviorRepository behaviorRepository;

    /**
     * Ghi nhận một hành vi của sinh viên
     */
    public StudentBehaviorEntity recordBehavior(UUID userId, StudentBehaviorEntity.BehaviorType type, String description) {
        return recordBehavior(userId, type, description, null, null, null, null, null);
    }

    public StudentBehaviorEntity recordBehavior(UUID userId, StudentBehaviorEntity.BehaviorType type, String description,
                                                UUID bookingId, Integer seatId, Integer zoneId, Integer pointsImpact, String metadata) {
        StudentBehaviorEntity behavior = StudentBehaviorEntity.builder()
                .userId(userId)
                .behaviorType(type)
                .description(description)
                .relatedBookingId(bookingId)
                .relatedSeatId(seatId)
                .relatedZoneId(zoneId)
                .pointsImpact(pointsImpact)
                .metadata(metadata)
                .build();

        return behaviorRepository.save(behavior);
    }

    /**
     * Ghi nhận no-show (gọi khi booking expired mà không confirmed)
     */
    public void recordNoShow(UUID userId, UUID bookingId) {
        recordBehavior(userId, StudentBehaviorEntity.BehaviorType.BOOKING_NO_SHOW,
                "Sinh viên không đến", bookingId, null, null, -10, null);
    }

    /**
     * Ghi nhận check-in đúng giờ
     */
    public void recordOnTimeCheckIn(UUID userId, UUID bookingId, Integer seatId, Integer zoneId) {
        recordBehavior(userId, StudentBehaviorEntity.BehaviorType.CHECKIN_ON_TIME,
                "Check-in đúng giờ", bookingId, seatId, zoneId, +5, null);
    }

    /**
     * Ghi nhận check-in muộn
     */
    public void recordLateCheckIn(UUID userId, UUID bookingId, Integer seatId, Integer zoneId, int minutesLate) {
        recordBehavior(userId, StudentBehaviorEntity.BehaviorType.CHECKIN_LATE,
                "Check-in muộn " + minutesLate + " phút", bookingId, seatId, zoneId, 0,
                "{\"minutesLate\": " + minutesLate + "}");
    }

    public boolean hasRecordedSeatHolding(UUID userId, UUID logId, LocalDateTime checkInTime) {
        String logMarker = logId != null ? "\"logId\":\"" + logId + "\"" : "";
        String checkInMarker = checkInTime != null ? "\"checkInTime\":\"" + checkInTime + "\"" : "";

        return behaviorRepository.existsRecordedBehavior(
                userId,
                StudentBehaviorEntity.BehaviorType.SEAT_HOLDING,
                logMarker,
                checkInMarker);
    }
}
