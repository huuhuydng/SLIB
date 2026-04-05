package slib.com.example.controller.ai;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import slib.com.example.dto.ai.AIConfigRequest;
import slib.com.example.entity.ai.AIConfigEntity;
import slib.com.example.service.ai.AIConfigService;
import slib.com.example.service.system.SystemLogService;

import java.util.Map;

/**
 * Admin endpoints for AI configuration
 * Supports both Ollama (local) and Gemini (cloud) AI providers
 */
@RestController
@RequestMapping("/slib/ai/admin")
@RequiredArgsConstructor
@Slf4j
public class AIConfigController {

    private final AIConfigService aiConfigService;
    private final SystemLogService systemLogService;

    /**
     * Get current AI config (API key masked)
     */
    @GetMapping("/config")
    public ResponseEntity<?> getConfig() {
        AIConfigEntity config = aiConfigService.getConfigForDisplay();
        if (config == null) {
            // Return default config if none exists
            return ResponseEntity.ok(Map.of(
                    "configured", false,
                    "message", "AI chưa được cấu hình",
                    "defaults", aiConfigService.getDefaultConfigForDisplay()));
        }
        return ResponseEntity.ok(Map.of(
                "configured", true,
                "config", config));
    }

    /**
     * Save AI config
     */
    @PostMapping("/config")
    public ResponseEntity<?> saveConfig(
            @Valid @RequestBody AIConfigRequest config,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            log.info("[AIConfigController] Saving config for provider={}, ollamaModel={}, ollamaUrl={}",
                    config.getProvider(), config.getOllamaModel(), config.getOllamaUrl());

            aiConfigService.saveConfig(config);
            systemLogService.logAudit("AIConfigController",
                    "Cập nhật cấu hình AI cho provider: " + config.getProvider(),
                    null, userDetails != null ? userDetails.getUsername() : null);

            log.info("[AIConfigController] Config saved successfully");

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Đã lưu cấu hình AI thành công",
                    "config", aiConfigService.getConfigForDisplay()));
        } catch (IllegalArgumentException e) {
            log.warn("[AIConfigController] Invalid config payload: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()));
        } catch (Exception e) {
            log.error("[AIConfigController] ERROR saving config", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "Không thể lưu cấu hình AI lúc này"));
        }
    }

    /**
     * Reset AI config to default values
     */
    @PostMapping("/config/reset")
    public ResponseEntity<?> resetConfig(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            log.info("[AIConfigController] Resetting config to defaults");

            aiConfigService.resetToDefault();
            systemLogService.logAudit("AIConfigController",
                    "Reset cấu hình AI về mặc định",
                    null, userDetails != null ? userDetails.getUsername() : null);

            log.info("[AIConfigController] Config reset successfully");

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Đã reset cấu hình AI về mặc định",
                    "config", aiConfigService.getConfigForDisplay()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()));
        } catch (Exception e) {
            log.error("[AIConfigController] ERROR resetting config", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "Không thể reset cấu hình AI lúc này"));
        }
    }
}
