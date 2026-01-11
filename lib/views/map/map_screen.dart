import 'package:flutter/material.dart';
import 'package:slib/assets/colors.dart';

class MapScreen extends StatefulWidget {
  const MapScreen({super.key});

  @override
  State<MapScreen> createState() => _MapScreenState();
}

class _MapScreenState extends State<MapScreen> {
  String _selectedZone = 'A'; // Quản lý Zone A hoặc B

  // Dữ liệu giả lập tọa độ các khu vực (Khớp với ảnh mẫu)
  // Canvas giả lập kích thước: Rộng 360 x Cao 600
  final Map<String, List<MapZone>> _zoneData = {
    'A': [
      // 1. Cột trái (Kệ sách)
      MapZone(
        id: "zk_shelf",
        name: "Kệ\nsách",
        x: 10,
        y: 50,
        width: 30,
        height: 500,
        color: Colors.grey.shade300,
        occupancy: 0,
        isBookable: false,
        borderRadius: 15,
      ),

      // 2. Cửa ra vào (Nằm trên cùng)
      MapZone(
        id: "zk_door",
        name: "Cửa ra vào",
        x: 80,
        y: 10,
        width: 80,
        height: 30,
        color: Colors.grey.shade300,
        occupancy: 0,
        isBookable: false,
        borderRadius: 15,
        isLabelOnly: true,
      ),

      // 3. Khối xám to (Sảnh chính)
      MapZone(
        id: "zk_lobby",
        name: "Sảnh chính",
        x: 60,
        y: 60,
        width: 130,
        height: 190,
        color: Colors.grey.shade300,
        occupancy: 0,
        isBookable: false,
      ),

      // 4. Khối xám nhỏ (Thủ thư)
      MapZone(
        id: "zk_lib",
        name: "Thủ thư\nReference",
        x: 60,
        y: 260,
        width: 130,
        height: 40,
        color: Colors.grey.shade300,
        occupancy: 0,
        isBookable: false,
      ),

      // 5. Khu Thảo Luận (Màu Xanh - Zone Trái Dưới)
      MapZone(
        id: "zk_discuss",
        name: "Khu Thảo Luận",
        x: 60,
        y: 310,
        width: 130,
        height: 240,
        color: const Color(0xFF76C079),
        occupancy: 30,
        isBookable: true,
      ),

      // 6. Thanh chắn giữa (Trang trí)
      MapZone(
        id: "zk_divider",
        name: "",
        x: 200,
        y: 310,
        width: 25,
        height: 240,
        color: Colors.grey.shade300,
        occupancy: 0,
        isBookable: false,
        borderRadius: 12,
      ),

      // 7. Khu Yên Tĩnh (Màu Vàng - Cột Phải Dài)
      MapZone(
        id: "zk_quiet",
        name: "Khu Yên Tĩnh",
        x: 235,
        y: 60,
        width: 130,
        height: 440,
        color: const Color(0xFFF6B543),
        occupancy: 70,
        isBookable: true,
      ),

      // 8. Khu Tự Học (Màu Đỏ - Góc Phải Dưới)
      MapZone(
        id: "zk_self",
        name: "Khu Tự Học",
        x: 235,
        y: 510,
        width: 130,
        height: 60,
        color: const Color(0xFFE53935),
        occupancy: 90,
        isBookable: true,
      ),
    ],
    'B': [
      // Dữ liệu Zone B (Demo rỗng hoặc copy Zone A đổi màu nếu cần)
      MapZone(
        id: "zb_center",
        name: "Khu vực Zone B\n(Đang bảo trì)",
        x: 60,
        y: 200,
        width: 240,
        height: 200,
        color: Colors.grey.shade300,
        occupancy: 0,
        isBookable: false,
      ),
    ],
  };

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.white, // Nền App trắng
      appBar: AppBar(
        title: const Text(
          "Sơ đồ thư viện",
          style: TextStyle(fontWeight: FontWeight.bold, fontSize: 20),
        ),
        centerTitle: true,
        backgroundColor: Colors.white,
        elevation: 0,
        leading: IconButton(
          icon: const Icon(Icons.arrow_back_ios_new, color: Colors.black),
          onPressed: () => Navigator.pop(context),
        ),
        actions: [
          IconButton(
            icon: const Icon(Icons.error_outline, color: Colors.black),
            onPressed: _showLegend,
          ),
        ],
      ),
      body: Column(
        children: [
          // 1. ZONE SELECTOR (Nút chọn Zone A / Zone B)
          _buildZoneSelector(),

          const SizedBox(height: 10),

          // 2. BẢN ĐỒ TƯƠNG TÁC
          Expanded(
            child: Container(
              width: double.infinity,
              color: const Color(0xFFF5F6F8), // Nền xám nhạt bao quanh bản đồ
              child: InteractiveViewer(
                minScale: 0.5,
                maxScale: 3.0,
                boundaryMargin: const EdgeInsets.all(20),
                child: Center(
                  child: Container(
                    width: 380,
                    height: 600,
                    decoration: BoxDecoration(
                      color: Colors.white, // Nền bản đồ trắng
                      border: Border.all(color: Colors.grey.shade300),
                    ),
                    child: Stack(
                      children: [
                        // A. Lưới nền (Grid)
                        Positioned.fill(
                          child: CustomPaint(painter: GridPainter()),
                        ),

                        // B. Vẽ các Zone
                        ..._zoneData[_selectedZone]!.map(
                          (zone) => _buildZoneWidget(zone),
                        ),
                      ],
                    ),
                  ),
                ),
              ),
            ),
          ),

          // 3. FOOTER HINT
          Container(
            padding: const EdgeInsets.all(16),
            color: Colors.white,
            width: double.infinity,
            child: Text(
              "Mẹo: Chạm vào khu vực màu để xem chi tiết và đặt chỗ",
              textAlign: TextAlign.center,
              style: TextStyle(color: Colors.grey[600], fontSize: 13),
            ),
          ),
        ],
      ),
    );
  }

  // --- WIDGETS CON ---

  // 1. Bộ chọn Zone (Pill Buttons)
  Widget _buildZoneSelector() {
    return Container(
      padding: const EdgeInsets.symmetric(vertical: 10),
      color: Colors.white,
      child: Row(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          _zoneButton("Zone A", _selectedZone == 'A'),
          const SizedBox(width: 15),
          _zoneButton("Zone B", _selectedZone == 'B'),
        ],
      ),
    );
  }

  Widget _zoneButton(String title, bool isActive) {
    return GestureDetector(
      onTap: () {
        setState(() {
          _selectedZone = title.endsWith('A') ? 'A' : 'B';
        });
      },
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 30, vertical: 10),
        decoration: BoxDecoration(
          color: isActive ? AppColors.brandColor : Colors.grey[300],
          borderRadius: BorderRadius.circular(20),
        ),
        child: Text(
          title,
          style: TextStyle(
            color: isActive ? Colors.white : Colors.black54,
            fontWeight: FontWeight.bold,
          ),
        ),
      ),
    );
  }

  // 2. Widget hiển thị từng Zone trên bản đồ
  Widget _buildZoneWidget(MapZone zone) {
    return Positioned(
      left: zone.x,
      top: zone.y,
      child: GestureDetector(
        onTap: () {
          if (zone.isBookable) {
            _showZoneDetails(zone);
          }
        },
        child: Container(
          width: zone.width,
          height: zone.height,
          decoration: BoxDecoration(
            color: zone.color,
            border: Border.all(color: Colors.black12, width: 1),
            borderRadius: BorderRadius.circular(zone.borderRadius),
          ),
          child: Stack(
            alignment: Alignment.center,
            children: [
              // Tên Khu vực
              Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Text(
                    zone.name,
                    textAlign: TextAlign.center,
                    style: TextStyle(
                      fontWeight: FontWeight.bold,
                      fontSize: zone.isLabelOnly ? 11 : 13,
                      color:
                          zone.isBookable || zone.color != Colors.grey.shade300
                          ? Colors.white
                          : Colors
                                .white, // Text trắng cho tất cả để nổi trên nền màu
                    ),
                  ),

                  // Badge phần trăm (Chỉ hiện cho khu vực book được)
                  if (zone.isBookable) ...[
                    const SizedBox(height: 6),
                    Container(
                      padding: const EdgeInsets.symmetric(
                        horizontal: 8,
                        vertical: 3,
                      ),
                      decoration: BoxDecoration(
                        color: Colors.white,
                        borderRadius: BorderRadius.circular(6),
                      ),
                      child: Text(
                        "${zone.occupancy}%",
                        style: TextStyle(
                          fontSize: 11,
                          fontWeight: FontWeight.bold,
                          color: _getTextColorForPercentage(zone.occupancy),
                        ),
                      ),
                    ),
                  ],
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }

  Color _getTextColorForPercentage(int p) {
    if (p > 80) return AppColors.error;
    if (p > 50) return const Color(0xFFF6B543);
    return const Color(0xFF76C079);
  }

  // 3. Dialog chi tiết (Khi bấm vào khu vực)
  void _showZoneDetails(MapZone zone) {
    showModalBottomSheet(
      context: context,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
      ),
      builder: (context) {
        return Container(
          padding: const EdgeInsets.all(24),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Text(
                    zone.name,
                    style: const TextStyle(
                      fontSize: 20,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                  Container(
                    padding: const EdgeInsets.symmetric(
                      horizontal: 10,
                      vertical: 5,
                    ),
                    decoration: BoxDecoration(
                      color: zone.color.withOpacity(0.1),
                      borderRadius: BorderRadius.circular(10),
                    ),
                    child: Text(
                      zone.occupancy > 90
                          ? "Hết chỗ"
                          : (zone.occupancy > 50 ? "Khá đông" : "Còn trống"),
                      style: TextStyle(
                        color: zone.color,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 16),
              const Text("Tiện ích:", style: TextStyle(color: Colors.grey)),
              const SizedBox(height: 8),
              Wrap(
                spacing: 8,
                children: ["Wifi 5G", "Ổ cắm", "Điều hòa"]
                    .map(
                      (e) => Chip(
                        label: Text(e, style: const TextStyle(fontSize: 11)),
                        backgroundColor: Colors.grey[100],
                      ),
                    )
                    .toList(),
              ),
              const SizedBox(height: 24),
              SizedBox(
                width: double.infinity,
                height: 50,
                child: ElevatedButton(
                  onPressed: () {
                    Navigator.pop(context); // Đóng sheet
                    // TODO: Navigate to Booking Screen with zone.id
                    // Navigator.push(context, MaterialPageRoute(builder: (_) => SeatSelectionScreen(zoneName: zone.name)));
                  },
                  style: ElevatedButton.styleFrom(
                    backgroundColor: AppColors.brandColor,
                    foregroundColor: Colors.white,
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(12),
                    ),
                  ),
                  child: const Text("ĐẶT CHỖ TẠI ĐÂY"),
                ),
              ),
            ],
          ),
        );
      },
    );
  }

  // 4. Dialog chú thích (Legend)
  void _showLegend() {
    showDialog(
      context: context,
      builder: (_) => AlertDialog(
        title: const Text("Chú thích"),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            _legendRow(const Color(0xFF76C079), "Trống (< 50%)"),
            _legendRow(const Color(0xFFF6B543), "Khá đông (50-80%)"),
            _legendRow(const Color(0xFFE53935), "Đông đúc (> 80%)"),
            _legendRow(Colors.grey.shade300, "Không phận sự"),
          ],
        ),
      ),
    );
  }

  Widget _legendRow(Color color, String text) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 6),
      child: Row(
        children: [
          Container(
            width: 24,
            height: 24,
            decoration: BoxDecoration(
              color: color,
              borderRadius: BorderRadius.circular(4),
            ),
          ),
          const SizedBox(width: 12),
          Text(text),
        ],
      ),
    );
  }
}

