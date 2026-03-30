package slib.com.example.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class SensitivePropertyValidator {

    @Value("${jwt.secret:}")
    private String jwtSecret;

    @Value("${gate.secret:}")
    private String gateSecret;

    @Value("${cloudinary.cloud-name:}")
    private String cloudinaryCloudName;

    @Value("${cloudinary.api-key:}")
    private String cloudinaryApiKey;

    @Value("${cloudinary.api-secret:}")
    private String cloudinaryApiSecret;

    @Value("${slib.internal.api-key:}")
    private String internalApiKey;

    @PostConstruct
    void validateSensitiveProperties() {
        Map<String, String> requiredProps = new LinkedHashMap<>();
        requiredProps.put("jwt.secret", jwtSecret);
        requiredProps.put("gate.secret", gateSecret);
        requiredProps.put("cloudinary.cloud-name", cloudinaryCloudName);
        requiredProps.put("cloudinary.api-key", cloudinaryApiKey);
        requiredProps.put("cloudinary.api-secret", cloudinaryApiSecret);
        requiredProps.put("slib.internal.api-key", internalApiKey);

        String missingProps = requiredProps.entrySet().stream()
                .filter(entry -> entry.getValue() == null || entry.getValue().isBlank())
                .map(Map.Entry::getKey)
                .collect(Collectors.joining(", "));

        if (!missingProps.isBlank()) {
            throw new IllegalStateException(
                    "Thiếu cấu hình bảo mật bắt buộc: " + missingProps
                            + ". Hãy cung cấp qua biến môi trường trước khi khởi động hệ thống.");
        }
    }
}
