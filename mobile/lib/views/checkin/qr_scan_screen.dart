import 'package:flutter/material.dart';
import 'package:mobile_scanner/mobile_scanner.dart';
import 'package:slib/assets/colors.dart'; // Import màu của bạn

class QrScanScreen extends StatefulWidget {
  const QrScanScreen({super.key});

  @override
  State<QrScanScreen> createState() => _QrScanScreenState();
}

class _QrScanScreenState extends State<QrScanScreen> {
  // Cấu hình Controller
  final MobileScannerController controller = MobileScannerController(
    detectionSpeed: DetectionSpeed.noDuplicates,
    returnImage: false,
    // Nếu muốn bật đèn ngay khi mở thì thêm: torchEnabled: true,
  );

  bool _isScanned = false; 

  @override
  void dispose() {
    controller.dispose(); // Nhớ dispose để tránh rò rỉ bộ nhớ
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.black,
      body: Stack(
        children: [
          // 1. CAMERA SCANNER
          MobileScanner(
            controller: controller,
            onDetect: (capture) {
              final List<Barcode> barcodes = capture.barcodes;
              if (barcodes.isNotEmpty && !_isScanned) {
                final String? code = barcodes.first.rawValue;
                if (code != null) {
                  _handleScanResult(code);
                }
              }
            },
          ),

          // 2. LỚP PHỦ TỐI (OVERLAY)
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
                      // Nút Back
                      IconButton(
                        onPressed: () => Navigator.pop(context),
                        icon: const Icon(Icons.arrow_back, color: Colors.white),
                        style: IconButton.styleFrom(backgroundColor: Colors.black45),
                      ),
                      
                      const Text(
                        "Quét mã QR Check-in",
                        style: TextStyle(color: Colors.white, fontSize: 18, fontWeight: FontWeight.bold),
                      ),

                      // Nút Flash (ĐÃ SỬA LỖI)
                      IconButton(
                        onPressed: () => controller.toggleTorch(),
                        icon: ValueListenableBuilder(
                          valueListenable: controller, // <--- SỬA: Lắng nghe controller
                          builder: (context, state, child) {
                            // <--- SỬA: Lấy torchState từ state
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
                
                // Text hướng dẫn
                const Padding(
                  padding: EdgeInsets.only(bottom: 80),
                  child: Text(
                    "Di chuyển camera đến vùng chứa mã QR",
                    style: TextStyle(color: Colors.white70, fontSize: 14),
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildOverlay(BuildContext context) {
    return ColorFiltered(
      colorFilter: const ColorFilter.mode(
        Colors.black54, 
        BlendMode.srcOut, 
      ),
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
                border: Border.all(color: Colors.white, width: 2), // Thêm viền trắng cho đẹp
              ),
            ),
          ),
        ],
      ),
    );
  }

  void _handleScanResult(String code) {
    setState(() {
      _isScanned = true;
    });

    // Hiện dialog kết quả
    showDialog(
      context: context,
      barrierDismissible: false,
      builder: (context) => AlertDialog(
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(15)),
        title: const Row(
          children: [
            Icon(Icons.check_circle, color: Colors.green),
            SizedBox(width: 10),
            Text("Quét thành công"),
          ],
        ),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text("Nội dung mã QR:"),
            const SizedBox(height: 5),
            Container(
              padding: const EdgeInsets.all(10),
              decoration: BoxDecoration(
                color: Colors.grey[100],
                borderRadius: BorderRadius.circular(8),
              ),
              child: Text(code, style: const TextStyle(fontWeight: FontWeight.bold)),
            ),
          ],
        ),
        actions: [
          TextButton(
            onPressed: () {
              setState(() => _isScanned = false);
              Navigator.pop(context);
            },
            child: const Text("Quét lại", style: TextStyle(color: Colors.grey)),
          ),
          ElevatedButton(
            onPressed: () {
              // TODO: GỌI API CHECK-IN
              Navigator.pop(context); 
              Navigator.pop(context);
              ScaffoldMessenger.of(context).showSnackBar(
                const SnackBar(content: Text("Đang xử lý check-in...")),
              );
            },
            style: ElevatedButton.styleFrom(
              backgroundColor: AppColors.brandColor,
              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
            ),
            child: const Text("Check-in ngay", style: TextStyle(color: Colors.white)),
          ),
        ],
      ),
    );
  }
}