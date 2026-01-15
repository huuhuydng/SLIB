package slib.com.example.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders; // 👉 Import cái này
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secretKey;

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // Supabase để email ở root claim, không phải trong sub
    public String extractUsername(String token) {
        return extractClaim(token, claims -> claims.get("email", String.class));
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    // Hàm này giữ lại để tham khảo (nếu cần lấy code từ token)
    public String extractStudentCode(String token) {
        return extractClaim(token, claims -> {
            Map<String, Object> metadata = claims.get("user_metadata", Map.class);
            if (metadata != null && metadata.containsKey("student_code")) {
                return (String) metadata.get("student_code");
            }
            return null;
        });
    }

    // 👉 QUAN TRỌNG: SỬA LẠI HÀM NÀY
    private Key getSignInKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    public boolean isTokenValid(String token, String username) {
        final String extractedEmail = extractUsername(token);
        // Kiểm tra email khớp và token chưa hết hạn
        return (extractedEmail != null && extractedEmail.equals(username) && !isTokenExpired(token));
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Tạo JWT token mới cho user với email và role
     * Dùng cho web admin tạo token custom hoặc testing
     */
    public String generateToken(String email, String role) {
        return Jwts.builder()
                .claim("email", email)
                .claim("role", role)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 24)) // 24 hours
                .signWith(getSignInKey())
                .compact();
    }
}