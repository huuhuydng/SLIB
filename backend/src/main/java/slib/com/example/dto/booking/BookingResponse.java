package slib.com.example.dto.booking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingResponse {

    private UUID reservationId;

    // Nested user info (frontend expects booking.user.fullName, etc.)
    private UserInfo user;

    // Nested seat info (frontend expects booking.seat.seatCode, booking.seat.zone.zoneName, etc.)
    private SeatInfo seat;

    // Booking info
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;
    private LocalDateTime createdAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfo {
        private UUID id;
        private String userCode;
        private String fullName;
        private String email;
        private String avtUrl;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SeatInfo {
        private Integer id;
        private String seatCode;
        private ZoneInfo zone;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ZoneInfo {
        private Integer id;
        private String zoneName;
        private AreaInfo area;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AreaInfo {
        private Long id;
        private String areaName;
    }
}
