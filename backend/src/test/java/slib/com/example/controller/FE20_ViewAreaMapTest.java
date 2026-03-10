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
import slib.com.example.service.AreaService;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for FE-20: View Area Map
 * Test Report: doc/Report/FE20_TestReport.md
 */
@WebMvcTest(value = AreaController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-20: View Area Map - Unit Tests")
class FE20_ViewAreaMapTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private AreaService areaService;

        @Test
        @DisplayName("UTCD01: View area map returns 200 OK")
        void viewAreaMap_validToken_returns200OK() throws Exception {
                mockMvc.perform(get("/slib/areas"))
                        .andExpect(status().isOk());
        }

        @Test
        @DisplayName("UTCD02: View area map without token returns 401")
        void viewAreaMap_noToken_returns401() throws Exception {
                mockMvc.perform(get("/slib/areas"))
                        .andExpect(status().isUnauthorized());
        }
}
