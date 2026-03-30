import 'dart:convert';
import 'package:crypto/crypto.dart';

/// Utility class for hashing NFC Tag UIDs before sending to backend.
/// This must match the hashing logic in backend's NfcUidHasher.java
class NfcUidHasher {
  /// Salt must match backend's nfc.uid.salt property
  static const String _salt = 'SLIB_NFC_SECRET_SALT_2026';

  /// Hash the NFC UID using SHA-256 with salt.
  /// This creates a one-way hash that matches the backend's hashing.
  ///
  /// [rawUid] The raw NFC UID in uppercase HEX format (e.g., "04A23C91")
  /// Returns Hashed UID as uppercase HEX string
  static String hashUid(String rawUid) {
    if (rawUid.isEmpty) {
      return '';
    }

    // Combine UID with salt before hashing (same as backend)
    final saltedUid = rawUid.toUpperCase() + _salt;
    
    // SHA-256 hash
    final bytes = utf8.encode(saltedUid);
    final digest = sha256.convert(bytes);
    
    // Convert to uppercase HEX string
    return digest.toString().toUpperCase();
  }

  /// Verify if a raw UID matches a stored hash.
  ///
  /// [rawUid] The raw NFC UID to verify
  /// [storedHash] The hash stored in database
  /// Returns true if the UID matches
  static bool verifyUid(String rawUid, String storedHash) {
    if (rawUid.isEmpty || storedHash.isEmpty) {
      return false;
    }
    final computedHash = hashUid(rawUid);
    return storedHash == computedHash;
  }
}
