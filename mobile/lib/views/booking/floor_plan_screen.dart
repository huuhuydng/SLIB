import 'dart:math';
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:slib/assets/colors.dart';
import 'package:slib/models/area.dart';
import 'package:slib/models/area_factory.dart';
import 'package:slib/models/seat.dart';
import 'package:slib/models/zones.dart';
import 'package:slib/services/auth_service.dart';
import 'package:slib/services/booking_service.dart';
import 'package:intl/intl.dart';

/// Màn hình sơ đồ mặt bằng thư viện
class FloorPlanScreen extends StatefulWidget {
  const FloorPlanScreen({super.key});

  @override
  State<FloorPlanScreen> createState() => _FloorPlanScreenState();
}

class _FloorPlanScreenState extends State<FloorPlanScreen> {
  final BookingService _bookingService = BookingService();
  
  List<Area> _areas = [];
  List<Zone> _zones = [];
  List<AreaFactory> _factories = [];
  Map<int, double> _zoneOccupancy = {};
  
  Area? _selectedArea;
  bool _isLoading = true;
  String? _errorMessage;

  // Calculated dimensions
  double _contentWidth = 300;
  double _contentHeight = 400;

  @override
  void initState() {
    super.initState();
    _loadData();
  }

  Future<void> _loadData() async {
    try {
      setState(() {
        _isLoading = true;
        _errorMessage = null;
      });
      
      final areas = await _bookingService.getAllAreas();
      setState(() {
        _areas = areas.where((a) => a.isActive).toList();
        _isLoading = false;
        if (_areas.isNotEmpty) {
          _selectedArea = _areas.first;
          _loadZonesAndFactories();
        }
      });
    } catch (e) {
      setState(() {
        _isLoading = false;
        _errorMessage = 'Không thể tải dữ liệu: $e';
      });
    }
  }

  Future<void> _loadZonesAndFactories() async {
    if (_selectedArea == null) return;
    
    try {
      // Load zones and factories in parallel
      final results = await Future.wait([
        _bookingService.getZonesByArea(_selectedArea!.areaId),
        _bookingService.getFactoriesByArea(_selectedArea!.areaId),
      ]);
      
      final zones = results[0] as List<Zone>;
      final factories = results[1] as List<AreaFactory>;
      
      // Load occupancy for each zone
      Map<int, double> occupancy = {};
      for (final zone in zones) {
        try {
          final seats = await _bookingService.getSeats(zone.zoneId);
          if (seats.isNotEmpty) {
            final bookedCount = seats.where((s) => s.seatStatus != 'AVAILABLE').length;
            occupancy[zone.zoneId] = (bookedCount / seats.length) * 100;
          } else {
            occupancy[zone.zoneId] = 0;
          }
        } catch (e) {
          occupancy[zone.zoneId] = 0;
        }
      }
      
      // Calculate content dimensions based on zones and factories
      _calculateContentSize(zones, factories);
      
      setState(() {
        _zones = zones;
        _factories = factories;
        _zoneOccupancy = occupancy;
      });
    } catch (e) {
      debugPrint('Error loading zones/factories: $e');
    }
  }

  void _calculateContentSize(List<Zone> zones, List<AreaFactory> factories) {
    double maxX = 0;
    double maxY = 0;
    
    for (final zone in zones) {
      final right = zone.positionX + zone.width;
      final bottom = zone.positionY + zone.height;
      if (right > maxX) maxX = right.toDouble();
      if (bottom > maxY) maxY = bottom.toDouble();
    }
    
    for (final factory in factories) {
      final right = factory.positionX + factory.width;
      final bottom = factory.positionY + factory.height;
      if (right > maxX) maxX = right.toDouble();
      if (bottom > maxY) maxY = bottom.toDouble();
    }
    
    // Add padding
    _contentWidth = maxX + 40;
    _contentHeight = maxY + 40;
  }

  Color _getZoneColor(double occupancy) {
    if (occupancy >= 90) {
      return const Color(0xFFE74C3C); // Red
    } else if (occupancy >= 50) {
      return const Color(0xFFF39C12); // Orange
    } else {
      return const Color(0xFF27AE60); // Green
    }
  }

  String _getStatusText(double occupancy) {
    if (occupancy >= 90) {
      return 'Hết chỗ';
    } else if (occupancy >= 50) {
      return 'Khá đông';
    } else {
      return 'Trống';
    }
  }

