package slib.com.example.controller.system;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import slib.com.example.service.chat.CloudinaryService;

import java.util.Map;

@RestController
@RequestMapping("/slib/files")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class FileUploadController {

    @Autowired
    private CloudinaryService cloudinaryService;

    @PostMapping("/upload_news_image")
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            String imageUrl = cloudinaryService.uploadImageNews(file);
            // Trả về Map để Spring tự convert thành JSON chuẩn {"url": "...", "type": "IMAGE"}
            return ResponseEntity.ok().body(Map.of("url", imageUrl, "type", "IMAGE"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi upload News: " + e.getMessage());
        }
    }

    @PostMapping("/upload_chat_image")
    public ResponseEntity<?> uploadChatImage(@RequestParam("file") MultipartFile file) {
        try {
            String imageUrl = cloudinaryService.uploadImageChat(file);
            return ResponseEntity.ok().body(Map.of("url", imageUrl, "type", "IMAGE"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi upload Chat: " + e.getMessage());
        }
    }

    @PostMapping("/upload_document")
    public ResponseEntity<?> uploadDocument(@RequestParam("file") MultipartFile file) {
        try {
            // Service đã được sửa để dùng resource_type: raw cho Document
            String fileUrl = cloudinaryService.uploadDocument(file);
            return ResponseEntity.ok().body(Map.of("url", fileUrl, "type", "FILE"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi upload tài liệu: " + e.getMessage());
        }
    }
}