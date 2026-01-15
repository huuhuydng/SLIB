package slib.com.example.controller.users;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import slib.com.example.dto.users.UserSettingDTO;
import slib.com.example.entity.users.UserSetting;
import slib.com.example.service.UserSettingService;

import java.util.UUID;

@RestController
@RequestMapping("/slib/settings")
public class UserSettingController {

    @Autowired
    private UserSettingService userSettingService;

    @GetMapping("/{userId}")
    public ResponseEntity<UserSetting> getUserSettings(@PathVariable UUID userId) {
        return ResponseEntity.ok(userSettingService.getSettings(userId));
    }

    @PutMapping("/{userId}")
    public ResponseEntity<UserSetting> updateUserSettings(
            @PathVariable UUID userId,
            @RequestBody UserSettingDTO settingDTO) {
        
        return ResponseEntity.ok(userSettingService.updateSettings(userId, settingDTO));
    }
}