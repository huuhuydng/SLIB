import 'package:flutter/material.dart';
import 'package:mobile_scanner/mobile_scanner.dart';
import 'package:provider/provider.dart';
import 'package:slib/assets/colors.dart';
import 'package:slib/services/kiosk_service.dart';
import 'package:slib/services/auth_service.dart';

class QrScanScreen extends StatefulWidget {
  const QrScanScreen({super.key});

  @override
  State<QrScanScreen> createState() => _QrScanScreenState();
}

class _QrScanScreenState extends State<QrScanScreen> {
  final MobileScannerController controller = MobileScannerController(
    detectionSpeed: DetectionSpeed.noDuplicates,
    returnImage: false,
  );

  bool _isScanned = false;
  bool _isProcessing = false;
  String? _kioskName;
  String? _sessionToken;

  // Trạng thái check-in (cho banner checkout)
  bool _isCheckedIn = false;
  bool _isCheckingOut = false;

  static const String _kioskCode = 'KIOSK_001';

  @override
  void initState() {
    super.initState();
    _checkCurrentStatus();
  }

  @override
  void dispose() {
    controller.dispose();
    super.dispose();
  }

  /// Kiểm tra trạng thái check-in hiện tại
  Future<void> _checkCurrentStatus() async {
    try {
      final authService = Provider.of<AuthService>(context, listen: false);
      final currentUser = authService.currentUser;
      if (currentUser == null) return;

      final isCheckedIn = await KioskService.checkStatus(currentUser.id);
      if (mounted) {
        setState(() => _isCheckedIn = isCheckedIn);
      }
    } catch (_) {}
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.black,
      body: Stack(
        children: [
          // 1. CAMERA SCANNER - luôn hiện
          MobileScanner(
            controller: controller,
            onDetect: (capture) {
              final List<Barcode> barcodes = capture.barcodes;
              if (barcodes.isNotEmpty && !_isScanned && !_isProcessing) {
                final String? code = barcodes.first.rawValue;
                if (code != null) {
                  _handleScanResult(code);
                }
              }
            },
          ),

          // 2. OVERLAY
          _buildOverlay(context),

          // 3. UI ĐIỀU KHIỂN
          SafeArea(
            child: Column(
              children: [
                // Header
                Padding(
                  padding: const EdgeInsets.all(16.0),
                  child: Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      IconButton(
                        onPressed: () => Navigator.pop(context),
                        icon: const Icon(Icons.arrow_back, color: Colors.white),
                        style: IconButton.styleFrom(backgroundColor: Colors.black45),
                      ),
                      const Text(
                        "Quét mã QR Kiosk",
                        style: TextStyle(color: Colors.white, fontSize: 18, fontWeight: FontWeight.bold),
                      ),
                      IconButton(
                        onPressed: () => controller.toggleTorch(),
                        icon: ValueListenableBuilder(
                          valueListenable: controller,
                          builder: (context, state, child) {
                            final isFlashOn = state.torchState == TorchState.on;
                            return Icon(
                              isFlashOn ? Icons.flash_on : Icons.flash_off,
                              color: isFlashOn ? Colors.yellow : Colors.white,
                            );
                          },
                        ),
                        style: IconButton.styleFrom(backgroundColor: Colors.black45),
                      ),
                    ],
                  ),
                ),

                const Spacer(),

                // Hướng dẫn
                Padding(
                  padding: EdgeInsets.only(bottom: _isCheckedIn ? 8 : 80),
                  child: const Text(
                    "Di chuyển camera đến vùng chứa mã QR",
                    style: TextStyle(color: Colors.white70, fontSize: 14),
                  ),
                ),

                // 4. BANNER CHECK-OUT (chỉ hiện khi đã check-in)
                if (_isCheckedIn) _buildCheckoutBanner(),
              ],
            ),
          ),
        ],
      ),
    );
  }

  /// Banner floating ở dưới khi đang check-in
  Widget _buildCheckoutBanner() {
    return Container(
      margin: const EdgeInsets.fromLTRB(16, 0, 16, 24),
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(16),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(0.2),
            blurRadius: 12,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      child: Row(
        children: [
          // Status icon
          Container(
            padding: const EdgeInsets.all(8),
            decoration: BoxDecoration(
              color: const Color(0xFF4CAF50).withOpacity(0.1),
              borderRadius: BorderRadius.circular(10),
            ),
            child: const Icon(Icons.check_circle, color: Color(0xFF4CAF50), size: 24),
          ),
          const SizedBox(width: 12),

          // Text
          const Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              mainAxisSize: MainAxisSize.min,
              children: [
                Text(
                  "Bạn đang trong thư viện",
                  style: TextStyle(fontSize: 14, fontWeight: FontWeight.bold, color: Color(0xFF333333)),
                ),
                SizedBox(height: 2),
                Text(
                  "Bấm để check out thư viện",
                  style: TextStyle(fontSize: 12, color: Color(0xFF999999)),
                ),
              ],
            ),
          ),

          // Check-out button
          SizedBox(
            height: 38,
            child: ElevatedButton(
              onPressed: _isCheckingOut ? null : _handleCheckout,
              style: ElevatedButton.styleFrom(
                backgroundColor: const Color(0xFFFF5722),
                foregroundColor: Colors.white,
                padding: const EdgeInsets.symmetric(horizontal: 16),
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
                elevation: 0,
              ),
              child: _isCheckingOut
                  ? const SizedBox(
                      width: 18, height: 18,
                      child: CircularProgressIndicator(color: Colors.white, strokeWidth: 2),
                    )
                  : const Text("Check-out", style: TextStyle(fontSize: 13, fontWeight: FontWeight.w600)),
            ),
          ),
        ],
      ),
    );
  }

  Future<void> _handleCheckout() async {
    final authService = Provider.of<AuthService>(context, listen: false);
    final currentUser = authService.currentUser;
    if (currentUser == null) return;

    setState(() => _isCheckingOut = true);

    try {
      await KioskService.checkOutMobile(currentUser.id);
      if (mounted) {
        setState(() {
          _isCheckedIn = false;
          _isCheckingOut = false;
        });
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text("Check-out thành công! Hẹn gặp lại"),
            backgroundColor: Colors.green,
          ),
        );
      }
    } catch (e) {
      if (mounted) {
        setState(() => _isCheckingOut = false);
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text("Lỗi check-out: ${e.toString()}"),
            backgroundColor: Colors.red,
          ),
        );
      }
    }
  }

  Widget _buildOverlay(BuildContext context) {
    return ColorFiltered(
      colorFilter: const ColorFilter.mode(Colors.black54, BlendMode.srcOut),
      child: Stack(
        children: [
          Container(
            decoration: const BoxDecoration(
              color: Colors.transparent,
              backgroundBlendMode: BlendMode.dstOut,
            ),
          ),
          Align(
            alignment: Alignment.center,
            child: Container(
              height: 280,
              width: 280,
              decoration: BoxDecoration(
                color: Colors.black,
                borderRadius: BorderRadius.circular(20),
                border: Border.all(color: Colors.white, width: 2),
              ),
            ),
          ),
        ],
      ),
    );
  }

  void _handleScanResult(String code) async {
    setState(() => _isScanned = true);

    // Parse QR payload để lấy kiosk code
    String kioskCode = _kioskCode;
    try {
      final parts = code.split('.');
      if (parts.isNotEmpty) {
        final payloadParts = parts[0].split(':');
        if (payloadParts.isNotEmpty) {
          kioskCode = payloadParts[0];
        }
      }
    } catch (_) {}

    // Validate QR
    try {
      final validateResult = await KioskService.validateQr(code, kioskCode);
      if (validateResult['success'] != true) {
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(
              content: Text("QR không hợp lệ: ${validateResult['message'] ?? 'Lỗi'}"),
              backgroundColor: Colors.red,
            ),
          );
          setState(() => _isScanned = false);
        }
        return;
      }
      _sessionToken = validateResult['sessionToken'];
      _kioskName = validateResult['kioskName'];
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text("Lỗi xác thực: ${e.toString()}"),
            backgroundColor: Colors.red,
          ),
        );
        setState(() => _isScanned = false);
      }
      return;
    }

    if (!mounted) return;

    // Hiện dialog xác nhận
    showDialog(
      context: context,
      barrierDismissible: false,
      builder: (context) => StatefulBuilder(
        builder: (context, setDialogState) => Dialog(
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              // Header gradient
              Container(
                width: double.infinity,
                padding: const EdgeInsets.symmetric(vertical: 28),
                decoration: const BoxDecoration(
                  gradient: LinearGradient(
                    colors: [Color(0xFFFF8C42), Color(0xFFFF6B35)],
                    begin: Alignment.topLeft,
                    end: Alignment.bottomRight,
                  ),
                  borderRadius: BorderRadius.only(
                    topLeft: Radius.circular(20),
                    topRight: Radius.circular(20),
                  ),
                ),
                child: const Column(
                  children: [
                    Icon(Icons.qr_code_scanner, size: 48, color: Colors.white),
                    SizedBox(height: 10),
                    Text(
                      "Xác nhận Check-in",
                      style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold, color: Colors.white),
                    ),
                  ],
                ),
              ),

              // Body
              Padding(
                padding: const EdgeInsets.fromLTRB(24, 20, 24, 8),
                child: Column(
                  children: [
                    const Text(
                      "Mã QR hợp lệ! Bạn muốn check-in tại đây?",
                      textAlign: TextAlign.center,
                      style: TextStyle(fontSize: 15, color: Color(0xFF555555)),
                    ),
                    const SizedBox(height: 16),
                    Container(
                      width: double.infinity,
                      padding: const EdgeInsets.all(14),
                      decoration: BoxDecoration(
                        color: const Color(0xFFFFF3E8),
                        borderRadius: BorderRadius.circular(12),
                        border: Border.all(color: const Color(0xFFFFD9B7)),
                      ),
                      child: Row(
                        children: [
                          Container(
                            padding: const EdgeInsets.all(10),
                            decoration: BoxDecoration(
                              color: AppColors.brandColor.withOpacity(0.15),
                              borderRadius: BorderRadius.circular(10),
                            ),
                            child: const Icon(Icons.location_on, color: AppColors.brandColor, size: 22),
                          ),
                          const SizedBox(width: 12),
                          Expanded(
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                const Text("Vị trí", style: TextStyle(fontSize: 12, color: Color(0xFF999999))),
                                const SizedBox(height: 2),
                                Text(
                                  _kioskName ?? kioskCode,
                                  style: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold, color: Color(0xFF333333)),
                                ),
                              ],
                            ),
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
              ),

              // Buttons
              Padding(
                padding: const EdgeInsets.fromLTRB(24, 12, 24, 24),
                child: Row(
                  children: [
                    Expanded(
                      child: OutlinedButton(
                        onPressed: _isProcessing ? null : () {
                          setState(() => _isScanned = false);
                          Navigator.pop(context);
                        },
                        style: OutlinedButton.styleFrom(
                          padding: const EdgeInsets.symmetric(vertical: 14),
                          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                          side: const BorderSide(color: Color(0xFFDDDDDD)),
                        ),
                        child: const Text("Hủy", style: TextStyle(color: Color(0xFF888888), fontSize: 15)),
                      ),
                    ),
                    const SizedBox(width: 12),
                    Expanded(
                      flex: 2,
                      child: ElevatedButton(
                        onPressed: _isProcessing ? null : () {
                          setDialogState(() {});
                          _processCheckIn();
                        },
                        style: ElevatedButton.styleFrom(
                          backgroundColor: AppColors.brandColor,
                          padding: const EdgeInsets.symmetric(vertical: 14),
                          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                          elevation: 0,
                        ),
                        child: _isProcessing
                          ? const SizedBox(
                              width: 22, height: 22,
                              child: CircularProgressIndicator(color: Colors.white, strokeWidth: 2.5),
                            )
                          : const Text(
                              "Check-in ngay",
                              style: TextStyle(color: Colors.white, fontSize: 15, fontWeight: FontWeight.w600),
                            ),
                      ),
                    ),
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Future<void> _processCheckIn() async {
    setState(() => _isProcessing = true);

    try {
      if (_sessionToken == null) {
        throw Exception('Không lấy được session token');
      }

      final authService = Provider.of<AuthService>(context, listen: false);
      final currentUser = authService.currentUser;
      if (currentUser == null) {
        throw Exception('Vui lòng đăng nhập lại');
      }

      await KioskService.completeSession(_sessionToken!, currentUser.id);

      if (mounted) {
        Navigator.pop(context); // Close dialog
        Navigator.pop(context); // Go back

        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text("Check-in thành công! Xin chào ${currentUser.fullName}"),
            backgroundColor: Colors.green,
          ),
        );
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text("Lỗi: ${e.toString()}"),
            backgroundColor: Colors.red,
          ),
        );
        setState(() {
          _isScanned = false;
          _isProcessing = false;
        });
      }
    }
  }
}
