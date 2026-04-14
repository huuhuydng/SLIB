package slib.com.example.controller.system;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import slib.com.example.dto.system.LibrarySettingDTO;
import slib.com.example.dto.booking.TimeSlotDTO;
import slib.com.example.dto.system.LibraryLockToggleRequest;
import slib.com.example.service.system.LibrarySettingService;

import java.util.List;

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
    public ResponseEntity<LibrarySettingDTO> updateSettings(@Valid @RequestBody LibrarySettingDTO dto) {
        return ResponseEntity.ok(librarySettingService.updateSettings(dto));
    }

    /**
     * Toggle đóng/mở thư viện (Admin only)
     * POST /slib/settings/library/toggle-lock
     * Body: {
     *   "closed": true/false,
     *   "reason": "Lý do đóng",
     *   "closedFrom": "2026-04-14T08:00:00",
     *   "closedUntil": "2026-04-16T08:00:00"
     * }
     */
    @PostMapping("/library/toggle-lock")
    public ResponseEntity<LibrarySettingDTO> toggleLock(@Valid @RequestBody LibraryLockToggleRequest body) {
        return ResponseEntity.ok(librarySettingService.toggleLibraryClosed(
                body.getClosed(),
                body.getReason(),
                body.getClosedFrom(),
                body.getClosedUntil()));
    }

    /**
     * Khôi phục cấu hình mặc định của thư viện (Admin only)
     * POST /slib/settings/library/reset
     */
    @PostMapping("/library/reset")
    public ResponseEntity<LibrarySettingDTO> resetSettings() {
        return ResponseEntity.ok(librarySettingService.resetSettings());
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
