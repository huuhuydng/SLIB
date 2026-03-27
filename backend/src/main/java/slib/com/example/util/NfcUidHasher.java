package slib.com.example.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utility class for normalizing, validating, and hashing NFC Tag UIDs.
 * 
 * All UID hashing is centralized here — clients (mobile, admin bridge)
 * must send raw UIDs, and only the backend hashes before storing/comparing.
 */
@Component
public class NfcUidHasher {

    @Value("${nfc.uid.salt:SLIB_NFC_SECRET_SALT_2026}")
    private String salt;

    /**
     * Normalize a raw NFC UID: strip separators (: - space), convert to uppercase.
     *
     * @param rawUid The raw NFC UID (e.g., "04:a2:3c:91" or "04A23C91")
     * @return Normalized UID (e.g., "04A23C91"), or null if input is null/empty
     */
    public String normalizeUid(String rawUid) {
        if (rawUid == null || rawUid.trim().isEmpty()) {
            return null;
        }
        return rawUid.replaceAll("[:\\-\\s]", "").toUpperCase().trim();
    }

    /**
     * Validate UID format: must be uppercase HEX with valid NFC tag UID length.
     *
     * Valid lengths:
     * - 4 bytes (8 chars): Standard MIFARE Classic
     * - 7 bytes (14 chars): MIFARE Ultralight, NTAG
     * - 10 bytes (20 chars): Double-size UID
     *
     * @param uid The normalized UID to validate
     * @return true if format is valid
     */
    public boolean isValidUidFormat(String uid) {
        if (uid == null || uid.isEmpty()) {
            return false;
        }
        if (!uid.matches("[0-9A-F]+")) {
            return false;
        }
        int len = uid.length();
        return len == 8 || len == 14 || len == 20;
    }

    /**
     * Create a masked version of a UID for safe display (e.g., "04A2****").
     *
     * @param rawUid The raw UID
     * @return Masked UID showing first 4 chars + asterisks
     */
    public String maskUid(String rawUid) {
        if (rawUid == null || rawUid.length() <= 4) {
            return "****";
        }
        String normalized = normalizeUid(rawUid);
        if (normalized == null || normalized.length() <= 4) {
            return "****";
        }
        return normalized.substring(0, 4) + "*".repeat(normalized.length() - 4);
    }

    /**
     * Hash the NFC UID using SHA-256 with salt.
     * Normalizes the UID before hashing.
     *
     * @param rawUid The raw NFC UID (any format — will be normalized)
     * @return Hashed UID as uppercase HEX string
     */
    public String hashUid(String rawUid) {
        String normalized = normalizeUid(rawUid);
        if (normalized == null) {
            return null;
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String saltedUid = normalized + salt;
            byte[] hashBytes = digest.digest(saltedUid.getBytes(StandardCharsets.UTF_8));

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
