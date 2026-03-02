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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import slib.com.example.controller.system.FileUploadController;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.service.chat.CloudinaryService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for FileUploadController
 * Testing Framework: JUnit 5, Mockito, MockMvc
 */
@WebMvcTest(value = FileUploadController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
        slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FileUploadController Unit Tests")
class FileUploadControllerUnitTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CloudinaryService cloudinaryService;

    // =========================================
    // === UPLOAD NEWS IMAGE ENDPOINT ===
    // =========================================

    @Test
    @DisplayName("uploadNewsImage_validFile_returns200WithUrl")
    void uploadNewsImage_validFile_returns200WithUrl() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test-image.jpg", "image/jpeg",
                "fake-image-content".getBytes());

        when(cloudinaryService.uploadImageNews(any())).thenReturn("https://res.cloudinary.com/news/test.jpg");

        mockMvc.perform(multipart("/slib/files/upload_news_image").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value("https://res.cloudinary.com/news/test.jpg"))
                .andExpect(jsonPath("$.type").value("IMAGE"));

        verify(cloudinaryService, times(1)).uploadImageNews(any());
    }

    @Test
    @DisplayName("uploadNewsImage_serviceThrowsException_returns400")
    void uploadNewsImage_serviceThrowsException_returns400() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test-image.jpg", "image/jpeg",
                "fake-image-content".getBytes());

        when(cloudinaryService.uploadImageNews(any()))
                .thenThrow(new RuntimeException("Cloudinary error"));

        mockMvc.perform(multipart("/slib/files/upload_news_image").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(
                        org.hamcrest.Matchers.containsString("Cloudinary error")));
    }

    @Test
    @DisplayName("uploadNewsImage_fileTooLarge_returns400WithSizeInfo")
    void uploadNewsImage_fileTooLarge_returns400WithSizeInfo() throws Exception {
        // Tạo file 11MB (vuot qua gioi han 10MB)
        byte[] largeContent = new byte[11 * 1024 * 1024];
        MockMultipartFile file = new MockMultipartFile(
                "file", "large-image.jpg", "image/jpeg", largeContent);

        mockMvc.perform(multipart("/slib/files/upload_news_image").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(
                        org.hamcrest.Matchers.containsString("File quá lớn")))
                .andExpect(jsonPath("$.maxSize").value("10MB"));

        verify(cloudinaryService, never()).uploadImageNews(any());
    }

    // =========================================
    // === UPLOAD CHAT IMAGE ENDPOINT ===
    // =========================================

    @Test
    @DisplayName("uploadChatImage_validFile_returns200WithUrl")
    void uploadChatImage_validFile_returns200WithUrl() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "chat-image.png", "image/png",
                "fake-image-data".getBytes());

        when(cloudinaryService.uploadImageChat(any())).thenReturn("https://res.cloudinary.com/chat/test.png");

        mockMvc.perform(multipart("/slib/files/upload_chat_image").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value("https://res.cloudinary.com/chat/test.png"))
                .andExpect(jsonPath("$.type").value("IMAGE"));

        verify(cloudinaryService, times(1)).uploadImageChat(any());
    }

    @Test
    @DisplayName("uploadChatImage_serviceError_returns400")
    void uploadChatImage_serviceError_returns400() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "chat-image.png", "image/png",
                "fake-image-data".getBytes());

        when(cloudinaryService.uploadImageChat(any()))
                .thenThrow(new RuntimeException("Upload failed"));

        mockMvc.perform(multipart("/slib/files/upload_chat_image").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(
                        org.hamcrest.Matchers.containsString("Upload failed")));
    }

    @Test
    @DisplayName("uploadChatImage_fileTooLarge_returns400WithSizeInfo")
    void uploadChatImage_fileTooLarge_returns400WithSizeInfo() throws Exception {
        // Tạo file 6MB (vuot qua gioi han 5MB)
        byte[] largeContent = new byte[6 * 1024 * 1024];
        MockMultipartFile file = new MockMultipartFile(
                "file", "large-chat-image.jpg", "image/jpeg", largeContent);

        mockMvc.perform(multipart("/slib/files/upload_chat_image").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(
                        org.hamcrest.Matchers.containsString("File quá lớn")))
                .andExpect(jsonPath("$.maxSize").value("5MB"));

        verify(cloudinaryService, never()).uploadImageChat(any());
    }

    // =========================================
    // === UPLOAD DOCUMENT ENDPOINT ===
    // =========================================

    @Test
    @DisplayName("uploadDocument_validFile_returns200WithUrl")
    void uploadDocument_validFile_returns200WithUrl() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "doc.pdf", "application/pdf",
                "fake-pdf-content".getBytes());

        when(cloudinaryService.uploadDocument(any())).thenReturn("https://res.cloudinary.com/docs/doc.pdf");

        mockMvc.perform(multipart("/slib/files/upload_document").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value("https://res.cloudinary.com/docs/doc.pdf"))
                .andExpect(jsonPath("$.type").value("FILE"));

        verify(cloudinaryService, times(1)).uploadDocument(any());
    }

    @Test
    @DisplayName("uploadDocument_serviceError_returns400")
    void uploadDocument_serviceError_returns400() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "doc.pdf", "application/pdf",
                "fake-pdf-content".getBytes());

        when(cloudinaryService.uploadDocument(any()))
                .thenThrow(new RuntimeException("Document upload failed"));

        mockMvc.perform(multipart("/slib/files/upload_document").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(
                        org.hamcrest.Matchers.containsString("Document upload failed")));
    }

    @Test
    @DisplayName("uploadDocument_fileTooLarge_returns400WithSizeInfo")
    void uploadDocument_fileTooLarge_returns400WithSizeInfo() throws Exception {
        // Tạo file 21MB (vuot qua gioi han 20MB)
        byte[] largeContent = new byte[21 * 1024 * 1024];
        MockMultipartFile file = new MockMultipartFile(
                "file", "large-doc.pdf", "application/pdf", largeContent);

        mockMvc.perform(multipart("/slib/files/upload_document").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(
                        org.hamcrest.Matchers.containsString("File quá lớn")))
                .andExpect(jsonPath("$.maxSize").value("20MB"));

        verify(cloudinaryService, never()).uploadDocument(any());
    }

    // =========================================
    // === MISSING FILE PARAMETER ===
    // =========================================

    @Test
    @DisplayName("uploadNewsImage_missingFile_returns400")
    void uploadNewsImage_missingFile_returns400() throws Exception {
        mockMvc.perform(multipart("/slib/files/upload_news_image"))
                .andExpect(status().isBadRequest());

        verify(cloudinaryService, never()).uploadImageNews(any());
    }

    @Test
    @DisplayName("uploadChatImage_missingFile_returns400")
    void uploadChatImage_missingFile_returns400() throws Exception {
        mockMvc.perform(multipart("/slib/files/upload_chat_image"))
                .andExpect(status().isBadRequest());

        verify(cloudinaryService, never()).uploadImageChat(any());
    }

    @Test
    @DisplayName("uploadDocument_missingFile_returns400")
    void uploadDocument_missingFile_returns400() throws Exception {
        mockMvc.perform(multipart("/slib/files/upload_document"))
                .andExpect(status().isBadRequest());

        verify(cloudinaryService, never()).uploadDocument(any());
    }
}
