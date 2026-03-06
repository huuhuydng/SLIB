package slib.com.example.controller.hce;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import jakarta.servlet.http.HttpServletRequest;
import slib.com.example.dto.hce.AccessLogDTO;
import slib.com.example.dto.hce.AccessLogStatsDTO;
import slib.com.example.dto.hce.CheckInRequest;
import slib.com.example.dto.hce.StudentDetailDTO;
import slib.com.example.service.CheckInService;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

class HCEControllerTest {
    @Mock
    private CheckInService checkInService;

    @Mock
    private HttpServletRequest httpRequest;

    @InjectMocks
    private HCEController hceController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // Inject gateSecretKey manually since @Value is not processed in unit tests
        hceController = new HCEController();
        hceController.checkInService = checkInService;
        hceController.gateSecretKey = "test-secret";
    }

    @Test
    void testCheckIn_Success() {
        CheckInRequest request = new CheckInRequest();
        when(httpRequest.getHeader("X-API-KEY")).thenReturn("test-secret");
        Map<String, String> result = Collections.singletonMap("status", "OK");
        when(checkInService.processCheckIn(request)).thenReturn(result);

        ResponseEntity<?> response = hceController.checkIn(request, httpRequest);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(result, response.getBody());
    }

    @Test
    void testCheckIn_Forbidden() {
        CheckInRequest request = new CheckInRequest();
        when(httpRequest.getHeader("X-API-KEY")).thenReturn("wrong-key");

        ResponseEntity<?> response = hceController.checkIn(request, httpRequest);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertEquals("FORBIDDEN", body.get("status"));
    }

    @Test
    void testCheckIn_Exception() {
        CheckInRequest request = new CheckInRequest();
        when(httpRequest.getHeader("X-API-KEY")).thenReturn("test-secret");
        when(checkInService.processCheckIn(request)).thenThrow(new RuntimeException("Some error"));

        ResponseEntity<?> response = hceController.checkIn(request, httpRequest);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertEquals("ERROR", body.get("status"));
        assertEquals("Some error", body.get("message"));
    }

    @Test
    void testGetLatestLogs_Success() {
        Map<String, Object> log = Collections.singletonMap("log", "log1");
        java.util.List<Map<String, Object>> logs = Collections.singletonList(log);
        when(checkInService.getLatest10Logs()).thenReturn(logs);
        ResponseEntity<?> response = hceController.getLatestLogs();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(logs, response.getBody());
    }

    @Test
    void testGetLatestLogs_Exception() {
        when(checkInService.getLatest10Logs()).thenThrow(new RuntimeException("error"));
        ResponseEntity<?> response = hceController.getLatestLogs();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() instanceof java.util.ArrayList);
        assertTrue(((java.util.ArrayList<?>) response.getBody()).isEmpty());
    }

    @Test
    void testGetAllAccessLogs_success() {
        List<AccessLogDTO> logs = List.of(new AccessLogDTO());
        when(checkInService.getAllAccessLogs()).thenReturn(logs);

        ResponseEntity<List<AccessLogDTO>> response = hceController.getAllAccessLogs();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(logs, response.getBody());
    }

    @Test
    void testGetAllAccessLogs_exception() {
        when(checkInService.getAllAccessLogs()).thenThrow(new RuntimeException("Error"));

        ResponseEntity<List<AccessLogDTO>> response = hceController.getAllAccessLogs();

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void testGetTodayAccessLogs_success() {
        List<AccessLogDTO> logs = List.of(new AccessLogDTO());
        when(checkInService.getTodayAccessLogs()).thenReturn(logs);

        ResponseEntity<List<AccessLogDTO>> response = hceController.getTodayAccessLogs();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(logs, response.getBody());
    }

    @Test
    void testGetTodayAccessLogs_exception() {
        when(checkInService.getTodayAccessLogs()).thenThrow(new RuntimeException("Error"));

        ResponseEntity<List<AccessLogDTO>> response = hceController.getTodayAccessLogs();

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void testGetTodayStats_success() {
        AccessLogStatsDTO stats = new AccessLogStatsDTO();
        when(checkInService.getTodayStats()).thenReturn(stats);

        ResponseEntity<AccessLogStatsDTO> response = hceController.getTodayStats();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(stats, response.getBody());
    }

    @Test
    void testGetTodayStats_exception() {
        when(checkInService.getTodayStats()).thenThrow(new RuntimeException("Error"));

        ResponseEntity<AccessLogStatsDTO> response = hceController.getTodayStats();

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void testGetAccessLogsByDateRange_withDates_success() {
        List<AccessLogDTO> logs = List.of(new AccessLogDTO());
        when(checkInService.getAccessLogsByDateRange(any(), any())).thenReturn(logs);

        ResponseEntity<List<AccessLogDTO>> response = hceController.getAccessLogsByDateRange("2024-01-01", "2024-01-31");

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void testGetAccessLogsByDateRange_noDates_returnsAll() {
        List<AccessLogDTO> logs = List.of(new AccessLogDTO());
        when(checkInService.getAllAccessLogs()).thenReturn(logs);

        ResponseEntity<List<AccessLogDTO>> response = hceController.getAccessLogsByDateRange(null, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(checkInService).getAllAccessLogs();
    }

    @Test
    void testGetStudentDetail_validUUID_success() {
        StudentDetailDTO detail = new StudentDetailDTO();
        when(checkInService.getStudentDetail(any())).thenReturn(detail);

        ResponseEntity<StudentDetailDTO> response = hceController.getStudentDetail(UUID.randomUUID().toString());

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void testGetStudentDetail_invalidUUID_badRequest() {
        ResponseEntity<StudentDetailDTO> response = hceController.getStudentDetail("invalid-uuid");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void testGetStudentDetail_notFound() {
        when(checkInService.getStudentDetail(any())).thenThrow(new RuntimeException("Not found"));

        ResponseEntity<StudentDetailDTO> response = hceController.getStudentDetail(UUID.randomUUID().toString());

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

}
