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
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import slib.com.example.controller.support.SupportRequestController;
import slib.com.example.dto.support.SupportRequestDTO;
import slib.com.example.entity.support.SupportRequestStatus;
import slib.com.example.entity.users.Role;
import slib.com.example.entity.users.User;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.repository.users.UserRepository;
import slib.com.example.service.support.SupportRequestService;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for FE-115: Send Request for Support
 * Test Report: doc/Report/UnitTestReport/FE115_TestReport.md
 */
@WebMvcTest(value = SupportRequestController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-115: Send Request for Support - Unit Tests")
class FE115_SendSupportRequestTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private SupportRequestService supportRequestService;

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
        @DisplayName("UTCID01: Create support request with valid data returns 201 Created")
        void createSupportRequest_validData_returns201() throws Exception {
                when(userRepository.findByEmail("student@fpt.edu.vn")).thenReturn(Optional.of(mockUser()));
                SupportRequestDTO result = SupportRequestDTO.builder()
                        .id(UUID.randomUUID()).studentId(studentId)
                        .description("May tinh bi hong")
                        .status(SupportRequestStatus.PENDING)
                        .createdAt(LocalDateTime.now()).build();
                when(supportRequestService.create(eq(studentId), eq("May tinh bi hong"), any())).thenReturn(result);

                mockMvc.perform(multipart("/slib/support-requests")
                                .param("description", "May tinh bi hong")
                                .with(securityContext(userDetails())))
                        .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("UTCID02: Create support request with empty description returns 400")
        void createSupportRequest_emptyDescription_returns400() throws Exception {
                when(userRepository.findByEmail("student@fpt.edu.vn")).thenReturn(Optional.of(mockUser()));
                when(supportRequestService.create(eq(studentId), eq(""), any()))
                        .thenThrow(new slib.com.example.exception.BadRequestException("Noi dung khong duoc de trong"));

                mockMvc.perform(multipart("/slib/support-requests")
                                .param("description", "")
                                .with(securityContext(userDetails())))
                        .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("UTCID03: Create support request with max length description returns 201")
        void createSupportRequest_maxLengthDescription_returns201() throws Exception {
                when(userRepository.findByEmail("student@fpt.edu.vn")).thenReturn(Optional.of(mockUser()));
                String longDesc = "A".repeat(1000);
                SupportRequestDTO result = SupportRequestDTO.builder()
                        .id(UUID.randomUUID()).studentId(studentId)
                        .description(longDesc)
                        .status(SupportRequestStatus.PENDING)
                        .createdAt(LocalDateTime.now()).build();
                when(supportRequestService.create(eq(studentId), eq(longDesc), any())).thenReturn(result);

                mockMvc.perform(multipart("/slib/support-requests")
                                .param("description", longDesc)
                                .with(securityContext(userDetails())))
                        .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("UTCID04: Service throws exception returns 500")
        void createSupportRequest_serviceException_returns500() throws Exception {
                when(userRepository.findByEmail("student@fpt.edu.vn")).thenReturn(Optional.of(mockUser()));
                when(supportRequestService.create(any(), any(), any()))
                        .thenThrow(new RuntimeException("Loi he thong"));

                mockMvc.perform(multipart("/slib/support-requests")
                                .param("description", "Test request")
                                .with(securityContext(userDetails())))
                        .andExpect(status().isInternalServerError());
        }

        @Test
        @DisplayName("UTCID05: Create support request without auth returns 403")
        void createSupportRequest_noAuth_returns403() throws Exception {
                mockMvc.perform(multipart("/slib/support-requests")
                                .param("description", "Test request"))
                        .andExpect(status().isForbidden());
        }
}
