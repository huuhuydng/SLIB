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
import org.springframework.test.web.servlet.MockMvc;
import slib.com.example.controller.support.SupportRequestController;
import slib.com.example.dto.support.SupportRequestDTO;
import slib.com.example.entity.support.SupportRequestStatus;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.repository.users.UserRepository;
import slib.com.example.service.support.SupportRequestService;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for FE-133: View list of support requests
 * Test Report: doc/Report/FE133_TestReport.md
 */
@WebMvcTest(value = SupportRequestController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-133: View list of support requests - Unit Tests")
class FE133_ViewSupportRequestsTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private SupportRequestService supportRequestService;

        @MockBean
        private UserRepository userRepository;

        private SupportRequestDTO buildDto(String description, SupportRequestStatus status) {
                return SupportRequestDTO.builder()
                        .id(UUID.randomUUID())
                        .studentId(UUID.randomUUID())
                        .studentName("Nguyen Van A")
                        .description(description)
                        .status(status)
                        .createdAt(LocalDateTime.now())
                        .build();
        }

        @Test
        @DisplayName("UTCID01: Get all support requests returns 200 with list")
        void getAllSupportRequests_returnsList() throws Exception {
                List<SupportRequestDTO> list = List.of(
                        buildDto("May tinh bi hong", SupportRequestStatus.PENDING),
                        buildDto("Dieu hoa khong hoat dong", SupportRequestStatus.IN_PROGRESS));
                when(supportRequestService.getAll()).thenReturn(list);

                mockMvc.perform(get("/slib/support-requests"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.length()").value(2));
        }

        @Test
        @DisplayName("UTCID02: Get support requests empty list returns 200")
        void getAllSupportRequests_emptyList_returns200() throws Exception {
                when(supportRequestService.getAll()).thenReturn(Collections.emptyList());

                mockMvc.perform(get("/slib/support-requests"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.length()").value(0));
        }

        @Test
        @DisplayName("UTCID03: Get support requests filtered by status returns 200")
        void getSupportRequests_filteredByStatus_returns200() throws Exception {
                List<SupportRequestDTO> list = List.of(
                        buildDto("May tinh bi hong", SupportRequestStatus.PENDING));
                when(supportRequestService.getByStatus(SupportRequestStatus.PENDING)).thenReturn(list);

                mockMvc.perform(get("/slib/support-requests")
                                .param("status", "PENDING"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.length()").value(1));
        }

        @Test
        @DisplayName("UTCID04: Service throws exception returns 500")
        void getAllSupportRequests_serviceException_returns500() throws Exception {
                when(supportRequestService.getAll()).thenThrow(new RuntimeException("Loi he thong"));

                mockMvc.perform(get("/slib/support-requests"))
                        .andExpect(status().isInternalServerError());
        }

        @Test
        @DisplayName("UTCID05: Get support requests with invalid status returns 400")
        void getSupportRequests_invalidStatus_returns400() throws Exception {
                mockMvc.perform(get("/slib/support-requests")
                                .param("status", "INVALID_STATUS"))
                        .andExpect(status().isBadRequest());
        }
}
