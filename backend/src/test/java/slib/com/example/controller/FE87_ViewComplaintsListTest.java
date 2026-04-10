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
import slib.com.example.entity.complaint.ComplaintEntity.ComplaintStatus;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.repository.users.UserRepository;
import slib.com.example.service.complaint.ComplaintService;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import slib.com.example.controller.complaint.ComplaintController;

/**
 * Unit Tests for FE-87: View list of complaints
 * Test Report: doc/Report/UnitTestReport/FE83_TestReport.md
 */
@WebMvcTest(value = ComplaintController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
        slib.com.example.security.JwtAuthenticationFilter.class}))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-87: View list of complaints - Unit Tests")
class FE87_ViewComplaintsListTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ComplaintService complaintService;

    @MockBean
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    // =========================================
    // === UTCID01: Get all complaints without filter ===
    // =========================================
    @Test
    @DisplayName("UTCID01: Get all complaints without status filter - returns 200 OK")
    void getAll_noFilter_returns200() throws Exception {
        List<ComplaintDTO> complaints = List.of(
                ComplaintDTO.builder().id(UUID.randomUUID()).subject("Khieu nai 1").status("PENDING").build(),
                ComplaintDTO.builder().id(UUID.randomUUID()).subject("Khieu nai 2").status("ACCEPTED").build());
        when(complaintService.getAll()).thenReturn(complaints);

        mockMvc.perform(get("/slib/complaints"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        verify(complaintService, times(1)).getAll();
        verify(complaintService, never()).getByStatus(any());
    }

    // =========================================
    // === UTCID02: Filter by PENDING status ===
    // =========================================
    @Test
    @DisplayName("UTCID02: Filter complaints by PENDING status - returns 200 OK")
    void getAll_filterPending_returns200() throws Exception {
        List<ComplaintDTO> pendingList = List.of(
                ComplaintDTO.builder().id(UUID.randomUUID()).subject("Pending complaint").status("PENDING").build());
        when(complaintService.getByStatus(ComplaintStatus.PENDING)).thenReturn(pendingList);

        mockMvc.perform(get("/slib/complaints").param("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("PENDING"));

        verify(complaintService, times(1)).getByStatus(ComplaintStatus.PENDING);
        verify(complaintService, never()).getAll();
    }

    // =========================================
    // === UTCID03: Filter by ACCEPTED status ===
    // =========================================
    @Test
    @DisplayName("UTCID03: Filter complaints by ACCEPTED status - returns 200 OK")
    void getAll_filterAccepted_returns200() throws Exception {
        List<ComplaintDTO> acceptedList = List.of(
                ComplaintDTO.builder().id(UUID.randomUUID()).subject("Accepted").status("ACCEPTED").build());
        when(complaintService.getByStatus(ComplaintStatus.ACCEPTED)).thenReturn(acceptedList);

        mockMvc.perform(get("/slib/complaints").param("status", "ACCEPTED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("ACCEPTED"));

        verify(complaintService, times(1)).getByStatus(ComplaintStatus.ACCEPTED);
    }

    // =========================================
    // === UTCID04: Invalid status filter ===
    // =========================================
    @Test
    @DisplayName("UTCID04: Invalid status filter - returns 400 Bad Request")
    void getAll_invalidStatus_returns400() throws Exception {
        mockMvc.perform(get("/slib/complaints").param("status", "INVALID_STATUS"))
                .andExpect(status().isBadRequest());

        verify(complaintService, never()).getAll();
        verify(complaintService, never()).getByStatus(any());
    }

    // =========================================
    // === UTCID05: Repository failure ===
    // =========================================
    @Test
    @DisplayName("UTCID05: Repository failure - returns 500 Internal Server Error")
    void getAll_repositoryFailure_returns500() throws Exception {
        when(complaintService.getAll()).thenThrow(new RuntimeException("Database connection failed"));

        mockMvc.perform(get("/slib/complaints"))
                .andExpect(status().isInternalServerError());
    }
}
