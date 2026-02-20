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
import slib.com.example.controller.ai.PromptTemplateController;
import slib.com.example.entity.ai.PromptTemplateEntity;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.service.ai.PromptTemplateService;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for PromptTemplateController
 */
@WebMvcTest(value = PromptTemplateController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
        slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("PromptTemplateController Unit Tests")
class PromptTemplateControllerUnitTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PromptTemplateService promptTemplateService;

    @Autowired
    private ObjectMapper objectMapper;

    private PromptTemplateEntity createMockEntity() {
        PromptTemplateEntity entity = new PromptTemplateEntity();
        entity.setId(1L);
        entity.setName("Test Prompt");
        entity.setPrompt("Test prompt content");
        return entity;
    }

    // =========================================
    // === GET ALL ===
    // =========================================

    @Test
    @DisplayName("getAll_returns200WithList")
    void getAll_returns200WithList() throws Exception {
        List<PromptTemplateEntity> list = List.of(createMockEntity());
        when(promptTemplateService.getAllPrompts()).thenReturn(list);

        mockMvc.perform(get("/slib/ai/admin/prompts")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1));
    }

    // =========================================
    // === GET BY ID ===
    // =========================================

    @Test
    @DisplayName("getById_existingId_returns200")
    void getById_existingId_returns200() throws Exception {
        PromptTemplateEntity entity = createMockEntity();
        when(promptTemplateService.getById(1L)).thenReturn(entity);

        mockMvc.perform(get("/slib/ai/admin/prompts/1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    // =========================================
    // === CREATE ===
    // =========================================

    @Test
    @DisplayName("create_validPrompt_returns200")
    void create_validPrompt_returns200() throws Exception {
        PromptTemplateEntity entity = createMockEntity();
        when(promptTemplateService.create(any(PromptTemplateEntity.class))).thenReturn(entity);

        mockMvc.perform(post("/slib/ai/admin/prompts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(entity)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(promptTemplateService).create(any(PromptTemplateEntity.class));
    }

    @Test
    @DisplayName("create_serviceThrowsException_returns400")
    void create_serviceThrowsException_returns400() throws Exception {
        PromptTemplateEntity entity = createMockEntity();
        when(promptTemplateService.create(any(PromptTemplateEntity.class)))
                .thenThrow(new RuntimeException("Create error"));

        mockMvc.perform(post("/slib/ai/admin/prompts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(entity)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    // =========================================
    // === UPDATE ===
    // =========================================

    @Test
    @DisplayName("update_validPrompt_returns200")
    void update_validPrompt_returns200() throws Exception {
        PromptTemplateEntity entity = createMockEntity();
        when(promptTemplateService.update(eq(1L), any(PromptTemplateEntity.class))).thenReturn(entity);

        mockMvc.perform(put("/slib/ai/admin/prompts/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(entity)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("update_serviceThrowsException_returns400")
    void update_serviceThrowsException_returns400() throws Exception {
        PromptTemplateEntity entity = createMockEntity();
        when(promptTemplateService.update(eq(1L), any(PromptTemplateEntity.class)))
                .thenThrow(new RuntimeException("Update error"));

        mockMvc.perform(put("/slib/ai/admin/prompts/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(entity)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    // =========================================
    // === DELETE ===
    // =========================================

    @Test
    @DisplayName("delete_existingId_returns200")
    void delete_existingId_returns200() throws Exception {
        doNothing().when(promptTemplateService).delete(1L);

        mockMvc.perform(delete("/slib/ai/admin/prompts/1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(promptTemplateService).delete(1L);
    }

    @Test
    @DisplayName("delete_serviceThrowsException_returns400")
    void delete_serviceThrowsException_returns400() throws Exception {
        doThrow(new RuntimeException("Delete error")).when(promptTemplateService).delete(999L);

        mockMvc.perform(delete("/slib/ai/admin/prompts/999")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
}
