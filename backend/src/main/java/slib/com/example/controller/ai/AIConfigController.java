package slib.com.example.controller.ai;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import slib.com.example.entity.ai.AIConfigEntity;
import slib.com.example.service.ai.AIConfigService;

import java.util.Map;

/**
 * Admin endpoints for AI configuration
 * Supports both Ollama (local) and Gemini (cloud) AI providers
 */
@RestController
@RequestMapping("/slib/ai/admin")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class AIConfigController {

    @Autowired
    private AIConfigService aiConfigService;

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
                    "defaults", Map.of(
                            "provider", "ollama",
                            "ollamaModel", "llama3.2",
                            "ollamaUrl", "http://localhost:11434",
                            "geminiModel", "gemini-2.0-flash",
                            "temperature", 0.7,
                            "maxTokens", 1024,
                            "enableContext", true,
                            "enableHistory", true,
                            "autoSuggest", true,
                            "responseLanguage", "vi")));
        }
        return ResponseEntity.ok(Map.of(
                "configured", true,
                "config", config));
    }

    /**
     * Save AI config
     */
    @PostMapping("/config")
    public ResponseEntity<?> saveConfig(@RequestBody AIConfigEntity config) {
        try {
            System.out.println("[AIConfigController] Saving config...");
            System.out.println("[AIConfigController] Provider: " + config.getProvider());
            System.out.println("[AIConfigController] Ollama Model: " + config.getOllamaModel());
            System.out.println("[AIConfigController] Ollama URL: " + config.getOllamaUrl());

            aiConfigService.saveConfig(config);

            System.out.println("[AIConfigController] Config saved successfully!");

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Đã lưu cấu hình AI thành công",
                    "config", aiConfigService.getConfigForDisplay()));
        } catch (Exception e) {
            System.err.println("[AIConfigController] ERROR saving config: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi lưu cấu hình: " + e.getMessage()));
        }
    }

    /**
     * Reset AI config to default values
     */
    @PostMapping("/config/reset")
    public ResponseEntity<?> resetConfig() {
        try {
            System.out.println("[AIConfigController] Resetting config to defaults...");

            aiConfigService.resetToDefault();

            System.out.println("[AIConfigController] Config reset successfully!");

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Đã reset cấu hình AI về mặc định",
                    "config", aiConfigService.getConfigForDisplay()));
        } catch (Exception e) {
            System.err.println("[AIConfigController] ERROR resetting config: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi reset cấu hình: " + e.getMessage()));
        }
    }
}
