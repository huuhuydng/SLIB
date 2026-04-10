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
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import slib.com.example.controller.complaint.ComplaintController;

/**
 * Unit Tests for FE-86: View history of sending complaint
 * Test Report: doc/Report/UnitTestReport/FE82_TestReport.md
 */
@WebMvcTest(value = ComplaintController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
        slib.com.example.security.JwtAuthenticationFilter.class}))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-86: View history of sending complaint - Unit Tests")
class FE86_ViewComplaintHistoryTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ComplaintService complaintService;

    @MockBean
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private final UUID studentId = UUID.randomUUID();

    private slib.com.example.entity.users.User mockUser() {
        slib.com.example.entity.users.User u = new slib.com.example.entity.users.User();
        u.setId(studentId);
        u.setEmail("student@fpt.edu.vn");
        u.setFullName("Nguyen Van A");
        u.setRole(slib.com.example.entity.users.Role.STUDENT);
        return u;
    }

    private UserDetails userDetails() {
        return org.springframework.security.core.userdetails.User.withUsername("student@fpt.edu.vn")
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

    // =========================================
    // === UTCID01: Student has complaints - returns list ===
    // =========================================
    @Test
    @DisplayName("UTCID01: Student has complaints - returns 200 OK with list")
    void getMyComplaints_withComplaints_returns200() throws Exception {
        when(userRepository.findByEmail("student@fpt.edu.vn")).thenReturn(Optional.of(mockUser()));
        List<ComplaintDTO> complaints = List.of(
                ComplaintDTO.builder().id(UUID.randomUUID()).studentId(studentId)
                        .subject("Tru diem sai").content("Toi bi tru diem khong chinh xac")
                        .status("PENDING").createdAt(LocalDateTime.now()).build());
        when(complaintService.getByStudent(studentId)).thenReturn(complaints);

        mockMvc.perform(get("/slib/complaints/my").with(securityContext(userDetails())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].subject").value("Tru diem sai"));

        verify(complaintService, times(1)).getByStudent(studentId);
    }

    // =========================================
    // === UTCID02: Student has multiple complaints ===
    // =========================================
    @Test
    @DisplayName("UTCID02: Student has multiple complaints - returns 200 OK")
    void getMyComplaints_multipleComplaints_returns200() throws Exception {
        when(userRepository.findByEmail("student@fpt.edu.vn")).thenReturn(Optional.of(mockUser()));
        List<ComplaintDTO> complaints = List.of(
                ComplaintDTO.builder().id(UUID.randomUUID()).studentId(studentId)
                        .subject("Khieu nai 1").content("Noi dung 1").status("PENDING").build(),
                ComplaintDTO.builder().id(UUID.randomUUID()).studentId(studentId)
                        .subject("Khieu nai 2").content("Noi dung 2").status("ACCEPTED").build());
        when(complaintService.getByStudent(studentId)).thenReturn(complaints);

        mockMvc.perform(get("/slib/complaints/my").with(securityContext(userDetails())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        verify(complaintService, times(1)).getByStudent(studentId);
    }

    // =========================================
    // === UTCID03: Student has complaints with different statuses ===
    // =========================================
    @Test
    @DisplayName("UTCID03: Student has complaints with mixed statuses - returns 200 OK")
    void getMyComplaints_mixedStatuses_returns200() throws Exception {
        when(userRepository.findByEmail("student@fpt.edu.vn")).thenReturn(Optional.of(mockUser()));
        List<ComplaintDTO> complaints = List.of(
                ComplaintDTO.builder().id(UUID.randomUUID()).studentId(studentId)
                        .subject("Pending").status("PENDING").build(),
                ComplaintDTO.builder().id(UUID.randomUUID()).studentId(studentId)
                        .subject("Denied").status("DENIED").build());
        when(complaintService.getByStudent(studentId)).thenReturn(complaints);

        mockMvc.perform(get("/slib/complaints/my").with(securityContext(userDetails())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    // =========================================
    // === UTCID04: Student has no complaints - returns empty list ===
    // =========================================
    @Test
    @DisplayName("UTCID04: Student has no complaints - returns 200 OK with empty list")
    void getMyComplaints_noComplaints_returns200EmptyList() throws Exception {
        when(userRepository.findByEmail("student@fpt.edu.vn")).thenReturn(Optional.of(mockUser()));
        when(complaintService.getByStudent(studentId)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/slib/complaints/my").with(securityContext(userDetails())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // =========================================
    // === UTCID05: Repository failure - returns 500 ===
    // =========================================
    @Test
    @DisplayName("UTCID05: Repository failure - returns 500 Internal Server Error")
    void getMyComplaints_repositoryFailure_returns500() throws Exception {
        when(userRepository.findByEmail("student@fpt.edu.vn")).thenReturn(Optional.of(mockUser()));
        when(complaintService.getByStudent(studentId)).thenThrow(new RuntimeException("Database error"));

        mockMvc.perform(get("/slib/complaints/my").with(securityContext(userDetails())))
                .andExpect(status().isInternalServerError());
    }
}
