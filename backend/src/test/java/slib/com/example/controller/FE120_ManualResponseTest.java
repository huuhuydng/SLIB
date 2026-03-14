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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import slib.com.example.controller.support.SupportRequestController;
import slib.com.example.dto.support.SupportRequestDTO;
import slib.com.example.entity.support.SupportRequestStatus;
import slib.com.example.entity.users.User;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.repository.users.UserRepository;
import slib.com.example.service.support.SupportRequestService;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for FE-120: Manual Response
 * Test Report: doc/Report/UnitTestReport/FE120_TestReport.md
 */
@WebMvcTest(value = SupportRequestController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-120: Manual Response - Unit Tests")
class FE120_ManualResponseTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private SupportRequestService supportRequestService;

        @MockBean
        private UserRepository userRepository;

        @Autowired
        private ObjectMapper objectMapper;

        private UUID mockLibrarian() {
                UUID librarianId = UUID.randomUUID();
                User librarian = new User();
                librarian.setId(librarianId);
                librarian.setEmail("librarian@fpt.edu.vn");
                when(userRepository.findByEmail("librarian@fpt.edu.vn")).thenReturn(Optional.of(librarian));
                return librarianId;
        }

        // =========================================
        // === UTCID01: Normal - respond to support request ===
        // =========================================
        @Test
        @WithMockUser(username = "librarian@fpt.edu.vn")
        @DisplayName("UTCID01: Respond to support request with valid response returns 200 OK")
        void respond_validResponse_returns200() throws Exception {
                UUID librarianId = mockLibrarian();
                UUID requestId = UUID.randomUUID();

                SupportRequestDTO responseDto = SupportRequestDTO.builder()
                                .id(requestId)
                                .status(SupportRequestStatus.RESOLVED)
                                .build();

                when(supportRequestService.respond(eq(requestId), eq("Da xu ly yeu cau cua ban"), eq(librarianId)))
                                .thenReturn(responseDto);

                mockMvc.perform(put("/slib/support-requests/{id}/respond", requestId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(Map.of("response", "Da xu ly yeu cau cua ban"))))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("RESOLVED"));

                verify(supportRequestService, times(1)).respond(eq(requestId), eq("Da xu ly yeu cau cua ban"), eq(librarianId));
        }

        // =========================================
        // === UTCID02: Respond with detailed content ===
        // =========================================
        @Test
        @WithMockUser(username = "librarian@fpt.edu.vn")
        @DisplayName("UTCID02: Respond with detailed content returns 200 OK")
        void respond_detailedContent_returns200() throws Exception {
                UUID librarianId = mockLibrarian();
                UUID requestId = UUID.randomUUID();

                SupportRequestDTO responseDto = SupportRequestDTO.builder()
                                .id(requestId)
                                .build();

                when(supportRequestService.respond(eq(requestId), anyString(), eq(librarianId)))
                                .thenReturn(responseDto);

                mockMvc.perform(put("/slib/support-requests/{id}/respond", requestId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(Map.of("response", "Ban vui long den quay thu vien de duoc ho tro truc tiep"))))
                                .andExpect(status().isOk());

                verify(supportRequestService, times(1)).respond(eq(requestId), anyString(), eq(librarianId));
        }

        // =========================================
        // === UTCID03: Respond updates status to resolved ===
        // =========================================
        @Test
        @WithMockUser(username = "librarian@fpt.edu.vn")
        @DisplayName("UTCID03: Response updates support request status to resolved returns 200 OK")
        void respond_updatesStatusResolved_returns200() throws Exception {
                UUID librarianId = mockLibrarian();
                UUID requestId = UUID.randomUUID();

                SupportRequestDTO responseDto = SupportRequestDTO.builder()
                                .id(requestId)
                                .status(SupportRequestStatus.RESOLVED)
                                .build();

                when(supportRequestService.respond(eq(requestId), anyString(), eq(librarianId)))
                                .thenReturn(responseDto);

                mockMvc.perform(put("/slib/support-requests/{id}/respond", requestId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(Map.of("response", "Phan hoi"))))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("RESOLVED"));
        }

        // =========================================
        // === UTCID04: Respond with empty content ===
        // =========================================
        @Test
        @WithMockUser(username = "librarian@fpt.edu.vn")
        @DisplayName("UTCID04: Respond with empty content returns 400 Bad Request")
        void respond_emptyContent_returns400() throws Exception {
                mockLibrarian();
                UUID requestId = UUID.randomUUID();

                mockMvc.perform(put("/slib/support-requests/{id}/respond", requestId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(Map.of("response", ""))))
                                .andExpect(status().isBadRequest());

                verify(supportRequestService, never()).respond(any(), anyString(), any());
        }

        // =========================================
        // === UTCID05: Respond to non-existent request ===
        // =========================================
        @Test
        @WithMockUser(username = "librarian@fpt.edu.vn")
        @DisplayName("UTCID05: Respond to non-existent support request returns error")
        void respond_requestNotFound_returnsError() throws Exception {
                UUID librarianId = mockLibrarian();
                UUID requestId = UUID.randomUUID();

                when(supportRequestService.respond(eq(requestId), anyString(), eq(librarianId)))
                                .thenThrow(new RuntimeException("Yeu cau ho tro khong ton tai"));

                mockMvc.perform(put("/slib/support-requests/{id}/respond", requestId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(Map.of("response", "Phan hoi"))))
                                .andExpect(status().isInternalServerError());
        }
}
