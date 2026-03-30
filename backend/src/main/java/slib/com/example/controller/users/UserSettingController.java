package slib.com.example.controller.users;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import slib.com.example.dto.users.UserSettingDTO;
import slib.com.example.entity.users.Role;
import slib.com.example.entity.users.User;
import slib.com.example.entity.users.UserSetting;
import slib.com.example.repository.users.UserRepository;
import slib.com.example.service.users.UserSettingService;

import java.util.UUID;

@RestController
@RequestMapping("/slib/settings")
public class UserSettingController {

    @Autowired
    private UserSettingService userSettingService;

    @Autowired
    private UserRepository userRepository;

    private UUID resolveAuthorizedUserId(UUID requestedUserId, UserDetails userDetails) {
        if (userDetails == null) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.");
        }

        User currentUser = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (currentUser.getRole() == Role.ADMIN || currentUser.getRole() == Role.LIBRARIAN) {
            return requestedUserId;
        }
        if (!currentUser.getId().equals(requestedUserId)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Bạn không có quyền truy cập thiết lập của người khác.");
        }
        return currentUser.getId();
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserSetting> getUserSettings(@PathVariable UUID userId,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID resolvedUserId = resolveAuthorizedUserId(userId, userDetails);
        return ResponseEntity.ok(userSettingService.getSettings(resolvedUserId));
    }

    @PutMapping("/{userId}")
    public ResponseEntity<UserSetting> updateUserSettings(
            @PathVariable UUID userId,
            @RequestBody UserSettingDTO settingDTO,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID resolvedUserId = resolveAuthorizedUserId(userId, userDetails);
        return ResponseEntity.ok(userSettingService.updateSettings(resolvedUserId, settingDTO));
    }
}
