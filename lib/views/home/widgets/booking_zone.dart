import 'dart:async';

import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:slib/assets/colors.dart';
import 'package:slib/models/seat.dart';
import 'package:slib/models/zones.dart';
import 'package:slib/services/auth_service.dart';
import 'package:slib/services/booking_service.dart';
import 'package:intl/intl.dart';
import 'package:slib/views/home/widgets/booking_confirm_screen.dart';

class BookingZoneScreen extends StatelessWidget {
  final List<Zones> zones;
  const BookingZoneScreen({super.key, required this.zones});

  @override
  Widget build(BuildContext context) {
    final bookingService = Provider.of<BookingService>(context, listen: false);

    return Scaffold(
      appBar: AppBar(
        title: const Text(
          "Chọn không gian",
          style: TextStyle(fontWeight: FontWeight.bold),
        ),
        backgroundColor: Colors.white,
        surfaceTintColor: Colors.transparent,
      ),
      body: zones.isEmpty
          ? const Center(child: Text("Không có dữ liệu zones"))
          : ListView.builder(
              padding: const EdgeInsets.all(16),
              itemCount: zones.length,
              itemBuilder: (context, index) {
                final zone = zones[index];

                return FutureBuilder<int>(
                  future: bookingService.getAvailableSeat(zone.id),
                  builder: (context, snapshot) {
                    if (snapshot.connectionState == ConnectionState.waiting) {
                      return const Center(child: CircularProgressIndicator());
                    }
                    if (snapshot.hasError) {
                      return Text("Lỗi: ${snapshot.error}");
                    }

                    final availableSeats = snapshot.data ?? 0;
                    final statusColor = availableSeats > 0
                        ? AppColors.success
                        : AppColors.error;

                    return GestureDetector(
                      onTap: () {
                        Navigator.push(
                          context,
                          MaterialPageRoute(
                            builder: (_) => SeatSelectionScreen(
                              zoneName: zone.name,
                              zoneId: zone.id,
                            ),
                          ),
                        );
                      },
                      child: Container(
                        margin: const EdgeInsets.only(bottom: 24),
                        decoration: BoxDecoration(
                          color: Colors.white,
                          borderRadius: BorderRadius.circular(20),
                          boxShadow: [
                            BoxShadow(
                              color: Colors.black.withOpacity(0.06),
                              blurRadius: 15,
                              offset: const Offset(0, 6),
                            ),
                          ],
                        ),
                        child: Column(
                          children: [
                            ClipRRect(
                              borderRadius: const BorderRadius.vertical(
                                top: Radius.circular(20),
                              ),
                              child: Image.network(
                                "https://picsum.photos/600/150?random=$index",
                                height: 150,
                                width: double.infinity,
                                fit: BoxFit.cover,
                              ),
                            ),
                            Padding(
                              padding: const EdgeInsets.all(20),
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  Row(
                                    mainAxisAlignment:
                                        MainAxisAlignment.spaceBetween,
                                    children: [
                                      Expanded(
                                        child: Text(
                                          zone.name,
                                          style: const TextStyle(
                                            fontSize: 18,
                                            fontWeight: FontWeight.bold,
                                          ),
                                        ),
                                      ),
                                      Container(
                                        padding: const EdgeInsets.symmetric(
                                          horizontal: 10,
                                          vertical: 6,
                                        ),
                                        decoration: BoxDecoration(
                                          color: statusColor.withOpacity(0.1),
                                          borderRadius: BorderRadius.circular(
                                            20,
                                          ),
                                        ),
                                        child: Text(
                                          "Còn $availableSeats chỗ",
                                          style: TextStyle(
                                            color: statusColor,
                                            fontWeight: FontWeight.bold,
                                            fontSize: 13,
                                          ),
                                        ),
                                      ),
                                    ],
                                  ),
                                  const SizedBox(height: 8),
                                  Text(
                                    zone.description,
                                    style: const TextStyle(
                                      color: AppColors.textGrey,
                                      fontSize: 14,
                                    ),
                                  ),
                                  const SizedBox(height: 16),
                                  Row(
                                    children: [
                                      Icon(
                                        Icons.power,
                                        size: 16,
                                        color: zone.hasPowerOutlet
                                            ? Colors.green
                                            : Colors.red,
                                      ),
                                      const SizedBox(width: 6),
                                      Text(
                                        zone.hasPowerOutlet
                                            ? "Có ổ cắm điện"
                                            : "Không có ổ cắm điện",
                                      ),
                                    ],
                                  ),
                                ],
                              ),
                            ),
                          ],
                        ),
                      ),
                    );
                  },
                );
              },
            ),
    );
  }
}

