package slib.com.example.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import slib.com.example.controller.booking.BookingController;
import slib.com.example.dto.booking.UpcomingBookingResponse;
import slib.com.example.entity.users.Role;
import slib.com.example.entity.users.User;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.repository.booking.ReservationRepository;
import slib.com.example.repository.users.UserRepository;
import slib.com.example.service.booking.BookingService;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = BookingController.class, excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = { slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-82: View warning for layout-affected booking - Unit Tests")
class FE82_ViewWarningForLayoutAffectedBookingTest {

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final String EMAIL = "student@fpt.edu.vn";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BookingService bookingService;

    @MockBean
    private ReservationRepository reservationRepository;

    @MockBean
    private UserRepository userRepository;

    @Test
    @WithMockUser(username = EMAIL, roles = "STUDENT")
    @DisplayName("UTCID01: Upcoming booking returns warning when layout has changed")
    void getUpcomingBooking_layoutAffected_returnsWarning() throws Exception {
        User user = new User();
        user.setId(USER_ID);
        user.setEmail(EMAIL);
        user.setRole(Role.STUDENT);

        UpcomingBookingResponse response = UpcomingBookingResponse.builder()
                .reservationId(UUID.randomUUID())
                .status("BOOKED")
                .seatCode("A4")
                .layoutChanged(true)
                .layoutChangeTitle("Sơ đồ thư viện đã thay đổi")
                .layoutChangeMessage("Ghế của bạn bị ảnh hưởng và cần đổi ghế hoặc hủy chỗ.")
                .canCancel(true)
                .canChangeSeat(true)
                .build();

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(bookingService.getUpcomingBooking(eq(USER_ID))).thenReturn(Optional.of(response));

        mockMvc.perform(get("/slib/bookings/upcoming/{userId}", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.layoutChanged").value(true))
                .andExpect(jsonPath("$.layoutChangeTitle").value("Sơ đồ thư viện đã thay đổi"))
                .andExpect(jsonPath("$.canCancel").value(true))
                .andExpect(jsonPath("$.canChangeSeat").value(true));
    }
}
