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

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for FE-51: View Kiosk Images
 * Test Report: doc/Report/UnitTestReport/FE51_TestReport.md
 */
@WebMvcTest(value = KioskSlideshowController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-51: View Kiosk Images - Unit Tests")
class FE51_ViewKioskImagesTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private KioskImageRepository kioskImageRepository;

        @MockBean
        private KioskCloudinaryService cloudinaryService;

        @Autowired
        private ObjectMapper objectMapper;

        // =========================================
        // === UTCID01: Populated image list ===
        // =========================================

        @Test
        @DisplayName("UTCID01: Get images with populated list returns 200 OK")
        void getImages_populatedList_returns200OK() throws Exception {
                List<KioskImageEntity> mockImages = List.of(
                                KioskImageEntity.builder()
                                                .id(1).imageName("Banner 1").imageUrl("http://cdn/img1.jpg")
                                                .isActive(true).displayOrder(0).durationSeconds(10)
                                                .build(),
                                KioskImageEntity.builder()
                                                .id(2).imageName("Banner 2").imageUrl("http://cdn/img2.jpg")
                                                .isActive(false).displayOrder(1).durationSeconds(10)
                                                .build());

                when(kioskImageRepository.findAllByOrderByDisplayOrderAscCreatedAtDesc())
                                .thenReturn(mockImages);

                mockMvc.perform(get("/api/slideshow/images"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.images.length()").value(2));

                verify(kioskImageRepository, times(1)).findAllByOrderByDisplayOrderAscCreatedAtDesc();
        }

        // =========================================
        // === UTCID02: Images with metadata ===
        // =========================================

        @Test
        @DisplayName("UTCID02: Get images returns active flag and order metadata")
        void getImages_withMetadata_returnsActiveAndOrder() throws Exception {
                List<KioskImageEntity> mockImages = List.of(
                                KioskImageEntity.builder()
                                                .id(1).imageName("Slide A").imageUrl("http://cdn/a.jpg")
                                                .isActive(true).displayOrder(0).durationSeconds(5)
                                                .build());

                when(kioskImageRepository.findAllByOrderByDisplayOrderAscCreatedAtDesc())
                                .thenReturn(mockImages);

                mockMvc.perform(get("/api/slideshow/images"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.images[0].imageName").value("Slide A"));

                verify(kioskImageRepository, times(1)).findAllByOrderByDisplayOrderAscCreatedAtDesc();
        }

        // =========================================
        // === UTCID03: Empty image list ===
        // =========================================

        @Test
        @DisplayName("UTCID03: Get images with empty list returns 200 OK")
        void getImages_emptyList_returns200OK() throws Exception {
                when(kioskImageRepository.findAllByOrderByDisplayOrderAscCreatedAtDesc())
                                .thenReturn(Collections.emptyList());

                mockMvc.perform(get("/api/slideshow/images"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.images.length()").value(0));

                verify(kioskImageRepository, times(1)).findAllByOrderByDisplayOrderAscCreatedAtDesc();
        }

        // =========================================
        // === UTCID04: Repository failure ===
        // =========================================

        @Test
        @DisplayName("UTCID04: Get images when repository fails returns 500")
        void getImages_repositoryFailure_returns500() throws Exception {
                when(kioskImageRepository.findAllByOrderByDisplayOrderAscCreatedAtDesc())
                                .thenThrow(new RuntimeException("Database connection failed"));

                mockMvc.perform(get("/api/slideshow/images"))
                                .andExpect(status().isInternalServerError())
                                .andExpect(jsonPath("$.success").value(false));

                verify(kioskImageRepository, times(1)).findAllByOrderByDisplayOrderAscCreatedAtDesc();
        }

        // =========================================
        // === UTCID05: Unexpected service error ===
        // =========================================

        @Test
        @DisplayName("UTCID05: Get images when unexpected error occurs returns 500")
        void getImages_unexpectedError_returns500() throws Exception {
                when(kioskImageRepository.findAllByOrderByDisplayOrderAscCreatedAtDesc())
                                .thenThrow(new RuntimeException("Unexpected error"));

                mockMvc.perform(get("/api/slideshow/images"))
                                .andExpect(status().isInternalServerError())
                                .andExpect(jsonPath("$.success").value(false));

                verify(kioskImageRepository, times(1)).findAllByOrderByDisplayOrderAscCreatedAtDesc();
        }
}
