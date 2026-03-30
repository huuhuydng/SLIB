package slib.com.example.service.kiosk;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import slib.com.example.entity.kiosk.KioskConfigEntity;
import slib.com.example.exception.BadRequestException;
import slib.com.example.exception.ResourceNotFoundException;
import slib.com.example.repository.kiosk.KioskConfigRepository;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Service quan ly device token cho kiosk.
 * Su dung cung JWT secret key voi JwtService de ky va xac thuc token.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KioskTokenService {

    private static final long DEVICE_TOKEN_EXPIRATION_DAYS = 30;
    private static final String TOKEN_TYPE_KIOSK_DEVICE = "kiosk_device";

    private final KioskConfigRepository kioskConfigRepository;

    @Value("${jwt.secret}")
    private String secretKey;

    /**
     * Tao device token cho kiosk.
     *
     * @param kioskId       ID cua kiosk config
     * @param issuedByUserId UUID cua nguoi tao token (admin/librarian)
     * @return token string
     */
    @Transactional
    public String generateDeviceToken(Integer kioskId, UUID issuedByUserId) {
        KioskConfigEntity kiosk = kioskConfigRepository.findById(kioskId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay kiosk voi ID: " + kioskId));

        if (!Boolean.TRUE.equals(kiosk.getIsActive())) {
            throw new BadRequestException("Kiosk hien dang bi vo hieu hoa, khong the tao token");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusDays(DEVICE_TOKEN_EXPIRATION_DAYS);

        Map<String, Object> claims = new HashMap<>();
        claims.put("type", TOKEN_TYPE_KIOSK_DEVICE);
        claims.put("kioskId", kiosk.getId());
        claims.put("kioskCode", kiosk.getKioskCode());

        long expirationMillis = DEVICE_TOKEN_EXPIRATION_DAYS * 24 * 60 * 60 * 1000L;

        String token = Jwts.builder()
                .setClaims(claims)
                .setSubject(kiosk.getKioskCode())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expirationMillis))
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();

        kiosk.setDeviceToken(token);
        kiosk.setDeviceTokenIssuedAt(now);
        kiosk.setDeviceTokenExpiresAt(expiresAt);
        kiosk.setDeviceTokenIssuedBy(issuedByUserId);
        kioskConfigRepository.save(kiosk);

        log.info("Da tao device token cho kiosk {} boi user {}", kiosk.getKioskCode(), issuedByUserId);
        return token;
    }

    /**
     * Xac thuc device token.
     *
     * @param token JWT token tu header Authorization
     * @return KioskConfigEntity neu hop le, null neu khong hop le
     */
    public KioskConfigEntity validateDeviceToken(String token) {
        try {
            Claims claims = extractAllClaims(token);

            String type = claims.get("type", String.class);
            if (!TOKEN_TYPE_KIOSK_DEVICE.equals(type)) {
                return null;
            }

            // Kiem tra token het han
            if (claims.getExpiration().before(new Date())) {
                log.warn("Device token da het han");
                return null;
            }

            // Kiem tra token khop voi ban ghi trong DB (revocation check)
            String kioskCode = claims.getSubject();
            KioskConfigEntity kiosk = kioskConfigRepository.findByKioskCode(kioskCode).orElse(null);
            if (kiosk == null) {
                log.warn("Khong tim thay kiosk voi code: {}", kioskCode);
                return null;
            }

            if (kiosk.getDeviceToken() == null || !kiosk.getDeviceToken().equals(token)) {
                log.warn("Device token da bi thu hoi cho kiosk: {}", kioskCode);
                return null;
            }

            if (!Boolean.TRUE.equals(kiosk.getIsActive())) {
                log.warn("Kiosk {} dang bi vo hieu hoa", kioskCode);
                return null;
            }

            return kiosk;
        } catch (Exception e) {
            log.debug("Loi xac thuc device token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Kiem tra token co phai la kiosk device token khong.
     * Chi parse claims, khong kiem tra DB.
     */
    public boolean isKioskDeviceToken(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return TOKEN_TYPE_KIOSK_DEVICE.equals(claims.get("type", String.class));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Lay kioskId tu token (khong kiem tra DB).
     */
    public Integer extractKioskId(String token) {
        Claims claims = extractAllClaims(token);
        return claims.get("kioskId", Integer.class);
    }

    /**
     * Lay kioskCode tu token (khong kiem tra DB).
     */
    public String extractKioskCode(String token) {
        Claims claims = extractAllClaims(token);
        return claims.get("kioskCode", String.class);
    }

    /**
     * Thu hoi device token.
     */
    @Transactional
    public void revokeDeviceToken(Integer kioskId) {
        KioskConfigEntity kiosk = kioskConfigRepository.findById(kioskId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay kiosk voi ID: " + kioskId));

        kiosk.setDeviceToken(null);
        kiosk.setDeviceTokenIssuedAt(null);
        kiosk.setDeviceTokenExpiresAt(null);
        kiosk.setDeviceTokenIssuedBy(null);
        kioskConfigRepository.save(kiosk);

        log.info("Da thu hoi device token cho kiosk {}", kiosk.getKioskCode());
    }

    /**
     * Cap nhat thoi gian hoat dong cuoi cung.
     */
    @Transactional
    public void updateLastActive(Integer kioskId) {
        kioskConfigRepository.findById(kioskId).ifPresent(kiosk -> {
            kiosk.setLastActiveAt(LocalDateTime.now());
            kioskConfigRepository.save(kiosk);
        });
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Key getSignInKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }
}
