package slib.com.example.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import slib.com.example.dto.LibrarySettingDTO;
import slib.com.example.dto.TimeSlotDTO;
import slib.com.example.service.LibrarySettingService;

import java.util.List;

@RestController
@RequestMapping("/slib/settings")
@CrossOrigin(origins = "*")
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
     * Lấy danh sách khung giờ đã được generate từ cấu hình
     * GET /slib/settings/time-slots
     */
    @GetMapping("/time-slots")
    public ResponseEntity<List<TimeSlotDTO>> getTimeSlots() {
        return ResponseEntity.ok(librarySettingService.generateTimeSlots());
    }
}
