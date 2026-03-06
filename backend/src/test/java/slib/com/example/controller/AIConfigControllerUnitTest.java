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
import slib.com.example.controller.ai.AIConfigController;
import slib.com.example.entity.ai.AIConfigEntity;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.service.ai.AIConfigService;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for AIConfigController
 */
@WebMvcTest(value = AIConfigController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
        slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AIConfigController Unit Tests")
class AIConfigControllerUnitTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AIConfigService aiConfigService;

    @Autowired
    private ObjectMapper objectMapper;

    // =========================================
    // === GET CONFIG ===
    // =========================================

    @Test
    @DisplayName("getConfig_configExists_returns200WithConfig")
    void getConfig_configExists_returns200WithConfig() throws Exception {
        AIConfigEntity config = new AIConfigEntity();
        config.setProvider("gemini");
        when(aiConfigService.getConfigForDisplay()).thenReturn(config);

        mockMvc.perform(get("/slib/ai/admin/config")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.configured").value(true))
                .andExpect(jsonPath("$.config").exists());
    }

    @Test
    @DisplayName("getConfig_noConfig_returns200WithDefaults")
    void getConfig_noConfig_returns200WithDefaults() throws Exception {
        when(aiConfigService.getConfigForDisplay()).thenReturn(null);

        mockMvc.perform(get("/slib/ai/admin/config")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.configured").value(false))
                .andExpect(jsonPath("$.defaults").exists());
    }

    // =========================================
    // === SAVE CONFIG ===
    // =========================================

    @Test
    @DisplayName("saveConfig_validConfig_returns200")
    void saveConfig_validConfig_returns200() throws Exception {
        AIConfigEntity config = new AIConfigEntity();
        config.setProvider("gemini");
        config.setOllamaModel("llama3.2");
        config.setOllamaUrl("http://localhost:11434");

        AIConfigEntity savedConfig = new AIConfigEntity();
        savedConfig.setProvider("gemini");
        when(aiConfigService.saveConfig(any(AIConfigEntity.class))).thenReturn(savedConfig);
        when(aiConfigService.getConfigForDisplay()).thenReturn(savedConfig);

        mockMvc.perform(post("/slib/ai/admin/config")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(config)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(aiConfigService).saveConfig(any(AIConfigEntity.class));
    }

    @Test
    @DisplayName("saveConfig_serviceThrowsException_returns400")
    void saveConfig_serviceThrowsException_returns400() throws Exception {
        AIConfigEntity config = new AIConfigEntity();
        doThrow(new RuntimeException("Save error")).when(aiConfigService).saveConfig(any(AIConfigEntity.class));

        mockMvc.perform(post("/slib/ai/admin/config")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(config)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    // =========================================
    // === RESET CONFIG ===
    // =========================================

    @Test
    @DisplayName("resetConfig_success_returns200")
    void resetConfig_success_returns200() throws Exception {
        AIConfigEntity defaultConfig = new AIConfigEntity();
        defaultConfig.setProvider("ollama");
        when(aiConfigService.resetToDefault()).thenReturn(defaultConfig);
        when(aiConfigService.getConfigForDisplay()).thenReturn(defaultConfig);

        mockMvc.perform(post("/slib/ai/admin/config/reset")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(aiConfigService).resetToDefault();
    }

    @Test
    @DisplayName("resetConfig_serviceThrowsException_returns400")
    void resetConfig_serviceThrowsException_returns400() throws Exception {
        doThrow(new RuntimeException("Reset error")).when(aiConfigService).resetToDefault();

        mockMvc.perform(post("/slib/ai/admin/config/reset")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
}
