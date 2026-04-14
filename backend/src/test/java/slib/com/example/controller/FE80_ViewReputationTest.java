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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import slib.com.example.controller.users.StudentProfileController;
import slib.com.example.dto.users.StudentProfileResponse;
import slib.com.example.entity.users.Role;
import slib.com.example.entity.users.User;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.service.users.StudentProfileService;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit Tests for FE-80: View reputation score
 */
@WebMvcTest(value = StudentProfileController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-80: View reputation score - Unit Tests")
class FE80_ViewReputationTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private StudentProfileService studentProfileService;

        private final UUID studentId = UUID.randomUUID();

        private User mockUser() {
                User u = new User();
                u.setId(studentId);
                u.setEmail("student@fpt.edu.vn");
                u.setFullName("Nguyen Van A");
                u.setRole(Role.STUDENT);
                return u;
        }

        private RequestPostProcessor authenticatedUser(User user) {
                return request -> {
                        SecurityContextHolder.getContext().setAuthentication(
                                        new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList()));
                        return request;
                };
        }

        @Test
        @DisplayName("UTCID01: View current student profile with reputation score")
        void viewCurrentStudentProfile_withReputationScore() throws Exception {
                User user = mockUser();
                StudentProfileResponse response = StudentProfileResponse.builder()
                                .userId(studentId)
                                .reputationScore(100)
                                .build();
                when(studentProfileService.getOrCreateProfile(any(User.class))).thenReturn(response);

                mockMvc.perform(get("/slib/student-profile/me")
                                .with(authenticatedUser(user)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.reputationScore").value(100));
        }

        @Test
        @DisplayName("UTCID02: View student profile by userId")
        void viewStudentProfile_byUserId() throws Exception {
                StudentProfileResponse response = StudentProfileResponse.builder()
                                .userId(studentId)
                                .reputationScore(85)
                                .build();
                when(studentProfileService.getProfileByUserId(studentId))
                                .thenReturn(Optional.of(response));

                mockMvc.perform(get("/slib/student-profile/{userId}", studentId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.reputationScore").value(85));
        }

        @Test
        @DisplayName("UTCID03: View student profile by userId when profile does not exist")
        void viewStudentProfile_byUserIdWhenProfileDoesNotExist() throws Exception {
                when(studentProfileService.getProfileByUserId(studentId))
                                .thenReturn(Optional.empty());

                mockMvc.perform(get("/slib/student-profile/{userId}", studentId))
                                .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("UTCID04: View current student profile with zero reputation score")
        void viewCurrentStudentProfile_withZeroReputationScore() throws Exception {
                User user = mockUser();
                StudentProfileResponse response = StudentProfileResponse.builder()
                                .userId(studentId)
                                .reputationScore(0)
                                .build();
                when(studentProfileService.getOrCreateProfile(any(User.class))).thenReturn(response);

                mockMvc.perform(get("/slib/student-profile/me")
                                .with(authenticatedUser(user)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.reputationScore").value(0));
        }

        @Test
        @DisplayName("UTCID05: View current student profile when service fails")
        void viewCurrentStudentProfile_whenServiceFails() throws Exception {
                User user = mockUser();
                when(studentProfileService.getOrCreateProfile(any(User.class)))
                                .thenThrow(new RuntimeException("Database error"));

                mockMvc.perform(get("/slib/student-profile/me")
                                .with(authenticatedUser(user)))
                                .andExpect(status().isInternalServerError());
        }
}
