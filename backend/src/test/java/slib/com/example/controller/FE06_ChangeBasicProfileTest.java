package slib.com.example.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import slib.com.example.controller.users.UserController;
import slib.com.example.dto.users.UserProfileResponse;
import slib.com.example.entity.users.User;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.service.AsyncImportService;
import slib.com.example.service.AuthService;
import slib.com.example.service.StagingImportService;
import slib.com.example.service.UserService;
import slib.com.example.service.chat.CloudinaryService;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for FE-06: Change Basic Profile
 * Test Report: doc/Report/FE06_TestReport.md
 */
@WebMvcTest(value = UserController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-06: Change Basic Profile - Unit Tests")
class FE06_ChangeBasicProfileTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private UserService userService;

        @MockBean
        private AuthService authService;

        @MockBean
        private CloudinaryService cloudinaryService;

        @MockBean
        private AsyncImportService asyncImportService;

        @MockBean
        private StagingImportService stagingImportService;

        @Autowired
        private ObjectMapper objectMapper;

        private RequestPostProcessor authenticatedUser(String email) {
                return request -> {
                        var user = org.springframework.security.core.userdetails.User.withUsername(email)
                                        .password("pass").roles("STUDENT").build();
                        SecurityContextHolder.getContext().setAuthentication(
                                        new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
                        return request;
                };
        }

        @BeforeEach
        void clearSecurityContext() {
                SecurityContextHolder.clearContext();
        }

        // UTCD01: Valid data - Success
        @Test
        @DisplayName("UTCD01: Update profile with valid data returns 200 OK")
        void updateProfile_validData_returns200OK() throws Exception {
                User updateRequest = User.builder()
                        .fullName("New Name")
                        .phone("0123456789")
                        .build();

                when(userService.getMyProfile(anyString())).thenReturn(
                        UserProfileResponse.builder()
                                .id(UUID.randomUUID())
                                .email("student@fpt.edu.vn")
                                .fullName("Old Name")
                                .build()
                );

                when(userService.updateUser(any(), any())).thenReturn(updateRequest);

                mockMvc.perform(patch("/slib/users/me")
                                .with(authenticatedUser("student@fpt.edu.vn"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(updateRequest)))
                        .andExpect(status().isOk());

                verify(userService, times(1)).updateUser(any(), any());
        }

        // UTCD02: No token - 401
        @Test
        @DisplayName("UTCD02: Update profile without token returns 401 Unauthorized")
        void updateProfile_noToken_returns401Unauthorized() throws Exception {
                User updateRequest = User.builder().fullName("New Name").build();

                mockMvc.perform(patch("/slib/users/me")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(updateRequest)))
                        .andExpect(status().isUnauthorized());
        }

        // UTCD04: Invalid data - 400
        @Test
        @DisplayName("UTCD04: Update profile with invalid data returns 400 Bad Request")
        void updateProfile_invalidData_returns400BadRequest() throws Exception {
                User updateRequest = User.builder().fullName("").phone("invalid").build();

                when(userService.getMyProfile(anyString())).thenReturn(
                        UserProfileResponse.builder()
                                .id(UUID.randomUUID())
                                .email("student@fpt.edu.vn")
                                .build()
                );

                when(userService.updateUser(any(), any()))
                        .thenThrow(new RuntimeException("Invalid data"));

                mockMvc.perform(patch("/slib/users/me")
                                .with(authenticatedUser("student@fpt.edu.vn"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(updateRequest)))
                        .andExpect(status().isBadRequest());
        }

        // UTCD05: Email already used - 400 (controller catches all exceptions as badRequest)
        @Test
        @DisplayName("UTCD05: Update email to existing email returns 400 Bad Request")
        void updateProfile_emailExists_returns400BadRequest() throws Exception {
                User updateRequest = User.builder().email("existing@fpt.edu.vn").build();

                when(userService.getMyProfile(anyString())).thenReturn(
                        UserProfileResponse.builder()
                                .id(UUID.randomUUID())
                                .email("student@fpt.edu.vn")
                                .build()
                );

                when(userService.updateUser(any(), any()))
                        .thenThrow(new RuntimeException("Email already used"));

                mockMvc.perform(patch("/slib/users/me")
                                .with(authenticatedUser("student@fpt.edu.vn"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(updateRequest)))
                        .andExpect(status().isBadRequest());
        }

        // UTCD06: User not found - 400 (controller catches all exceptions as badRequest)
        @Test
        @DisplayName("UTCD06: Update profile with non-existent user returns 400 Bad Request")
        void updateProfile_userNotFound_returns400BadRequest() throws Exception {
                User updateRequest = User.builder().fullName("New Name").build();

                when(userService.getMyProfile(anyString()))
                        .thenThrow(new RuntimeException("User not found"));

                mockMvc.perform(patch("/slib/users/me")
                                .with(authenticatedUser("unknown@fpt.edu.vn"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(updateRequest)))
                        .andExpect(status().isBadRequest());
        }

        // UTCD08: System error - 400 (controller catches all exceptions as badRequest)
        @Test
        @DisplayName("UTCD08: System error returns 400 Bad Request")
        void updateProfile_systemError_returns400BadRequest() throws Exception {
                User updateRequest = User.builder().fullName("New Name").build();

                when(userService.getMyProfile(anyString())).thenReturn(
                        UserProfileResponse.builder()
                                .id(UUID.randomUUID())
                                .email("student@fpt.edu.vn")
                                .build()
                );

                when(userService.updateUser(any(), any()))
                        .thenThrow(new RuntimeException("Database error"));

                mockMvc.perform(patch("/slib/users/me")
                                .with(authenticatedUser("student@fpt.edu.vn"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(updateRequest)))
                        .andExpect(status().isBadRequest());
        }
}
