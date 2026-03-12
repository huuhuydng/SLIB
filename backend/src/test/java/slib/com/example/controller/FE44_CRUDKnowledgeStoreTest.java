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
import slib.com.example.entity.ai.KnowledgeStoreEntity;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.service.ai.KnowledgeStoreService;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for FE-44: CRUD Knowledge Store
 * Test Report: doc/Report/UnitTestReport/FE44_TestReport.md
 */
@WebMvcTest(value = KnowledgeStoreController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-44: CRUD Knowledge Store - Unit Tests")
class FE44_CRUDKnowledgeStoreTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private KnowledgeStoreService knowledgeStoreService;

        @Autowired
        private ObjectMapper objectMapper;

        // =========================================
        // === UTCID01: Create knowledge store - Success ===
        // =========================================

        @Test
        @DisplayName("UTCID01: Create knowledge store with valid data returns 200 OK")
        void createKnowledgeStore_validData_returns200OK() throws Exception {
                KnowledgeStoreDTO.CreateRequest request = KnowledgeStoreDTO.CreateRequest.builder()
                                .name("Kho tri thuc moi")
                                .description("Mo ta kho tri thuc")
                                .itemIds(Set.of(1L, 2L))
                                .build();

                KnowledgeStoreDTO.Response mockResponse = KnowledgeStoreDTO.Response.builder()
                                .id(1L)
                                .name("Kho tri thuc moi")
                                .description("Mo ta kho tri thuc")
                                .createdBy("admin")
                                .status(KnowledgeStoreEntity.SyncStatus.CHANGED)
                                .active(true)
                                .itemCount(2)
                                .createdAt(LocalDateTime.now())
                                .build();

                when(knowledgeStoreService.createKnowledgeStore(any(KnowledgeStoreDTO.CreateRequest.class), anyString()))
                                .thenReturn(mockResponse);

                mockMvc.perform(post("/slib/ai/admin/knowledge-stores")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(1))
                                .andExpect(jsonPath("$.name").value("Kho tri thuc moi"));

                verify(knowledgeStoreService, times(1)).createKnowledgeStore(any(KnowledgeStoreDTO.CreateRequest.class), anyString());
        }

        // =========================================
        // === UTCID02: Create knowledge store with empty itemIds ===
        // =========================================

        @Test
        @DisplayName("UTCID02: Create knowledge store without items returns 200 OK")
        void createKnowledgeStore_noItems_returns200OK() throws Exception {
                KnowledgeStoreDTO.CreateRequest request = KnowledgeStoreDTO.CreateRequest.builder()
                                .name("Kho tri thuc rong")
                                .description("Khong co item")
                                .build();

                KnowledgeStoreDTO.Response mockResponse = KnowledgeStoreDTO.Response.builder()
                                .id(2L)
                                .name("Kho tri thuc rong")
                                .status(KnowledgeStoreEntity.SyncStatus.CHANGED)
                                .active(true)
                                .itemCount(0)
                                .build();

                when(knowledgeStoreService.createKnowledgeStore(any(KnowledgeStoreDTO.CreateRequest.class), anyString()))
                                .thenReturn(mockResponse);

                mockMvc.perform(post("/slib/ai/admin/knowledge-stores")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.itemCount").value(0));

                verify(knowledgeStoreService, times(1)).createKnowledgeStore(any(KnowledgeStoreDTO.CreateRequest.class), anyString());
        }

        // =========================================
        // === UTCID03: Update knowledge store - Success ===
        // =========================================

        @Test
        @DisplayName("UTCID03: Update existing knowledge store returns 200 OK")
        void updateKnowledgeStore_existingId_returns200OK() throws Exception {
                KnowledgeStoreDTO.UpdateRequest request = KnowledgeStoreDTO.UpdateRequest.builder()
                                .name("Kho tri thuc cap nhat")
                                .description("Mo ta moi")
                                .itemIds(Set.of(1L, 2L, 3L))
                                .build();

                KnowledgeStoreDTO.Response mockResponse = KnowledgeStoreDTO.Response.builder()
                                .id(1L)
                                .name("Kho tri thuc cap nhat")
                                .description("Mo ta moi")
                                .status(KnowledgeStoreEntity.SyncStatus.CHANGED)
                                .itemCount(3)
                                .build();

                when(knowledgeStoreService.updateKnowledgeStore(eq(1L), any(KnowledgeStoreDTO.UpdateRequest.class)))
                                .thenReturn(mockResponse);

                mockMvc.perform(put("/slib/ai/admin/knowledge-stores/1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.name").value("Kho tri thuc cap nhat"));

                verify(knowledgeStoreService, times(1)).updateKnowledgeStore(eq(1L), any(KnowledgeStoreDTO.UpdateRequest.class));
        }

        // =========================================
        // === UTCID04: Delete knowledge store - Success ===
        // =========================================

        @Test
        @DisplayName("UTCID04: Delete existing knowledge store returns 200 OK")
        void deleteKnowledgeStore_existingId_returns200OK() throws Exception {
                doNothing().when(knowledgeStoreService).deleteKnowledgeStore(1L);

                mockMvc.perform(delete("/slib/ai/admin/knowledge-stores/1"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true));

                verify(knowledgeStoreService, times(1)).deleteKnowledgeStore(1L);
        }

        // =========================================
        // === UTCID05: Sync knowledge store - Success ===
        // =========================================

        @Test
        @DisplayName("UTCID05: Sync knowledge store returns 200 OK")
        void syncKnowledgeStore_existingId_returns200OK() throws Exception {
                KnowledgeStoreDTO.SyncResult mockResult = KnowledgeStoreDTO.SyncResult.builder()
                                .knowledgeStoreId(1L)
                                .knowledgeStoreName("Kho tri thuc A")
                                .chunksCreated(10)
                                .newStatus(KnowledgeStoreEntity.SyncStatus.SYNCED)
                                .message("Sync completed")
                                .build();

                when(knowledgeStoreService.syncKnowledgeStore(1L)).thenReturn(mockResult);

                mockMvc.perform(post("/slib/ai/admin/knowledge-stores/1/sync"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.chunksCreated").value(10));

                verify(knowledgeStoreService, times(1)).syncKnowledgeStore(1L);
        }

        // =========================================
        // === UTCID06: Update non-existent store ===
        // =========================================

        @Test
        @DisplayName("UTCID06: Update non-existent knowledge store returns 500")
        void updateKnowledgeStore_nonExistentId_returnsError() throws Exception {
                KnowledgeStoreDTO.UpdateRequest request = KnowledgeStoreDTO.UpdateRequest.builder()
                                .name("Khong ton tai")
                                .build();

                when(knowledgeStoreService.updateKnowledgeStore(eq(999L), any(KnowledgeStoreDTO.UpdateRequest.class)))
                                .thenThrow(new RuntimeException("Knowledge store not found with id: 999"));

                mockMvc.perform(put("/slib/ai/admin/knowledge-stores/999")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isInternalServerError());

                verify(knowledgeStoreService, times(1)).updateKnowledgeStore(eq(999L), any(KnowledgeStoreDTO.UpdateRequest.class));
        }

        // =========================================
        // === UTCID07: Delete non-existent store ===
        // =========================================

        @Test
        @DisplayName("UTCID07: Delete non-existent knowledge store returns 500")
        void deleteKnowledgeStore_nonExistentId_returnsError() throws Exception {
                doThrow(new RuntimeException("Knowledge store not found with id: 999"))
                                .when(knowledgeStoreService).deleteKnowledgeStore(999L);

                mockMvc.perform(delete("/slib/ai/admin/knowledge-stores/999"))
                                .andExpect(status().isInternalServerError());

                verify(knowledgeStoreService, times(1)).deleteKnowledgeStore(999L);
        }
}
