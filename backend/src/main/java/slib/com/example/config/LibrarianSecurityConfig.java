package slib.com.example.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class LibrarianSecurityConfig {

    /**
     * Security filter chain riêng cho Librarian endpoints
     * Order = 1 để xử lý trước SecurityConfig chính (Order = 2 mặc định)
     */
    @Bean
    @Order(1)
    public SecurityFilterChain librarianSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/api/librarian/**")
            .cors(cors -> cors.configurationSource(librarianCorsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/api/librarian/login",
                    "/api/librarian/forgot-password",
                    "/api/librarian/verify-otp",
                    "/api/librarian/resend-otp",
                    "/api/librarian/update-password"
                ).permitAll()
                .anyRequest().authenticated()
            );
        
        return http.build();
    }
    
    @Bean
    public CorsConfigurationSource librarianCorsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*")); // Cho phép tất cả origins
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(false);
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/librarian/**", configuration);
        return source;
    }
}