  void _showLegendDialog() {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        backgroundColor: const Color(0xFFFFF5EE),
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
        title: const Text(
          'Chú thích bản đồ',
          style: TextStyle(fontWeight: FontWeight.bold),
        ),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            _buildLegendRow(const Color(0xFF27AE60), 'Trống', 'Mật độ < 50%'),
            const SizedBox(height: 12),
            _buildLegendRow(const Color(0xFFF39C12), 'Khá đông', 'Mật độ 50-80%'),
            const SizedBox(height: 12),
            _buildLegendRow(const Color(0xFFE74C3C), 'Hết chỗ', 'Mật độ > 90%'),
            const SizedBox(height: 12),
            _buildLegendRow(const Color(0xFFBDC3C7), 'Khu vực không đặt chỗ', ''),
          ],
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: Text('Đóng', style: TextStyle(color: AppColors.brandColor)),
          ),
        ],
      ),
    );
  }

  Widget _buildLegendRow(Color color, String label, String sublabel) {
    return Row(
      children: [
        Container(
          width: 24,
          height: 24,
          decoration: BoxDecoration(
            color: color,
            borderRadius: BorderRadius.circular(6),
          ),
        ),
        const SizedBox(width: 12),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(label, style: const TextStyle(fontWeight: FontWeight.w500)),
              if (sublabel.isNotEmpty)
                Text(sublabel, style: TextStyle(fontSize: 12, color: Colors.grey[600])),
            ],
          ),
        ),
      ],
    );
  }

  void _showZoneBottomSheet(Zone zone) {
    final occupancy = _zoneOccupancy[zone.zoneId] ?? 0;
    final statusText = _getStatusText(occupancy);
    final statusColor = _getZoneColor(occupancy);

    showModalBottomSheet(
      context: context,
      backgroundColor: const Color(0xFFFFF5EE),
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
      ),
      builder: (context) => Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Expanded(
                  child: Text(
                    zone.zoneName,
                    style: const TextStyle(fontSize: 22, fontWeight: FontWeight.bold),
                  ),
                ),
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                  decoration: BoxDecoration(
                    color: statusColor.withAlpha(30),
                    borderRadius: BorderRadius.circular(20),
                    border: Border.all(color: statusColor),
                  ),
                  child: Text(
                    statusText,
                    style: TextStyle(color: statusColor, fontWeight: FontWeight.bold),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 16),
            
            if (zone.description != null && zone.description!.isNotEmpty)
              Padding(
                padding: const EdgeInsets.only(bottom: 16),
                child: Text(zone.description!, style: TextStyle(color: Colors.grey[700])),
              ),
            
            const Text('Tiện ích:', style: TextStyle(fontSize: 14, color: Colors.grey)),
            const SizedBox(height: 8),
            Wrap(
              spacing: 8,
              runSpacing: 8,
              children: [
                _buildAmenityChip('Wifi 5G', Icons.wifi),
                if (zone.hasPowerOutlet) _buildAmenityChip('Ổ cắm', Icons.power),
                _buildAmenityChip('Điều hòa', Icons.ac_unit),
              ],
            ),
            const SizedBox(height: 24),
            
            SizedBox(
              width: double.infinity,
              child: ElevatedButton(
                onPressed: occupancy >= 100 ? null : () {
                  Navigator.pop(context);
                  Navigator.push(
                    context,
                    MaterialPageRoute(
                      builder: (_) => SeatGridScreen(zone: zone, occupancy: occupancy),
                    ),
                  );
                },
                style: ElevatedButton.styleFrom(
                  backgroundColor: AppColors.brandColor,
                  foregroundColor: Colors.white,
                  padding: const EdgeInsets.symmetric(vertical: 16),
                  shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                ),
                child: const Text(
                  'ĐẶT CHỖ TẠI ĐÂY',
                  style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildAmenityChip(String label, IconData icon) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(20),
        border: Border.all(color: Colors.grey[300]!),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(icon, size: 16, color: Colors.grey[600]),
          const SizedBox(width: 4),
          Text(label, style: TextStyle(color: Colors.grey[700])),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFFF5F7FA),
      appBar: AppBar(
        title: const Text('Sơ đồ thư viện', style: TextStyle(fontWeight: FontWeight.bold)),
        backgroundColor: Colors.white,
        surfaceTintColor: Colors.transparent,
        actions: [
          IconButton(
            icon: const Icon(Icons.info_outline),
            onPressed: _showLegendDialog,
          ),
          IconButton(
            icon: const Icon(Icons.refresh),
            onPressed: _loadData,
          ),
        ],
      ),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator())
          : _errorMessage != null
              ? Center(child: Text(_errorMessage!, style: const TextStyle(color: Colors.red)))
              : Column(
                  children: [
                    // Area tabs
                    if (_areas.length > 1) _buildAreaTabs(),
                    // Floor plan
                    Expanded(child: _buildFloorPlan()),
                    // Tip
                    Container(
                      padding: const EdgeInsets.all(12),
                      color: const Color(0xFFFFF5EE),
                      child: const Text(
                        'Mẹo: Chạm vào khu vực màu để xem chi tiết và đặt chỗ',
                        textAlign: TextAlign.center,
                        style: TextStyle(color: Colors.grey, fontSize: 12),
                      ),
                    ),
                  ],
                ),
    );
  }

  Widget _buildAreaTabs() {
    return Container(
      height: 50,
      color: Colors.white,
      child: ListView.builder(
        scrollDirection: Axis.horizontal,
        padding: const EdgeInsets.symmetric(horizontal: 12),
        itemCount: _areas.length,
        itemBuilder: (context, index) {
          final area = _areas[index];
          final isSelected = _selectedArea?.areaId == area.areaId;
          return GestureDetector(
            onTap: () {
              setState(() => _selectedArea = area);
              _loadZonesAndFactories();
            },
            child: Container(
              padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 10),
              margin: const EdgeInsets.symmetric(horizontal: 4, vertical: 8),
              decoration: BoxDecoration(
                color: isSelected ? AppColors.brandColor : Colors.grey[200],
                borderRadius: BorderRadius.circular(20),
              ),
              child: Text(
                area.areaName,
                style: TextStyle(
                  color: isSelected ? Colors.white : Colors.black87,
                  fontWeight: isSelected ? FontWeight.bold : FontWeight.normal,
                ),
              ),
            ),
          );
        },
      ),
    );
  }

  Widget _buildFloorPlan() {
    // Calculate scale to fit screen
    final screenWidth = MediaQuery.of(context).size.width - 32;
    final availableHeight = MediaQuery.of(context).size.height - 250;
    
    final scaleX = screenWidth / _contentWidth;
    final scaleY = availableHeight / _contentHeight;
    final scale = min(scaleX, scaleY).clamp(0.3, 1.5);

    final scaledWidth = _contentWidth * scale;
    final scaledHeight = _contentHeight * scale;

    return InteractiveViewer(
      boundaryMargin: const EdgeInsets.all(20),
      minScale: 0.5,
      maxScale: 3.0,
      child: Center(
        child: Container(
          margin: const EdgeInsets.all(16),
          decoration: BoxDecoration(
            color: Colors.white,
            borderRadius: BorderRadius.circular(16),
            boxShadow: [
              BoxShadow(
                color: Colors.black.withAlpha(15),
                blurRadius: 10,
                offset: const Offset(0, 4),
              ),
            ],
          ),
          child: ClipRRect(
            borderRadius: BorderRadius.circular(16),
            child: CustomPaint(
              painter: _GridPainter(),
              child: SizedBox(
                width: scaledWidth,
                height: scaledHeight,
                child: Stack(
                  children: [
                    // Factories (obstacles) - gray
                    ..._factories.map((f) => _buildFactoryWidget(f, scale)),
                    // Zones - colored by occupancy
                    ..._zones.map((z) => _buildZoneWidget(z, scale)),
                  ],
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildFactoryWidget(AreaFactory factory, double scale) {
    final x = factory.positionX * scale;
    final y = factory.positionY * scale;
    final w = factory.width * scale;
    final h = factory.height * scale;

    return Positioned(
      left: x + 10,
      top: y + 10,
      child: Container(
        width: w,
        height: h,
        decoration: BoxDecoration(
          color: const Color(0xFFBDC3C7), // Gray
          borderRadius: BorderRadius.circular(8),
        ),
        child: Center(
          child: Padding(
            padding: const EdgeInsets.all(4),
            child: Text(
              factory.factoryName,
              style: TextStyle(
                color: Colors.grey[700],
                fontSize: 11,
                fontWeight: FontWeight.w500,
              ),
              textAlign: TextAlign.center,
              overflow: TextOverflow.ellipsis,
              maxLines: 2,
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildZoneWidget(Zone zone, double scale) {
    final occupancy = _zoneOccupancy[zone.zoneId] ?? 0;
    final color = _getZoneColor(occupancy);

    final x = zone.positionX * scale;
    final y = zone.positionY * scale;
    final w = zone.width * scale;
    final h = zone.height * scale;

    return Positioned(
      left: x + 10,
      top: y + 10,
      child: GestureDetector(
        onTap: () => _showZoneBottomSheet(zone),
        child: Container(
          width: w,
          height: h,
          decoration: BoxDecoration(
            color: color,
            borderRadius: BorderRadius.circular(12),
            boxShadow: [
              BoxShadow(
                color: color.withAlpha(80),
                blurRadius: 8,
                offset: const Offset(0, 4),
              ),
            ],
          ),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Padding(
                padding: const EdgeInsets.symmetric(horizontal: 4),
                child: Text(
                  zone.zoneName,
                  style: const TextStyle(
                    color: Colors.white,
                    fontWeight: FontWeight.bold,
                    fontSize: 13,
                  ),
                  textAlign: TextAlign.center,
                  overflow: TextOverflow.ellipsis,
                  maxLines: 2,
                ),
              ),
              const SizedBox(height: 4),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
                decoration: BoxDecoration(
                  color: Colors.white.withAlpha(60),
                  borderRadius: BorderRadius.circular(10),
                ),
                child: Text(
                  '${occupancy.toInt()}%',
                  style: const TextStyle(
                    color: Colors.white,
                    fontSize: 11,
                    fontWeight: FontWeight.w500,
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _GridPainter extends CustomPainter {
  @override
  void paint(Canvas canvas, Size size) {
    final paint = Paint()
      ..color = Colors.grey[200]!
      ..strokeWidth = 0.5;
    
    const gridSize = 20.0;
    
    for (double x = 0; x <= size.width; x += gridSize) {
      canvas.drawLine(Offset(x, 0), Offset(x, size.height), paint);
    }
    for (double y = 0; y <= size.height; y += gridSize) {
      canvas.drawLine(Offset(0, y), Offset(size.width, y), paint);
    }
  }

  @override
  bool shouldRepaint(covariant CustomPainter oldDelegate) => false;
}

/// Màn hình chọn ghế theo grid row/column
class SeatGridScreen extends StatefulWidget {
  final Zone zone;
  final double occupancy;

  const SeatGridScreen({
    super.key,
    required this.zone,
    required this.occupancy,
  });

  @override
  State<SeatGridScreen> createState() => _SeatGridScreenState();
}

class _SeatGridScreenState extends State<SeatGridScreen> {
  final BookingService _bookingService = BookingService();

  List<Seat> _seats = [];
  int? _selectedIndex;
  DateTime? _selectedDate;
  String? _selectedTime;
  bool _isLoading = true;

  final List<String> _timeSlots = [
    "07:00 - 09:00",
    "09:00 - 11:00",
    "13:00 - 15:00",
    "15:00 - 17:00",
  ];

  @override
  void initState() {
    super.initState();
    _loadSeats();
  }

  Future<void> _loadSeats() async {
    setState(() => _isLoading = true);
    try {
      final seats = await _bookingService.getSeats(widget.zone.zoneId);
      seats.sort((a, b) {
        if (a.rowNumber != b.rowNumber) return a.rowNumber.compareTo(b.rowNumber);
        return a.columnNumber.compareTo(b.columnNumber);
      });
      setState(() {
        _seats = seats;
        _isLoading = false;
      });
    } catch (e) {
      setState(() => _isLoading = false);
    }
  }

  Future<void> _loadSeatsByTime() async {
    if (_selectedDate == null || _selectedTime == null) return;

    setState(() => _isLoading = true);
    try {
      final parts = _selectedTime!.split(' - ');
      final seats = await _bookingService.getSeatsByTime(
        widget.zone.zoneId,
        _selectedDate!,
        parts[0],
        parts[1],
      );
      seats.sort((a, b) {
        if (a.rowNumber != b.rowNumber) return a.rowNumber.compareTo(b.rowNumber);
        return a.columnNumber.compareTo(b.columnNumber);
      });
      setState(() {
        _seats = seats;
        _isLoading = false;
        _selectedIndex = null;
      });
    } catch (e) {
      setState(() => _isLoading = false);
    }
  }

  Future<void> _confirmBooking() async {
    if (_selectedIndex == null || _selectedTime == null || _selectedDate == null) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Vui lòng chọn ngày, giờ và ghế')),
      );
      return;
    }

    final authService = Provider.of<AuthService>(context, listen: false);
    final userId = authService.currentUser?.id;
    if (userId == null) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Vui lòng đăng nhập')),
      );
      return;
    }

    final parts = _selectedTime!.split(' - ');
    try {
      await _bookingService.createBooking(
        userId: userId,
        seatId: _seats[_selectedIndex!].seatId,
        date: _selectedDate!,
        start: parts[0],
        end: parts[1],
      );

      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('Đặt ghế ${_seats[_selectedIndex!].seatCode} thành công!'),
            backgroundColor: AppColors.success,
          ),
        );
        Navigator.pop(context);
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Lỗi: $e'), backgroundColor: AppColors.error),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.white,
      appBar: AppBar(
        title: Text(widget.zone.zoneName),
        backgroundColor: Colors.white,
        surfaceTintColor: Colors.transparent,
      ),
      body: Column(
        children: [
          Padding(
            padding: const EdgeInsets.all(16),
            child: ElevatedButton.icon(
              icon: const Icon(Icons.date_range),
              label: Text(
                _selectedDate != null
                    ? DateFormat('dd/MM/yyyy').format(_selectedDate!)
                    : 'Chọn ngày đặt',
              ),
              style: ElevatedButton.styleFrom(
                backgroundColor: Colors.amber[800],
                foregroundColor: Colors.white,
                padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 12),
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
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
                    _selectedDate = picked;
                    _selectedTime = null;
                    _selectedIndex = null;
                  });
                }
              },
            ),
          ),

          Container(
            padding: const EdgeInsets.symmetric(horizontal: 16),
            child: SingleChildScrollView(
              scrollDirection: Axis.horizontal,
              child: Row(
                children: _timeSlots.map((slot) {
                  final isSelected = _selectedTime == slot;
                  return Padding(
                    padding: const EdgeInsets.only(right: 8),
                    child: ChoiceChip(
                      label: Text(slot),
                      selected: isSelected,
                      selectedColor: Colors.orange.withAlpha(80),
                      backgroundColor: Colors.orange.shade50,
                      labelStyle: TextStyle(
                        color: isSelected ? Colors.orange : Colors.black87,
                        fontWeight: FontWeight.bold,
                      ),
                      onSelected: (_) {
                        setState(() => _selectedTime = slot);
                        _loadSeatsByTime();
                      },
                    ),
                  );
                }).toList(),
              ),
            ),
          ),

          const Divider(height: 32),

          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 16),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                _legend(AppColors.seatAvailable, 'Trống'),
                const SizedBox(width: 16),
                _legend(AppColors.seatOccupied, 'Đã đặt'),
                const SizedBox(width: 16),
                _legend(AppColors.brandColor, 'Đang chọn'),
              ],
            ),
          ),

          const SizedBox(height: 16),

          Expanded(
            child: _isLoading
                ? const Center(child: CircularProgressIndicator())
                : _seats.isEmpty
                    ? const Center(child: Text('Không có ghế'))
                    : _buildSeatGrid(),
          ),

          _buildFooter(),
        ],
      ),
    );
  }

  Widget _buildSeatGrid() {
    final Map<int, List<Seat>> seatsByRow = {};
    for (final seat in _seats) {
      seatsByRow.putIfAbsent(seat.rowNumber, () => []).add(seat);
    }
    final sortedRows = seatsByRow.keys.toList()..sort();

    // Find max seats per row for display
    int maxSeatsPerRow = 0;
    for (final row in seatsByRow.values) {
      if (row.length > maxSeatsPerRow) maxSeatsPerRow = row.length;
    }

    return ListView.builder(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
      itemCount: sortedRows.length,
      itemBuilder: (context, index) {
        final rowNum = sortedRows[index];
        final rowSeats = seatsByRow[rowNum]!;
        final rowLetter = String.fromCharCode(64 + rowNum);

        return Padding(
          padding: const EdgeInsets.only(bottom: 16),
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.center,
            children: [
              // Row label with seat count
              Container(
                width: 40,
                child: Column(
                  children: [
                    Text(
                      rowLetter,
                      style: const TextStyle(
                        fontWeight: FontWeight.bold,
                        fontSize: 18,
                        color: Colors.black87,
                      ),
                    ),
                    Text(
                      '${rowSeats.length}',
                      style: TextStyle(
                        fontSize: 10,
                        color: Colors.grey[500],
                      ),
                    ),
                  ],
                ),
              ),
              // Horizontal scrollable seats
              Expanded(
                child: SingleChildScrollView(
                  scrollDirection: Axis.horizontal,
                  child: Row(
                    children: rowSeats.map((seat) {
                      final globalIndex = _seats.indexOf(seat);
                      final isSelected = _selectedIndex == globalIndex;
                      final isAvailable = seat.seatStatus == 'AVAILABLE';
                      
                      final color = isSelected
                          ? AppColors.brandColor
                          : isAvailable
                              ? AppColors.seatAvailable
                              : AppColors.seatOccupied;

                      return Padding(
                        padding: const EdgeInsets.only(right: 8),
                        child: GestureDetector(
                          onTap: (isAvailable && _selectedTime != null)
                              ? () => setState(() => _selectedIndex = globalIndex)
                              : null,
                          child: AnimatedContainer(
                            duration: const Duration(milliseconds: 200),
                            width: 48,
                            height: 48,
                            decoration: BoxDecoration(
                              color: color,
                              borderRadius: BorderRadius.circular(10),
                              border: isSelected
                                  ? Border.all(color: Colors.white, width: 2)
                                  : null,
                              boxShadow: isSelected
                                  ? [
                                      BoxShadow(
                                        color: AppColors.brandColor.withAlpha(100),
                                        blurRadius: 8,
                                        spreadRadius: 2,
                                      ),
                                    ]
                                  : null,
                            ),
                            child: Center(
                              child: Text(
                                '${seat.columnNumber}',
                                style: TextStyle(
                                  color: (isSelected || !isAvailable) ? Colors.white : Colors.black54,
                                  fontWeight: FontWeight.bold,
                                  fontSize: 14,
                                ),
                              ),
                            ),
                          ),
                        ),
                      );
                    }).toList(),
                  ),
                ),
              ),
              // Scroll indicator if many seats
              if (rowSeats.length > 6)
                Icon(Icons.chevron_right, color: Colors.grey[400], size: 20),
            ],
          ),
        );
      },
    );
  }

  Widget _legend(Color c, String l) => Row(
    children: [
      Container(
        width: 14,
        height: 14,
        decoration: BoxDecoration(color: c, borderRadius: BorderRadius.circular(4)),
      ),
      const SizedBox(width: 4),
      Text(l, style: const TextStyle(fontSize: 12)),
    ],
  );

  Widget _buildFooter() {
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: const BoxDecoration(
        color: Colors.white,
        boxShadow: [BoxShadow(color: Colors.black12, blurRadius: 10, offset: Offset(0, -5))],
      ),
      child: Row(
        children: [
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              mainAxisSize: MainAxisSize.min,
              children: [
                const Text('Ghế đã chọn:', style: TextStyle(color: Colors.grey, fontSize: 12)),
                Text(
                  _selectedIndex != null
                      ? '${_seats[_selectedIndex!].seatCode} • ${_selectedTime ?? "Chọn giờ"}'
                      : 'Chưa chọn',
                  style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16),
                ),
              ],
            ),
          ),
          ElevatedButton(
            onPressed: (_selectedIndex != null && _selectedTime != null && _selectedDate != null)
                ? _confirmBooking
                : null,
            style: ElevatedButton.styleFrom(
              backgroundColor: AppColors.brandColor,
              foregroundColor: Colors.white,
              padding: const EdgeInsets.symmetric(horizontal: 30, vertical: 12),
              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
            ),
            child: const Text('XÁC NHẬN'),
          ),
        ],
      ),
    );
  }
}
