package slib.com.example.dto.booking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpcomingBookingResponse {

    private UUID reservationId;
    private String status;

    // Seat info
    private Integer seatId;
    private String seatCode;

    // Zone info
    private Integer zoneId;
    private String zoneName;

    // Area info
    private Integer areaId;
    private String areaName;
    private Integer floorNumber;

    // Time info
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    // Formatted for display
    private String dayOfWeek; // "TH 2", "TH 3", etc.
    private int dayOfMonth; // 24
    private String timeRange; // "14:00 - 16:00"

    // Layout change warning
    private Boolean layoutChanged;
    private String layoutChangeTitle;
    private String layoutChangeMessage;
    private LocalDateTime layoutChangedAt;
    private Boolean canCancel;
    private Boolean canChangeSeat;
}
