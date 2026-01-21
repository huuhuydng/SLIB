package slib.com.example.controller.ai;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import slib.com.example.entity.ai.AIConfigEntity;
import slib.com.example.service.ai.AIConfigService;
import slib.com.example.service.ai.GeminiService;

import java.util.Map;

/**
 * Admin endpoints for AI configuration
 */
@RestController
@RequestMapping("/slib/ai/admin")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class AIConfigController {

    @Autowired
    private AIConfigService aiConfigService;

    @Autowired
    private GeminiService geminiService;

    /**
     * Get current AI config (API key masked)
     */
    @GetMapping("/config")
    public ResponseEntity<?> getConfig() {
        AIConfigEntity config = aiConfigService.getConfigForDisplay();
        if (config == null) {
            return ResponseEntity.ok(Map.of(
                    "configured", false,
                    "message", "AI chưa được cấu hình"));
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
            System.out.println("[AIConfigController] Received API Key: " +
                    (config.getApiKey() != null
                            ? config.getApiKey().substring(0, Math.min(10, config.getApiKey().length())) + "..."
                            : "NULL"));
            System.out.println("[AIConfigController] Model: " + config.getModel());

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
     * Test Gemini API connection
     */
    @PostMapping("/test-api")
    public ResponseEntity<?> testApi() {
        try {
            boolean connected = geminiService.testConnection();
            return ResponseEntity.ok(Map.of(
                    "success", connected,
                    "status", connected ? "connected" : "error",
                    "message", connected ? "Kết nối thành công với Gemini API" : "Không thể kết nối với Gemini API"));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "status", "error",
                    "message", "Lỗi test kết nối: " + e.getMessage()));
        }
    }
}
