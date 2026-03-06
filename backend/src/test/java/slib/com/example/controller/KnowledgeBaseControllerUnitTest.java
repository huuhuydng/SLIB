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
import slib.com.example.controller.ai.KnowledgeBaseController;
import slib.com.example.entity.ai.KnowledgeBaseEntity;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.service.ai.KnowledgeBaseService;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for KnowledgeBaseController
 */
@WebMvcTest(value = KnowledgeBaseController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
        slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("KnowledgeBaseController Unit Tests")
class KnowledgeBaseControllerUnitTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private KnowledgeBaseService knowledgeBaseService;

    @Autowired
    private ObjectMapper objectMapper;

    private KnowledgeBaseEntity createMockEntity() {
        KnowledgeBaseEntity entity = new KnowledgeBaseEntity();
        entity.setId(1L);
        entity.setTitle("Test title");
        entity.setContent("Test content");
        return entity;
    }

    // =========================================
    // === GET ALL ===
    // =========================================

    @Test
    @DisplayName("getAll_returns200WithList")
    void getAll_returns200WithList() throws Exception {
        List<KnowledgeBaseEntity> list = List.of(createMockEntity());
        when(knowledgeBaseService.getAllKnowledge()).thenReturn(list);

        mockMvc.perform(get("/slib/ai/admin/knowledge")
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
        KnowledgeBaseEntity entity = createMockEntity();
        when(knowledgeBaseService.getById(1L)).thenReturn(entity);

        mockMvc.perform(get("/slib/ai/admin/knowledge/1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    // =========================================
    // === CREATE ===
    // =========================================

    @Test
    @DisplayName("create_validKnowledge_returns200")
    void create_validKnowledge_returns200() throws Exception {
        KnowledgeBaseEntity entity = createMockEntity();
        when(knowledgeBaseService.create(any(KnowledgeBaseEntity.class))).thenReturn(entity);

        mockMvc.perform(post("/slib/ai/admin/knowledge")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(entity)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(knowledgeBaseService).create(any(KnowledgeBaseEntity.class));
    }

    @Test
    @DisplayName("create_serviceThrowsException_returns400")
    void create_serviceThrowsException_returns400() throws Exception {
        KnowledgeBaseEntity entity = createMockEntity();
        when(knowledgeBaseService.create(any(KnowledgeBaseEntity.class)))
                .thenThrow(new RuntimeException("Create error"));

        mockMvc.perform(post("/slib/ai/admin/knowledge")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(entity)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    // =========================================
    // === UPDATE ===
    // =========================================

    @Test
    @DisplayName("update_validKnowledge_returns200")
    void update_validKnowledge_returns200() throws Exception {
        KnowledgeBaseEntity entity = createMockEntity();
        when(knowledgeBaseService.update(eq(1L), any(KnowledgeBaseEntity.class))).thenReturn(entity);

        mockMvc.perform(put("/slib/ai/admin/knowledge/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(entity)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("update_serviceThrowsException_returns400")
    void update_serviceThrowsException_returns400() throws Exception {
        KnowledgeBaseEntity entity = createMockEntity();
        when(knowledgeBaseService.update(eq(1L), any(KnowledgeBaseEntity.class)))
                .thenThrow(new RuntimeException("Update error"));

        mockMvc.perform(put("/slib/ai/admin/knowledge/1")
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
        doNothing().when(knowledgeBaseService).delete(1L);

        mockMvc.perform(delete("/slib/ai/admin/knowledge/1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(knowledgeBaseService).delete(1L);
    }

    @Test
    @DisplayName("delete_serviceThrowsException_returns400")
    void delete_serviceThrowsException_returns400() throws Exception {
        doThrow(new RuntimeException("Delete error")).when(knowledgeBaseService).delete(999L);

        mockMvc.perform(delete("/slib/ai/admin/knowledge/999")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
}
