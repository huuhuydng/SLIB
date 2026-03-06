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
import org.springframework.test.web.servlet.MockMvc;
import slib.com.example.controller.zone_config.SeatHoldController;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.service.SeatHoldService;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for SeatHoldController
 */
@WebMvcTest(value = SeatHoldController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
        slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("SeatHoldController Unit Tests")
class SeatHoldControllerUnitTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SeatHoldService seatHoldService;

    @Autowired
    private ObjectMapper objectMapper;

    // =========================================
    // === HOLD SEAT ===
    // =========================================

    @Test
    @DisplayName("holdSeat_validRequest_returns200")
    void holdSeat_validRequest_returns200() throws Exception {
        UUID userId = UUID.randomUUID();
        Map<String, Object> result = Map.of("success", true, "message", "Seat held");

        when(seatHoldService.holdSeat(eq(1), eq(userId), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(result);

        Map<String, String> body = Map.of(
                "userId", userId.toString(),
                "startTime", "2026-02-20T08:00:00",
                "endTime", "2026-02-20T10:00:00");

        mockMvc.perform(post("/slib/seats/1/hold")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("holdSeat_invalidFormat_returns400")
    void holdSeat_invalidFormat_returns400() throws Exception {
        Map<String, String> body = Map.of(
                "userId", "invalid-uuid",
                "startTime", "invalid",
                "endTime", "invalid");

        mockMvc.perform(post("/slib/seats/1/hold")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("holdSeat_seatNotAvailable_returns400")
    void holdSeat_seatNotAvailable_returns400() throws Exception {
        UUID userId = UUID.randomUUID();
        when(seatHoldService.holdSeat(eq(1), eq(userId), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenThrow(new RuntimeException("Seat is not available"));

        Map<String, String> body = Map.of(
                "userId", userId.toString(),
                "startTime", "2026-02-20T08:00:00",
                "endTime", "2026-02-20T10:00:00");

        mockMvc.perform(post("/slib/seats/1/hold")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Seat is not available"));
    }

    // =========================================
    // === RELEASE SEAT ===
    // =========================================

    @Test
    @DisplayName("releaseSeat_validRequest_returns200")
    void releaseSeat_validRequest_returns200() throws Exception {
        UUID userId = UUID.randomUUID();
        Map<String, Object> result = Map.of("success", true, "message", "Seat released");

        when(seatHoldService.releaseSeat(eq(1), eq(userId), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(result);

        Map<String, String> body = Map.of(
                "userId", userId.toString(),
                "startTime", "2026-02-20T08:00:00",
                "endTime", "2026-02-20T10:00:00");

        mockMvc.perform(delete("/slib/seats/1/hold")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("releaseSeat_seatNotHeld_returns400")
    void releaseSeat_seatNotHeld_returns400() throws Exception {
        UUID userId = UUID.randomUUID();
        when(seatHoldService.releaseSeat(eq(1), eq(userId), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenThrow(new RuntimeException("Seat is not held"));

        Map<String, String> body = Map.of(
                "userId", userId.toString(),
                "startTime", "2026-02-20T08:00:00",
                "endTime", "2026-02-20T10:00:00");

        mockMvc.perform(delete("/slib/seats/1/hold")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Seat is not held"));
    }

    // =========================================
    // === CHECK AVAILABILITY ===
    // =========================================

    @Test
    @DisplayName("checkAvailability_seatAvailable_returns200WithTrue")
    void checkAvailability_seatAvailable_returns200WithTrue() throws Exception {
        when(seatHoldService.isSeatAvailable(eq(1), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(true);

        mockMvc.perform(get("/slib/seats/1/available")
                .param("startTime", "2026-02-20T08:00:00")
                .param("endTime", "2026-02-20T10:00:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(true))
                .andExpect(jsonPath("$.seatId").value(1));
    }

    @Test
    @DisplayName("checkAvailability_seatNotAvailable_returns200WithFalse")
    void checkAvailability_seatNotAvailable_returns200WithFalse() throws Exception {
        when(seatHoldService.isSeatAvailable(eq(2), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(false);

        mockMvc.perform(get("/slib/seats/2/available")
                .param("startTime", "2026-02-20T08:00:00")
                .param("endTime", "2026-02-20T10:00:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(false))
                .andExpect(jsonPath("$.seatId").value(2));
    }

    @Test
    @DisplayName("checkAvailability_invalidDateTime_returns400")
    void checkAvailability_invalidDateTime_returns400() throws Exception {
        mockMvc.perform(get("/slib/seats/1/available")
                .param("startTime", "invalid")
                .param("endTime", "invalid"))
                .andExpect(status().isBadRequest());
    }
}
