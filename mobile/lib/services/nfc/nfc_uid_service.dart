import 'dart:io';
// ignore_for_file: implementation_imports, invalid_use_of_protected_member

import 'package:flutter/foundation.dart';
import 'package:nfc_manager/nfc_manager.dart';
import 'package:nfc_manager/src/nfc_manager_android/pigeon.g.dart' as android;
import 'package:nfc_manager/src/nfc_manager_ios/pigeon.g.dart' as ios;

/// NFC UID Service for reading NFC tag unique identifiers.
///
/// This service implements the UID Mapping Strategy where only the tag's
/// unique identifier (UID) is read, not the NDEF data. The UID is then
/// used to look up the corresponding seat in the backend database.
///
/// UID Format: Uppercase HEX string without separators (e.g., "04A23C91")
/// This format MUST match the Node.js NFC Bridge Server format exactly.
class NfcUidService {
  /// Singleton instance
  static final NfcUidService _instance = NfcUidService._internal();
  factory NfcUidService() => _instance;
  NfcUidService._internal();

  /// Check if NFC is available on the device
  Future<bool> isNfcAvailable() async {
    try {
      final availability = await NfcManager.instance.checkAvailability();
      return availability == NfcAvailability.enabled;
    } catch (e) {
      debugPrint('NfcUidService: Error checking NFC availability: $e');
      return false;
    }
  }

  /// Start NFC scanning for tag UID.
  ///
  /// [onUidFound] - Callback invoked when a tag UID is successfully read.
  ///                The UID is in uppercase HEX format (e.g., "04A23C91").
  /// [onError] - Callback invoked when an error occurs.
  /// [onNfcUnavailable] - Callback invoked when NFC is not available.
  ///
  /// Returns true if scanning started successfully, false otherwise.
  Future<bool> startUidScan({
    required Function(String uid) onUidFound,
    Function(String errorMessage)? onError,
    Function()? onNfcUnavailable,
  }) async {
    // On iOS, skip pre-check and try starting session directly.
    // iOS will show native "Ready to Scan" dialog if NFC is supported.
    // Pre-check can return false on free dev accounts even when NFC works.
    if (!Platform.isIOS) {
      final isAvailable = await isNfcAvailable();
      if (!isAvailable) {
        debugPrint('NfcUidService: NFC is not available on this device');
        onNfcUnavailable?.call();
        return false;
      }
    }

    try {
      // Start NFC session
      await NfcManager.instance.startSession(
        alertMessageIos: 'Chạm iPhone vào nhãn NFC trên ghế',
        pollingOptions: {NfcPollingOption.iso14443, NfcPollingOption.iso15693},
        onDiscovered: (NfcTag tag) async {
          try {
            final uid = _extractUidFromTag(tag);

            if (uid != null && uid.isNotEmpty) {
              // Valid UID found
              debugPrint('NfcUidService: Found tag UID: $uid');

              // Stop session before callback
              await _stopSession();

              onUidFound(uid);
            } else {
              // Could not extract UID
              debugPrint('NfcUidService: Could not extract UID from tag');

              if (Platform.isIOS) {
                await _stopSession();
                onError?.call('Không thể đọc UID từ thẻ NFC');
              } else {
                // Android: Continue scanning
                onError?.call('Không thể đọc UID. Vui lòng thử lại.');
              }
            }
          } catch (e) {
            debugPrint('NfcUidService: Error processing tag: $e');

            if (Platform.isIOS) {
              await _stopSession();
            }
            onError?.call('Lỗi đọc thẻ NFC: $e');
          }
        },
      );

      debugPrint('NfcUidService: NFC UID scanning started successfully');
      return true;
    } catch (e) {
      debugPrint('NfcUidService: Failed to start NFC session: $e');

      // Check if user cancelled
      if (e.toString().contains('cancel') ||
          e.toString().contains('Cancel') ||
          e.toString().contains('cancelled')) {
        debugPrint('NfcUidService: User cancelled NFC scanning');
        onError?.call('Đã hủy quét NFC');
      } else {
        onError?.call('Không thể bắt đầu quét NFC: $e');
      }
      return false;
    }
  }

