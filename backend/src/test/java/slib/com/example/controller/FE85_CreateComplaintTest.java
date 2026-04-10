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
import slib.com.example.dto.complaint.ComplaintDTO;
import slib.com.example.entity.users.Role;
import slib.com.example.entity.users.User;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.repository.users.UserRepository;
import slib.com.example.service.complaint.ComplaintService;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import slib.com.example.controller.complaint.ComplaintController;

/**
 * Unit Tests for FE-85: Create complaint
 * Test Report: doc/Report/FE85_TestReport.md
 */
@WebMvcTest(value = ComplaintController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-85: Create complaint - Unit Tests")
class FE85_CreateComplaintTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private ComplaintService complaintService;

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
        @DisplayName("UTCD01: Create complaint with valid data returns 201 Created")
        void createComplaint_validData_returns201() throws Exception {
                when(userRepository.findByEmail("student@fpt.edu.vn")).thenReturn(Optional.of(mockUser()));
                ComplaintDTO result = ComplaintDTO.builder()
                        .id(UUID.randomUUID()).studentId(studentId)
                        .subject("Test").content("Test complaint")
                        .status("PENDING").createdAt(LocalDateTime.now()).build();
                when(complaintService.create(eq(studentId), eq("Test"), eq("Test complaint"), any(), any()))
                        .thenReturn(result);

                mockMvc.perform(post("/slib/complaints")
                                .with(securityContext(userDetails()))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"subject\":\"Test\",\"content\":\"Test complaint\"}"))
                        .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("UTCD02: Create complaint without required fields returns 400")
        void createComplaint_missingFields_returns400() throws Exception {
                when(userRepository.findByEmail("student@fpt.edu.vn")).thenReturn(Optional.of(mockUser()));

                mockMvc.perform(post("/slib/complaints")
                                .with(securityContext(userDetails()))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"subject\":\"\",\"content\":\"\"}"))
                        .andExpect(status().isBadRequest());
        }
}