class SeatSelectionScreen extends StatefulWidget {
  final String zoneName;
  final int zoneId;
  const SeatSelectionScreen({
    super.key,
    required this.zoneName,
    required this.zoneId,
  });
  @override
  State<SeatSelectionScreen> createState() => _SeatSelectionScreenState();
}

class _SeatSelectionScreenState extends State<SeatSelectionScreen> {
  DateTime? selectedDate;
  List<Seat> seats = [];
  int? selectedIndex;
  String? selectedTime;

  final List<String> timeSlots = [
    "07:00 - 09:00",
    "09:00 - 11:00",
    "13:00 - 15:00",
    "15:00 - 17:00",
  ];

  int availableSeats = 0;
  Timer? _timer;

  @override
  void initState() {
    super.initState();
    final bookingService = Provider.of<BookingService>(context, listen: false);
    final today = DateTime.now();
    bookingService.getSeatsByDate(widget.zoneId, today).then((data) {
      setState(() {
        seats = data;
      });
    });

    _fetchAvailableSeats();

    _timer = Timer.periodic(const Duration(seconds: 30), (timer) {
      _fetchAvailableSeats();
    });
  }

  void _fetchAvailableSeats() async {
    final bookingService = Provider.of<BookingService>(context, listen: false);
    final count = await bookingService.getAvailableSeat(widget.zoneId);
    setState(() {
      availableSeats = count;
    });
  }

