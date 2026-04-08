import 'dart:io';
import 'package:flutter/material.dart';
import 'package:slib/assets/colors.dart';
import 'package:slib/services/nfc/nfc_uid_service.dart';
import 'package:slib/services/booking/booking_service.dart';

enum NfcSeatActionMode { checkIn, leaveSeat }

/// Screen for verifying seat reservation via NFC scanning.
///
/// This screen allows users to tap their phone on an NFC tag attached to
/// a library seat to confirm their reservation. Uses UID Mapping strategy:
/// reads NFC tag UID -> hashes it -> looks up seat in backend.
class NfcSeatVerificationScreen extends StatefulWidget {
  /// The expected seat code (e.g., "A01") that the user has reserved.
  final String expectedSeatCode;

  /// The expected seat ID that the user has reserved.
  final int expectedSeatId;

  /// The reservation ID for updating status after verification.
  final String reservationId;

  final NfcSeatActionMode mode;

  /// Callback when verification is successful.
  final Function(String seatId)? onVerificationSuccess;

  /// Callback when user cancels the verification.
  final VoidCallback? onCancel;

  const NfcSeatVerificationScreen({
    super.key,
    required this.expectedSeatCode,
    required this.expectedSeatId,
    required this.reservationId,
    this.mode = NfcSeatActionMode.checkIn,
    this.onVerificationSuccess,
    this.onCancel,
  });

  @override
  State<NfcSeatVerificationScreen> createState() =>
      _NfcSeatVerificationScreenState();
}

