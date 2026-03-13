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
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.exception.ResourceNotFoundException;
import slib.com.example.service.AreaService;

import java.util.Collections;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import slib.com.example.controller.zone_config.AreaController;

/**
 * Unit Tests for FE-21: View Area Map
 * Test Report: doc/Report/FE21_TestReport.md
 */
@WebMvcTest(value = AreaController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-21: View Area Map - Unit Tests")
class FE21_ViewAreaMapTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private AreaService areaService;

        @Test
        @DisplayName("UTCD01: View area map returns 200 OK")
        void viewAreaMap_validToken_returns200OK() throws Exception {
                when(areaService.getAllAreas()).thenReturn(Collections.emptyList());

                mockMvc.perform(get("/slib/areas"))
                        .andExpect(status().isOk());

                verify(areaService, times(1)).getAllAreas();
        }

        @Test
        @DisplayName("UTCD02: View area map with non-existent area ID returns 404 Not Found")
        void viewAreaMap_nonExistentId_returns404() throws Exception {
                when(areaService.getAreaById(999L))
                        .thenThrow(new ResourceNotFoundException("Area not found with id: 999"));

                mockMvc.perform(get("/slib/areas/999"))
                        .andExpect(status().isNotFound());

                verify(areaService, times(1)).getAreaById(999L);
        }
}
