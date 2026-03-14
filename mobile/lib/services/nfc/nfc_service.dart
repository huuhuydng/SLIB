import 'dart:io';
import 'package:flutter/foundation.dart';
import 'package:nfc_manager/nfc_manager.dart';
import 'package:nfc_manager/ndef_record.dart';
import 'package:nfc_manager_ndef/nfc_manager_ndef.dart';

/// NFC Seat Service for handling seat confirmation via NFC scanning.
///
/// This service reads NFC tags containing seat IDs in the format "SLIB-SEAT-XXX"
/// and validates them for library seat confirmation.
class NfcSeatService {
  /// Singleton instance
  static final NfcSeatService _instance = NfcSeatService._internal();
  factory NfcSeatService() => _instance;
  NfcSeatService._internal();

  /// Prefix that valid seat IDs must start with
  static const String seatIdPrefix = 'SLIB-SEAT-';

  /// Check if NFC is available on the device
  Future<bool> isNfcAvailable() async {
    try {
      final availability = await NfcManager.instance.checkAvailability();
      return availability == NfcAvailability.enabled;
    } catch (e) {
      debugPrint('NfcSeatService: Error checking NFC availability: $e');
      return false;
    }
  }

  /// Start NFC scanning for seat confirmation.
  ///
  /// [onSeatFound] - Callback invoked when a valid seat ID is found.
  /// [onError] - Callback invoked when an error occurs.
  /// [onNfcUnavailable] - Callback invoked when NFC is not available.
  ///
  /// Returns true if scanning started successfully, false otherwise.
  Future<bool> startNFCScan({
    required Function(String seatId) onSeatFound,
    Function(String errorMessage)? onError,
    Function()? onNfcUnavailable,
  }) async {
    // Check NFC availability first
    final isAvailable = await isNfcAvailable();
    if (!isAvailable) {
      debugPrint('NfcSeatService: NFC is not available on this device');
      onNfcUnavailable?.call();
      return false;
    }

    try {
      // Start NFC session
      await NfcManager.instance.startSession(
        pollingOptions: {NfcPollingOption.iso14443},
        onDiscovered: (NfcTag tag) async {
          try {
            final seatId = await _processNfcTag(tag);

            if (seatId != null) {
              // Valid seat ID found
              debugPrint('NfcSeatService: Found valid seat ID: $seatId');

              // Stop session before callback
              await _stopSession();

              onSeatFound(seatId);
            } else {
              // Invalid tag - notify error but continue scanning on Android
              debugPrint('NfcSeatService: Invalid NFC tag scanned');

              if (Platform.isIOS) {
                // iOS: Stop session with error
                await _stopSession();
                onError?.call(
                  'Thẻ NFC không hợp lệ. Vui lòng quét thẻ SLIB-SEAT.',
                );
              } else {
                // Android: Continue scanning, just notify the error
                onError?.call(
                  'Thẻ NFC không hợp lệ. Vui lòng quét thẻ SLIB-SEAT hợp lệ.',
                );
              }
            }
          } catch (e) {
            debugPrint('NfcSeatService: Error processing tag: $e');

            if (Platform.isIOS) {
              await _stopSession();
            }
            onError?.call('Lỗi đọc thẻ NFC: $e');
          }
        },
      );

      debugPrint('NfcSeatService: NFC scanning started successfully');
      return true;
    } catch (e) {
      debugPrint('NfcSeatService: Failed to start NFC session: $e');
      
      // Check if user cancelled
      if (e.toString().contains('cancel') ||
          e.toString().contains('Cancel') ||
          e.toString().contains('cancelled')) {
        debugPrint('NfcSeatService: User cancelled NFC scanning');
        onError?.call('Đã hủy quét NFC');
      } else {
        onError?.call('Không thể bắt đầu quét NFC: $e');
      }
      return false;
    }
  }

  /// Stop the current NFC session.
  Future<void> stopNFCScan({String? errorMessage}) async {
    await _stopSession();
  }

  /// Internal method to stop NFC session.
  Future<void> _stopSession() async {
    try {
      await NfcManager.instance.stopSession();
      debugPrint('NfcSeatService: NFC session stopped');
    } catch (e) {
      debugPrint('NfcSeatService: Error stopping NFC session: $e');
    }
  }

  /// Process an NFC tag and extract the seat ID.
  ///
  /// Returns the seat ID if valid, null otherwise.
  Future<String?> _processNfcTag(NfcTag tag) async {
    // Try to read NDEF data from the tag
    final ndef = Ndef.from(tag);
    if (ndef == null) {
      debugPrint('NfcSeatService: Tag does not contain NDEF data');
      return null;
    }

    try {
      // Try cached message first, then read
      NdefMessage? message = ndef.cachedMessage;
      message ??= await ndef.read();
      
      if (message == null || message.records.isEmpty) {
        debugPrint('NfcSeatService: No NDEF records found');
        return null;
      }

      // Process each record to find seat ID
      for (final record in message.records) {
        final text = _extractTextFromRecord(record);
        if (text != null) {
          debugPrint('NfcSeatService: Extracted text from NFC: $text');

          // Validate seat ID format
          if (_isValidSeatId(text)) {
            return text;
          }
        }
      }
    } catch (e) {
      debugPrint('NfcSeatService: Error reading NDEF: $e');
    }

    debugPrint('NfcSeatService: No valid seat ID found in NFC tag');
    return null;
  }

