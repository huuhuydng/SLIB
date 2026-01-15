package slib.com.example.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        
        System.out.println("🔍 [JWT Filter] Request: " + request.getMethod() + " " + request.getRequestURI());
        
        // Skip JWT validation for OPTIONS requests (CORS preflight)
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            System.out.println("⏭️ [JWT Filter] Skipping OPTIONS request");
            filterChain.doFilter(request, response);
            return;
        }

        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userEmail;

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            System.out.println("❌ [JWT Filter] No Bearer token found");
            filterChain.doFilter(request, response);
            return;
        }

        jwt = authHeader.substring(7).trim();
        System.out.println("✅ [JWT Filter] Token found: " + jwt.substring(0, Math.min(30, jwt.length())) + "...");

        try {
            userEmail = jwtService.extractUsername(jwt);
            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                // Load user details từ database (bao gồm role từ User entity)
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);
                
                System.out.println("🎯 [JWT Filter] User email: " + userEmail);
                System.out.println("🔑 [JWT Filter] Authorities: " + userDetails.getAuthorities());
                
                if (jwtService.isTokenValid(jwt, userDetails.getUsername())) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null, 
                            userDetails.getAuthorities() // Role từ User entity (via UserDetailsService)
                    );
                    authToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    
                    System.out.println("✅ [JWT Filter] Authentication set successfully");
                }
            }
        } catch (Exception e) {
            System.err.println("❌ [JWT Filter] Cannot set user authentication: " + e.getMessage());
        }
        filterChain.doFilter(request, response);
    }
}