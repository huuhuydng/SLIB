package slib.com.example.controller.system;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import slib.com.example.service.chat.CloudinaryService;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FileUploadControllerTest {

    @InjectMocks
    private FileUploadController fileUploadController;

    @Mock
    private CloudinaryService cloudinaryService;

    @Mock
    private MultipartFile multipartFile;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void uploadFile_success() throws Exception {
        when(cloudinaryService.uploadImageNews(multipartFile)).thenReturn("http://image.url/news.jpg");
        ResponseEntity<?> response = fileUploadController.uploadFile(multipartFile);
        assertEquals(200, response.getStatusCode().value()); // Sửa getStatusCodeValue() thành getStatusCode().value() cho Java mới
        assertEquals(Map.of("url", "http://image.url/news.jpg", "type", "IMAGE"), response.getBody());
    }

    @Test
    void uploadFile_failure() throws Exception {
        when(cloudinaryService.uploadImageNews(multipartFile)).thenThrow(new RuntimeException("error"));
        ResponseEntity<?> response = fileUploadController.uploadFile(multipartFile);
        assertEquals(400, response.getStatusCode().value());
        assertTrue(response.getBody().toString().contains("Lỗi upload News"));
    }

    @Test
    void uploadChatImage_success() throws Exception {
        when(cloudinaryService.uploadImageChat(multipartFile)).thenReturn("http://image.url/chat.jpg");
        ResponseEntity<?> response = fileUploadController.uploadChatImage(multipartFile);
        assertEquals(200, response.getStatusCode().value());
        assertEquals(Map.of("url", "http://image.url/chat.jpg", "type", "IMAGE"), response.getBody());
    }

    @Test
    void uploadChatImage_failure() throws Exception {
        when(cloudinaryService.uploadImageChat(multipartFile)).thenThrow(new RuntimeException("error"));
        ResponseEntity<?> response = fileUploadController.uploadChatImage(multipartFile);
        assertEquals(400, response.getStatusCode().value());
        assertTrue(response.getBody().toString().contains("Lỗi upload Chat"));
    }

    @Test
    void uploadDocument_success() throws Exception {
        when(cloudinaryService.uploadDocument(multipartFile)).thenReturn("http://file.url/doc.pdf");
        ResponseEntity<?> response = fileUploadController.uploadDocument(multipartFile);
        assertEquals(200, response.getStatusCode().value());
        assertEquals(Map.of("url", "http://file.url/doc.pdf", "type", "FILE"), response.getBody());
    }

    @Test
    void uploadDocument_failure() throws Exception {
        when(cloudinaryService.uploadDocument(multipartFile)).thenThrow(new RuntimeException("error"));
        ResponseEntity<?> response = fileUploadController.uploadDocument(multipartFile);
        assertEquals(400, response.getStatusCode().value());
        assertTrue(response.getBody().toString().contains("Lỗi upload tài liệu"));
    }
}