import 'dart:io';
import 'package:flutter/material.dart';
import 'package:slib/assets/colors.dart';
import 'package:slib/services/nfc_service.dart';

/// Screen for verifying seat reservation via NFC scanning.
///
/// This screen allows users to tap their phone on an NFC tag attached to
/// a library seat to confirm their reservation.
class NfcSeatVerificationScreen extends StatefulWidget {
  /// The expected seat code (e.g., "A01") that the user has reserved.
  final String expectedSeatCode;

  /// The reservation ID for updating status after verification.
  final String reservationId;

  /// Callback when verification is successful.
  final Function(String seatId)? onVerificationSuccess;

  /// Callback when user cancels the verification.
  final VoidCallback? onCancel;

  const NfcSeatVerificationScreen({
    super.key,
    required this.expectedSeatCode,
    required this.reservationId,
    this.onVerificationSuccess,
    this.onCancel,
  });

  @override
  State<NfcSeatVerificationScreen> createState() =>
      _NfcSeatVerificationScreenState();
}

class _NfcSeatVerificationScreenState extends State<NfcSeatVerificationScreen>
    with SingleTickerProviderStateMixin {
  final NfcSeatService _nfcService = NfcSeatService();

  bool _isScanning = false;
  bool _isNfcAvailable = true;
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
    final isAvailable = await _nfcService.isNfcAvailable();
    if (!isAvailable) {
      setState(() {
        _isNfcAvailable = false;
        _errorMessage = 'Thiết bị không hỗ trợ NFC hoặc NFC bị tắt';
      });
      return;
    }

    await _startNfcScan();
  }

  Future<void> _startNfcScan() async {
    setState(() {
      _isScanning = true;
      _errorMessage = null;
      _verificationSuccess = false;
    });

    final success = await _nfcService.startNFCScan(
      onSeatFound: _handleSeatFound,
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
      setState(() => _isScanning = false);
    }
  }

  void _handleSeatFound(String seatId) {
    if (!mounted) return;

    final seatCode = _nfcService.getSeatCodeFromId(seatId);

    setState(() {
      _isScanning = false;
    });

    // Verify if scanned seat matches expected seat
    if (seatCode == widget.expectedSeatCode) {
      setState(() => _verificationSuccess = true);
      _pulseController.stop();

      // Show success and call callback
      Future.delayed(const Duration(milliseconds: 500), () {
        if (mounted) {
          widget.onVerificationSuccess?.call(seatId);
        }
      });
    } else {
      setState(() {
        _errorMessage =
            'Ghế quét ($seatCode) không khớp với ghế đã đặt (${widget.expectedSeatCode}).\nVui lòng quét đúng ghế.';
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
      _errorMessage = errorMessage;
    });

    // Don't auto-close on cancel - let user see the error and manually close
    // This prevents double-pop issues
  }

  Future<void> _stopNfcScan() async {
    await _nfcService.stopNFCScan();
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
        title: const Text(
          'Xác nhận chỗ ngồi',
          style: TextStyle(fontWeight: FontWeight.bold, color: Colors.black),
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
          child: const Icon(
            Icons.nfc_rounded,
            size: 60,
            color: Colors.red,
          ),
        ),
      ),
    );
  }

  Widget _buildStatusText() {
    if (_verificationSuccess) {
      return Column(
        children: [
          const Text(
            'Xác nhận thành công!',
            style: TextStyle(
              fontSize: 24,
              fontWeight: FontWeight.bold,
              color: Colors.green,
            ),
          ),
          const SizedBox(height: 8),
          Text(
            'Ghế ${widget.expectedSeatCode} đã được xác nhận',
            style: TextStyle(
              fontSize: 16,
              color: Colors.grey[600],
            ),
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
            style: TextStyle(
              fontSize: 14,
              color: Colors.grey[600],
            ),
          ),
        ],
      );
    }

    if (_errorMessage != null) {
      return Column(
        children: [
          const Text(
            'Không thể xác nhận',
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
            style: TextStyle(
              fontSize: 14,
              color: Colors.grey[600],
            ),
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
          'Chạm điện thoại vào nhãn dán NFC\ntrên ghế ${widget.expectedSeatCode}',
          textAlign: TextAlign.center,
          style: TextStyle(
            fontSize: 16,
            color: Colors.grey[600],
          ),
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
                'Ghế đã đặt',
                style: TextStyle(
                  color: Colors.grey[600],
                  fontSize: 13,
                ),
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
    required String reservationId,
  }) {
    return showModalBottomSheet<bool>(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (dialogContext) => Container(
        height: MediaQuery.of(dialogContext).size.height * 0.85,
        decoration: const BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
        ),
        child: NfcSeatVerificationScreen(
          expectedSeatCode: seatCode,
          reservationId: reservationId,
          onVerificationSuccess: (seatId) {
            if (Navigator.canPop(dialogContext)) {
              Navigator.pop(dialogContext, true);
            }
          },
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
