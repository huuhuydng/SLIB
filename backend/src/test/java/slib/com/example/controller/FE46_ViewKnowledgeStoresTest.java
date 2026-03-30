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
import org.springframework.test.web.servlet.MockMvc;
import slib.com.example.controller.ai.KnowledgeStoreController;
import slib.com.example.dto.ai.KnowledgeStoreDTO;
import slib.com.example.entity.ai.KnowledgeStoreEntity;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.service.ai.KnowledgeStoreService;
import slib.com.example.service.system.SystemLogService;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for FE-46: View Knowledge Stores
 * Test Report: doc/Report/UnitTestReport/FE46_TestReport.md
 */
@WebMvcTest(value = KnowledgeStoreController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-46: View Knowledge Stores - Unit Tests")
class FE46_ViewKnowledgeStoresTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private KnowledgeStoreService knowledgeStoreService;

        @MockBean
        private SystemLogService systemLogService;

        @Autowired
        private ObjectMapper objectMapper;

        // =========================================
        // === UTCID01: Populated knowledge-store list ===
        // =========================================

        @Test
        @DisplayName("UTCID01: Get all knowledge stores with populated list returns 200 OK")
        void getAllKnowledgeStores_populatedList_returns200OK() throws Exception {
                List<KnowledgeStoreDTO.Response> mockList = List.of(
                                KnowledgeStoreDTO.Response.builder()
                                                .id(1L).name("Kho A").description("Mo ta A")
                                                .status(KnowledgeStoreEntity.SyncStatus.SYNCED)
                                                .active(true).itemCount(5)
                                                .createdAt(LocalDateTime.now())
                                                .build(),
                                KnowledgeStoreDTO.Response.builder()
                                                .id(2L).name("Kho B").description("Mo ta B")
                                                .status(KnowledgeStoreEntity.SyncStatus.CHANGED)
                                                .active(true).itemCount(2)
                                                .createdAt(LocalDateTime.now().minusDays(1))
                                                .build());

                when(knowledgeStoreService.getAllKnowledgeStores()).thenReturn(mockList);

                mockMvc.perform(get("/slib/ai/admin/knowledge-stores"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.length()").value(2))
                                .andExpect(jsonPath("$[0].name").value("Kho A"));

                verify(knowledgeStoreService, times(1)).getAllKnowledgeStores();
        }

        // =========================================
        // === UTCID02: Stores with items and sync metadata ===
        // =========================================

        @Test
        @DisplayName("UTCID02: Get knowledge stores with sync metadata returns correct status")
        void getAllKnowledgeStores_withSyncMetadata_returnsStatus() throws Exception {
                List<KnowledgeStoreDTO.Response> mockList = List.of(
                                KnowledgeStoreDTO.Response.builder()
                                                .id(1L).name("Kho C")
                                                .status(KnowledgeStoreEntity.SyncStatus.SYNCED)
                                                .lastSyncedAt(LocalDateTime.now())
                                                .itemCount(10)
                                                .build());

                when(knowledgeStoreService.getAllKnowledgeStores()).thenReturn(mockList);

                mockMvc.perform(get("/slib/ai/admin/knowledge-stores"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].status").value("SYNCED"))
                                .andExpect(jsonPath("$[0].itemCount").value(10));

                verify(knowledgeStoreService, times(1)).getAllKnowledgeStores();
        }

        // =========================================
        // === UTCID03: Empty knowledge-store list ===
        // =========================================

        @Test
        @DisplayName("UTCID03: Get all knowledge stores with empty list returns 200 OK")
        void getAllKnowledgeStores_emptyList_returns200OK() throws Exception {
                when(knowledgeStoreService.getAllKnowledgeStores()).thenReturn(Collections.emptyList());

                mockMvc.perform(get("/slib/ai/admin/knowledge-stores"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.length()").value(0));

                verify(knowledgeStoreService, times(1)).getAllKnowledgeStores();
        }

        // =========================================
        // === UTCID04: Repository failure ===
        // =========================================

        @Test
        @DisplayName("UTCID04: Get all knowledge stores when repository fails returns 500")
        void getAllKnowledgeStores_repositoryFailure_returns500() throws Exception {
                when(knowledgeStoreService.getAllKnowledgeStores())
                                .thenThrow(new RuntimeException("Database connection failed"));

                mockMvc.perform(get("/slib/ai/admin/knowledge-stores"))
                                .andExpect(status().isInternalServerError());

                verify(knowledgeStoreService, times(1)).getAllKnowledgeStores();
        }

        // =========================================
        // === UTCID05: Service layer exception ===
        // =========================================

        @Test
        @DisplayName("UTCID05: Get all knowledge stores when service throws exception returns 500")
        void getAllKnowledgeStores_serviceException_returns500() throws Exception {
                when(knowledgeStoreService.getAllKnowledgeStores())
                                .thenThrow(new RuntimeException("Unexpected service error"));

                mockMvc.perform(get("/slib/ai/admin/knowledge-stores"))
                                .andExpect(status().isInternalServerError());

                verify(knowledgeStoreService, times(1)).getAllKnowledgeStores();
        }
}
