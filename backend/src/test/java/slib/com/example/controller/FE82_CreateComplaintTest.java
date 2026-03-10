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
import slib.com.example.service.ComplaintService;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for FE-82: Create Complaint
 * Test Report: doc/Report/FE77_TestReport.md
 */
@WebMvcTest(value = ComplaintController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-82: Create Complaint - Unit Tests")
class FE82_CreateComplaintTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private ComplaintService complaintService;

        @Test
        @DisplayName("UTCD01: Create complaint returns 200 OK")
        void createComplaint_validToken_returns200OK() throws Exception {
                mockMvc.perform(post("/slib/complaints")
                                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                                .content("{\"title\":\"Test\",\"description\":\"Test complaint\"}"))
                        .andExpect(status().isOk());
        }

        @Test
        @DisplayName("UTCD02: Create complaint without token returns 401")
        void createComplaint_noToken_returns401() throws Exception {
                mockMvc.perform(post("/slib/complaints"))
                        .andExpect(status().isUnauthorized());
        }
}
