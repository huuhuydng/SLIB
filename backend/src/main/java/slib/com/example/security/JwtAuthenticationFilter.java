package slib.com.example.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import slib.com.example.entity.kiosk.KioskConfigEntity;
import slib.com.example.service.kiosk.KioskTokenService;

import java.io.IOException;

@Component
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final KioskTokenService kioskTokenService;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            UserDetailsService userDetailsService,
            @Lazy KioskTokenService kioskTokenService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.kioskTokenService = kioskTokenService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        final String jwt;

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        jwt = authHeader.substring(7).trim();

        try {
            // Kiem tra neu la kiosk device token truoc
            if (kioskTokenService.isKioskDeviceToken(jwt)) {
                handleKioskAuthentication(jwt);
                filterChain.doFilter(request, response);
                return;
            }

            // Xu ly user JWT binh thuong
            String userEmail = jwtService.extractUsername(jwt);
            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);
                if (jwtService.isTokenValid(jwt, userDetails.getUsername())) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
                    authToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception e) {
            log.debug("Khong the xac thuc: {}", e.getMessage());
        }
        filterChain.doFilter(request, response);
    }

    /**
     * Xu ly xac thuc cho kiosk device token.
     * Neu token hop le, tao KioskDevicePrincipal va dat vao SecurityContext.
     */
    private void handleKioskAuthentication(String token) {
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            return;
        }

        KioskConfigEntity kiosk = kioskTokenService.validateDeviceToken(token);
        if (kiosk != null) {
            KioskDevicePrincipal principal = new KioskDevicePrincipal(
                    kiosk.getId(), kiosk.getKioskCode());
            SecurityContextHolder.getContext().setAuthentication(principal);
            log.debug("Kiosk device xac thuc thanh cong: {}", kiosk.getKioskCode());
        } else {
            log.debug("Kiosk device token khong hop le hoac da bi thu hoi");
        }
    }
}