package slib.com.example.dto.booking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeatNfcActionStatusResponse {
    private UUID reservationId;
    private String reservationStatus;
    private Boolean checkedIntoLibrary;
    private Boolean canConfirmSeatWithNfc;
    private Boolean canLeaveSeatWithNfc;
    private String message;
}
