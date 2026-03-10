package slib.com.example.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.test.web.servlet.MockMvc;
import slib.com.example.dto.users.UserProfileResponse;
import slib.com.example.entity.users.User;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.service.UserService;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for FE-05: Change Basic Profile
 * Test Report: doc/Report/FE05_TestReport.md
 */
@WebMvcTest(value = UserController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-05: Change Basic Profile - Unit Tests")
class FE05_ChangeBasicProfileTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private UserService userService;

        @Autowired
        private ObjectMapper objectMapper;

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
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(updateRequest)))
                        .andExpect(status().isBadRequest());
        }

        // UTCD05: Email already used - 409
        @Test
        @DisplayName("UTCD05: Update email to existing email returns 409 Conflict")
        void updateProfile_emailExists_returns409Conflict() throws Exception {
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
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(updateRequest)))
                        .andExpect(status().isConflict());
        }

        // UTCD06: User not found - 404
        @Test
        @DisplayName("UTCD06: Update profile with non-existent user returns 404 Not Found")
        void updateProfile_userNotFound_returns404NotFound() throws Exception {
                User updateRequest = User.builder().fullName("New Name").build();

                when(userService.getMyProfile(anyString()))
                        .thenThrow(new RuntimeException("User not found"));

                mockMvc.perform(patch("/slib/users/me")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(updateRequest)))
                        .andExpect(status().isNotFound());
        }

        // UTCD08: System error - 500
        @Test
        @DisplayName("UTCD08: System error returns 500 Internal Server Error")
        void updateProfile_systemError_returns500InternalServerError() throws Exception {
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
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(updateRequest)))
                        .andExpect(status().isInternalServerError());
        }
}
