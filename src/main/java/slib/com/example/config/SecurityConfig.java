package slib.com.example.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
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
                .authorizeHttpRequests(request -> request.anyRequest().permitAll());
        return http.build();
    }

    // @Bean
    // public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    //     http
    //             .csrf(AbstractHttpConfigurer::disable)
    //             .cors(cors -> cors.configurationSource(corsConfigurationSource()))
    //             .authorizeHttpRequests(request -> request
    //                     .requestMatchers(
    //                         "/slib/users/login-google",
    //                             "/slib/bookings/getall", "/slib/bookings/create",
    //                             "slib/bookings/updateStatusReserv/{reservationId}", "/slib/zones/getAllZones",
    //                             "/slib/seats/getAllSeat/{zoneId}",
    //                             "/slib/seats/getAvailableSeat/{zoneId}", "/slib/seats/getSeatsByTime/{zoneId}",
    //                             "slib/seats/getSeatsByDate/{zoneId}", "slib/bookings/cancel/{reservationId}", "/slib/hce/**", "/slib/news/public/**",
    //                             "/slib/areas/*","/slib/seats/*","/slib/zones","/slib/zone_amenities"
                                
                                
    //                             )
    //                     .permitAll()
    //                     .anyRequest().authenticated())
    //             .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
    //     return http.build();
    // }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}