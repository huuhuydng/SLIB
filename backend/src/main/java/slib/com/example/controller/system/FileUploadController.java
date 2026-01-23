package slib.com.example.controller.system;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import slib.com.example.service.CloudinaryService;

@RestController
@RequestMapping("/slib/files")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class FileUploadController {

    @Autowired
    private CloudinaryService cloudinaryService;

    @PostMapping("/upload_news_image")
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file) {
        String imageUrl = cloudinaryService.uploadImageNews(file);
        return ResponseEntity.ok(imageUrl);
    }
}