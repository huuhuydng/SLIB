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
import slib.com.example.controller.kiosk.KioskSlideshowController;
import slib.com.example.entity.kiosk.KioskImageEntity;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.repository.kiosk.KioskImageRepository;
import slib.com.example.service.kiosk.KioskCloudinaryService;

import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for FE-119: Change image status
 * Test Report: doc/Report/UnitTestReport/FE53_TestReport.md
 */
@WebMvcTest(value = KioskSlideshowController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-119: Change image status - Unit Tests")
class FE119_ChangeImageStatusTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private KioskImageRepository kioskImageRepository;

        @MockBean
        private KioskCloudinaryService cloudinaryService;

        @Autowired
        private ObjectMapper objectMapper;

        // =========================================
        // === UTCID01: Toggle from inactive to active ===
        // =========================================

        @Test
        @DisplayName("UTCID01: Toggle from inactive to active returns 200 OK")
        void toggleStatus_inactiveToActive_returns200OK() throws Exception {
                KioskImageEntity existingImage = KioskImageEntity.builder()
                                .id(1).imageName("Slide A").imageUrl("http://cdn/a.jpg")
                                .isActive(false).displayOrder(0)
                                .build();

                KioskImageEntity updatedImage = KioskImageEntity.builder()
                                .id(1).imageName("Slide A").imageUrl("http://cdn/a.jpg")
                                .isActive(true).displayOrder(0)
                                .build();

                when(kioskImageRepository.findById(1)).thenReturn(Optional.of(existingImage));
                when(kioskImageRepository.save(any(KioskImageEntity.class))).thenReturn(updatedImage);

                mockMvc.perform(patch("/api/slideshow/images/1/status")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(Map.of("isActive", true))))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true));

                verify(kioskImageRepository, times(1)).findById(1);
                verify(kioskImageRepository, times(1)).save(any(KioskImageEntity.class));
        }

        // =========================================
        // === UTCID02: Toggle from active to inactive ===
        // =========================================

        @Test
        @DisplayName("UTCID02: Toggle from active to inactive returns 200 OK")
        void toggleStatus_activeToInactive_returns200OK() throws Exception {
                KioskImageEntity existingImage = KioskImageEntity.builder()
                                .id(1).imageName("Slide B").imageUrl("http://cdn/b.jpg")
                                .isActive(true).displayOrder(0)
                                .build();

                KioskImageEntity updatedImage = KioskImageEntity.builder()
                                .id(1).imageName("Slide B").imageUrl("http://cdn/b.jpg")
                                .isActive(false).displayOrder(0)
                                .build();

                when(kioskImageRepository.findById(1)).thenReturn(Optional.of(existingImage));
                when(kioskImageRepository.save(any(KioskImageEntity.class))).thenReturn(updatedImage);

                mockMvc.perform(patch("/api/slideshow/images/1/status")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(Map.of("isActive", false))))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true));

                verify(kioskImageRepository, times(1)).findById(1);
                verify(kioskImageRepository, times(1)).save(any(KioskImageEntity.class));
        }

        // =========================================
        // === UTCID03: Status unchanged (no-op) ===
        // =========================================

        @Test
        @DisplayName("UTCID03: Toggle with same status is no-op returns 200 OK")
        void toggleStatus_sameStatus_noOpReturns200OK() throws Exception {
                KioskImageEntity existingImage = KioskImageEntity.builder()
                                .id(1).imageName("Slide C").imageUrl("http://cdn/c.jpg")
                                .isActive(true).displayOrder(0)
                                .build();

                when(kioskImageRepository.findById(1)).thenReturn(Optional.of(existingImage));

                mockMvc.perform(patch("/api/slideshow/images/1/status")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(Map.of("isActive", true))))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true));

                verify(kioskImageRepository, times(1)).findById(1);
                // save should NOT be called when status is unchanged
                verify(kioskImageRepository, never()).save(any(KioskImageEntity.class));
        }

        // =========================================
        // === UTCID04: Image with null active status ===
        // =========================================

        @Test
        @DisplayName("UTCID04: Toggle image with null active status returns 200 OK")
        void toggleStatus_nullActiveStatus_returns200OK() throws Exception {
                KioskImageEntity existingImage = KioskImageEntity.builder()
                                .id(1).imageName("Slide D").imageUrl("http://cdn/d.jpg")
                                .isActive(null).displayOrder(0)
                                .build();

                KioskImageEntity updatedImage = KioskImageEntity.builder()
                                .id(1).imageName("Slide D").imageUrl("http://cdn/d.jpg")
                                .isActive(true).displayOrder(0)
                                .build();

                when(kioskImageRepository.findById(1)).thenReturn(Optional.of(existingImage));
                when(kioskImageRepository.save(any(KioskImageEntity.class))).thenReturn(updatedImage);

                mockMvc.perform(patch("/api/slideshow/images/1/status")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(Map.of("isActive", true))))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true));

                verify(kioskImageRepository, times(1)).findById(1);
                verify(kioskImageRepository, times(1)).save(any(KioskImageEntity.class));
        }

        // =========================================
        // === UTCID05: Image lookup fails ===
        // =========================================

        @Test
        @DisplayName("UTCID05: Toggle status when image lookup fails returns 500")
        void toggleStatus_imageNotFound_returns500() throws Exception {
                when(kioskImageRepository.findById(999))
                                .thenReturn(Optional.empty());

                mockMvc.perform(patch("/api/slideshow/images/999/status")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(Map.of("isActive", true))))
                                .andExpect(status().isInternalServerError())
                                .andExpect(jsonPath("$.success").value(false));

                verify(kioskImageRepository, times(1)).findById(999);
        }
}