class _NfcSeatVerificationScreenState extends State<NfcSeatVerificationScreen>
    with SingleTickerProviderStateMixin {
  final NfcUidService _nfcUidService = NfcUidService();
  final BookingService _bookingService = BookingService();

  bool _isScanning = false;
  bool _isNfcAvailable = true;
  bool _isLookingUp = false; // Looking up seat from backend
  String? _errorMessage;
  bool _verificationSuccess = false;
  bool _isClosed = false; // Prevent multiple closes

  late AnimationController _pulseController;
  late Animation<double> _pulseAnimation;

  @override
  void initState() {
    super.initState();
    _initAnimation();
    _checkNfcAndStartScan();
  }

  void _initAnimation() {
    _pulseController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 1500),
    )..repeat(reverse: true);

    _pulseAnimation = Tween<double>(begin: 1.0, end: 1.15).animate(
      CurvedAnimation(parent: _pulseController, curve: Curves.easeInOut),
    );
  }

  @override
  void dispose() {
    _pulseController.dispose();
    _stopNfcScan();
    super.dispose();
  }

  Future<void> _checkNfcAndStartScan() async {
    // On iOS, skip pre-check — go straight to scan.
    // iOS native dialog will appear if NFC is supported.
    if (!Platform.isIOS) {
      final isAvailable = await _nfcUidService.isNfcAvailable();
      if (!isAvailable) {
        setState(() {
          _isNfcAvailable = false;
          _errorMessage = 'Thiết bị không hỗ trợ NFC hoặc NFC bị tắt';
        });
        return;
      }
    }

    await _startNfcScan();
  }

  Future<void> _startNfcScan() async {
    setState(() {
      _isScanning = true;
      _isLookingUp = false;
      _errorMessage = null;
      _verificationSuccess = false;
    });

    final success = await _nfcUidService.startUidScan(
      onUidFound: _handleUidFound,
      onError: _handleError,
      onNfcUnavailable: () {
        setState(() {
          _isNfcAvailable = false;
          _isScanning = false;
          _errorMessage = 'NFC không khả dụng trên thiết bị này';
        });
      },
    );

    if (!success && mounted) {
      setState(() {
        _isScanning = false;
        // If start failed on iOS, NFC is truly unavailable
        if (Platform.isIOS) {
          _isNfcAvailable = false;
          _errorMessage = 'Thiết bị không hỗ trợ NFC hoặc NFC bị tắt';
        }
      });
    }
  }

  /// Handle NFC UID found for check-in or leave-seat flow.
  Future<void> _handleUidFound(String uid) async {
    if (!mounted) return;

    debugPrint('NfcVerification: Found UID: $uid');

    setState(() {
      _isScanning = false;
      _isLookingUp = true;
      _errorMessage = null;
    });

    try {
      if (widget.mode == NfcSeatActionMode.leaveSeat) {
        await _bookingService.leaveSeatWithNfcUid(widget.reservationId, uid);
        debugPrint('NfcVerification: Seat checkout completed successfully');
      } else {
        await _bookingService.confirmSeatWithNfcUid(
          widget.reservationId,
          uid,
          widget.expectedSeatId,
        );
        debugPrint('NfcVerification: Booking confirmed successfully');
      }

      if (!mounted) return;

      setState(() {
        _isLookingUp = false;
        _verificationSuccess = true;
      });
      _pulseController.stop();
    } catch (e) {
      debugPrint('NfcVerification: Error confirming booking: $e');

      if (!mounted) return;

      String errorMsg = e.toString().replaceAll('Exception: ', '');

      // Provide user-friendly messages
      if (errorMsg.contains('không tìm thấy') || errorMsg.contains('404')) {
        errorMsg =
            'Thẻ NFC này chưa được gán cho ghế nào.\nVui lòng liên hệ quản trị viên.';
      } else if (errorMsg.contains('không khớp')) {
        // Seat mismatch — message already good from backend
      } else if (errorMsg.contains('check-in')) {
        // Time window error — message already good from backend
      } else if (errorMsg.contains('trả chỗ')) {
        // Leave-seat error — keep backend message
      }

      setState(() {
        _isLookingUp = false;
        _errorMessage = errorMsg;
      });

      // Allow retry on Android
      if (Platform.isAndroid) {
        Future.delayed(const Duration(seconds: 2), () {
          if (mounted && !_verificationSuccess) {
            _startNfcScan();
          }
        });
      }
    }
  }

  void _handleError(String errorMessage) {
    if (!mounted) return;

    setState(() {
      _isScanning = false;
      _isLookingUp = false;
      _errorMessage = errorMessage;
    });
  }

  Future<void> _stopNfcScan() async {
    await _nfcUidService.stopUidScan();
  }

  void _cancel() {
    // Prevent multiple closes
    if (_isClosed) return;
    _isClosed = true;

    _stopNfcScan();

    // Only close if we can pop
    if (!mounted) return;

    if (widget.onCancel != null) {
      widget.onCancel!.call();
    } else if (Navigator.canPop(context)) {
      Navigator.pop(context);
    }
  }

  void _retry() {
    _startNfcScan();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFFF5F7FA),
      appBar: AppBar(
        title: Text(
          widget.mode == NfcSeatActionMode.leaveSeat
              ? 'Trả chỗ ngồi'
              : 'Xác nhận chỗ ngồi',
          style: const TextStyle(
            fontWeight: FontWeight.bold,
            color: Colors.black,
          ),
        ),
        backgroundColor: Colors.white,
        elevation: 0,
        centerTitle: true,
        leading: IconButton(
          icon: const Icon(Icons.close, color: Colors.black),
          onPressed: _cancel,
        ),
      ),
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Column(
            children: [
              const Spacer(),
              _buildNfcIndicator(),
              const SizedBox(height: 32),
              _buildStatusText(),
              const SizedBox(height: 24),
              _buildSeatInfo(),
              const Spacer(),
              _buildActionButtons(),
              const SizedBox(height: 16),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildNfcIndicator() {
    if (_verificationSuccess) {
      return _buildSuccessIcon();
    }

    if (!_isNfcAvailable) {
      return _buildErrorIcon();
    }

    return _buildScanningIndicator();
  }

  Widget _buildScanningIndicator() {
    return AnimatedBuilder(
      animation: _pulseAnimation,
      builder: (context, child) {
        return Transform.scale(
          scale: _isScanning ? _pulseAnimation.value : 1.0,
          child: Container(
            width: 180,
            height: 180,
            decoration: BoxDecoration(
              color: AppColors.brandColor.withAlpha(25),
              shape: BoxShape.circle,
              border: Border.all(
                color: _isScanning
                    ? AppColors.brandColor.withAlpha(100)
                    : Colors.grey.withAlpha(100),
                width: 3,
              ),
            ),
            child: Center(
              child: Container(
                width: 120,
                height: 120,
                decoration: BoxDecoration(
                  color: Colors.white,
                  shape: BoxShape.circle,
                  boxShadow: [
                    BoxShadow(
                      color: AppColors.brandColor.withAlpha(40),
                      blurRadius: 20,
                      spreadRadius: 5,
                    ),
                  ],
                ),
                child: Icon(
                  Icons.nfc_rounded,
                  size: 60,
                  color: _isScanning ? AppColors.brandColor : Colors.grey,
                ),
              ),
            ),
          ),
        );
      },
    );
  }

  Widget _buildSuccessIcon() {
    return Container(
      width: 180,
      height: 180,
      decoration: BoxDecoration(
        color: Colors.green.withAlpha(25),
        shape: BoxShape.circle,
      ),
      child: Center(
        child: Container(
          width: 120,
          height: 120,
          decoration: BoxDecoration(
            color: Colors.green[50],
            shape: BoxShape.circle,
          ),
          child: const Icon(
            Icons.check_circle_rounded,
            size: 80,
            color: Colors.green,
          ),
        ),
      ),
    );
  }

  Widget _buildErrorIcon() {
    return Container(
      width: 180,
      height: 180,
      decoration: BoxDecoration(
        color: Colors.red.withAlpha(25),
        shape: BoxShape.circle,
      ),
      child: Center(
        child: Container(
          width: 120,
          height: 120,
          decoration: BoxDecoration(
            color: Colors.red[50],
            shape: BoxShape.circle,
          ),
          child: const Icon(Icons.nfc_rounded, size: 60, color: Colors.red),
        ),
      ),
    );
  }

  Widget _buildStatusText() {
    if (_verificationSuccess) {
      return Column(
        children: [
          Text(
            widget.mode == NfcSeatActionMode.leaveSeat
                ? 'Trả chỗ thành công!'
                : 'Xác nhận thành công!',
            style: TextStyle(
              fontSize: 24,
              fontWeight: FontWeight.bold,
              color: Colors.green,
            ),
          ),
          const SizedBox(height: 8),
          Text(
            widget.mode == NfcSeatActionMode.leaveSeat
                ? 'Ghế ${widget.expectedSeatCode} đã được giải phóng'
                : 'Ghế ${widget.expectedSeatCode} đã được xác nhận',
            style: TextStyle(fontSize: 16, color: Colors.grey[600]),
          ),
        ],
      );
    }

    if (_isLookingUp) {
      return Column(
        children: [
          const Text(
            'Đang xác thực ghế...',
            style: TextStyle(
              fontSize: 20,
              fontWeight: FontWeight.bold,
              color: AppColors.brandColor,
            ),
          ),
          const SizedBox(height: 8),
          Text(
            widget.mode == NfcSeatActionMode.leaveSeat
                ? 'Hệ thống đang đối chiếu thẻ NFC để trả ghế ${widget.expectedSeatCode}'
                : 'Hệ thống đang đối chiếu thẻ NFC với ghế ${widget.expectedSeatCode}',
            textAlign: TextAlign.center,
            style: TextStyle(fontSize: 14, color: Colors.grey[600]),
          ),
        ],
      );
    }

    if (!_isNfcAvailable) {
      return Column(
        children: [
          const Text(
            'NFC không khả dụng',
            style: TextStyle(
              fontSize: 20,
              fontWeight: FontWeight.bold,
              color: Colors.red,
            ),
          ),
          const SizedBox(height: 8),
          Text(
            _errorMessage ?? 'Vui lòng kiểm tra cài đặt NFC trên thiết bị',
            textAlign: TextAlign.center,
            style: TextStyle(fontSize: 14, color: Colors.grey[600]),
          ),
        ],
      );
    }

    if (_errorMessage != null) {
      return Column(
        children: [
          Text(
            widget.mode == NfcSeatActionMode.leaveSeat
                ? 'Không thể trả chỗ'
                : 'Không thể xác nhận',
            style: TextStyle(
              fontSize: 20,
              fontWeight: FontWeight.bold,
              color: Colors.orange,
            ),
          ),
          const SizedBox(height: 8),
          Text(
            _errorMessage!,
            textAlign: TextAlign.center,
            style: TextStyle(fontSize: 14, color: Colors.grey[600]),
          ),
        ],
      );
    }

    return Column(
      children: [
        Text(
          _isScanning ? 'Đang quét...' : 'Sẵn sàng quét',
          style: const TextStyle(
            fontSize: 24,
            fontWeight: FontWeight.bold,
            color: Colors.black87,
          ),
        ),
        const SizedBox(height: 8),
        Text(
          widget.mode == NfcSeatActionMode.leaveSeat
              ? 'Chạm điện thoại vào nhãn dán NFC\nđúng ghế ${widget.expectedSeatCode} để trả chỗ'
              : 'Chạm điện thoại vào nhãn dán NFC\ntrên ghế ${widget.expectedSeatCode}',
          textAlign: TextAlign.center,
          style: TextStyle(fontSize: 16, color: Colors.grey[600]),
        ),
      ],
    );
  }

  Widget _buildSeatInfo() {
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(16),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withAlpha(10),
            blurRadius: 10,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Container(
            padding: const EdgeInsets.all(12),
            decoration: BoxDecoration(
              color: AppColors.brandColor.withAlpha(25),
              borderRadius: BorderRadius.circular(12),
            ),
            child: const Icon(
              Icons.chair_alt_rounded,
              color: AppColors.brandColor,
              size: 28,
            ),
          ),
          const SizedBox(width: 16),
          Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                widget.mode == NfcSeatActionMode.leaveSeat
                    ? 'Ghế đang sử dụng'
                    : 'Ghế đã đặt',
                style: TextStyle(color: Colors.grey[600], fontSize: 13),
              ),
              const SizedBox(height: 4),
              Text(
                widget.expectedSeatCode,
                style: const TextStyle(
                  fontSize: 20,
                  fontWeight: FontWeight.bold,
                  color: Colors.black87,
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildActionButtons() {
    if (_verificationSuccess) {
      return SizedBox(
        width: double.infinity,
        child: ElevatedButton(
          onPressed: () {
            // Đóng dialog và trả về true để cập nhật status
            Navigator.pop(context, true);
          },
          style: ElevatedButton.styleFrom(
            backgroundColor: Colors.green,
            padding: const EdgeInsets.symmetric(vertical: 16),
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(12),
            ),
          ),
          child: const Text(
            'Hoàn tất',
            style: TextStyle(
              color: Colors.white,
              fontSize: 16,
              fontWeight: FontWeight.bold,
            ),
          ),
        ),
      );
    }

    if (!_isNfcAvailable) {
      return Column(
        children: [
          SizedBox(
            width: double.infinity,
            child: ElevatedButton.icon(
              onPressed: _openNfcSettings,
              icon: const Icon(Icons.settings, color: Colors.white),
              label: const Text(
                'Mở cài đặt NFC',
                style: TextStyle(color: Colors.white, fontSize: 16),
              ),
              style: ElevatedButton.styleFrom(
                backgroundColor: AppColors.brandColor,
                padding: const EdgeInsets.symmetric(vertical: 16),
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(12),
                ),
              ),
            ),
          ),
          const SizedBox(height: 12),
          TextButton(
            onPressed: _cancel,
            child: Text(
              'Hủy',
              style: TextStyle(color: Colors.grey[600], fontSize: 16),
            ),
          ),
        ],
      );
    }

    if (_errorMessage != null && !_isScanning) {
      return Column(
        children: [
          SizedBox(
            width: double.infinity,
            child: ElevatedButton(
              onPressed: _retry,
              style: ElevatedButton.styleFrom(
                backgroundColor: AppColors.brandColor,
                padding: const EdgeInsets.symmetric(vertical: 16),
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(12),
                ),
              ),
              child: const Text(
                'Thử lại',
                style: TextStyle(
                  color: Colors.white,
                  fontSize: 16,
                  fontWeight: FontWeight.bold,
                ),
              ),
            ),
          ),
          const SizedBox(height: 12),
          TextButton(
            onPressed: _cancel,
            child: Text(
              'Hủy',
              style: TextStyle(color: Colors.grey[600], fontSize: 16),
            ),
          ),
        ],
      );
    }

    // While scanning
    return Column(
      children: [
        if (_isScanning)
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 12),
            decoration: BoxDecoration(
              color: AppColors.brandColor.withAlpha(25),
              borderRadius: BorderRadius.circular(30),
            ),
            child: Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                SizedBox(
                  width: 16,
                  height: 16,
                  child: CircularProgressIndicator(
                    strokeWidth: 2,
                    valueColor: AlwaysStoppedAnimation<Color>(
                      AppColors.brandColor,
                    ),
                  ),
                ),
                const SizedBox(width: 12),
                const Text(
                  'Đang chờ quét NFC...',
                  style: TextStyle(
                    color: AppColors.brandColor,
                    fontWeight: FontWeight.w500,
                  ),
                ),
              ],
            ),
          ),
        const SizedBox(height: 16),
        TextButton(
          onPressed: _cancel,
          child: Text(
            'Hủy',
            style: TextStyle(color: Colors.grey[600], fontSize: 16),
          ),
        ),
      ],
    );
  }

  void _openNfcSettings() {
    // This would open device NFC settings
    // For now, just show a message
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(
        content: Text('Vui lòng mở cài đặt và bật NFC'),
        duration: Duration(seconds: 3),
      ),
    );
  }
}

/// Dialog to show NFC verification option for an upcoming booking
class NfcVerificationDialog {
  static Future<bool?> show(
    BuildContext context, {
    required String seatCode,
    required int seatId,
    required String reservationId,
    NfcSeatActionMode mode = NfcSeatActionMode.checkIn,
  }) {
    return showModalBottomSheet<bool>(
      context: context,
      isScrollControlled: true,
      isDismissible: false,
      enableDrag: false,
      backgroundColor: Colors.transparent,
      builder: (dialogContext) => Container(
        height: MediaQuery.of(dialogContext).size.height * 0.85,
        decoration: const BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
        ),
        child: NfcSeatVerificationScreen(
          expectedSeatCode: seatCode,
          expectedSeatId: seatId,
          reservationId: reservationId,
          mode: mode,
          onVerificationSuccess:
              null, // Không dùng callback - dùng Navigator.pop với result
          onCancel: () {
            if (Navigator.canPop(dialogContext)) {
              Navigator.pop(dialogContext, false);
            }
          },
        ),
      ),
    );
  }
}
