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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import slib.com.example.dto.complaint.ComplaintDTO;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.repository.users.UserRepository;
import slib.com.example.service.complaint.ComplaintService;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import slib.com.example.controller.complaint.ComplaintController;

/**
 * Unit Tests for FE-98: Verify complaint
 * Test Report: doc/Report/UnitTestReport/FE85_TestReport.md
 */
@WebMvcTest(value = ComplaintController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
        slib.com.example.security.JwtAuthenticationFilter.class}))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-98: Verify complaint - Unit Tests")
class FE98_VerifyComplaintTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ComplaintService complaintService;

    @MockBean
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private final UUID librarianId = UUID.randomUUID();
    private final UUID complaintId = UUID.randomUUID();

    private slib.com.example.entity.users.User mockLibrarian() {
        slib.com.example.entity.users.User u = new slib.com.example.entity.users.User();
        u.setId(librarianId);
        u.setEmail("librarian@fpt.edu.vn");
        u.setFullName("Thu Thu A");
        u.setRole(slib.com.example.entity.users.Role.LIBRARIAN);
        return u;
    }

    private UserDetails librarianDetails() {
        return org.springframework.security.core.userdetails.User.withUsername("librarian@fpt.edu.vn")
                .password("pass")
                .roles("LIBRARIAN")
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

    // =========================================
    // === UTCID01: Accept complaint with note ===
    // =========================================
    @Test
    @DisplayName("UTCID01: Accept complaint with note - returns 200 OK")
    void accept_withNote_returns200() throws Exception {
        when(userRepository.findByEmail("librarian@fpt.edu.vn")).thenReturn(Optional.of(mockLibrarian()));
        ComplaintDTO result = ComplaintDTO.builder()
                .id(complaintId).status("ACCEPTED")
                .resolutionNote("Da xac nhan").resolvedByName("Thu Thu A")
                .resolvedAt(LocalDateTime.now()).build();
        when(complaintService.accept(eq(complaintId), eq(librarianId), eq("Da xac nhan"))).thenReturn(result);

        mockMvc.perform(put("/slib/complaints/{id}/accept", complaintId)
                        .with(securityContext(librarianDetails()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("note", "Da xac nhan"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.resolutionNote").value("Da xac nhan"));

        verify(complaintService, times(1)).accept(complaintId, librarianId, "Da xac nhan");
    }

    // =========================================
    // === UTCID02: Accept complaint without note ===
    // =========================================
    @Test
    @DisplayName("UTCID02: Accept complaint without note - returns 200 OK")
    void accept_withoutNote_returns200() throws Exception {
        when(userRepository.findByEmail("librarian@fpt.edu.vn")).thenReturn(Optional.of(mockLibrarian()));
        ComplaintDTO result = ComplaintDTO.builder()
                .id(complaintId).status("ACCEPTED").resolvedByName("Thu Thu A").build();
        when(complaintService.accept(eq(complaintId), eq(librarianId), isNull())).thenReturn(result);

        mockMvc.perform(put("/slib/complaints/{id}/accept", complaintId)
                        .with(securityContext(librarianDetails()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));
    }

    // =========================================
    // === UTCID03: Deny complaint with note ===
    // =========================================
    @Test
    @DisplayName("UTCID03: Deny complaint with note - returns 200 OK")
    void deny_withNote_returns200() throws Exception {
        when(userRepository.findByEmail("librarian@fpt.edu.vn")).thenReturn(Optional.of(mockLibrarian()));
        ComplaintDTO result = ComplaintDTO.builder()
                .id(complaintId).status("DENIED")
                .resolutionNote("Khong du bang chung").resolvedByName("Thu Thu A").build();
        when(complaintService.deny(eq(complaintId), eq(librarianId), eq("Khong du bang chung"))).thenReturn(result);

        mockMvc.perform(put("/slib/complaints/{id}/deny", complaintId)
                        .with(securityContext(librarianDetails()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("note", "Khong du bang chung"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DENIED"));
    }

    // =========================================
    // === UTCID04: Deny complaint without note ===
    // =========================================
    @Test
    @DisplayName("UTCID04: Deny complaint without note - returns 200 OK")
    void deny_withoutNote_returns200() throws Exception {
        when(userRepository.findByEmail("librarian@fpt.edu.vn")).thenReturn(Optional.of(mockLibrarian()));
        ComplaintDTO result = ComplaintDTO.builder()
                .id(complaintId).status("DENIED").resolvedByName("Thu Thu A").build();
        when(complaintService.deny(eq(complaintId), eq(librarianId), isNull())).thenReturn(result);

        mockMvc.perform(put("/slib/complaints/{id}/deny", complaintId)
                        .with(securityContext(librarianDetails()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DENIED"));
    }

    // =========================================
    // === UTCID05: Complaint not found ===
    // =========================================
    @Test
    @DisplayName("UTCID05: Complaint not found - returns 500 (service throws RuntimeException)")
    void accept_complaintNotFound_returns500() throws Exception {
        when(userRepository.findByEmail("librarian@fpt.edu.vn")).thenReturn(Optional.of(mockLibrarian()));
        when(complaintService.accept(any(), any(), any()))
                .thenThrow(new RuntimeException("Complaint not found"));

        mockMvc.perform(put("/slib/complaints/{id}/accept", complaintId)
                        .with(securityContext(librarianDetails()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isInternalServerError());
    }

    // =========================================
    // === UTCID06: Librarian not found ===
    // =========================================
    @Test
    @DisplayName("UTCID06: Librarian user not found - returns 500")
    void accept_librarianNotFound_returns500() throws Exception {
        when(userRepository.findByEmail("librarian@fpt.edu.vn")).thenReturn(Optional.empty());

        mockMvc.perform(put("/slib/complaints/{id}/accept", complaintId)
                        .with(securityContext(librarianDetails()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isInternalServerError());
    }
}
