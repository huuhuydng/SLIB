package slib.com.example.service.feedback;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import slib.com.example.entity.booking.ReservationEntity;
import slib.com.example.entity.users.User;
import slib.com.example.entity.zone_config.SeatEntity;
import slib.com.example.entity.zone_config.ZoneEntity;
import slib.com.example.repository.booking.ReservationRepository;
import slib.com.example.repository.chat.ConversationRepository;
import slib.com.example.repository.feedback.FeedbackRepository;
import slib.com.example.repository.users.UserRepository;
import slib.com.example.service.notification.LibrarianNotificationService;
import slib.com.example.service.notification.PushNotificationService;

@ExtendWith(MockitoExtension.class)
@DisplayName("FeedbackService pending feedback logic")
class FeedbackServiceTest {

    @Mock
    private FeedbackRepository feedbackRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private LibrarianNotificationService librarianNotificationService;

    @Mock
    private PushNotificationService pushNotificationService;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private FeedbackService feedbackService;

    @Test
    @DisplayName("Returns pending feedback immediately after early checkout")
    void checkPendingFeedbackReturnsPendingForEarlyCheckout() {
        UUID userId = UUID.randomUUID();
        UUID reservationId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime actualEndTime = now.minusMinutes(3);
        LocalDateTime scheduledEndTime = now.plusHours(2);

        when(reservationRepository.findCompletedReservationsEligibleForFeedback(
                eq(userId),
                any(LocalDateTime.class),
                eq(List.of("COMPLETED")))).thenReturn(List.of(buildReservation(
                        reservationId,
                        actualEndTime,
                        scheduledEndTime)));
        when(feedbackRepository.existsByReservationId(reservationId)).thenReturn(false);

        Map<String, Object> result = feedbackService.checkPendingFeedback(userId);

        assertTrue((Boolean) result.get("hasPending"));
        assertEquals(reservationId.toString(), result.get("reservationId"));
        assertEquals(actualEndTime.toString(), result.get("endedAt"));
        assertEquals("Khu tự học", result.get("zoneName"));
        assertEquals("A01", result.get("seatCode"));
    }

    @Test
    @DisplayName("Returns no pending feedback when reservation already has feedback")
    void checkPendingFeedbackReturnsFalseWhenFeedbackExists() {
        UUID userId = UUID.randomUUID();
        UUID reservationId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        when(reservationRepository.findCompletedReservationsEligibleForFeedback(
                eq(userId),
                any(LocalDateTime.class),
                eq(List.of("COMPLETED")))).thenReturn(List.of(buildReservation(
                        reservationId,
                        now.minusMinutes(5),
                        now.plusMinutes(10))));
        when(feedbackRepository.existsByReservationId(reservationId)).thenReturn(true);

        Map<String, Object> result = feedbackService.checkPendingFeedback(userId);

        assertFalse((Boolean) result.get("hasPending"));
        verify(feedbackRepository).existsByReservationId(reservationId);
    }

    private ReservationEntity buildReservation(UUID reservationId, LocalDateTime actualEndTime, LocalDateTime endTime) {
        ZoneEntity zone = new ZoneEntity();
        zone.setZoneName("Khu tự học");

        SeatEntity seat = new SeatEntity();
        seat.setSeatCode("A01");
        seat.setZone(zone);

        User user = new User();
        user.setId(UUID.randomUUID());

        return ReservationEntity.builder()
                .reservationId(reservationId)
                .user(user)
                .seat(seat)
                .startTime(endTime.minusHours(2))
                .confirmedAt(endTime.minusHours(2))
                .actualEndTime(actualEndTime)
                .endTime(endTime)
                .status("COMPLETED")
                .build();
    }
}
