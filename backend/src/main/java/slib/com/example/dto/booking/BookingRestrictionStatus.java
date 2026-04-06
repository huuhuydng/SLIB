package slib.com.example.dto.booking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingRestrictionStatus {

    private Boolean allowedNow;
    private String restrictionReason;
    private String policyHint;
    private LocalDateTime blockedUntil;
    private Integer remainingDays;
    private Integer remainingHours;
}
