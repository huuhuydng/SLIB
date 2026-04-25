package slib.com.example.controller;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import slib.com.example.controller.feedback.FeedbackController;
import slib.com.example.dto.feedback.FeedbackDTO;
import slib.com.example.entity.users.Role;
import slib.com.example.entity.users.User;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.repository.users.UserRepository;
import slib.com.example.service.feedback.FeedbackService;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = FeedbackController.class, excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = { slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-99: Create feedback after check-out - Unit Tests")
class FE99_CreateFeedbackTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FeedbackService feedbackService;

    @MockBean
    private UserRepository userRepository;

    private final UUID studentId = UUID.fromString("44444444-4444-4444-4444-444444444444");

    @Test
    @DisplayName("UTCID01: Create feedback with rating and content")
    void createFeedbackWithRatingAndContent() throws Exception {
        mockCurrentUser();
        when(feedbackService.create(eq(studentId), eq(5), eq("Great service"), eq(null), eq(null), eq(null)))
                .thenReturn(feedback(5, "Great service", null));

        mockMvc.perform(post("/slib/feedbacks")
                        .with(securityContext(userDetails()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"rating":5,"content":"Great service"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.studentId").value(studentId.toString()))
                .andExpect(jsonPath("$.rating").value(5))
                .andExpect(jsonPath("$.status").value("NEW"));
    }

    @Test
    @DisplayName("UTCID02: Create feedback with rating, category, and reservationId")
    void createFeedbackWithRatingCategoryAndReservationId() throws Exception {
        UUID reservationId = UUID.randomUUID();
        mockCurrentUser();
        when(feedbackService.create(eq(studentId), eq(4), eq(null), eq("GENERAL"), eq(null), eq(reservationId)))
                .thenReturn(FeedbackDTO.builder()
                        .id(UUID.randomUUID())
                        .studentId(studentId)
                        .rating(4)
                        .category("GENERAL")
                        .reservationId(reservationId)
                        .status("NEW")
                        .createdAt(LocalDateTime.now())
                        .build());

        mockMvc.perform(post("/slib/feedbacks")
                        .with(securityContext(userDetails()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"rating":4,"category":"GENERAL","reservationId":"%s"}
                                """.formatted(reservationId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.category").value("GENERAL"))
                .andExpect(jsonPath("$.reservationId").value(reservationId.toString()));
    }

    @Test
    @DisplayName("UTCID03: Create feedback without rating")
    void createFeedbackWithoutRating() throws Exception {
        mockMvc.perform(post("/slib/feedbacks")
                        .with(securityContext(userDetails()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"No rating"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors.rating").value("Đánh giá không được để trống"));
    }

    @Test
    @DisplayName("UTCID04: Create feedback with rating greater than 5")
    void createFeedbackWithRatingGreaterThan5() throws Exception {
        mockMvc.perform(post("/slib/feedbacks")
                        .with(securityContext(userDetails()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"rating":6,"content":"Too high"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors.rating").value("Đánh giá phải từ 1 đến 5 sao"));
    }

    @Test
    @DisplayName("UTCID05: Create feedback without authenticated user")
    void createFeedbackWithoutAuthenticatedUser() throws Exception {
        mockMvc.perform(post("/slib/feedbacks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"rating":5,"content":"Great service"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    @DisplayName("UTCID06: Create feedback when feedback service throws runtime exception")
    void createFeedbackWhenFeedbackServiceThrowsRuntimeException() throws Exception {
        mockCurrentUser();
        when(feedbackService.create(eq(studentId), eq(5), eq("Great service"), eq(null), eq(null), eq(null)))
                .thenThrow(new RuntimeException("Database error"));

        mockMvc.perform(post("/slib/feedbacks")
                        .with(securityContext(userDetails()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"rating":5,"content":"Great service"}
                                """))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value("Database error"));
    }

    private void mockCurrentUser() {
        User user = new User();
        user.setId(studentId);
        user.setEmail("student@fpt.edu.vn");
        user.setFullName("Nguyen Van A");
        user.setRole(Role.STUDENT);
        when(userRepository.findByEmail("student@fpt.edu.vn")).thenReturn(Optional.of(user));
    }

    private UserDetails userDetails() {
        return org.springframework.security.core.userdetails.User
                .withUsername("student@fpt.edu.vn")
                .password("pass")
                .roles("STUDENT")
                .build();
    }

    private RequestPostProcessor securityContext(UserDetails userDetails) {
        return request -> {
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities()));
            return request;
        };
    }

    private FeedbackDTO feedback(Integer rating, String content, String category) {
        return FeedbackDTO.builder()
                .id(UUID.randomUUID())
                .studentId(studentId)
                .studentName("Nguyen Van A")
                .rating(rating)
                .content(content)
                .category(category)
                .status("NEW")
                .createdAt(LocalDateTime.now())
                .build();
    }
}
