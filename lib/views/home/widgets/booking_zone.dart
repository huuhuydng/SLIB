import 'package:flutter/material.dart';
import 'package:slib/assets/colors.dart';

class LibraryZone {
  final String name;
  final String location; // Thêm vị trí cụ thể (Tầng mấy)
  final String description;
  final int availableSeats;
  final List<String> facilities;
  final String imageUrl; // Đường dẫn ảnh

  LibraryZone({
    required this.name,
    required this.location,
    required this.description,
    required this.availableSeats,
    required this.facilities,
    required this.imageUrl,
  });
}


class BookingZoneScreen extends StatelessWidget {
  const BookingZoneScreen({super.key});

  @override
  Widget build(BuildContext context) {
    // Dữ liệu mẫu với Link ảnh thực tế (Demo)
    final List<LibraryZone> zones = [
      LibraryZone(
        name: 'Khu Yên Tĩnh',
        location: 'Tầng 2',
        description: 'Không gian tĩnh lặng, ánh sáng vàng ấm áp.',
        availableSeats: 15,
        facilities: ['Ổ cắm', 'Đèn riêng', 'Ghế đệm'],
        // Ảnh thư viện yên tĩnh
        imageUrl: 'https://images.unsplash.com/photo-1481627834876-b7833e8f5570?q=80&w=600&auto=format&fit=crop',
      ),
      LibraryZone(
        name: 'Khu Thảo Luận',
        location: 'Tầng 1',
        description: 'Thoải mái trao đổi, bảng trắng hỗ trợ.',
        availableSeats: 5,
        facilities: ['Bàn tròn', 'Bảng trắng', 'TV'],
        // Ảnh không gian làm việc nhóm
        imageUrl: 'https://images.unsplash.com/photo-1522071820081-009f0129c71c?q=80&w=600&auto=format&fit=crop',
      ),
      LibraryZone(
        name: 'Khu Tự Học (View đẹp)',
        location: 'Tầng 3',
        description: 'Không gian mở, ngắm nhìn khuôn viên.',
        availableSeats: 22,
        facilities: ['Wifi 5G', 'View cửa sổ', 'Cây xanh'],
        // Ảnh không gian sáng sủa
        imageUrl: 'https://images.unsplash.com/photo-1499951360447-b19be8fe80f5?q=80&w=600&auto=format&fit=crop',
      ),
    ];

    return Scaffold(
      appBar: AppBar(
        title: const Text("Chọn không gian", style: TextStyle(fontWeight: FontWeight.bold)),
        backgroundColor: Colors.white,
        surfaceTintColor: Colors.transparent,
      ),
      body: ListView.builder(
        padding: const EdgeInsets.all(16),
        itemCount: zones.length,
        itemBuilder: (context, index) {
          final zone = zones[index];
          // Logic tính màu trạng thái
          double occupancy = 1 - (zone.availableSeats / 50);
          Color statusColor = occupancy > 0.9 ? AppColors.error : AppColors.success;

          return GestureDetector(
            onTap: () => Navigator.push(context, MaterialPageRoute(builder: (_) => SeatSelectionScreen(zoneName: zone.name))),
            child: Container(
              margin: const EdgeInsets.only(bottom: 24), // Cách xa nhau hơn chút cho thoáng
              decoration: BoxDecoration(
                color: Colors.white,
                borderRadius: BorderRadius.circular(20),
                boxShadow: [
                  BoxShadow(
                    color: Colors.black.withOpacity(0.06),
                    blurRadius: 15,
                    offset: const Offset(0, 6),
                  )
                ],
              ),
              child: Column(
                children: [
                  // --- PHẦN ẢNH BÌA (REAL PHOTO) ---
                  Stack(
                    children: [
                      // 1. Ảnh nền
                      ClipRRect(
                        borderRadius: const BorderRadius.vertical(top: Radius.circular(20)),
                        child: Image.network(
                          zone.imageUrl,
                          height: 150, // Chiều cao ảnh
                          width: double.infinity,
                          fit: BoxFit.cover, // Cắt ảnh để lấp đầy khung
                          loadingBuilder: (context, child, loadingProgress) {
                            if (loadingProgress == null) return child;
                            return Container(
                              height: 150,
                              color: Colors.grey[200],
                              child: const Center(child: CircularProgressIndicator()),
                            );
                          },
                          errorBuilder: (context, error, stackTrace) => Container(
                            height: 150,
                            color: Colors.grey[300],
                            child: const Icon(Icons.broken_image, color: Colors.grey),
                          ),
                        ),
                      ),
                      
                      // 2. Lớp phủ đen mờ (Gradient) để text nổi hơn nếu cần viết đè lên ảnh
                      Positioned.fill(
                        child: Container(
                          decoration: BoxDecoration(
                            borderRadius: const BorderRadius.vertical(top: Radius.circular(20)),
                            gradient: LinearGradient(
                              begin: Alignment.topCenter,
                              end: Alignment.bottomCenter,
                              colors: [
                                Colors.transparent,
                                Colors.black.withOpacity(0.05), // Bóng nhẹ phía dưới ảnh
                              ],
                            ),
                          ),
                        ),
                      ),

                      // 3. Badge Vị trí (Tầng mấy)
                      Positioned(
                        top: 12,
                        left: 12,
                        child: Container(
                          padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
                          decoration: BoxDecoration(
                            color: Colors.black.withOpacity(0.6), // Nền đen bán trong suốt
                            borderRadius: BorderRadius.circular(8),
                            border: Border.all(color: Colors.white.withOpacity(0.2)),
                          ),
                          child: Row(
                            children: [
                              const Icon(Icons.location_on_rounded, color: Colors.white, size: 14),
                              const SizedBox(width: 4),
                              Text(
                                zone.location,
                                style: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 12),
                              ),
                            ],
                          ),
                        ),
                      )
                    ],
                  ),

                  // --- PHẦN NỘI DUNG ---
                  Padding(
                    padding: const EdgeInsets.all(20),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Row(
                          mainAxisAlignment: MainAxisAlignment.spaceBetween,
                          crossAxisAlignment: CrossAxisAlignment.start, // Căn lề trên
                          children: [
                            Expanded(
                              child: Text(
                                zone.name,
                                style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold, height: 1.2),
                              ),
                            ),
                            const SizedBox(width: 8),
                            Container(
                              padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
                              decoration: BoxDecoration(
                                color: statusColor.withOpacity(0.1),
                                borderRadius: BorderRadius.circular(20),
                              ),
                              child: Text(
                                "${zone.availableSeats} chỗ",
                                style: TextStyle(color: statusColor, fontWeight: FontWeight.bold, fontSize: 13),
                              ),
                            )
                          ],
                        ),
                        const SizedBox(height: 8),
                        Text(zone.description, style: const TextStyle(color: AppColors.textGrey, fontSize: 14)),
                        const SizedBox(height: 16),
                        
                        // Tiện ích
                        Wrap(
                          spacing: 8,
                          children: zone.facilities.map((f) => Container(
                            padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
                            decoration: BoxDecoration(
                                color: AppColors.greyLight,
                                borderRadius: BorderRadius.circular(6)
                            ),
                            child: Text(f, style: const TextStyle(fontSize: 11, color: Colors.black87)),
                          )).toList(),
                        )
                      ],
                    ),
                  ),
                ],
              ),
            ),
          );
        },
      ),
    );
  }
}


