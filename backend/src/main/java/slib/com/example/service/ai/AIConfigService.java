package slib.com.example.service.ai;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import slib.com.example.entity.ai.AIConfigEntity;
import slib.com.example.repository.ai.AIConfigRepository;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

@Service
public class AIConfigService {

    @Autowired
    private AIConfigRepository aiConfigRepository;

    // Simple encryption key - in production, use environment variable
    private static final String ENCRYPTION_KEY = "SLIB2025SecretKy"; // 16 chars for AES-128

    public Optional<AIConfigEntity> getConfig() {
        return aiConfigRepository.getConfig();
    }

    @Transactional
    public AIConfigEntity saveConfig(AIConfigEntity config) {
        // Encrypt API key before saving
        if (config.getApiKey() != null && !config.getApiKey().startsWith("ENC:")) {
            config.setApiKey("ENC:" + encrypt(config.getApiKey()));
        }

        // If config exists, update it; otherwise create new
        Optional<AIConfigEntity> existing = aiConfigRepository.getConfig();
        if (existing.isPresent()) {
            AIConfigEntity existingConfig = existing.get();
            existingConfig.setApiKey(config.getApiKey());
            existingConfig.setModel(config.getModel());
            existingConfig.setTemperature(config.getTemperature());
            existingConfig.setMaxTokens(config.getMaxTokens());
            existingConfig.setSystemPrompt(config.getSystemPrompt());
            existingConfig.setEnableContext(config.getEnableContext());
            existingConfig.setEnableHistory(config.getEnableHistory());
            existingConfig.setResponseLanguage(config.getResponseLanguage());
            return aiConfigRepository.save(existingConfig);
        }

        return aiConfigRepository.save(config);
    }

    public String getDecryptedApiKey() {
        return getConfig()
                .map(config -> {
                    String apiKey = config.getApiKey();
                    if (apiKey != null && apiKey.startsWith("ENC:")) {
                        return decrypt(apiKey.substring(4));
                    }
                    return apiKey;
                })
                .orElse(null);
    }

    // Simple AES encryption
    private String encrypt(String value) {
        try {
            SecretKeySpec key = new SecretKeySpec(ENCRYPTION_KEY.getBytes(StandardCharsets.UTF_8), "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("Error encrypting API key", e);
        }
    }

    private String decrypt(String encrypted) {
        try {
            SecretKeySpec key = new SecretKeySpec(ENCRYPTION_KEY.getBytes(StandardCharsets.UTF_8), "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(encrypted));
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Error decrypting API key", e);
        }
    }

    // Get config for display (mask API key)
    public AIConfigEntity getConfigForDisplay() {
        return getConfig()
                .map(config -> {
                    AIConfigEntity display = AIConfigEntity.builder()
                            .id(config.getId())
                            .apiKey(maskApiKey(config.getApiKey()))
                            .model(config.getModel())
                            .temperature(config.getTemperature())
                            .maxTokens(config.getMaxTokens())
                            .systemPrompt(config.getSystemPrompt())
                            .enableContext(config.getEnableContext())
                            .enableHistory(config.getEnableHistory())
                            .responseLanguage(config.getResponseLanguage())
                            .createdAt(config.getCreatedAt())
                            .updatedAt(config.getUpdatedAt())
                            .build();
                    return display;
                })
                .orElse(null);
    }

    private String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.length() < 10)
            return "***";
        // Show first 10 chars, mask rest
        return apiKey.substring(0, 10) + "•".repeat(20);
    }
}
