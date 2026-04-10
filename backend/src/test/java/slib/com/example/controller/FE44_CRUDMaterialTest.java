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
import slib.com.example.controller.ai.MaterialController;
import slib.com.example.dto.ai.MaterialDTO;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.service.ai.MaterialService;
import slib.com.example.service.system.SystemLogService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for FE-44: CRUD material
 * Test Report: doc/Report/UnitTestReport/FE43_TestReport.md
 */
@WebMvcTest(value = MaterialController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-44: CRUD material - Unit Tests")
class FE44_CRUDMaterialTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private MaterialService materialService;

        @MockBean
        private SystemLogService systemLogService;

        @Autowired
        private ObjectMapper objectMapper;

        // =========================================
        // === UTCID01: Create material - Success ===
        // =========================================

        @Test
        @DisplayName("UTCID01: Create material with valid data returns 200 OK")
        void createMaterial_validData_returns200OK() throws Exception {
                MaterialDTO.MaterialRequest request = MaterialDTO.MaterialRequest.builder()
                                .name("Tai lieu huong dan")
                                .description("Mo ta tai lieu")
                                .build();

                MaterialDTO.MaterialResponse mockResponse = MaterialDTO.MaterialResponse.builder()
                                .id(1L)
                                .name("Tai lieu huong dan")
                                .description("Mo ta tai lieu")
                                .createdBy("admin")
                                .active(true)
                                .createdAt(LocalDateTime.now())
                                .build();

                when(materialService.createMaterial(any(MaterialDTO.MaterialRequest.class), anyString()))
                                .thenReturn(mockResponse);

                mockMvc.perform(post("/slib/ai/admin/materials")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(1))
                                .andExpect(jsonPath("$.name").value("Tai lieu huong dan"));

                verify(materialService, times(1)).createMaterial(any(MaterialDTO.MaterialRequest.class), anyString());
        }

        // =========================================
        // === UTCID02: Update material - Success ===
        // =========================================

        @Test
        @DisplayName("UTCID02: Update material with valid data returns 200 OK")
        void updateMaterial_validData_returns200OK() throws Exception {
                MaterialDTO.MaterialRequest request = MaterialDTO.MaterialRequest.builder()
                                .name("Tai lieu cap nhat")
                                .description("Mo ta moi")
                                .build();

                MaterialDTO.MaterialResponse mockResponse = MaterialDTO.MaterialResponse.builder()
                                .id(1L)
                                .name("Tai lieu cap nhat")
                                .description("Mo ta moi")
                                .active(true)
                                .build();

                when(materialService.updateMaterial(eq(1L), any(MaterialDTO.MaterialRequest.class)))
                                .thenReturn(mockResponse);

                mockMvc.perform(put("/slib/ai/admin/materials/1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.name").value("Tai lieu cap nhat"));

                verify(materialService, times(1)).updateMaterial(eq(1L), any(MaterialDTO.MaterialRequest.class));
        }

        // =========================================
        // === UTCID03: Delete material - Success ===
        // =========================================

        @Test
        @DisplayName("UTCID03: Delete existing material returns 200 OK")
        void deleteMaterial_existingId_returns200OK() throws Exception {
                doNothing().when(materialService).deleteMaterial(1L);

                mockMvc.perform(delete("/slib/ai/admin/materials/1"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true));

                verify(materialService, times(1)).deleteMaterial(1L);
        }

        // =========================================
        // === UTCID04: Toggle material active - Success ===
        // =========================================

        @Test
        @DisplayName("UTCID04: Toggle material active status returns 200 OK")
        void toggleActive_existingMaterial_returns200OK() throws Exception {
                doNothing().when(materialService).toggleMaterialActive(1L, false);

                mockMvc.perform(patch("/slib/ai/admin/materials/1/toggle")
                                .param("active", "false"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.active").value(false));

                verify(materialService, times(1)).toggleMaterialActive(1L, false);
        }

        // =========================================
        // === UTCID05: Get material by ID - Success ===
        // =========================================

        @Test
        @DisplayName("UTCID05: Get material by valid ID returns 200 OK")
        void getMaterialById_validId_returns200OK() throws Exception {
                MaterialDTO.MaterialResponse mockResponse = MaterialDTO.MaterialResponse.builder()
                                .id(1L)
                                .name("Tai lieu A")
                                .description("Mo ta A")
                                .active(true)
                                .build();

                when(materialService.getMaterialById(1L)).thenReturn(mockResponse);

                mockMvc.perform(get("/slib/ai/admin/materials/1"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(1))
                                .andExpect(jsonPath("$.name").value("Tai lieu A"));

                verify(materialService, times(1)).getMaterialById(1L);
        }

        // =========================================
        // === UTCID06: Update non-existent material - Not Found ===
        // =========================================

        @Test
        @DisplayName("UTCID06: Update non-existent material returns 500 Internal Server Error")
        void updateMaterial_nonExistentId_returnsError() throws Exception {
                MaterialDTO.MaterialRequest request = MaterialDTO.MaterialRequest.builder()
                                .name("Tai lieu khong ton tai")
                                .build();

                when(materialService.updateMaterial(eq(999L), any(MaterialDTO.MaterialRequest.class)))
                                .thenThrow(new RuntimeException("Material not found with id: 999"));

                mockMvc.perform(put("/slib/ai/admin/materials/999")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isInternalServerError());

                verify(materialService, times(1)).updateMaterial(eq(999L), any(MaterialDTO.MaterialRequest.class));
        }

        // =========================================
        // === UTCID07: Delete non-existent material - Not Found ===
        // =========================================

        @Test
        @DisplayName("UTCID07: Delete non-existent material returns 500 Internal Server Error")
        void deleteMaterial_nonExistentId_returnsError() throws Exception {
                doThrow(new RuntimeException("Material not found with id: 999"))
                                .when(materialService).deleteMaterial(999L);

                mockMvc.perform(delete("/slib/ai/admin/materials/999"))
                                .andExpect(status().isInternalServerError());

                verify(materialService, times(1)).deleteMaterial(999L);
        }
}