  /// Stop the current NFC session.
  Future<void> stopUidScan({String? errorMessage}) async {
    await _stopSession();
  }

  /// Internal method to stop NFC session.
  Future<void> _stopSession() async {
    try {
      await NfcManager.instance.stopSession();
      debugPrint('NfcUidService: NFC session stopped');
    } catch (e) {
      debugPrint('NfcUidService: Error stopping NFC session: $e');
    }
  }

  /// Extract the UID from an NFC tag.
  ///
  /// On Android, uses TagPigeon.id field directly.
  /// On iOS, uses the identifier from technology-specific data.
  ///
  /// Returns the UID as an uppercase HEX string without separators,
  /// or null if UID could not be extracted.
  String? _extractUidFromTag(NfcTag tag) {
    try {
      Uint8List? identifier;

      if (Platform.isAndroid) {
        // Cast to Android TagPigeon and get id field
        final data = tag.data as android.TagPigeon;
        identifier = data.id;
        debugPrint(
          'NfcUidService: Got Android tag id, length: ${identifier.length}',
        );
      } else if (Platform.isIOS) {
        // iOS has different tag structures - try each technology
        final data = tag.data;
        debugPrint('NfcUidService: iOS tag data type: ${data.runtimeType}');

        // For iOS, try to cast to specific technology types
        if (data is ios.MiFarePigeon) {
          identifier = data.identifier;
          debugPrint('NfcUidService: Got iOS MiFare identifier');
        } else if (data is ios.Iso7816Pigeon) {
          identifier = data.identifier;
          debugPrint('NfcUidService: Got iOS ISO7816 identifier');
        } else if (data is ios.Iso15693Pigeon) {
          identifier = data.identifier;
          debugPrint('NfcUidService: Got iOS ISO15693 identifier');
        } else {
          debugPrint(
            'NfcUidService: Unknown iOS tag type: ${data.runtimeType}',
          );
        }
      } else {
        debugPrint('NfcUidService: Unsupported platform');
        return null;
      }

      // Convert to uppercase HEX string
      if (identifier != null && identifier.isNotEmpty) {
        final uid = _bytesToHexUppercase(identifier);
        debugPrint('NfcUidService: Extracted UID: $uid');
        return uid;
      }

      debugPrint('NfcUidService: No identifier found in tag data');
      return null;
    } catch (e) {
      debugPrint('NfcUidService: Error extracting UID: $e');
      return null;
    }
  }

  /// Convert byte array to uppercase HEX string without separators.
  ///
  /// Example: [0x04, 0xA2, 0x3C, 0x91] -> "04A23C91"
  ///
  /// This format MUST match the Node.js NFC Bridge Server format:
  /// ```javascript
  /// function formatUid(uid) {
  ///   if (Buffer.isBuffer(uid)) {
  ///     return uid.toString('hex').toUpperCase();
  ///   }
  ///   return uid.replace(/[:\-\s]/g, '').toUpperCase();
  /// }
  /// ```
  String _bytesToHexUppercase(Uint8List bytes) {
    return bytes
        .map((byte) => byte.toRadixString(16).padLeft(2, '0'))
        .join()
        .toUpperCase();
  }

  /// Validate UID format (uppercase HEX string).
  ///
  /// Valid UIDs are:
  /// - 4 bytes (8 characters): Standard MIFARE Classic
  /// - 7 bytes (14 characters): MIFARE Ultralight, NTAG
  /// - 10 bytes (20 characters): Double-size UID
  bool isValidUidFormat(String uid) {
    if (uid.isEmpty) return false;

    // Check if all characters are valid HEX
    final hexRegex = RegExp(r'^[0-9A-F]+$');
    if (!hexRegex.hasMatch(uid)) return false;

    // Check valid lengths
    final validLengths = [8, 14, 20]; // 4, 7, or 10 bytes
    return validLengths.contains(uid.length);
  }
}