class SeatSelectionScreen extends StatefulWidget {
  final String zoneName;
  const SeatSelectionScreen({super.key, required this.zoneName});
  @override
  State<SeatSelectionScreen> createState() => _SeatSelectionScreenState();
}

class _SeatSelectionScreenState extends State<SeatSelectionScreen> {
  // 0: Trống, 1: Đã đặt, -1: Lối đi
  List<int> seatMap = [1, 0, 0, -1, 0, 0, 0, 1, 0, -1, 1, 1, 0, 0, 0, -1, 0, 0, -1, -1, -1, -1, -1, -1, 0, 0, 0, -1, 0, 0, 0, 0, 0, -1, 1, 0];
  int? selectedIndex;
  String selectedTime = "09:00 - 11:00";

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.white,
      appBar: AppBar(title: Text(widget.zoneName)),
      body: Column(
        children: [
          // Time Selector
          Container(
            padding: const EdgeInsets.all(16),
            child: SingleChildScrollView(
              scrollDirection: Axis.horizontal,
              child: Row(
                children: ["07:00 - 09:00", "09:00 - 11:00", "13:00 - 15:00"].map((time) => Padding(
                  padding: const EdgeInsets.only(right: 8),
                  child: ChoiceChip(
                    label: Text(time),
                    selected: selectedTime == time,
                    selectedColor: AppColors.brandColor.withOpacity(0.2),
                    labelStyle: TextStyle(color: selectedTime == time ? AppColors.brandColor : Colors.black87, fontWeight: FontWeight.bold),
                    onSelected: (val) => setState(() => selectedTime = time),
                  ),
                )).toList(),
              ),
            ),
          ),
          const Divider(height: 1),
          // Legend
          Padding(
            padding: const EdgeInsets.symmetric(vertical: 16),
            child: Row(mainAxisAlignment: MainAxisAlignment.center, children: [
              _legend(AppColors.seatAvailable, "Trống"), const SizedBox(width: 16),
              _legend(AppColors.seatOccupied, "Đã đặt"), const SizedBox(width: 16),
              _legend(AppColors.brandColor, "Đang chọn"),
            ]),
          ),
          // Grid
          Expanded(
            child: Container(
              color: const Color(0xFFFAFAFA),
              padding: const EdgeInsets.all(20),
              child: GridView.builder(
                itemCount: seatMap.length,
                gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(crossAxisCount: 6, crossAxisSpacing: 10, mainAxisSpacing: 10),
                itemBuilder: (context, index) {
                  int status = seatMap[index];
                  if (status == -1) return const SizedBox();
                  bool isSelected = selectedIndex == index;
                  Color color = status == 1 ? AppColors.seatOccupied : (isSelected ? AppColors.brandColor : AppColors.seatAvailable);
                  return GestureDetector(
                    onTap: status == 1 ? null : () => setState(() => selectedIndex = index),
                    child: Container(
                      decoration: BoxDecoration(color: color, borderRadius: BorderRadius.circular(8), border: isSelected ? Border.all(color: AppColors.brandColor, width: 2) : null),
                      child: Center(child: Text("${String.fromCharCode(65 + (index ~/ 6))}${index % 6 + 1}", style: TextStyle(color: status == 1 || isSelected ? Colors.white : Colors.black54, fontWeight: FontWeight.bold, fontSize: 10))),
                    ),
                  );
                },
              ),
            ),
          ),
          // Footer
          Container(
            padding: const EdgeInsets.all(20),
            decoration: const BoxDecoration(color: Colors.white, boxShadow: [BoxShadow(color: Colors.black12, blurRadius: 10, offset: Offset(0, -5))]),
            child: Row(children: [
              Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, mainAxisSize: MainAxisSize.min, children: [
                const Text("Ghế đã chọn:", style: TextStyle(color: Colors.grey, fontSize: 12)),
                Text(selectedIndex != null ? "${String.fromCharCode(65 + (selectedIndex! ~/ 6))}${selectedIndex! % 6 + 1} • $selectedTime" : "Chưa chọn", style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
              ])),
              ElevatedButton(
                onPressed: selectedIndex == null ? null : () => _showSuccess(),
                style: ElevatedButton.styleFrom(backgroundColor: AppColors.brandColor, foregroundColor: Colors.white, padding: const EdgeInsets.symmetric(horizontal: 30, vertical: 12)),
                child: const Text("XÁC NHẬN"),
              )
            ]),
          )
        ],
      ),
    );
  }

  Widget _legend(Color c, String l) => Row(children: [Container(width: 14, height: 14, decoration: BoxDecoration(color: c, borderRadius: BorderRadius.circular(4))), const SizedBox(width: 4), Text(l, style: const TextStyle(fontSize: 12))]);
  
  void _showSuccess() {
    showDialog(context: context, builder: (_) => AlertDialog(
      title: const Icon(Icons.check_circle, color: AppColors.success, size: 50),
      content: const Text("Đặt chỗ thành công!\nVui lòng check-in trước 15 phút.", textAlign: TextAlign.center),
      actions: [TextButton(onPressed: () { Navigator.pop(context); Navigator.pop(context); }, child: const Text("Đóng"))],
    ));
  }
}