// --- MODEL & PAINTER ---

class MapZone {
  final String id;
  final String name;
  final double x;
  final double y;
  final double width;
  final double height;
  final Color color;
  final int occupancy;
  final bool isBookable;
  final bool isLabelOnly;
  final double borderRadius;

  MapZone({
    required this.id,
    required this.name,
    required this.x,
    required this.y,
    required this.width,
    required this.height,
    required this.color,
    required this.occupancy,
    this.isBookable = true,
    this.isLabelOnly = false,
    this.borderRadius = 8.0,
  });
}

class GridPainter extends CustomPainter {
  @override
  void paint(Canvas canvas, Size size) {
    final paint = Paint()
      ..color = Colors.grey
          .withOpacity(0.2) // Màu lưới nhạt hơn
      ..strokeWidth = 1;

    // Vẽ lưới ô vuông 40x40 (Khớp với tỉ lệ ảnh)
    for (double x = 0; x < size.width; x += 40) {
      canvas.drawLine(Offset(x, 0), Offset(x, size.height), paint);
    }
    for (double y = 0; y < size.height; y += 40) {
      canvas.drawLine(Offset(0, y), Offset(size.width, y), paint);
    }
  }

  @override
  bool shouldRepaint(covariant CustomPainter oldDelegate) => false;
}
