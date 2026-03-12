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
import org.springframework.test.web.servlet.MockMvc;
import slib.com.example.dto.complaint.ComplaintDTO;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.repository.UserRepository;
import slib.com.example.service.ComplaintService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for FE-85: View Complaint Details
 * Test Report: doc/Report/UnitTestReport/FE85_TestReport.md
 */
@WebMvcTest(value = ComplaintController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
        slib.com.example.security.JwtAuthenticationFilter.class}))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-85: View Complaint Details - Unit Tests")
class FE85_ViewComplaintDetailsTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ComplaintService complaintService;

    @MockBean
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    // =========================================
    // === UTCID01: Complaint exists with full detail fields ===
    // =========================================
    @Test
    @DisplayName("UTCID01: Complaint exists with full detail fields - returns 200 OK")
    void getAll_complaintWithFullDetails_returns200() throws Exception {
        UUID complaintId = UUID.randomUUID();
        ComplaintDTO dto = ComplaintDTO.builder()
                .id(complaintId)
                .studentId(UUID.randomUUID())
                .studentName("Nguyen Van A")
                .subject("Tru diem sai")
                .content("Toi bi tru diem khong chinh xac")
                .status("PENDING")
                .createdAt(LocalDateTime.now())
                .build();
        when(complaintService.getAll()).thenReturn(List.of(dto));

        mockMvc.perform(get("/slib/complaints"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(complaintId.toString()))
                .andExpect(jsonPath("$[0].subject").value("Tru diem sai"))
                .andExpect(jsonPath("$[0].content").value("Toi bi tru diem khong chinh xac"))
                .andExpect(jsonPath("$[0].status").value("PENDING"));
    }

    // =========================================
    // === UTCID02: Complaint with resolution note ===
    // =========================================
    @Test
    @DisplayName("UTCID02: Complaint with resolution details - returns 200 OK")
    void getAll_complaintWithResolution_returns200() throws Exception {
        ComplaintDTO dto = ComplaintDTO.builder()
                .id(UUID.randomUUID())
                .subject("Khieu nai da xu ly")
                .content("Noi dung")
                .status("ACCEPTED")
                .resolutionNote("Da hoan tien diem")
                .resolvedByName("Thu Thu A")
                .resolvedAt(LocalDateTime.now())
                .build();
        when(complaintService.getAll()).thenReturn(List.of(dto));

        mockMvc.perform(get("/slib/complaints"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].resolutionNote").value("Da hoan tien diem"))
                .andExpect(jsonPath("$[0].resolvedByName").value("Thu Thu A"));
    }

    // =========================================
    // === UTCID03: Complaint with evidence URL ===
    // =========================================
    @Test
    @DisplayName("UTCID03: Complaint with evidence URL - returns 200 OK")
    void getAll_complaintWithEvidence_returns200() throws Exception {
        ComplaintDTO dto = ComplaintDTO.builder()
                .id(UUID.randomUUID())
                .subject("Khieu nai co bang chung")
                .content("Noi dung")
                .evidenceUrl("https://cloudinary.com/evidence.jpg")
                .status("PENDING")
                .build();
        when(complaintService.getAll()).thenReturn(List.of(dto));

        mockMvc.perform(get("/slib/complaints"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].evidenceUrl").value("https://cloudinary.com/evidence.jpg"));
    }

    // =========================================
    // === UTCID04: Empty complaint list ===
    // =========================================
    @Test
    @DisplayName("UTCID04: No complaints found - returns 200 OK with empty list")
    void getAll_noComplaints_returns200Empty() throws Exception {
        when(complaintService.getAll()).thenReturn(List.of());

        mockMvc.perform(get("/slib/complaints"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // =========================================
    // === UTCID05: Repository failure ===
    // =========================================
    @Test
    @DisplayName("UTCID05: Repository failure - returns 500 Internal Server Error")
    void getAll_repositoryFailure_returns500() throws Exception {
        when(complaintService.getAll()).thenThrow(new RuntimeException("Database error"));

        mockMvc.perform(get("/slib/complaints"))
                .andExpect(status().isInternalServerError());
    }
}
