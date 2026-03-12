package slib.com.example.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import slib.com.example.controller.system.SystemInfoController;
import slib.com.example.exception.GlobalExceptionHandler;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = SystemInfoController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
        slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("SystemInfoController Unit Tests")
class SystemInfoControllerUnitTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("getSystemInfo_returns200WithMetrics")
    void getSystemInfo_returns200WithMetrics() throws Exception {
        mockMvc.perform(get("/slib/system/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cpu").exists())
                .andExpect(jsonPath("$.memory").exists())
                .andExpect(jsonPath("$.disk").exists())
                .andExpect(jsonPath("$.uptime").exists())
                .andExpect(jsonPath("$.javaVersion").exists())
                .andExpect(jsonPath("$.osName").exists())
                .andExpect(jsonPath("$.availableProcessors").exists());
    }
}
