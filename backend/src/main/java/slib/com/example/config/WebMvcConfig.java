package slib.com.example.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * WebMvc Configuration.
 * KioskAuthInterceptor da duoc thay the boi Spring Security filter chain
 * voi kiosk device token authentication (xem JwtAuthenticationFilter va SecurityConfig).
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    // KioskAuthInterceptor khong con duoc dang ky o day.
    // Xac thuc kiosk duoc xu ly qua JWT device token trong JwtAuthenticationFilter.
}
