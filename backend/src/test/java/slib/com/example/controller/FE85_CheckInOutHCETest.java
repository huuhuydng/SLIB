package slib.com.example.controller;

import java.time.LocalDateTime;
import java.util.Map;
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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import slib.com.example.controller.hce.HCEController;
import slib.com.example.dto.hce.StudentDetailDTO;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.service.hce.CheckInService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = HCEController.class, excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = { slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = "gate.secret=TEST_SECRET_KEY")
@DisplayName("FE-85: Check-in/Check-out library via HCE - Unit Tests")
class FE85_CheckInOutHCETest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CheckInService checkInService;

    @Test
    @DisplayName("UTCID01: HCE check-in with valid API key")
    void hceCheckInWithValidApiKey() throws Exception {
        when(checkInService.processCheckIn(any())).thenReturn(Map.of(
                "status", "SUCCESS",
                "type", "CHECK_IN",
                "message", "Xin chào, Nguyen Van A"));

        mockMvc.perform(post("/slib/hce/checkin")
                        .header("X-API-KEY", "TEST_SECRET_KEY")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"11111111-1111-1111-1111-111111111111\",\"gateId\":\"gate-1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.type").value("CHECK_IN"));

        verify(checkInService).processCheckIn(any());
    }

    @Test
    @DisplayName("UTCID02: HCE check-out with valid API key")
    void hceCheckOutWithValidApiKey() throws Exception {
        when(checkInService.processCheckIn(any())).thenReturn(Map.of(
                "status", "SUCCESS",
                "type", "CHECK_OUT",
                "message", "Tạm biệt, Nguyen Van A"));

        mockMvc.perform(post("/slib/hce/checkin")
                        .header("X-API-KEY", "TEST_SECRET_KEY")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"11111111-1111-1111-1111-111111111111\",\"gateId\":\"gate-2\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.type").value("CHECK_OUT"));

        verify(checkInService).processCheckIn(any());
    }

    @Test
    @DisplayName("UTCID03: HCE check-in without API key")
    void hceCheckInWithoutApiKey() throws Exception {
        mockMvc.perform(post("/slib/hce/checkin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"11111111-1111-1111-1111-111111111111\",\"gateId\":\"gate-1\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("UTCID04: HCE check-in with wrong API key")
    void hceCheckInWithWrongApiKey() throws Exception {
        mockMvc.perform(post("/slib/hce/checkin")
                        .header("X-API-KEY", "WRONG_KEY")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"11111111-1111-1111-1111-111111111111\",\"gateId\":\"gate-1\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("Truy cập bị từ chối: Sai API Key bảo mật"));
    }

    @Test
    @DisplayName("UTCID05: HCE check-in when service throws runtime exception")
    void hceCheckInWhenServiceThrowsRuntimeException() throws Exception {
        when(checkInService.processCheckIn(any()))
                .thenThrow(new RuntimeException("Bạn đã tắt chức năng Check-in NFC. Bật lại trong Cài đặt ứng dụng."));

        mockMvc.perform(post("/slib/hce/checkin")
                        .header("X-API-KEY", "TEST_SECRET_KEY")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"11111111-1111-1111-1111-111111111111\",\"gateId\":\"gate-1\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.message").value("Bạn đã tắt chức năng Check-in NFC. Bật lại trong Cài đặt ứng dụng."));

        verify(checkInService).processCheckIn(any());
    }

    @Test
    @DisplayName("UTCID06: View student detail for HCE librarian screen")
    void viewStudentDetailForHceLibrarianScreen() throws Exception {
        UUID userId = UUID.randomUUID();
        when(checkInService.getStudentDetail(userId))
                .thenReturn(StudentDetailDTO.builder()
                        .id(userId)
                        .fullName("Nguyen Van A")
                        .userCode("SE150001")
                        .email("student@fpt.edu.vn")
                        .totalCheckIns(12)
                        .totalStudyMinutes(480)
                        .createdAt(LocalDateTime.of(2026, 1, 1, 8, 0))
                        .build());

        mockMvc.perform(get("/slib/hce/student-detail/" + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.fullName").value("Nguyen Van A"))
                .andExpect(jsonPath("$.totalCheckIns").value(12));

        verify(checkInService).getStudentDetail(userId);
    }

    @Test
    @DisplayName("UTCID07: View student detail for non-existent userId")
    void viewStudentDetailForNonExistentUserId() throws Exception {
        UUID userId = UUID.randomUUID();
        when(checkInService.getStudentDetail(userId))
                .thenThrow(new RuntimeException("Không tìm thấy sinh viên với ID: " + userId));

        mockMvc.perform(get("/slib/hce/student-detail/" + userId))
                .andExpect(status().isNotFound());

        verify(checkInService).getStudentDetail(userId);
    }
}
