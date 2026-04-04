package slib.com.example.service.ai;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import slib.com.example.dto.ai.AIConfigRequest;
import slib.com.example.entity.ai.AIConfigEntity;
import slib.com.example.repository.ai.AIConfigRepository;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;

@Service
public class AIConfigService {

    private static final String LEGACY_ENCRYPTION_KEY = "SLIB2025SecretKy";
    private static final String LEGACY_PREFIX = "ENC:";
    private static final String MODERN_PREFIX = "ENCv2:";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Autowired
    private AIConfigRepository aiConfigRepository;

    @Value("${slib.ai.config-secret:}")
    private String configSecret;

    @Value("${app.ai.default-ollama-url:http://localhost:11434}")
    private String defaultOllamaUrl;

    public Optional<AIConfigEntity> getConfig() {
        return aiConfigRepository.getConfig();
    }

    @Transactional
    public AIConfigEntity saveConfig(AIConfigRequest request) {
        validateConfigRequest(request);

        Optional<AIConfigEntity> existing = aiConfigRepository.getConfig();
        String preservedApiKey = existing.map(AIConfigEntity::getApiKey).orElse(null);
        String nextApiKey = resolveNextApiKey(request.getApiKey(), preservedApiKey);

        if (existing.isPresent()) {
            AIConfigEntity existingConfig = existing.get();
            existingConfig.setProvider(normalizeProvider(request.getProvider()));
            existingConfig.setOllamaModel(defaultIfBlank(request.getOllamaModel(), "llama3.2"));
            existingConfig.setOllamaUrl(defaultIfBlank(request.getOllamaUrl(), defaultOllamaUrl));
            existingConfig.setGeminiModel(defaultIfBlank(request.getGeminiModel(), "gemini-2.0-flash"));
            existingConfig.setApiKey(nextApiKey);
            existingConfig.setTemperature(request.getTemperature());
            existingConfig.setMaxTokens(request.getMaxTokens());
            existingConfig.setSystemPrompt(defaultIfBlank(request.getSystemPrompt(), getDefaultSystemPrompt()));
            existingConfig.setEnableContext(request.getEnableContext());
            existingConfig.setEnableHistory(request.getEnableHistory());
            existingConfig.setAutoSuggest(request.getAutoSuggest());
            existingConfig.setResponseLanguage(defaultIfBlank(request.getResponseLanguage(), "vi"));
            return aiConfigRepository.save(existingConfig);
        }

        AIConfigEntity config = AIConfigEntity.builder()
                .provider(normalizeProvider(request.getProvider()))
                .ollamaModel(defaultIfBlank(request.getOllamaModel(), "llama3.2"))
                .ollamaUrl(defaultIfBlank(request.getOllamaUrl(), defaultOllamaUrl))
                .geminiModel(defaultIfBlank(request.getGeminiModel(), "gemini-2.0-flash"))
                .apiKey(nextApiKey)
                .temperature(request.getTemperature())
                .maxTokens(request.getMaxTokens())
                .systemPrompt(defaultIfBlank(request.getSystemPrompt(), getDefaultSystemPrompt()))
                .enableContext(request.getEnableContext())
                .enableHistory(request.getEnableHistory())
                .autoSuggest(request.getAutoSuggest())
                .responseLanguage(defaultIfBlank(request.getResponseLanguage(), "vi"))
                .build();

        return aiConfigRepository.save(config);
    }

    @Transactional
    public AIConfigEntity resetToDefault() {
        AIConfigEntity defaultConfig = buildDefaultConfig();
        Optional<AIConfigEntity> existing = aiConfigRepository.getConfig();
        if (existing.isPresent()) {
            AIConfigEntity existingConfig = existing.get();
            defaultConfig.setId(existingConfig.getId());
            defaultConfig.setApiKey(existingConfig.getApiKey());
        }

        return aiConfigRepository.save(defaultConfig);
    }

    public String getDecryptedApiKey() {
        return getConfig()
                .map(AIConfigEntity::getApiKey)
                .map(this::decryptStoredApiKey)
                .orElse(null);
    }

    public AIConfigEntity getDefaultConfigForDisplay() {
        AIConfigEntity defaultConfig = buildDefaultConfig();
        defaultConfig.setApiKey(maskApiKey(null));
        return defaultConfig;
    }

    public AIConfigEntity getConfigForDisplay() {
        return getConfig()
                .map(config -> AIConfigEntity.builder()
                        .id(config.getId())
                        .provider(config.getProvider())
                        .ollamaModel(config.getOllamaModel())
                        .ollamaUrl(config.getOllamaUrl())
                        .geminiModel(config.getGeminiModel())
                        .apiKey(maskApiKey(decryptStoredApiKey(config.getApiKey())))
                        .temperature(config.getTemperature())
                        .maxTokens(config.getMaxTokens())
                        .systemPrompt(config.getSystemPrompt())
                        .enableContext(config.getEnableContext())
                        .enableHistory(config.getEnableHistory())
                        .autoSuggest(config.getAutoSuggest())
                        .responseLanguage(config.getResponseLanguage())
                        .createdAt(config.getCreatedAt())
                        .updatedAt(config.getUpdatedAt())
                        .build())
                .orElse(null);
    }

    private AIConfigEntity buildDefaultConfig() {
        return AIConfigEntity.builder()
                .provider("ollama")
                .ollamaModel("llama3.2")
                .ollamaUrl(defaultOllamaUrl)
                .geminiModel("gemini-2.0-flash")
                .temperature(0.7)
                .maxTokens(1024)
                .systemPrompt(getDefaultSystemPrompt())
                .enableContext(true)
                .enableHistory(true)
                .autoSuggest(true)
                .responseLanguage("vi")
                .build();
    }

