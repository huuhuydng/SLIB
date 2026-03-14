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
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import slib.com.example.dto.feedback.FeedbackDTO;
import slib.com.example.entity.users.Role;
import slib.com.example.entity.users.User;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.repository.users.UserRepository;
import slib.com.example.service.feedback.FeedbackService;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import slib.com.example.controller.feedback.FeedbackController;

/**
 * Unit Tests for FE-86: Create Feedback
 * Test Report: doc/Report/FE86_TestReport.md
 */
@WebMvcTest(value = FeedbackController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-86: Create Feedback - Unit Tests")
class FE86_CreateFeedbackTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private FeedbackService feedbackService;

        @MockBean
        private UserRepository userRepository;

        private final UUID studentId = UUID.randomUUID();

        private User mockUser() {
                User u = new User();
                u.setId(studentId);
                u.setEmail("student@fpt.edu.vn");
                u.setFullName("Nguyen Van A");
                u.setRole(Role.STUDENT);
                return u;
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

        @Test
        @DisplayName("UTCD01: Create feedback with valid data returns 201 Created")
        void createFeedback_validData_returns201() throws Exception {
                when(userRepository.findByEmail("student@fpt.edu.vn")).thenReturn(Optional.of(mockUser()));
                FeedbackDTO result = FeedbackDTO.builder()
                        .id(UUID.randomUUID()).studentId(studentId)
                        .rating(5).content("Great service")
                        .status("NEW").createdAt(LocalDateTime.now()).build();
                when(feedbackService.create(eq(studentId), eq(5), eq("Great service"))).thenReturn(result);

                mockMvc.perform(post("/slib/feedbacks")
                                .with(securityContext(userDetails()))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"rating\":5,\"content\":\"Great service\"}"))
                        .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("UTCD02: Create feedback without rating returns 400")
        void createFeedback_noRating_returns400() throws Exception {
                when(userRepository.findByEmail("student@fpt.edu.vn")).thenReturn(Optional.of(mockUser()));

                mockMvc.perform(post("/slib/feedbacks")
                                .with(securityContext(userDetails()))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"content\":\"No rating\"}"))
                        .andExpect(status().isBadRequest());
        }
}