  /// Extract text content from an NDEF record.
  ///
  /// Handles the NDEF Text Record format where:
  /// - First byte: Status byte (contains language code length)
  /// - Next N bytes: Language code (e.g., "en")
  /// - Remaining bytes: Actual text content
  String? _extractTextFromRecord(NdefRecord record) {
    try {
      final payload = record.payload;
      if (payload.isEmpty) {
        return null;
      }

      // Check record type - Text Record (TNF_WELL_KNOWN + RTD_TEXT)
      if (record.typeNameFormat == TypeNameFormat.wellKnown) {
        // Check if it's a Text record (type = "T")
        if (record.type.length == 1 && record.type[0] == 0x54) {
          // "T" in ASCII
          return _parseTextRecord(payload);
        }
        // Check if it's a URI record (type = "U")
        else if (record.type.length == 1 && record.type[0] == 0x55) {
          // "U" in ASCII
          return _parseUriRecord(payload);
        }
      }

      // Handle Media type - plain text or custom types
      if (record.typeNameFormat == TypeNameFormat.media) {
        // Try to decode as UTF-8 text
        return String.fromCharCodes(payload);
      }

      // Handle External type
      if (record.typeNameFormat == TypeNameFormat.external) {
        // Try to decode as UTF-8 text
        return String.fromCharCodes(payload);
      }

      // Fallback: try to decode payload as plain text
      return String.fromCharCodes(payload).trim();
    } catch (e) {
      debugPrint('NfcSeatService: Error extracting text from record: $e');
      return null;
    }
  }

  /// Parse NDEF Text Record payload.
  ///
  /// Text Record format:
  /// - Byte 0: Status byte
  ///   - Bit 7: UTF encoding (0 = UTF-8, 1 = UTF-16)
  ///   - Bit 6: Reserved
  ///   - Bits 5-0: Language code length
  /// - Bytes 1 to (1 + language code length - 1): Language code
  /// - Remaining bytes: Text content
  String? _parseTextRecord(Uint8List payload) {
    if (payload.isEmpty) return null;

    try {
      // Get status byte
      final statusByte = payload[0];

      // Extract language code length (lower 6 bits)
      final languageCodeLength = statusByte & 0x3F;

      // Check if UTF-16 encoding (bit 7)
      final isUtf16 = (statusByte & 0x80) != 0;

      // Calculate text start position
      final textStartIndex = 1 + languageCodeLength;

      if (textStartIndex >= payload.length) {
        debugPrint('NfcSeatService: Invalid text record - no text content');
        return null;
      }

      // Extract text content
      final textBytes = payload.sublist(textStartIndex);

      if (isUtf16) {
        // UTF-16 decoding
        // Check for BOM (Byte Order Mark)
        if (textBytes.length >= 2) {
          // Simple UTF-16 BE decoding (without external package)
          final buffer = StringBuffer();
          for (var i = 0; i < textBytes.length - 1; i += 2) {
            final charCode = (textBytes[i] << 8) | textBytes[i + 1];
            buffer.writeCharCode(charCode);
          }
          return buffer.toString().trim();
        }
        return null;
      } else {
        // UTF-8 decoding
        return String.fromCharCodes(textBytes).trim();
      }
    } catch (e) {
      debugPrint('NfcSeatService: Error parsing text record: $e');
      return null;
    }
  }

  /// Parse NDEF URI Record payload.
  String? _parseUriRecord(Uint8List payload) {
    if (payload.isEmpty) return null;

    try {
      // First byte is the URI identifier code
      final identifierCode = payload[0];
      final uriField = String.fromCharCodes(payload.sublist(1));

      // Common URI prefixes
      const uriPrefixes = {
        0x00: '',
        0x01: 'http://www.',
        0x02: 'https://www.',
        0x03: 'http://',
        0x04: 'https://',
        0x05: 'tel:',
        0x06: 'mailto:',
        // Add more as needed
      };

      final prefix = uriPrefixes[identifierCode] ?? '';
      return '$prefix$uriField'.trim();
    } catch (e) {
      debugPrint('NfcSeatService: Error parsing URI record: $e');
      return null;
    }
  }

  /// Validate if the extracted text is a valid seat ID.
  bool _isValidSeatId(String text) {
    // Check if text starts with the required prefix
    if (!text.startsWith(seatIdPrefix)) {
      debugPrint(
        'NfcSeatService: Text "$text" does not start with "$seatIdPrefix"',
      );
      return false;
    }

    // Optionally validate the seat code format (e.g., A01, B12, etc.)
    final seatCode = text.substring(seatIdPrefix.length);
    if (seatCode.isEmpty) {
      debugPrint('NfcSeatService: Seat code is empty');
      return false;
    }

    debugPrint('NfcSeatService: Valid seat ID found: $text (code: $seatCode)');
    return true;
  }

  /// Get the seat code from a full seat ID.
  ///
  /// Example: "SLIB-SEAT-A01" -> "A01"
  String? getSeatCodeFromId(String seatId) {
    if (!seatId.startsWith(seatIdPrefix)) {
      return null;
    }
    return seatId.substring(seatIdPrefix.length);
  }
}
