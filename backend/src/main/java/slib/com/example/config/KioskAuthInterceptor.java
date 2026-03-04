package slib.com.example.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;
import java.util.List;

/**
 * Kiosk Authentication Interceptor
 * Kiểm tra kiosk device authentication trước khi cho phép truy cập API /slib/kiosk/**
 *
 * Cơ chế:
 * - localhost/127.0.0.1: luôn cho phép (dev mode)
 * - IP whitelist: cho phép nếu IP trong whitelist
 * - Kiosk key: yêu cầu X-Kiosk-Code header với giá trị hợp lệ
 */
@Component
@Slf4j
public class KioskAuthInterceptor implements HandlerInterceptor {

    @Value("${slib.kiosk.whitelist-ips:127.0.0.1,localhost,0:0:0:0:0:0:0:1}")
    private String whitelistIps;

    @Value("${slib.kiosk.secret-key:}")
    private String kioskSecretKey;

    @Value("${slib.kiosk.enabled:false}")
    private boolean kioskAuthEnabled;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // Nếu chưa bật kiosk auth, cho phép tất cả
        if (!kioskAuthEnabled) {
            return true;
        }

        String requestUri = request.getRequestURI();

        // Chỉ kiểm tra cho /slib/kiosk/**
        if (!requestUri.startsWith("/slib/kiosk")) {
            return true;
        }

        String clientIp = getClientIp(request);
        String kioskCode = request.getHeader("X-Kiosk-Code");

        log.debug("Kiosk auth check - IP: {}, KioskCode: {}, URI: {}", clientIp, kioskCode, requestUri);

        // 1. Kiểm tra IP whitelist
        List<String> allowedIps = Arrays.asList(whitelistIps.split(","));
        if (allowedIps.stream().anyMatch(ip -> ip.equalsIgnoreCase(clientIp) || "*".equals(ip.trim()))) {
            log.debug("Kiosk access allowed - IP whitelisted: {}", clientIp);
            return true;
        }

        // 2. Kiểm tra kiosk code (nếu không có trong whitelist)
        if (kioskCode == null || kioskCode.isBlank()) {
            log.warn("Kiosk access denied - No kiosk code provided. IP: {}", clientIp);
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write("{\"error\": \"Kiosk code required\"}");
            return false;
        }

        // Validate kiosk code (có thể kiểm tra với DB hoặc secret key)
        if (!isValidKioskCode(kioskCode)) {
            log.warn("Kiosk access denied - Invalid kiosk code: {}. IP: {}", kioskCode, clientIp);
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write("{\"error\": \"Invalid kiosk code\"}");
            return false;
        }

        log.debug("Kiosk access allowed - Valid kiosk code: {}", kioskCode);
        return true;
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }

    private boolean isValidKioskCode(String kioskCode) {
        // Option 1: Kiểm tra với secret key (đơn giản)
        if (kioskSecretKey != null && !kioskSecretKey.isEmpty()) {
            return kioskCode.equals(kioskSecretKey);
        }

        // Option 2: Nếu không có secret key, chấp nhận bất kỳ code nào (dev mode)
        return true;
    }
}
