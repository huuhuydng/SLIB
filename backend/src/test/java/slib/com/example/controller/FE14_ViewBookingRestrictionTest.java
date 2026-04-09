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
import slib.com.example.controller.users.StudentProfileController;
import slib.com.example.dto.users.StudentProfileResponse;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.service.users.StudentProfileService;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for FE-14: View booking restriction status by reputation
 */
@WebMvcTest(value = StudentProfileController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-14: View booking restriction status by reputation - Unit Tests")
class FE14_ViewBookingRestrictionTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private StudentProfileService studentProfileService;

        private static final UUID TEST_USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

        @Test
        @WithMockUser(username = "student@fpt.edu.vn", roles = "STUDENT")
        @DisplayName("UTCD01: View profile with good reputation - no restriction - returns 200")
        void viewProfile_goodReputation_returns200() throws Exception {
                StudentProfileResponse resp = StudentProfileResponse.builder()
                                .userId(TEST_USER_ID)
                                .reputationScore(100)
                                .violationCount(0)
                                .build();
                when(studentProfileService.getProfileByUserId(TEST_USER_ID)).thenReturn(Optional.of(resp));

                mockMvc.perform(get("/slib/student-profile/{userId}", TEST_USER_ID))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.reputationScore").value(100));

                verify(studentProfileService, times(1)).getProfileByUserId(TEST_USER_ID);
        }

        @Test
        @WithMockUser(username = "student@fpt.edu.vn", roles = "STUDENT")
        @DisplayName("UTCD02: View profile with low reputation - booking restricted - returns 200")
        void viewProfile_lowReputation_bookingRestricted_returns200() throws Exception {
                StudentProfileResponse resp = StudentProfileResponse.builder()
                                .userId(TEST_USER_ID)
                                .reputationScore(20)
                                .violationCount(5)
                                .build();
                when(studentProfileService.getProfileByUserId(TEST_USER_ID)).thenReturn(Optional.of(resp));

                mockMvc.perform(get("/slib/student-profile/{userId}", TEST_USER_ID))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.reputationScore").value(20));
        }

        @Test
        @WithMockUser(username = "student@fpt.edu.vn", roles = "STUDENT")
        @DisplayName("UTCD03: View profile with zero reputation score - returns 200")
        void viewProfile_zeroReputation_returns200() throws Exception {
                StudentProfileResponse resp = StudentProfileResponse.builder()
                                .userId(TEST_USER_ID)
                                .reputationScore(0)
                                .violationCount(10)
                                .build();
                when(studentProfileService.getProfileByUserId(TEST_USER_ID)).thenReturn(Optional.of(resp));

                mockMvc.perform(get("/slib/student-profile/{userId}", TEST_USER_ID))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.reputationScore").value(0));
        }

        @Test
        @WithMockUser(username = "student@fpt.edu.vn", roles = "STUDENT")
        @DisplayName("UTCD04: View profile for non-existent user - returns 404")
        void viewProfile_userNotFound_returns404() throws Exception {
                UUID unknownId = UUID.randomUUID();
                when(studentProfileService.getProfileByUserId(unknownId)).thenReturn(Optional.empty());

                mockMvc.perform(get("/slib/student-profile/{userId}", unknownId))
                                .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser(username = "student@fpt.edu.vn", roles = "STUDENT")
        @DisplayName("UTCD05: Service throws RuntimeException - returns 500")
        void viewProfile_serviceError_returns500() throws Exception {
                when(studentProfileService.getProfileByUserId(TEST_USER_ID))
                                .thenThrow(new RuntimeException("Database connection failed"));

                mockMvc.perform(get("/slib/student-profile/{userId}", TEST_USER_ID))
                                .andExpect(status().isInternalServerError());
        }

        @Test
        @WithMockUser(username = "student@fpt.edu.vn", roles = "STUDENT")
        @DisplayName("UTCD06: View profile returns bookingRestriction field")
        void viewProfile_containsBookingRestrictionField_returns200() throws Exception {
                StudentProfileResponse resp = StudentProfileResponse.builder()
                                .userId(TEST_USER_ID)
                                .reputationScore(50)
                                .violationCount(3)
                                .build();
                when(studentProfileService.getProfileByUserId(TEST_USER_ID)).thenReturn(Optional.of(resp));

                mockMvc.perform(get("/slib/student-profile/{userId}", TEST_USER_ID))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.reputationScore").value(50))
                                .andExpect(jsonPath("$.violationCount").value(3));
        }
}
