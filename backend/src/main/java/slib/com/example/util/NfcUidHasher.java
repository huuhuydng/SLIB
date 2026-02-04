package slib.com.example.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utility class for hashing NFC Tag UIDs before storing in database.
 * This prevents UID theft if database is compromised.
 */
@Component
public class NfcUidHasher {

    @Value("${nfc.uid.salt:SLIB_NFC_SECRET_SALT_2026}")
    private String salt;

    /**
     * Hash the NFC UID using SHA-256 with salt.
     * This creates a one-way hash that cannot be reversed.
     *
     * @param rawUid The raw NFC UID in uppercase HEX format (e.g., "04A23C91")
     * @return Hashed UID as uppercase HEX string
     */
    public String hashUid(String rawUid) {
        if (rawUid == null || rawUid.isEmpty()) {
            return null;
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            // Combine UID with salt before hashing
            String saltedUid = rawUid.toUpperCase() + salt;
            byte[] hashBytes = digest.digest(saltedUid.getBytes(StandardCharsets.UTF_8));

            // Convert bytes to HEX string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString().toUpperCase();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Verify if a raw UID matches a stored hash.
     *
     * @param rawUid     The raw NFC UID to verify
     * @param storedHash The hash stored in database
     * @return true if the UID matches
     */
    public boolean verifyUid(String rawUid, String storedHash) {
        if (rawUid == null || storedHash == null) {
            return false;
        }
        String computedHash = hashUid(rawUid);
        return storedHash.equals(computedHash);
    }
}
