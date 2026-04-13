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
public class BookingHistoryResponse {

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

    // Time info
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime actualEndTime;
    private LocalDateTime createdAt;
    private String cancellationReason;
    private Boolean cancelledByStaff;
    private Boolean layoutChanged;
    private String layoutChangeTitle;
    private String layoutChangeMessage;
    private LocalDateTime layoutChangedAt;
    private Boolean canCancel;
    private Boolean canChangeSeat;
}
