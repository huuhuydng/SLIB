package slib.com.example.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.util.matcher.RegexRequestMatcher;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import lombok.RequiredArgsConstructor;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import slib.com.example.security.JwtAuthenticationFilter;
import org.springframework.http.HttpMethod;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private static final String[] LIBRARY_LAYOUT_READ_ROLES = {"STUDENT", "TEACHER", "LIBRARIAN", "ADMIN", "KIOSK"};
    private static final String[] PATRON_BOOKING_ROLES = {"STUDENT", "TEACHER", "KIOSK"};

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
                        .requestMatchers("/slib/users/getall").hasAnyRole("ADMIN", "LIBRARIAN")
                        .requestMatchers("/slib/users/admin/**").hasRole("ADMIN")
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/actuator/**").hasRole("ADMIN")
                        // Open WebSocket endpoints (important for realtime)
                        .requestMatchers("/ws/**").permitAll()
                        .requestMatchers("/ws-mobile/**").permitAll()
                        // AI Admin endpoints (cấu hình tri thức, đồng bộ vector)
                        .requestMatchers("/slib/ai/admin/**").hasRole("ADMIN")
                        // AI analytics endpoints are internal librarian/admin tools
                        .requestMatchers("/slib/ai/analytics/**").hasAnyRole("ADMIN", "LIBRARIAN")
                        // AI endpoints (proxy-chat + chat) - cần authenticated
                        .requestMatchers("/slib/ai/**").authenticated()
                        .requestMatchers("/slib/system/**").hasRole("ADMIN")
                        .requestMatchers("/slib/seed/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/slib/files/proxy-image").permitAll()
                        .requestMatchers("/slib/files/**").authenticated()
                        .requestMatchers("/slib/dashboard/test-broadcast").hasRole("ADMIN")
                        .requestMatchers("/slib/dashboard/**").hasAnyRole("ADMIN", "LIBRARIAN")
                        .requestMatchers("/slib/statistics/**").hasAnyRole("ADMIN", "LIBRARIAN")
                        .requestMatchers("/slib/hce/access-logs/**").hasAnyRole("ADMIN", "LIBRARIAN")
                        .requestMatchers("/slib/hce/latest-logs").hasAnyRole("ADMIN", "LIBRARIAN")
                        .requestMatchers("/slib/hce/student-detail/**").hasAnyRole("ADMIN", "LIBRARIAN")
                        // News public endpoints (cho mobile/student)
                        .requestMatchers("/slib/news/public/**").permitAll()
                        .requestMatchers("/slib/new-books/public/**").permitAll()
                        .requestMatchers("/slib/new-books/admin/**").hasAnyRole("ADMIN", "LIBRARIAN")
                        .requestMatchers("/slib/news-categories").permitAll()
                        // Settings read endpoints
                        .requestMatchers(HttpMethod.GET, "/slib/settings/library").permitAll()
                        .requestMatchers(HttpMethod.GET, "/slib/settings/time-slots").permitAll()
                        // User settings endpoints (authorization is enforced again in controller)
                        .requestMatchers(new RegexRequestMatcher("^/slib/settings/[0-9a-fA-F-]{36}$", "GET"))
                        .authenticated()
                        .requestMatchers(new RegexRequestMatcher("^/slib/settings/[0-9a-fA-F-]{36}$", "PUT"))
                        .authenticated()
                        // Settings write endpoints
                        .requestMatchers("/slib/settings/**").hasRole("ADMIN")
                        // Slideshow public endpoints (cho kiosk)
                        .requestMatchers("/api/slideshow/config").permitAll()
                        .requestMatchers("/api/slideshow/images").permitAll()
                        // User management endpoints (Admin only)
                        .requestMatchers(HttpMethod.POST, "/slib/users").hasRole("ADMIN")
                        .requestMatchers("/slib/users/import").hasRole("ADMIN")
                        .requestMatchers("/slib/users/*/status").hasRole("ADMIN")
                        // Area management endpoints (Admin only)
                        .requestMatchers("/slib/areas/{id}/locked").hasRole("ADMIN")
                        // Protected endpoints
                        .requestMatchers("/slib/users/me").authenticated()
                        .requestMatchers("/slib/users/logout-all").authenticated()

                        // HCE gate endpoints (Raspberry Pi uses X-API-KEY, not JWT)
                        .requestMatchers(HttpMethod.POST, "/slib/hce/checkin").permitAll()
                        .requestMatchers(HttpMethod.POST, "/slib/hce/stations/*/heartbeat").permitAll()
                        .requestMatchers("/slib/hce/stations/**").hasAnyRole("ADMIN", "LIBRARIAN")
                        .requestMatchers("/slib/hce/**").hasAnyRole("ADMIN", "LIBRARIAN")

                        // Kiosk admin endpoints - yeu cau ADMIN hoac LIBRARIAN
                        .requestMatchers("/slib/kiosk/admin/**").hasAnyRole("ADMIN", "LIBRARIAN")
                        // Kiosk activation - public (token duoc xac thuc trong endpoint)
                        .requestMatchers("/slib/kiosk/session/activate").permitAll()
                        .requestMatchers("/slib/kiosk/session/activate-code").permitAll()
                        // Kiosk booking flow - cho phep KIOSK device token truy cap
                        .requestMatchers(HttpMethod.POST, "/slib/areas/**").hasAnyRole("ADMIN", "LIBRARIAN")
                        .requestMatchers(HttpMethod.PUT, "/slib/areas/**").hasAnyRole("ADMIN", "LIBRARIAN")
                        .requestMatchers(HttpMethod.DELETE, "/slib/areas/**").hasAnyRole("ADMIN", "LIBRARIAN")
                        .requestMatchers(HttpMethod.POST, "/slib/zones/**").hasAnyRole("ADMIN", "LIBRARIAN")
                        .requestMatchers(HttpMethod.PUT, "/slib/zones/**").hasAnyRole("ADMIN", "LIBRARIAN")
                        .requestMatchers(HttpMethod.DELETE, "/slib/zones/**").hasAnyRole("ADMIN", "LIBRARIAN")
                        .requestMatchers(HttpMethod.POST, "/slib/seats/**").hasAnyRole("ADMIN", "LIBRARIAN")
                        .requestMatchers(HttpMethod.PUT, "/slib/seats/**").hasAnyRole("ADMIN", "LIBRARIAN")
                        .requestMatchers(HttpMethod.DELETE, "/slib/seats/**").hasAnyRole("ADMIN", "LIBRARIAN")
                        .requestMatchers(HttpMethod.POST, "/slib/area_factories/**").hasAnyRole("ADMIN", "LIBRARIAN")
                        .requestMatchers(HttpMethod.PUT, "/slib/area_factories/**").hasAnyRole("ADMIN", "LIBRARIAN")
                        .requestMatchers(HttpMethod.PATCH, "/slib/area_factories/**").hasAnyRole("ADMIN", "LIBRARIAN")
                        .requestMatchers(HttpMethod.DELETE, "/slib/area_factories/**").hasAnyRole("ADMIN", "LIBRARIAN")
                        .requestMatchers(HttpMethod.POST, "/slib/zone_amenities/**").hasAnyRole("ADMIN", "LIBRARIAN")
                        .requestMatchers(HttpMethod.PUT, "/slib/zone_amenities/**").hasAnyRole("ADMIN", "LIBRARIAN")
                        .requestMatchers(HttpMethod.DELETE, "/slib/zone_amenities/**").hasAnyRole("ADMIN", "LIBRARIAN")
                        .requestMatchers(HttpMethod.GET, "/slib/seats/nfc-mappings").hasAnyRole("ADMIN", "LIBRARIAN")
                        .requestMatchers(HttpMethod.GET, "/slib/seats/*/nfc-info").hasAnyRole("ADMIN", "LIBRARIAN")
                        .requestMatchers("/slib/layout-admin/**").hasAnyRole("ADMIN", "LIBRARIAN")
                        .requestMatchers(HttpMethod.GET, "/slib/areas/**").hasAnyRole(LIBRARY_LAYOUT_READ_ROLES)
                        .requestMatchers(HttpMethod.GET, "/slib/zones/**").hasAnyRole(LIBRARY_LAYOUT_READ_ROLES)
                        .requestMatchers(HttpMethod.GET, "/slib/seats/**").hasAnyRole(LIBRARY_LAYOUT_READ_ROLES)
                        .requestMatchers(HttpMethod.GET, "/slib/area_factories/**").hasAnyRole(LIBRARY_LAYOUT_READ_ROLES)
                        .requestMatchers(HttpMethod.GET, "/slib/zone_amenities/**").hasAnyRole(LIBRARY_LAYOUT_READ_ROLES)
                        .requestMatchers("/slib/bookings/create").hasAnyRole(PATRON_BOOKING_ROLES)
                        .requestMatchers("/slib/bookings/cancel/**").hasAnyRole(PATRON_BOOKING_ROLES)
                        .requestMatchers("/slib/bookings/manual-confirm/**").hasAnyRole("ADMIN", "LIBRARIAN")
                        .requestMatchers("/slib/bookings/confirm-nfc/**").hasAnyRole(PATRON_BOOKING_ROLES)
                        .requestMatchers("/slib/bookings/confirm-nfc-uid/**").hasAnyRole(PATRON_BOOKING_ROLES)
                        // Cac endpoint khac
                        .anyRequest().authenticated())
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
                "http://127.0.0.1:3000",
                "https://slibsystem.site",
                "https://api.slibsystem.site"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
