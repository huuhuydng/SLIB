package slib.com.example.entity.ai;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * AI Configuration Entity
 * Singleton pattern - only 1 config record should exist
 * Supports both Ollama (local) and Gemini (cloud) providers
 */
@Entity
@Table(name = "ai_config")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AIConfigEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    // AI Provider: "ollama" or "gemini"
    @Builder.Default
    @Column(name = "provider", nullable = false, length = 20)
    private String provider = "ollama";

    // Ollama Configuration (default provider)
    @Builder.Default
    @Column(name = "ollama_model", length = 50)
    private String ollamaModel = "llama3.2";

    @Builder.Default
    @Column(name = "ollama_url", length = 200)
    private String ollamaUrl = "http://localhost:11434";

    // Gemini Configuration (fallback)
    @Column(name = "api_key", length = 500)
    private String apiKey; // Encrypted - only for Gemini

    @Builder.Default
    @Column(name = "gemini_model", length = 50)
    private String geminiModel = "gemini-2.0-flash";

    // Common AI Settings
    @Builder.Default
    @Column(name = "temperature", nullable = false)
    private Double temperature = 0.7;

    @Builder.Default
    @Column(name = "max_tokens", nullable = false)
    private Integer maxTokens = 1024;

    @Builder.Default
    @Column(name = "system_prompt", columnDefinition = "TEXT")
    private String systemPrompt = "Bạn là SLIB AI Assistant - trợ lý thông minh của hệ thống Thư viện thông minh SLIB. Hãy trả lời ngắn gọn, thân thiện và chính xác bằng tiếng Việt.";

    @Builder.Default
    @Column(name = "enable_context", nullable = false)
    private Boolean enableContext = true;

    @Builder.Default
    @Column(name = "enable_history", nullable = false)
    private Boolean enableHistory = true;

    @Builder.Default
    @Column(name = "auto_suggest", nullable = false)
    private Boolean autoSuggest = true;

    @Builder.Default
    @Column(name = "response_language", length = 10)
    private String responseLanguage = "vi";

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Legacy getter for backward compatibility
    public String getModel() {
        return "ollama".equals(provider) ? ollamaModel : geminiModel;
    }
}
