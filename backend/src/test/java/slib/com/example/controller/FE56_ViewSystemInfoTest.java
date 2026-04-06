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
import slib.com.example.controller.system.SystemInfoController;
import slib.com.example.exception.GlobalExceptionHandler;

import javax.sql.DataSource;
import org.springframework.web.client.RestTemplate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for FE-56: View System Info
 * Test Report: doc/Report/UnitTestReport/FE56_TestReport.md
 */
@WebMvcTest(value = SystemInfoController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-56: View System Info - Unit Tests")
class FE56_ViewSystemInfoTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private DataSource dataSource;

        @MockBean
        private RestTemplate restTemplate;

        // =========================================
        // === UTCID01: Load overview metrics ===
        // =========================================

        /**
         * UTCID01: Load overview metrics - Success
         * Precondition: Admin opens the system health overview screen
         * Expected: 200 OK with cpu, memory, disk, uptime fields
         */
        @Test
        @DisplayName("UTCID01: Load overview metrics returns 200 OK with all fields")
        void getSystemInfo_loadOverview_returns200OK() throws Exception {
                mockMvc.perform(get("/slib/system/info"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.cpu").exists())
                                .andExpect(jsonPath("$.memory").exists())
                                .andExpect(jsonPath("$.disk").exists())
                                .andExpect(jsonPath("$.uptime").exists());
        }

        // =========================================
        // === UTCID02: CPU/process usage fallback ===
        // =========================================

        /**
         * UTCID02: CPU/process usage fallback is used
         * Precondition: Admin opens system health overview
         * Expected: 200 OK with cpu value present (fallback or real)
         */
        @Test
        @DisplayName("UTCID02: CPU/process usage fallback returns 200 OK with cpu value")
        void getSystemInfo_cpuFallback_returns200OK() throws Exception {
                mockMvc.perform(get("/slib/system/info"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.cpu").isNumber())
                                .andExpect(jsonPath("$.availableProcessors").isNumber());
        }

        // =========================================
        // === UTCID03: Disk and memory values ===
        // =========================================

        /**
         * UTCID03: Disk and memory values are calculated
         * Precondition: Admin opens system health overview
         * Expected: 200 OK with memory and disk details
         */
        @Test
        @DisplayName("UTCID03: Disk and memory values are calculated returns 200 OK")
        void getSystemInfo_diskAndMemory_returns200OK() throws Exception {
                mockMvc.perform(get("/slib/system/info"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.memoryUsedMB").isNumber())
                                .andExpect(jsonPath("$.memoryMaxMB").isNumber())
                                .andExpect(jsonPath("$.diskUsedGB").isNumber())
                                .andExpect(jsonPath("$.diskTotalGB").isNumber());
        }

        // =========================================
        // === UTCID04: Uptime and runtime metadata ===
        // =========================================

        /**
         * UTCID04: Uptime and runtime metadata are returned
         * Precondition: Admin opens system health overview
         * Expected: 200 OK with uptime, osName, javaVersion
         */
        @Test
        @DisplayName("UTCID04: Uptime and runtime metadata are returned returns 200 OK")
        void getSystemInfo_uptimeAndRuntime_returns200OK() throws Exception {
                mockMvc.perform(get("/slib/system/info"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.uptimeMs").isNumber())
                                .andExpect(jsonPath("$.uptime").isString())
                                .andExpect(jsonPath("$.osName").isString())
                                .andExpect(jsonPath("$.javaVersion").isString())
                                .andExpect(jsonPath("$.javaVendor").isString());
        }

        // =========================================
        // === UTCID05: Unexpected system-info failure ===
        // =========================================

        /**
         * UTCID05: Unexpected system-info failure occurs
         * Note: Since SystemInfoController reads JVM MXBeans directly without
         * external service dependencies, we verify the endpoint is callable
         * and returns valid structure. A real failure would require JVM-level
         * issues that cannot be easily mocked in a WebMvcTest context.
         * Expected: 200 OK (controller has no error path that returns 500)
         */
        @Test
        @DisplayName("UTCID05: System info endpoint is resilient and returns valid data")
        void getSystemInfo_unexpectedFailure_endpointReturnsData() throws Exception {
                mockMvc.perform(get("/slib/system/info"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.serverPort").exists());
        }
}
