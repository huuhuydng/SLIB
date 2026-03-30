package slib.com.example.controller.system;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import slib.com.example.dto.system.LibrarySettingDTO;
import slib.com.example.dto.booking.TimeSlotDTO;
import slib.com.example.service.system.LibrarySettingService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/slib/settings")
@RequiredArgsConstructor
public class LibrarySettingController {

    private final LibrarySettingService librarySettingService;

    /**
     * Lấy cấu hình thư viện
     * GET /slib/settings/library
     */
    @GetMapping("/library")
    public ResponseEntity<LibrarySettingDTO> getSettings() {
        return ResponseEntity.ok(librarySettingService.getSettingsDTO());
    }

    /**
     * Cập nhật cấu hình thư viện (Admin only)
     * PUT /slib/settings/library
     */
    @PutMapping("/library")
    public ResponseEntity<LibrarySettingDTO> updateSettings(@RequestBody LibrarySettingDTO dto) {
        return ResponseEntity.ok(librarySettingService.updateSettings(dto));
    }

    /**
     * Toggle đóng/mở thư viện (Admin only)
     * POST /slib/settings/library/toggle-lock
     * Body: { "closed": true/false, "reason": "Lý do đóng" }
     */
    @PostMapping("/library/toggle-lock")
    public ResponseEntity<LibrarySettingDTO> toggleLock(@RequestBody Map<String, Object> body) {
        Boolean closed = (Boolean) body.get("closed");
        String reason = (String) body.get("reason");
        return ResponseEntity.ok(librarySettingService.toggleLibraryClosed(closed, reason));
    }

    /**
     * Lấy danh sách khung giờ đã được generate từ cấu hình
     * GET /slib/settings/time-slots
     */
    @GetMapping("/time-slots")
    public ResponseEntity<List<TimeSlotDTO>> getTimeSlots() {
        return ResponseEntity.ok(librarySettingService.generateTimeSlots());
    }
}
