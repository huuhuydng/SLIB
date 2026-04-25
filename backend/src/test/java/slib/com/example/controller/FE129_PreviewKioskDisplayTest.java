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
import slib.com.example.controller.kiosk.KioskSlideshowController;
import slib.com.example.entity.kiosk.KioskImageEntity;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.repository.kiosk.KioskImageRepository;
import slib.com.example.service.kiosk.KioskCloudinaryService;

import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for FE-129: Preview Kiosk display
 * Test Report: doc/Report/UnitTestReport/FE54_TestReport.md
 */
@WebMvcTest(value = KioskSlideshowController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-129: Preview Kiosk display - Unit Tests")
class FE129_PreviewKioskDisplayTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private KioskImageRepository kioskImageRepository;

        @MockBean
        private KioskCloudinaryService cloudinaryService;

        @Autowired
        private ObjectMapper objectMapper;

        // =========================================
        // === UTCID01: Preview with active images ===
        // =========================================

        @Test
        @DisplayName("UTCID01: Preview with active images returns 200 OK for both endpoints")
        void preview_activeImages_returns200OK() throws Exception {
                List<KioskImageEntity> mockImages = List.of(
                                KioskImageEntity.builder()
                                                .id(1).imageName("Active Slide").imageUrl("http://cdn/a.jpg")
                                                .isActive(true).displayOrder(0).durationSeconds(5)
                                                .build());

                when(kioskImageRepository.findAllByOrderByDisplayOrderAscCreatedAtDesc())
                                .thenReturn(mockImages);

                // Verify image list endpoint
                mockMvc.perform(get("/api/slideshow/images"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.images.length()").value(1));

                // Verify config endpoint
                mockMvc.perform(get("/api/slideshow/config"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.config.duration").value(5000));
        }

        // =========================================
        // === UTCID02: Preview with mixed active/inactive images ===
        // =========================================

        @Test
        @DisplayName("UTCID02: Preview with mixed images returns all images")
        void preview_mixedImages_returnsAll() throws Exception {
                List<KioskImageEntity> mockImages = List.of(
                                KioskImageEntity.builder()
                                                .id(1).imageName("Active").imageUrl("http://cdn/a.jpg")
                                                .isActive(true).displayOrder(0)
                                                .build(),
                                KioskImageEntity.builder()
                                                .id(2).imageName("Inactive").imageUrl("http://cdn/b.jpg")
                                                .isActive(false).displayOrder(1)
                                                .build());

                when(kioskImageRepository.findAllByOrderByDisplayOrderAscCreatedAtDesc())
                                .thenReturn(mockImages);

                mockMvc.perform(get("/api/slideshow/images"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.images.length()").value(2));
        }

        // =========================================
        // === UTCID03: Config endpoint always returns static config ===
        // =========================================

        @Test
        @DisplayName("UTCID03: Config endpoint returns static duration config")
        void getConfig_returns200WithDuration() throws Exception {
                mockMvc.perform(get("/api/slideshow/config"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.config.duration").value(5000));
        }

        // =========================================
        // === UTCID04: Image list failure, config independent ===
        // =========================================

        @Test
        @DisplayName("UTCID04: Image list fails but config remains independent")
        void preview_imageListFails_configStillWorks() throws Exception {
                when(kioskImageRepository.findAllByOrderByDisplayOrderAscCreatedAtDesc())
                                .thenThrow(new RuntimeException("Database unavailable"));

                // Image list fails
                mockMvc.perform(get("/api/slideshow/images"))
                                .andExpect(status().isInternalServerError())
                                .andExpect(jsonPath("$.success").value(false));

                // Config still works
                mockMvc.perform(get("/api/slideshow/config"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true));
        }

        // =========================================
        // === UTCID05: Image list throws, error payload returned ===
        // =========================================

        @Test
        @DisplayName("UTCID05: Image list throws error, returns error payload")
        void preview_imageListThrows_returnsErrorPayload() throws Exception {
                when(kioskImageRepository.findAllByOrderByDisplayOrderAscCreatedAtDesc())
                                .thenThrow(new RuntimeException("Unexpected failure"));

                mockMvc.perform(get("/api/slideshow/images"))
                                .andExpect(status().isInternalServerError())
                                .andExpect(jsonPath("$.success").value(false))
                                .andExpect(jsonPath("$.message").exists());
        }
}
