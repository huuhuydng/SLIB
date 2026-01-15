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
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.service.CloudinaryService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for FileUploadController
 * Testing Framework: JUnit 5, Mockito, MockMvc
 * Test Type: @WebMvcTest (Unit Tests only - no full context)
 */
@WebMvcTest(value = FileUploadController.class, 
    excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, 
    classes = {slib.com.example.security.JwtAuthenticationFilter.class}))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FileUploadController Unit Tests")
class FileUploadControllerUnitTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CloudinaryService cloudinaryService;

    // =============================================
    // === UPLOAD NEWS IMAGE ENDPOINT ===
    // =============================================

    @Test
    @DisplayName("uploadFile_validImageFile_returns200WithImageUrl")
    void uploadFile_validImageFile_returns200WithImageUrl() throws Exception {
        // Arrange
        MockMultipartFile imageFile = new MockMultipartFile(
                "file",
                "test-image.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "test image content".getBytes()
        );

        String expectedImageUrl = "https://res.cloudinary.com/demo/image/upload/v123456/news/test-image.jpg";

        when(cloudinaryService.uploadImageNews(any())).thenReturn(expectedImageUrl);

        // Act & Assert
        mockMvc.perform(multipart("/slib/files/upload_news_image")
                        .file(imageFile))
                .andExpect(status().isOk())
                .andExpect(content().string(expectedImageUrl));

        verify(cloudinaryService, times(1)).uploadImageNews(any());
    }

    @Test
    @DisplayName("uploadFile_validPngImage_returns200WithImageUrl")
    void uploadFile_validPngImage_returns200WithImageUrl() throws Exception {
        // Arrange
        MockMultipartFile imageFile = new MockMultipartFile(
                "file",
                "test-image.png",
                MediaType.IMAGE_PNG_VALUE,
                "png image content".getBytes()
        );

        String expectedImageUrl = "https://res.cloudinary.com/demo/image/upload/v123456/news/test-image.png";

        when(cloudinaryService.uploadImageNews(any())).thenReturn(expectedImageUrl);

        // Act & Assert
        mockMvc.perform(multipart("/slib/files/upload_news_image")
                        .file(imageFile))
                .andExpect(status().isOk())
                .andExpect(content().string(expectedImageUrl));

        verify(cloudinaryService, times(1)).uploadImageNews(any());
    }

    @Test
    @DisplayName("uploadFile_largeImageFile_returns200WithImageUrl")
    void uploadFile_largeImageFile_returns200WithImageUrl() throws Exception {
        // Arrange
        byte[] largeContent = new byte[5 * 1024 * 1024]; // 5MB
        MockMultipartFile largeImageFile = new MockMultipartFile(
                "file",
                "large-image.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                largeContent
        );

        String expectedImageUrl = "https://res.cloudinary.com/demo/image/upload/v123456/news/large-image.jpg";

        when(cloudinaryService.uploadImageNews(any())).thenReturn(expectedImageUrl);

        // Act & Assert
        mockMvc.perform(multipart("/slib/files/upload_news_image")
                        .file(largeImageFile))
                .andExpect(status().isOk())
                .andExpect(content().string(expectedImageUrl));

        verify(cloudinaryService, times(1)).uploadImageNews(any());
    }

    @Test
    @DisplayName("uploadFile_missingFileParameter_returns400")
    void uploadFile_missingFileParameter_returns400() throws Exception {
        // Act & Assert
        mockMvc.perform(multipart("/slib/files/upload_news_image"))
                .andExpect(status().isBadRequest());

        verify(cloudinaryService, never()).uploadImageNews(any());
    }

    @Test
    @DisplayName("uploadFile_emptyFile_returns400OrError")
    void uploadFile_emptyFile_returns400OrError() throws Exception {
        // Arrange
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "empty.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                new byte[0]
        );

        when(cloudinaryService.uploadImageNews(any()))
                .thenThrow(new RuntimeException("File is empty"));

        // Act & Assert
        mockMvc.perform(multipart("/slib/files/upload_news_image")
                        .file(emptyFile))
                .andExpect(status().isInternalServerError());

        verify(cloudinaryService, times(1)).uploadImageNews(any());
    }

    @Test
    @DisplayName("uploadFile_invalidFileType_throwsRuntimeException")
    void uploadFile_invalidFileType_throwsRuntimeException() throws Exception {
        // Arrange
        MockMultipartFile textFile = new MockMultipartFile(
                "file",
                "document.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "This is not an image".getBytes()
        );

        when(cloudinaryService.uploadImageNews(any()))
                .thenThrow(new RuntimeException("Invalid file type"));

        // Act & Assert
        mockMvc.perform(multipart("/slib/files/upload_news_image")
                        .file(textFile))
                .andExpect(status().isInternalServerError());

        verify(cloudinaryService, times(1)).uploadImageNews(any());
    }

    @Test
    @DisplayName("uploadFile_cloudinaryServiceError_returns500")
    void uploadFile_cloudinaryServiceError_returns500() throws Exception {
        // Arrange
        MockMultipartFile imageFile = new MockMultipartFile(
                "file",
                "test-image.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "test image content".getBytes()
        );

        when(cloudinaryService.uploadImageNews(any()))
                .thenThrow(new RuntimeException("Cloudinary API error"));

        // Act & Assert
        mockMvc.perform(multipart("/slib/files/upload_news_image")
                        .file(imageFile))
                .andExpect(status().isInternalServerError());

        verify(cloudinaryService, times(1)).uploadImageNews(any());
    }

    @Test
    @DisplayName("uploadFile_networkError_returns500")
    void uploadFile_networkError_returns500() throws Exception {
        // Arrange
        MockMultipartFile imageFile = new MockMultipartFile(
                "file",
                "test-image.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "test image content".getBytes()
        );

        when(cloudinaryService.uploadImageNews(any()))
                .thenThrow(new RuntimeException("Network timeout"));

        // Act & Assert
        mockMvc.perform(multipart("/slib/files/upload_news_image")
                        .file(imageFile))
                .andExpect(status().isInternalServerError());

        verify(cloudinaryService, times(1)).uploadImageNews(any());
    }

    @Test
    @DisplayName("uploadFile_specialCharactersInFilename_returns200WithImageUrl")
    void uploadFile_specialCharactersInFilename_returns200WithImageUrl() throws Exception {
        // Arrange
        MockMultipartFile imageFile = new MockMultipartFile(
                "file",
                "ảnh-tin-tức-2024.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "image with special chars".getBytes()
        );

        String expectedImageUrl = "https://res.cloudinary.com/demo/image/upload/v123456/news/anh-tin-tuc-2024.jpg";

        when(cloudinaryService.uploadImageNews(any())).thenReturn(expectedImageUrl);

        // Act & Assert
        mockMvc.perform(multipart("/slib/files/upload_news_image")
                        .file(imageFile))
                .andExpect(status().isOk())
                .andExpect(content().string(expectedImageUrl));

        verify(cloudinaryService, times(1)).uploadImageNews(any());
    }

    @Test
    @DisplayName("uploadFile_gifImage_returns200WithImageUrl")
    void uploadFile_gifImage_returns200WithImageUrl() throws Exception {
        // Arrange
        MockMultipartFile gifFile = new MockMultipartFile(
                "file",
                "animated.gif",
                MediaType.IMAGE_GIF_VALUE,
                "gif image content".getBytes()
        );

        String expectedImageUrl = "https://res.cloudinary.com/demo/image/upload/v123456/news/animated.gif";

        when(cloudinaryService.uploadImageNews(any())).thenReturn(expectedImageUrl);

        // Act & Assert
        mockMvc.perform(multipart("/slib/files/upload_news_image")
                        .file(gifFile))
                .andExpect(status().isOk())
                .andExpect(content().string(expectedImageUrl));

        verify(cloudinaryService, times(1)).uploadImageNews(any());
    }

    @Test
    @DisplayName("uploadFile_webpImage_returns200WithImageUrl")
    void uploadFile_webpImage_returns200WithImageUrl() throws Exception {
        // Arrange
        MockMultipartFile webpFile = new MockMultipartFile(
                "file",
                "modern-image.webp",
                "image/webp",
                "webp image content".getBytes()
        );

        String expectedImageUrl = "https://res.cloudinary.com/demo/image/upload/v123456/news/modern-image.webp";

        when(cloudinaryService.uploadImageNews(any())).thenReturn(expectedImageUrl);

        // Act & Assert
        mockMvc.perform(multipart("/slib/files/upload_news_image")
                        .file(webpFile))
                .andExpect(status().isOk())
                .andExpect(content().string(expectedImageUrl));

        verify(cloudinaryService, times(1)).uploadImageNews(any());
    }

    @Test
    @DisplayName("uploadFile_wrongParameterName_returns400")
    void uploadFile_wrongParameterName_returns400() throws Exception {
        // Arrange - using wrong parameter name "image" instead of "file"
        MockMultipartFile imageFile = new MockMultipartFile(
                "image",
                "test-image.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "test image content".getBytes()
        );

        // Act & Assert
        mockMvc.perform(multipart("/slib/files/upload_news_image")
                        .file(imageFile))
                .andExpect(status().isBadRequest());

        verify(cloudinaryService, never()).uploadImageNews(any());
    }

    @Test
    @DisplayName("uploadFile_uploadSuccessReturnsHttpsUrl_returns200")
    void uploadFile_uploadSuccessReturnsHttpsUrl_returns200() throws Exception {
        // Arrange
        MockMultipartFile imageFile = new MockMultipartFile(
                "file",
                "secure-upload.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "secure image".getBytes()
        );

        String secureUrl = "https://res.cloudinary.com/slib/image/upload/secure/v123456789/news/secure-upload.jpg";

        when(cloudinaryService.uploadImageNews(any())).thenReturn(secureUrl);

        // Act & Assert
        mockMvc.perform(multipart("/slib/files/upload_news_image")
                        .file(imageFile))
                .andExpect(status().isOk())
                .andExpect(content().string(secureUrl))
                .andExpect(content().string(org.hamcrest.Matchers.startsWith("https://")));

        verify(cloudinaryService, times(1)).uploadImageNews(any());
    }
}
