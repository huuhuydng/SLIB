package slib.com.example.controller.kiosk;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import slib.com.example.entity.kiosk.KioskImageEntity;
import slib.com.example.repository.kiosk.KioskImageRepository;
import slib.com.example.service.kiosk.KioskCloudinaryService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/slideshow")
@Slf4j
public class KioskSlideshowController {

    @Autowired
    private KioskCloudinaryService cloudinaryService;

    @Autowired
    private KioskImageRepository kioskImageRepository;

    // 1. Lấy danh sách ảnh
    @GetMapping("/images")
    public ResponseEntity<?> getImages() {
        try {
            List<KioskImageEntity> images = kioskImageRepository.findAllByOrderByDisplayOrderAscCreatedAtDesc();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("images", images);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Lỗi lấy danh sách ảnh", e);
            return ResponseEntity.status(500).body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // 1.1 Lấy cấu hình slideshow (Duration)
    @GetMapping("/config")
    public ResponseEntity<?> getConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("duration", 5000); // Mặc định 5s
        return ResponseEntity.ok(Map.of("success", true, "config", config));
    }

    // 2. Upload ảnh (Hỗ trợ nhiều file)
    @PostMapping("/images")
    public ResponseEntity<?> uploadImages(@RequestParam("images") MultipartFile[] files) {
        try {
            List<KioskImageEntity> savedImages = new ArrayList<>();
            for (MultipartFile file : files) {
                Map<String, Object> result = cloudinaryService.uploadSlideShowImage(file);
                
                KioskImageEntity image = KioskImageEntity.builder()
                        .imageUrl((String) result.get("url"))
                        .publicId((String) result.get("public_id"))
                        .imageName(file.getOriginalFilename())
                        .isActive(false) // Mặc định là dự phòng
                        .displayOrder(9999) // Mặc định xuống cuối
                        .durationSeconds(10) // Mặc định 10s theo entity
                        .build();
                
                savedImages.add(kioskImageRepository.save(image));
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("images", savedImages);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Lỗi upload ảnh", e);
            return ResponseEntity.status(500).body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // 3. Xóa ảnh
    @DeleteMapping("/images/{id}")
    public ResponseEntity<?> deleteImage(@PathVariable Integer id) {
        try {
            KioskImageEntity image = kioskImageRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Ảnh không tồn tại"));

            // Ưu tiên dùng publicId nếu có, nếu không thì fallback về URL cũ
            if (image.getPublicId() != null) {
                cloudinaryService.deleteSlideShowImage(image.getPublicId());
            } else {
                cloudinaryService.deleteSlideShowImage(image.getImageUrl());
            }
            
            // Xóa trong DB
            kioskImageRepository.delete(image);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            log.error("Lỗi xóa ảnh", e);
            return ResponseEntity.status(500).body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // 4. Đổi tên ảnh
    @PutMapping("/images/{id}")
    public ResponseEntity<?> renameImage(@PathVariable Integer id, @RequestBody Map<String, String> payload) {
        try {
            String newName = payload.get("newName");
            
            KioskImageEntity image = kioskImageRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Ảnh không tồn tại"));

            if (newName != null && !newName.trim().isEmpty()) {
                image.setImageName(newName);
            }

            KioskImageEntity updated = kioskImageRepository.save(image);
            
            return ResponseEntity.ok(Map.of("success", true, "image", updated));
        } catch (Exception e) {
            log.error("Lỗi đổi tên ảnh", e);
            return ResponseEntity.status(500).body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // 5. Toggle trạng thái (Active <-> Backup)
    @PatchMapping("/images/{id}/status")
    public ResponseEntity<?> toggleStatus(@PathVariable Integer id, @RequestBody Map<String, Boolean> payload) {
        try {
            Boolean isActive = payload.get("isActive");
            
            KioskImageEntity image = kioskImageRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Ảnh không tồn tại"));

            if (image.getIsActive() != null && image.getIsActive().equals(isActive)) {
                return ResponseEntity.ok(Map.of("success", true, "image", image));
            }
            
            image.setIsActive(isActive);
            KioskImageEntity updated = kioskImageRepository.save(image);

            return ResponseEntity.ok(Map.of("success", true, "image", updated));

        } catch (Exception e) {
            log.error("Lỗi cập nhật trạng thái", e);
            return ResponseEntity.status(500).body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // 6. Sắp xếp lại thứ tự ảnh
    @PutMapping("/reorder")
    public ResponseEntity<?> reorderImages(@RequestBody List<Integer> orderedIds) {
        try {
            for (int i = 0; i < orderedIds.size(); i++) {
                Integer id = orderedIds.get(i);
                KioskImageEntity image = kioskImageRepository.findById(id).orElse(null);
                if (image != null) {
                    image.setDisplayOrder(i);
                    kioskImageRepository.save(image);
                }
            }
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            log.error("Lỗi sắp xếp ảnh", e);
            return ResponseEntity.status(500).body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}