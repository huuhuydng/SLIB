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
import slib.com.example.dto.hce.CheckInRequest;
import slib.com.example.service.CheckInService;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

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
}