  @override
  void dispose() {
    _timer?.cancel();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final now = DateTime.now();
    final todayString = DateFormat('dd/MM/yyyy').format(now);

    return Scaffold(
      backgroundColor: Colors.white,
      appBar: AppBar(title: Text(widget.zoneName)),
      body: Column(
        children: [
          Padding(
            padding: const EdgeInsets.all(16),
            child: ElevatedButton.icon(
              icon: const Icon(Icons.date_range),
              label: Text(
                selectedDate != null
                    ? DateFormat('dd/MM/yyyy').format(selectedDate!)
                    : "Chọn ngày đặt",
                style: const TextStyle(color: Colors.white),
              ),
              style: ElevatedButton.styleFrom(
                backgroundColor: Colors.amber[800],
                foregroundColor: Colors.white,
                padding: const EdgeInsets.symmetric(
                  horizontal: 20,
                  vertical: 12,
                ),
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(8),
                ),
              ),
              onPressed: () async {
                final now = DateTime.now();
                final picked = await showDatePicker(
                  context: context,
                  initialDate: now,
                  firstDate: now,
                  lastDate: now.add(const Duration(days: 30)),
                );
                if (picked != null) {
                  setState(() {
                    selectedDate = picked;
                    selectedTime = null;
                    selectedIndex = null;
                  });
                }
              },
            ),
          ),

          Container(
            padding: const EdgeInsets.all(16),
            child: SingleChildScrollView(
              scrollDirection: Axis.horizontal,
              child: Row(
                children: timeSlots.map((slot) {
                  final parts = slot.split(" - ");
                  final startParts = parts[0].split(":");
                  final endParts = parts[1].split(":");

                  final baseDate = selectedDate ?? now;
                  final startTime = DateTime(
                    baseDate.year,
                    baseDate.month,
                    baseDate.day,
                    int.parse(startParts[0]),
                    int.parse(startParts[1]),
                  );
                  final endTime = DateTime(
                    baseDate.year,
                    baseDate.month,
                    baseDate.day,
                    int.parse(endParts[0]),
                    int.parse(endParts[1]),
                  );
                  final isPast = selectedDate == null
                      ? now.isAfter(endTime)
                      : (selectedDate!.isAtSameMomentAs(
                              DateTime(now.year, now.month, now.day),
                            )
                            ? now.isAfter(endTime)
                            : false);

                  return Padding(
                    padding: const EdgeInsets.only(right: 8),
                    child: ChoiceChip(
                      label: Text(slot),
                      selected: selectedTime == slot,
                      selectedColor: Colors.orange.withOpacity(0.3),
                      backgroundColor: Colors.orange.shade50,
                      labelStyle: TextStyle(
                        color: isPast
                            ? Colors.grey
                            : (selectedTime == slot
                                  ? Colors.orange
                                  : Colors.black87),
                        fontWeight: FontWeight.bold,
                      ),
                      onSelected: isPast
                          ? null
                          : (val) async {
                              setState(() => selectedTime = slot);

                              if (selectedDate != null) {
                                final bookingService =
                                    Provider.of<BookingService>(
                                      context,
                                      listen: false,
                                    );

                                final parts = slot.split(" - ");
                                final start = parts[0]; // "07:00"
                                final end = parts[1]; // "09:00"

                                final data = await bookingService
                                    .getSeatsByTime(
                                      widget.zoneId,
                                      selectedDate!,
                                      start,
                                      end,
                                    );

                                setState(() {
                                  seats = data;
                                });
                              }
                            },
                    ),
                  );
                }).toList(),
              ),
            ),
          ),
          const Divider(height: 1),
          // Legend
          Padding(
            padding: const EdgeInsets.symmetric(vertical: 16),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                _legend(AppColors.seatAvailable, "Trống"),
                const SizedBox(width: 16),
                _legend(AppColors.seatOccupied, "Đã đặt"),
                const SizedBox(width: 16),
                _legend(AppColors.brandColor, "Đang chọn"),
              ],
            ),
          ),
          Expanded(
            child: Container(
              color: const Color(0xFFFAFAFA),
              padding: const EdgeInsets.all(20),
              child:
                  (selectedDate != null &&
                      (selectedDate!.weekday == DateTime.saturday ||
                          selectedDate!.weekday == DateTime.sunday))
                  ? const Center(
                      child: Text(
                        "Thư viện ko hoạt động\nvào khung giờ này",
                        style: TextStyle(
                          fontSize: 16,
                          fontWeight: FontWeight.bold,
                          color: Colors.redAccent,
                        ),
                        textAlign: TextAlign.center,
                      ),
                    )
                  : (seats.isEmpty
                        ? const Center(child: CircularProgressIndicator())
                        : GridView.builder(
                            itemCount: seats.length,
                            gridDelegate:
                                const SliverGridDelegateWithFixedCrossAxisCount(
                                  crossAxisCount: 6,
                                  crossAxisSpacing: 10,
                                  mainAxisSpacing: 10,
                                ),
                            itemBuilder: (context, index) {
                              final seat = seats[index];
                              final isSelected = selectedIndex == index;
                              final color = seat.seatStatus == 'AVAILABLE'
                                  ? (isSelected
                                        ? AppColors.brandColor
                                        : AppColors.seatAvailable)
                                  : (seat.seatStatus == 'BOOKED'
                                        ? AppColors.seatOccupied
                                        : Colors.grey);

                              return GestureDetector(
                                onTap:
                                    (seat.seatStatus == 'AVAILABLE' &&
                                        selectedTime != null)
                                    ? () =>
                                          setState(() => selectedIndex = index)
                                    : null,
                                child: Container(
                                  decoration: BoxDecoration(
                                    color: color,
                                    borderRadius: BorderRadius.circular(8),
                                    border: isSelected
                                        ? Border.all(
                                            color: AppColors.brandColor,
                                            width: 2,
                                          )
                                        : null,
                                  ),
                                  child: Center(
                                    child: Text(
                                      seat.seatCode,
                                      style: TextStyle(
                                        color:
                                            (seat.seatStatus != 'AVAILABLE' ||
                                                isSelected)
                                            ? Colors.white
                                            : Colors.black54,
                                        fontWeight: FontWeight.bold,
                                        fontSize: 10,
                                      ),
                                    ),
                                  ),
                                ),
                              );
                            },
                          )),
            ),
          ),

          // Footer
          Container(
            padding: const EdgeInsets.all(20),
            decoration: const BoxDecoration(
              color: Colors.white,
              boxShadow: [
                BoxShadow(
                  color: Colors.black12,
                  blurRadius: 10,
                  offset: Offset(0, -5),
                ),
              ],
            ),
            child: Row(
              children: [
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      const Text(
                        "Ghế đã chọn:",
                        style: TextStyle(color: Colors.grey, fontSize: 12),
                      ),
                      Text(
                        selectedIndex != null
                            ? (selectedTime != null
                                  ? "${seats[selectedIndex!].seatCode} • $selectedTime"
                                  : "${seats[selectedIndex!].seatCode} • Chọn thời gian")
                            : "Chưa chọn",
                        style: const TextStyle(
                          fontWeight: FontWeight.bold,
                          fontSize: 16,
                        ),
                      ),
                    ],
                  ),
                ),
                ElevatedButton(
                  onPressed:
                      (selectedIndex != null &&
                          selectedTime != null &&
                          selectedDate != null)
                      ? () async {
                          final bookingService = Provider.of<BookingService>(
                            context,
                            listen: false,
                          );
                          final authService = Provider.of<AuthService>(
                            context,
                            listen: false,
                          );
                          final currentUserId = authService.currentUser?.id;
                          if (currentUserId == null) {
                            ScaffoldMessenger.of(context).showSnackBar(
                              const SnackBar(
                                content: Text("Vui lòng đăng nhập"),
                              ),
                            );
                            return;
                          }

                          final parts = selectedTime!.split(" - ");
                          final start = parts[0];
                          final end = parts[1];

                          try {
                            final reserv = await bookingService.createBooking(
                              userId: currentUserId,
                              seatId: seats[selectedIndex!].seatId,
                              date: selectedDate!,
                              start: start,
                              end: end,
                            );
                            final reservationId = reserv["reservationId"];
                            _showSuccess(reservationId);
                          } catch (e) {
                            ScaffoldMessenger.of(context).showSnackBar(
                              SnackBar(content: Text("Đặt chỗ thất bại: $e")),
                            );
                          }
                        }
                      : null,
                  style: ElevatedButton.styleFrom(
                    backgroundColor: AppColors.brandColor,
                    foregroundColor: Colors.white,
                    padding: const EdgeInsets.symmetric(
                      horizontal: 30,
                      vertical: 12,
                    ),
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(8),
                    ),
                  ),
                  child: const Text("XÁC NHẬN"),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _legend(Color c, String l) => Row(
    children: [
      Container(
        width: 14,
        height: 14,
        decoration: BoxDecoration(
          color: c,
          borderRadius: BorderRadius.circular(4),
        ),
      ),
      const SizedBox(width: 4),
      Text(l, style: const TextStyle(fontSize: 12)),
    ],
  );

  void _showSuccess(String reservationId) {
    final authService = Provider.of<AuthService>(context, listen: false);

    showDialog(
      context: context,
      barrierDismissible: false,
      builder: (_) => AlertDialog(
        title: const Icon(
          Icons.check_circle,
          color: AppColors.success,
          size: 50,
        ),
        content: const Text(
          "Đang chuyển hướng sang màn hình đặt...",
          textAlign: TextAlign.center,
        ),
      ),
    );

    Future.delayed(const Duration(seconds: 3), () {
      Navigator.of(context, rootNavigator: true).pop();
      Navigator.push(
        context,
        MaterialPageRoute(
          builder: (_) => BookingConfirmScreen(
            seat: seats[selectedIndex!],
            date: selectedDate!,
            timeSlot: selectedTime!,
            zoneName: widget.zoneName,
            reservationId: reservationId,
            userId: authService.currentUser!.id,
          ),
        ),
      );
    });
  }
}