    private String getDefaultSystemPrompt() {
        return "Bạn là SLIB AI Assistant - trợ lý thông minh của hệ thống Thư viện thông minh SLIB. Hãy trả lời ngắn gọn, thân thiện và chính xác bằng tiếng Việt.";
    }

    private void validateConfigRequest(AIConfigRequest request) {
        String provider = normalizeProvider(request.getProvider());
        if ("ollama".equals(provider)) {
            if (trimToNull(request.getOllamaModel()) == null) {
                throw new IllegalArgumentException("Vui lòng nhập tên mô hình Ollama");
            }
            String ollamaUrl = trimToNull(request.getOllamaUrl());
            if (ollamaUrl == null || !(ollamaUrl.startsWith("http://") || ollamaUrl.startsWith("https://"))) {
                throw new IllegalArgumentException("Địa chỉ Ollama phải bắt đầu bằng http:// hoặc https://");
            }
        }

        if ("gemini".equals(provider)) {
            if (trimToNull(request.getGeminiModel()) == null) {
                throw new IllegalArgumentException("Vui lòng nhập tên mô hình Gemini");
            }
            String candidateKey = trimToNull(request.getApiKey());
            boolean missingNewKey = candidateKey == null || candidateKey.contains("•");
            boolean missingStoredKey = getConfig()
                    .map(AIConfigEntity::getApiKey)
                    .map(this::trimToNull)
                    .isEmpty();
            if (missingNewKey && missingStoredKey) {
                throw new IllegalArgumentException("Vui lòng nhập API key Gemini trước khi lưu cấu hình");
            }
        }
    }

    private String resolveNextApiKey(@Nullable String apiKeyFromRequest, @Nullable String existingApiKey) {
        String candidate = trimToNull(apiKeyFromRequest);
        if (candidate == null || candidate.contains("•")) {
            return existingApiKey;
        }
        if (candidate.startsWith(MODERN_PREFIX) || candidate.startsWith(LEGACY_PREFIX)) {
            return candidate;
        }
        return MODERN_PREFIX + encrypt(candidate);
    }

    private String encrypt(String value) {
        try {
            SecretKeySpec key = buildSecretKey();
            byte[] iv = new byte[GCM_IV_LENGTH];
            SECURE_RANDOM.nextBytes(iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));

            byte[] payload = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, payload, 0, iv.length);
            System.arraycopy(encrypted, 0, payload, iv.length, encrypted.length);

            return Base64.getEncoder().encodeToString(payload);
        } catch (Exception e) {
            throw new RuntimeException("Lỗi mã hóa API key", e);
        }
    }

    private String decryptStoredApiKey(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            return null;
        }
        if (apiKey.startsWith(MODERN_PREFIX)) {
            return decrypt(apiKey.substring(MODERN_PREFIX.length()));
        }
        if (apiKey.startsWith(LEGACY_PREFIX)) {
            return decryptLegacy(apiKey.substring(LEGACY_PREFIX.length()));
        }
        return apiKey;
    }

    private String decrypt(String encrypted) {
        try {
            SecretKeySpec key = buildSecretKey();
            byte[] payload = Base64.getDecoder().decode(encrypted);
            if (payload.length <= GCM_IV_LENGTH) {
                throw new IllegalArgumentException("Dữ liệu mã hóa không hợp lệ");
            }

            byte[] iv = Arrays.copyOfRange(payload, 0, GCM_IV_LENGTH);
            byte[] cipherBytes = Arrays.copyOfRange(payload, GCM_IV_LENGTH, payload.length);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] decrypted = cipher.doFinal(cipherBytes);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Lỗi giải mã API key", e);
        }
    }

    private String decryptLegacy(String encrypted) {
        try {
            SecretKeySpec key = new SecretKeySpec(LEGACY_ENCRYPTION_KEY.getBytes(StandardCharsets.UTF_8), "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(encrypted));
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Không thể giải mã API key cũ", e);
        }
    }

    private SecretKeySpec buildSecretKey() {
        String normalizedSecret = trimToNull(configSecret);
        if (normalizedSecret == null) {
            throw new IllegalStateException("Thiếu cấu hình slib.ai.config-secret để mã hóa API key AI");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = digest.digest(normalizedSecret.getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(keyBytes, "AES");
        } catch (Exception e) {
            throw new RuntimeException("Không thể khởi tạo khóa mã hóa cấu hình AI", e);
        }
    }

    private String normalizeProvider(String provider) {
        String normalized = defaultIfBlank(provider, "ollama").toLowerCase();
        if (!"ollama".equals(normalized) && !"gemini".equals(normalized)) {
            throw new IllegalArgumentException("Nhà cung cấp AI không hợp lệ");
        }
        return normalized;
    }

    private String defaultIfBlank(String value, String fallback) {
        String trimmed = trimToNull(value);
        return trimmed != null ? trimmed : fallback;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String maskApiKey(String rawApiKey) {
        if (rawApiKey == null || rawApiKey.isBlank()) {
            return "";
        }
        if (rawApiKey.length() <= 8) {
            return "•".repeat(rawApiKey.length());
        }
        return rawApiKey.substring(0, 6) + "•".repeat(Math.max(6, rawApiKey.length() - 6));
    }
}
