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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import slib.com.example.controller.kiosk.KioskSlideshowController;
import slib.com.example.entity.kiosk.KioskImageEntity;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.repository.kiosk.KioskImageRepository;
import slib.com.example.service.kiosk.KioskCloudinaryService;

import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for FE-127: CRUD Kiosk image
 * Test Report: doc/Report/UnitTestReport/FE52_TestReport.md
 */
@WebMvcTest(value = KioskSlideshowController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-127: CRUD Kiosk image - Unit Tests")
class FE127_CRUDKioskImageTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private KioskImageRepository kioskImageRepository;

        @MockBean
        private KioskCloudinaryService cloudinaryService;

        @Autowired
        private ObjectMapper objectMapper;

        // =========================================
        // === UTCID01: Fetch slideshow image list ===
        // =========================================

        @Test
        @DisplayName("UTCID01: Fetch slideshow image list returns 200 OK")
        void getImages_returns200OK() throws Exception {
                List<KioskImageEntity> mockImages = List.of(
                                KioskImageEntity.builder()
                                                .id(1).imageName("Banner 1").imageUrl("http://cdn/img1.jpg")
                                                .isActive(true).displayOrder(0).durationSeconds(10)
                                                .build());

                when(kioskImageRepository.findAllByOrderByDisplayOrderAscCreatedAtDesc())
                                .thenReturn(mockImages);

                mockMvc.perform(get("/api/slideshow/images"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true));

                verify(kioskImageRepository, times(1)).findAllByOrderByDisplayOrderAscCreatedAtDesc();
        }

        // =========================================
        // === UTCID02: Upload valid slideshow images ===
        // =========================================

        @Test
        @DisplayName("UTCID02: Upload valid slideshow images returns 200 OK")
        void uploadImages_validFiles_returns200OK() throws Exception {
                MockMultipartFile file = new MockMultipartFile(
                                "images", "test.jpg", "image/jpeg", "fake-image-data".getBytes());

                Map<String, Object> cloudResult = Map.of(
                                "url", "http://cdn/uploaded.jpg",
                                "public_id", "slideshow/test");

                when(cloudinaryService.uploadSlideShowImage(any())).thenReturn(cloudResult);

                KioskImageEntity savedImage = KioskImageEntity.builder()
                                .id(10).imageName("test.jpg").imageUrl("http://cdn/uploaded.jpg")
                                .publicId("slideshow/test").isActive(false).displayOrder(9999)
                                .build();

                when(kioskImageRepository.save(any(KioskImageEntity.class))).thenReturn(savedImage);

                mockMvc.perform(multipart("/api/slideshow/images")
                                .file(file))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true));

                verify(cloudinaryService, times(1)).uploadSlideShowImage(any());
                verify(kioskImageRepository, times(1)).save(any(KioskImageEntity.class));
        }

        // =========================================
        // === UTCID03: Rename existing image ===
        // =========================================

        @Test
        @DisplayName("UTCID03: Rename existing image returns 200 OK")
        void renameImage_existingId_returns200OK() throws Exception {
                KioskImageEntity existingImage = KioskImageEntity.builder()
                                .id(1).imageName("Old Name").imageUrl("http://cdn/img.jpg")
                                .isActive(true).displayOrder(0)
                                .build();

                KioskImageEntity updatedImage = KioskImageEntity.builder()
                                .id(1).imageName("New Name").imageUrl("http://cdn/img.jpg")
                                .isActive(true).displayOrder(0)
                                .build();

                when(kioskImageRepository.findById(1)).thenReturn(Optional.of(existingImage));
                when(kioskImageRepository.save(any(KioskImageEntity.class))).thenReturn(updatedImage);

                mockMvc.perform(put("/api/slideshow/images/1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(Map.of("newName", "New Name"))))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true));

                verify(kioskImageRepository, times(1)).findById(1);
                verify(kioskImageRepository, times(1)).save(any(KioskImageEntity.class));
        }

        // =========================================
        // === UTCID04: Delete existing image ===
        // =========================================

        @Test
        @DisplayName("UTCID04: Delete existing image returns 200 OK")
        void deleteImage_existingId_returns200OK() throws Exception {
                KioskImageEntity existingImage = KioskImageEntity.builder()
                                .id(1).imageName("To Delete").imageUrl("http://cdn/img.jpg")
                                .publicId("slideshow/img").isActive(true).displayOrder(0)
                                .build();

                when(kioskImageRepository.findById(1)).thenReturn(Optional.of(existingImage));
                when(cloudinaryService.deleteSlideShowImage(anyString())).thenReturn(Map.of("result", "ok"));

                mockMvc.perform(delete("/api/slideshow/images/1"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true));

                verify(kioskImageRepository, times(1)).findById(1);
                verify(cloudinaryService, times(1)).deleteSlideShowImage("slideshow/img");
                verify(kioskImageRepository, times(1)).delete(any(KioskImageEntity.class));
        }

        // =========================================
        // === UTCID05: Toggle image active status ===
        // =========================================

        @Test
        @DisplayName("UTCID05: Toggle image active status returns 200 OK")
        void toggleStatus_existingImage_returns200OK() throws Exception {
                KioskImageEntity existingImage = KioskImageEntity.builder()
                                .id(1).imageName("Slide").imageUrl("http://cdn/img.jpg")
                                .isActive(false).displayOrder(0)
                                .build();

                KioskImageEntity updatedImage = KioskImageEntity.builder()
                                .id(1).imageName("Slide").imageUrl("http://cdn/img.jpg")
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
        }

        // =========================================
        // === UTCID06: Reorder slideshow images ===
        // =========================================

        @Test
        @DisplayName("UTCID06: Reorder slideshow images returns 200 OK")
        void reorderImages_validIds_returns200OK() throws Exception {
                KioskImageEntity img1 = KioskImageEntity.builder().id(2).imageName("Img 2").displayOrder(0).build();
                KioskImageEntity img2 = KioskImageEntity.builder().id(1).imageName("Img 1").displayOrder(1).build();

                when(kioskImageRepository.findById(2)).thenReturn(Optional.of(img1));
                when(kioskImageRepository.findById(1)).thenReturn(Optional.of(img2));
                when(kioskImageRepository.save(any(KioskImageEntity.class))).thenAnswer(i -> i.getArgument(0));

                mockMvc.perform(put("/api/slideshow/reorder")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(List.of(2, 1))))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true));

                verify(kioskImageRepository, times(2)).findById(anyInt());
                verify(kioskImageRepository, times(2)).save(any(KioskImageEntity.class));
        }

        // =========================================
        // === UTCID07: Cloudinary or repository operation fails ===
        // =========================================

        @Test
        @DisplayName("UTCID07: Operation fails when repository throws returns 500")
        void getImages_repositoryFails_returns500() throws Exception {
                when(kioskImageRepository.findAllByOrderByDisplayOrderAscCreatedAtDesc())
                                .thenThrow(new RuntimeException("Cloudinary unavailable"));

                mockMvc.perform(get("/api/slideshow/images"))
                                .andExpect(status().isInternalServerError())
                                .andExpect(jsonPath("$.success").value(false));

                verify(kioskImageRepository, times(1)).findAllByOrderByDisplayOrderAscCreatedAtDesc();
        }
}
