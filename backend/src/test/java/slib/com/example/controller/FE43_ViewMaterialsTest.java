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
import slib.com.example.controller.ai.MaterialController;
import slib.com.example.dto.ai.MaterialDTO;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.service.ai.MaterialService;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for FE-43: View Materials
 * Test Report: doc/Report/UnitTestReport/FE43_TestReport.md
 */
@WebMvcTest(value = MaterialController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-43: View Materials - Unit Tests")
class FE43_ViewMaterialsTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private MaterialService materialService;

        @Autowired
        private ObjectMapper objectMapper;

        // =========================================
        // === UTCID01: Populated material list ===
        // =========================================

        @Test
        @DisplayName("UTCID01: Get all materials with populated list returns 200 OK")
        void getAllMaterials_populatedList_returns200OK() throws Exception {
                List<MaterialDTO.MaterialResponse> mockList = List.of(
                                MaterialDTO.MaterialResponse.builder()
                                                .id(1L).name("Tai lieu A").description("Mo ta A")
                                                .active(true).itemCount(3)
                                                .createdAt(LocalDateTime.now())
                                                .build(),
                                MaterialDTO.MaterialResponse.builder()
                                                .id(2L).name("Tai lieu B").description("Mo ta B")
                                                .active(true).itemCount(1)
                                                .createdAt(LocalDateTime.now().minusDays(1))
                                                .build());

                when(materialService.getAllMaterials()).thenReturn(mockList);

                mockMvc.perform(get("/slib/ai/admin/materials"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.length()").value(2))
                                .andExpect(jsonPath("$[0].name").value("Tai lieu A"));

                verify(materialService, times(1)).getAllMaterials();
        }

        // =========================================
        // === UTCID02: Materials with items metadata ===
        // =========================================

        @Test
        @DisplayName("UTCID02: Get materials with item metadata returns correct item counts")
        void getAllMaterials_withItemMetadata_returnsItemCounts() throws Exception {
                List<MaterialDTO.MaterialResponse> mockList = List.of(
                                MaterialDTO.MaterialResponse.builder()
                                                .id(1L).name("Tai lieu C")
                                                .active(true).itemCount(5)
                                                .items(List.of(
                                                                MaterialDTO.ItemResponse.builder().id(1L).name("Item 1").build()))
                                                .build());

                when(materialService.getAllMaterials()).thenReturn(mockList);

                mockMvc.perform(get("/slib/ai/admin/materials"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].itemCount").value(5));

                verify(materialService, times(1)).getAllMaterials();
        }

        // =========================================
        // === UTCID03: Empty material list ===
        // =========================================

        @Test
        @DisplayName("UTCID03: Get all materials with empty list returns 200 OK with empty array")
        void getAllMaterials_emptyList_returns200OK() throws Exception {
                when(materialService.getAllMaterials()).thenReturn(Collections.emptyList());

                mockMvc.perform(get("/slib/ai/admin/materials"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.length()").value(0));

                verify(materialService, times(1)).getAllMaterials();
        }

        // =========================================
        // === UTCID04: Repository failure ===
        // =========================================

        @Test
        @DisplayName("UTCID04: Get all materials when repository fails returns 500")
        void getAllMaterials_repositoryFailure_returns500() throws Exception {
                when(materialService.getAllMaterials())
                                .thenThrow(new RuntimeException("Database connection failed"));

                mockMvc.perform(get("/slib/ai/admin/materials"))
                                .andExpect(status().isInternalServerError());

                verify(materialService, times(1)).getAllMaterials();
        }

        // =========================================
        // === UTCID05: Service layer exception ===
        // =========================================

        @Test
        @DisplayName("UTCID05: Get all materials when service throws exception returns 500")
        void getAllMaterials_serviceException_returns500() throws Exception {
                when(materialService.getAllMaterials())
                                .thenThrow(new RuntimeException("Unexpected service error"));

                mockMvc.perform(get("/slib/ai/admin/materials"))
                                .andExpect(status().isInternalServerError());

                verify(materialService, times(1)).getAllMaterials();
        }
}
