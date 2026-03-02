package slib.com.example.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import lombok.RequiredArgsConstructor;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import slib.com.example.security.JwtAuthenticationFilter;
import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Auth endpoints that require authentication
                        .requestMatchers("/slib/auth/change-password").authenticated()
                        .requestMatchers("/slib/auth/admin-reset-password").hasRole("ADMIN")
                        // Public auth endpoints
                        .requestMatchers("/slib/auth/**").permitAll()
                        .requestMatchers("/slib/users/login-google").permitAll()
                        .requestMatchers("/slib/users/getall").permitAll()
                        .requestMatchers("/actuator/**").permitAll()
                        // Open WebSocket endpoints (important for realtime)
                        .requestMatchers("/ws/**").permitAll()
                        .requestMatchers("/ws-mobile/**").permitAll()
                        // AI Admin endpoints (cho thủ thư)
                        .requestMatchers("/slib/ai/admin/**").permitAll()
                        // AI Chat endpoints (cho sinh viên - cần authenticated)
                        .requestMatchers("/slib/ai/chat/**").authenticated()
                        .requestMatchers("/slib/files/**").permitAll()
                        // User management endpoints (Admin only)
                        .requestMatchers("/slib/users/import").hasRole("ADMIN")
                        .requestMatchers("/slib/users/*/status").hasRole("ADMIN")
                        // Protected endpoints
                        .requestMatchers("/slib/users/me").authenticated()
                        .requestMatchers("/slib/users/logout-all").authenticated()

                        // Các endpoint khác
                        .anyRequest().permitAll()) // Tạm để permitAll để test, sau này nên đổi thành authenticated()
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(
                "http://localhost:5173",
                "http://localhost:3000",
                "http://127.0.0.1:5173",
                "http://127.0.0.1:3000"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}