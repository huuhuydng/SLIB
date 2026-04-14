package slib.com.example.controller;

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
import slib.com.example.controller.zone_config.AreaController;
import slib.com.example.dto.zone_config.AreaResponse;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.exception.ResourceNotFoundException;
import slib.com.example.service.zone_config.AreaService;

import java.util.List;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = AreaController.class, excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = { slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-22: View area map - Unit Tests")
class FE22_ViewAreaMapTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AreaService areaService;

    private AreaResponse buildArea(Long id, String name, Integer width, Integer height) {
        return new AreaResponse(id, name, width, height, 10, 20, true, false);
    }

    @Test
    @DisplayName("UTCID01: View all areas with existing data")
    void viewAllAreas_withExistingData() throws Exception {
        when(areaService.getAllAreas()).thenReturn(List.of(buildArea(1L, "Khu A", 100, 80)));

        mockMvc.perform(get("/slib/areas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].areaId").value(1))
                .andExpect(jsonPath("$[0].areaName").value("Khu A"))
                .andExpect(jsonPath("$[0].width").value(100));

        verify(areaService, times(1)).getAllAreas();
    }

    @Test
    @DisplayName("UTCID02: View area detail with valid areaId")
    void viewAreaDetail_withValidAreaId() throws Exception {
        when(areaService.getAreaById(2L)).thenReturn(buildArea(2L, "Khu B", 120, 90));

        mockMvc.perform(get("/slib/areas/{id}", 2L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.areaId").value(2))
                .andExpect(jsonPath("$.areaName").value("Khu B"));
    }

    @Test
    @DisplayName("UTCID03: View area detail with invalid areaId format")
    void viewAreaDetail_withInvalidAreaIdFormat() throws Exception {
        mockMvc.perform(get("/slib/areas/{id}", "invalid-id"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("UTCID04: View area detail with non-existent areaId")
    void viewAreaDetail_withNonExistentAreaId() throws Exception {
        when(areaService.getAreaById(999L))
                .thenThrow(new ResourceNotFoundException("Area not found with id: 999"));

        mockMvc.perform(get("/slib/areas/{id}", 999L))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("UTCID05: View area map when service throws runtime exception")
    void viewAreaMap_whenServiceThrowsRuntimeException() throws Exception {
        when(areaService.getAllAreas()).thenThrow(new RuntimeException("Database error"));

        mockMvc.perform(get("/slib/areas"))
                .andExpect(status().isInternalServerError());
    }
}
