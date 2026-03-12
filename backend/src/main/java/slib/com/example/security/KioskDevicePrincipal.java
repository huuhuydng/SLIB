package slib.com.example.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

/**
 * Authentication token dai dien cho kiosk device.
 * Duoc tao khi kiosk xac thuc bang device token thay vi user JWT.
 */
public class KioskDevicePrincipal extends AbstractAuthenticationToken {

    private final Integer kioskId;
    private final String kioskCode;

    public KioskDevicePrincipal(Integer kioskId, String kioskCode) {
        super(List.of(new SimpleGrantedAuthority("ROLE_KIOSK")));
        this.kioskId = kioskId;
        this.kioskCode = kioskCode;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public Object getPrincipal() {
        return kioskCode;
    }

    public Integer getKioskId() {
        return kioskId;
    }

    public String getKioskCode() {
        return kioskCode;
    }
}
