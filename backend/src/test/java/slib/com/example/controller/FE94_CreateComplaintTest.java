package slib.com.example.controller;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

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

import com.fasterxml.jackson.databind.ObjectMapper;

import slib.com.example.controller.complaint.ComplaintController;
import slib.com.example.dto.complaint.ComplaintDTO;
import slib.com.example.entity.users.Role;
import slib.com.example.entity.users.User;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.repository.users.UserRepository;
import slib.com.example.service.complaint.ComplaintService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = ComplaintController.class, excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = { slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-94: Create complaint - Unit Tests")
class FE94_CreateComplaintTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ComplaintService complaintService;

    @MockBean
    private UserRepository userRepository;

    private final UUID studentId = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Test
    @DisplayName("UTCID01: Create complaint with subject and content")
    void createComplaintWithSubjectAndContent() throws Exception {
        mockCurrentUser();
        when(complaintService.create(eq(studentId), eq("Mất ví"), eq("Tôi bị mất ví ở khu A"), any(), any(), any()))
                .thenReturn(complaint("Mất ví", "Tôi bị mất ví ở khu A"));

        mockMvc.perform(post("/slib/complaints")
                        .with(securityContext(userDetails()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"subject":"Mất ví","content":"Tôi bị mất ví ở khu A"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.studentId").value(studentId.toString()))
                .andExpect(jsonPath("$.subject").value("Mất ví"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @DisplayName("UTCID02: Create complaint with evidenceUrl and violationReportId")
    void createComplaintWithEvidenceUrlAndViolationReportId() throws Exception {
        UUID violationReportId = UUID.randomUUID();
        mockCurrentUser();
        when(complaintService.create(eq(studentId), eq("Khiếu nại trừ điểm"), eq("Tôi muốn xem lại biên bản"),
                eq("https://evidence.example.com/1"), eq(null), eq(violationReportId)))
                .thenReturn(ComplaintDTO.builder()
                        .id(UUID.randomUUID())
                        .studentId(studentId)
                        .subject("Khiếu nại trừ điểm")
                        .content("Tôi muốn xem lại biên bản")
                        .evidenceUrl("https://evidence.example.com/1")
                        .violationReportId(violationReportId)
                        .status("PENDING")
                        .createdAt(LocalDateTime.now())
                        .build());

        mockMvc.perform(post("/slib/complaints")
                        .with(securityContext(userDetails()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "subject":"Khiếu nại trừ điểm",
                                  "content":"Tôi muốn xem lại biên bản",
                                  "evidenceUrl":"https://evidence.example.com/1",
                                  "violationReportId":"%s"
                                }
                                """.formatted(violationReportId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.evidenceUrl").value("https://evidence.example.com/1"))
                .andExpect(jsonPath("$.violationReportId").value(violationReportId.toString()));
    }

    @Test
    @DisplayName("UTCID03: Create complaint with blank subject and content")
    void createComplaintWithBlankSubjectAndContent() throws Exception {
        mockMvc.perform(post("/slib/complaints")
                        .with(securityContext(userDetails()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"subject":"","content":""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors.subject").value("Tiêu đề khiếu nại không được để trống"))
                .andExpect(jsonPath("$.errors.content").value("Nội dung khiếu nại không được để trống"));
    }

    @Test
    @DisplayName("UTCID04: Create complaint with subject longer than 255 characters")
    void createComplaintWithSubjectLongerThan255Characters() throws Exception {
        String longSubject = "A".repeat(256);

        mockMvc.perform(post("/slib/complaints")
                        .with(securityContext(userDetails()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "subject", longSubject,
                                "content", "Noi dung hop le"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors.subject").value("Tiêu đề khiếu nại không được vượt quá 255 ký tự"));
    }

    @Test
    @DisplayName("UTCID05: Create complaint without authenticated user")
    void createComplaintWithoutAuthenticatedUser() throws Exception {
        mockMvc.perform(post("/slib/complaints")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"subject":"Mất ví","content":"Tôi bị mất ví ở khu A"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    @DisplayName("UTCID06: Create complaint when complaint service throws runtime exception")
    void createComplaintWhenComplaintServiceThrowsRuntimeException() throws Exception {
        mockCurrentUser();
        when(complaintService.create(eq(studentId), eq("Mất ví"), eq("Tôi bị mất ví ở khu A"), any(), any(), any()))
                .thenThrow(new RuntimeException("Database error"));

        mockMvc.perform(post("/slib/complaints")
                        .with(securityContext(userDetails()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"subject":"Mất ví","content":"Tôi bị mất ví ở khu A"}
                                """))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value("Database error"));
    }

    private void mockCurrentUser() {
        User user = new User();
        user.setId(studentId);
        user.setEmail("student@fpt.edu.vn");
        user.setFullName("Nguyen Van A");
        user.setRole(Role.STUDENT);
        when(userRepository.findByEmail("student@fpt.edu.vn")).thenReturn(Optional.of(user));
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

    private ComplaintDTO complaint(String subject, String content) {
        return ComplaintDTO.builder()
                .id(UUID.randomUUID())
                .studentId(studentId)
                .studentName("Nguyen Van A")
                .subject(subject)
                .content(content)
                .status("PENDING")
                .createdAt(LocalDateTime.now())
                .build();
    }
}
