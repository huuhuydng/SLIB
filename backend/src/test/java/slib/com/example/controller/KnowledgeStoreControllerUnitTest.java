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
import slib.com.example.controller.ai.KnowledgeStoreController;
import slib.com.example.dto.ai.KnowledgeStoreDTO;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.service.ai.KnowledgeStoreService;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for KnowledgeStoreController
 */
@WebMvcTest(value = KnowledgeStoreController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
        slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("KnowledgeStoreController Unit Tests")
class KnowledgeStoreControllerUnitTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private KnowledgeStoreService knowledgeStoreService;

    @Autowired
    private ObjectMapper objectMapper;

    private KnowledgeStoreDTO.Response createMockResponse() {
        KnowledgeStoreDTO.Response response = new KnowledgeStoreDTO.Response();
        response.setId(1L);
        response.setName("Test Store");
        return response;
    }

    // =========================================
    // === GET ALL ===
    // =========================================

    @Test
    @DisplayName("getAllKnowledgeStores_returns200WithList")
    void getAllKnowledgeStores_returns200WithList() throws Exception {
        List<KnowledgeStoreDTO.Response> list = List.of(createMockResponse());
        when(knowledgeStoreService.getAllKnowledgeStores()).thenReturn(list);

        mockMvc.perform(get("/slib/ai/admin/knowledge-stores")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1));
    }

    // =========================================
    // === GET BY ID ===
    // =========================================

    @Test
    @DisplayName("getKnowledgeStoreById_existingId_returns200")
    void getKnowledgeStoreById_existingId_returns200() throws Exception {
        KnowledgeStoreDTO.Response response = createMockResponse();
        when(knowledgeStoreService.getKnowledgeStoreById(1L)).thenReturn(response);

        mockMvc.perform(get("/slib/ai/admin/knowledge-stores/1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    // =========================================
    // === CREATE ===
    // =========================================

    @Test
    @DisplayName("createKnowledgeStore_validRequest_returns200")
    void createKnowledgeStore_validRequest_returns200() throws Exception {
        KnowledgeStoreDTO.CreateRequest request = new KnowledgeStoreDTO.CreateRequest();
        request.setName("New Store");

        KnowledgeStoreDTO.Response response = createMockResponse();
        when(knowledgeStoreService.createKnowledgeStore(any(KnowledgeStoreDTO.CreateRequest.class), eq("admin")))
                .thenReturn(response);

        mockMvc.perform(post("/slib/ai/admin/knowledge-stores")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    // =========================================
    // === UPDATE ===
    // =========================================

    @Test
    @DisplayName("updateKnowledgeStore_validRequest_returns200")
    void updateKnowledgeStore_validRequest_returns200() throws Exception {
        KnowledgeStoreDTO.UpdateRequest request = new KnowledgeStoreDTO.UpdateRequest();
        request.setName("Updated Store");

        KnowledgeStoreDTO.Response response = createMockResponse();
        response.setName("Updated Store");
        when(knowledgeStoreService.updateKnowledgeStore(eq(1L), any(KnowledgeStoreDTO.UpdateRequest.class)))
                .thenReturn(response);

        mockMvc.perform(put("/slib/ai/admin/knowledge-stores/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    // =========================================
    // === DELETE ===
    // =========================================

    @Test
    @DisplayName("deleteKnowledgeStore_existingId_returns200")
    void deleteKnowledgeStore_existingId_returns200() throws Exception {
        doNothing().when(knowledgeStoreService).deleteKnowledgeStore(1L);

        mockMvc.perform(delete("/slib/ai/admin/knowledge-stores/1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(knowledgeStoreService).deleteKnowledgeStore(1L);
    }

    // =========================================
    // === SYNC ===
    // =========================================

    @Test
    @DisplayName("syncKnowledgeStore_existingId_returns200")
    void syncKnowledgeStore_existingId_returns200() throws Exception {
        KnowledgeStoreDTO.SyncResult syncResult = new KnowledgeStoreDTO.SyncResult();
        when(knowledgeStoreService.syncKnowledgeStore(1L)).thenReturn(syncResult);

        mockMvc.perform(post("/slib/ai/admin/knowledge-stores/1/sync")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(knowledgeStoreService).syncKnowledgeStore(1L);
    }
}
