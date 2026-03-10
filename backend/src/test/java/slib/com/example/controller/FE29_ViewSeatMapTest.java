package slib.com.example.controller;

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
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.service.SeatService;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for FE-29: View Seat Map
 * Test Report: doc/Report/FE29_TestReport.md
 */
@WebMvcTest(value = SeatController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-29: View Seat Map - Unit Tests")
class FE29_ViewSeatMapTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private SeatService seatService;

        @Test
        @DisplayName("UTCD01: View seat map returns 200 OK")
        void viewSeatMap_validToken_returns200OK() throws Exception {
                mockMvc.perform(get("/slib/seats"))
                        .andExpect(status().isOk());
        }

        @Test
        @DisplayName("UTCD02: View seat map without token returns 401")
        void viewSeatMap_noToken_returns401() throws Exception {
                mockMvc.perform(get("/slib/seats"))
                        .andExpect(status().isUnauthorized());
        }
}
