package slib.com.example.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.function.Function;

import java.util.Map;

@Service
public class JwtService {
    private static final String SECRET_KEY = "super-secret-jwt-token-with-at-least-32-characters-long"; 

    public String extractUsername(String token) {
        return extractClaim(token, claims -> claims.get("email", String.class));
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public String extractStudentCode(String token) {
    return extractClaim(token, claims -> {
        // 1. Lấy object user_metadata ra dưới dạng Map
        Map<String, Object> metadata = claims.get("user_metadata", Map.class);
        
        // 2. Nếu có metadata, lấy tiếp field "student_code"
        if (metadata != null && metadata.containsKey("student_code")) {
            return (String) metadata.get("student_code");
        }
        return null; // Hoặc ném lỗi nếu bắt buộc phải có
    });
}

    // private Key getSignInKey() {
    //     byte[] keyBytes = Decoders.BASE64.decode(SECRET_KEY);
    //     return Keys.hmacShaKeyFor(keyBytes);
    // }

    private Key getSignInKey() {
    // Dùng dòng này cho Local (Text thường)
    return Keys.hmacShaKeyFor(SECRET_KEY.getBytes(java.nio.charset.StandardCharsets.UTF_8));

    // KHÔNG DÙNG dòng dưới (chỉ dùng cho Base64 trên môi trường Production Cloud)
    // return Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET_KEY)); 
}

    public boolean isTokenValid(String token, String username) {
        final String extractedEmail = extractUsername(token);
        // Supabase token đã được verify chữ ký ở bước parseClaimsJws rồi
        // Ta chỉ cần check xem email có khớp và token còn hạn không
        return (extractedEmail.equals(username) && !isTokenExpired(token));
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }
}