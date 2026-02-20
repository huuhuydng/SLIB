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

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for MaterialController
 */
@WebMvcTest(value = MaterialController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
        slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("MaterialController Unit Tests")
class MaterialControllerUnitTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MaterialService materialService;

    @Autowired
    private ObjectMapper objectMapper;

    private MaterialDTO.MaterialResponse createMockMaterialResponse() {
        MaterialDTO.MaterialResponse response = new MaterialDTO.MaterialResponse();
        response.setId(1L);
        response.setName("Test Material");
        return response;
    }

    private MaterialDTO.ItemResponse createMockItemResponse() {
        MaterialDTO.ItemResponse response = new MaterialDTO.ItemResponse();
        response.setId(1L);
        response.setName("Test Item");
        return response;
    }

    // =========================================
    // === MATERIAL CRUD ===
    // =========================================

    @Test
    @DisplayName("getAllMaterials_returns200WithList")
    void getAllMaterials_returns200WithList() throws Exception {
        List<MaterialDTO.MaterialResponse> list = List.of(createMockMaterialResponse());
        when(materialService.getAllMaterials()).thenReturn(list);

        mockMvc.perform(get("/slib/ai/admin/materials")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("getMaterialById_existingId_returns200")
    void getMaterialById_existingId_returns200() throws Exception {
        MaterialDTO.MaterialResponse response = createMockMaterialResponse();
        when(materialService.getMaterialById(1L)).thenReturn(response);

        mockMvc.perform(get("/slib/ai/admin/materials/1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("createMaterial_validRequest_returns200")
    void createMaterial_validRequest_returns200() throws Exception {
        MaterialDTO.MaterialRequest request = new MaterialDTO.MaterialRequest();
        request.setName("New Material");

        MaterialDTO.MaterialResponse response = createMockMaterialResponse();
        when(materialService.createMaterial(any(MaterialDTO.MaterialRequest.class), eq("admin")))
                .thenReturn(response);

        mockMvc.perform(post("/slib/ai/admin/materials")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("updateMaterial_validRequest_returns200")
    void updateMaterial_validRequest_returns200() throws Exception {
        MaterialDTO.MaterialRequest request = new MaterialDTO.MaterialRequest();
        request.setName("Updated Material");

        MaterialDTO.MaterialResponse response = createMockMaterialResponse();
        when(materialService.updateMaterial(eq(1L), any(MaterialDTO.MaterialRequest.class)))
                .thenReturn(response);

        mockMvc.perform(put("/slib/ai/admin/materials/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("deleteMaterial_existingId_returns200")
    void deleteMaterial_existingId_returns200() throws Exception {
        doNothing().when(materialService).deleteMaterial(1L);

        mockMvc.perform(delete("/slib/ai/admin/materials/1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(materialService).deleteMaterial(1L);
    }

    @Test
    @DisplayName("toggleActive_validRequest_returns200")
    void toggleActive_validRequest_returns200() throws Exception {
        doNothing().when(materialService).toggleMaterialActive(1L, true);

        mockMvc.perform(patch("/slib/ai/admin/materials/1/toggle")
                .param("active", "true")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.active").value(true));
    }

    // =========================================
    // === ITEM OPERATIONS ===
    // =========================================

    @Test
    @DisplayName("getItems_existingMaterialId_returns200")
    void getItems_existingMaterialId_returns200() throws Exception {
        List<MaterialDTO.ItemResponse> items = List.of(createMockItemResponse());
        when(materialService.getItemsByMaterialId(1L)).thenReturn(items);

        mockMvc.perform(get("/slib/ai/admin/materials/1/items")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("addTextItem_validRequest_returns200")
    void addTextItem_validRequest_returns200() throws Exception {
        MaterialDTO.ItemRequest request = new MaterialDTO.ItemRequest();
        request.setName("New Text Item");

        MaterialDTO.ItemResponse response = createMockItemResponse();
        when(materialService.addTextItem(eq(1L), any(MaterialDTO.ItemRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/slib/ai/admin/materials/1/items/text")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("updateItem_validRequest_returns200")
    void updateItem_validRequest_returns200() throws Exception {
        MaterialDTO.ItemRequest request = new MaterialDTO.ItemRequest();
        request.setName("Updated Item");

        MaterialDTO.ItemResponse response = createMockItemResponse();
        when(materialService.updateItem(eq(1L), eq(2L), any(MaterialDTO.ItemRequest.class)))
                .thenReturn(response);

        mockMvc.perform(put("/slib/ai/admin/materials/1/items/2")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("deleteItem_existingIds_returns200")
    void deleteItem_existingIds_returns200() throws Exception {
        doNothing().when(materialService).deleteItem(1L, 2L);

        mockMvc.perform(delete("/slib/ai/admin/materials/1/items/2")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(materialService).deleteItem(1L, 2L);
    }
}
