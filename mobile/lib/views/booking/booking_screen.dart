import 'package:flutter/material.dart';
import 'package:slib/assets/colors.dart';
import 'package:slib/models/area.dart';
import 'package:slib/models/seat.dart';
import 'package:slib/models/zones.dart';
import 'package:slib/services/booking_service.dart';
import 'package:intl/intl.dart';

/// Màn hình đặt chỗ với sơ đồ trực quan
class BookingScreen extends StatefulWidget {
  const BookingScreen({super.key});

  @override
  State<BookingScreen> createState() => _BookingScreenState();
}

class _BookingScreenState extends State<BookingScreen> {
  final BookingService _bookingService = BookingService();

  // Data
  List<Area> _areas = [];
  List<Zone> _zones = [];
  List<Seat> _seats = [];

  // Selected items
  Area? _selectedArea;
  Zone? _selectedZone;
  Seat? _selectedSeat;

  // Date/time
  DateTime _selectedDate = DateTime.now();
  TimeOfDay _startTime = const TimeOfDay(hour: 8, minute: 0);
  TimeOfDay _endTime = const TimeOfDay(hour: 10, minute: 0);

  // Loading states
  bool _isLoading = true;
  bool _isLoadingSeats = false;
  String? _errorMessage;

  // View mode: 'map' or 'seats'
  String _viewMode = 'map';

  @override
  void initState() {
    super.initState();
    _loadAreas();
  }

  Future<void> _loadAreas() async {
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
          _loadZones();
        }
      });
    } catch (e) {
      setState(() {
        _isLoading = false;
        _errorMessage = 'Không thể tải danh sách khu vực: $e';
      });
    }
  }

  Future<void> _loadZones() async {
    if (_selectedArea == null) return;

    try {
      final zones = await _bookingService.getZonesByArea(_selectedArea!.areaId);
      setState(() {
        _zones = zones;
        _selectedZone = null;
        _seats = [];
      });
    } catch (e) {
      debugPrint('Error loading zones: $e');
    }
  }

  Future<void> _loadSeats() async {
    if (_selectedZone == null) return;

    try {
      setState(() {
        _isLoadingSeats = true;
        _errorMessage = null;
      });

      final startStr = '${_startTime.hour.toString().padLeft(2, '0')}:${_startTime.minute.toString().padLeft(2, '0')}';
      final endStr = '${_endTime.hour.toString().padLeft(2, '0')}:${_endTime.minute.toString().padLeft(2, '0')}';

      final seats = await _bookingService.getSeatsByTime(
        _selectedZone!.zoneId,
        _selectedDate,
        startStr,
        endStr,
      );

      // Sort seats by row and column
      seats.sort((a, b) {
        if (a.rowNumber != b.rowNumber) {
          return a.rowNumber.compareTo(b.rowNumber);
        }
        return a.columnNumber.compareTo(b.columnNumber);
      });

      setState(() {
        _seats = seats;
        _isLoadingSeats = false;
        _selectedSeat = null;
        _viewMode = 'seats';
      });
    } catch (e) {
      setState(() {
        _isLoadingSeats = false;
        _errorMessage = 'Không thể tải danh sách ghế: $e';
      });
    }
  }

  void _onZoneTap(Zone zone) {
    setState(() {
      _selectedZone = zone;
    });
    _loadSeats();
  }

  void _onSeatTap(Seat seat) {
    if (!seat.isAvailable) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Ghế này đã được đặt')),
      );
      return;
    }
    setState(() {
      _selectedSeat = seat;
    });
  }

  Future<void> _selectDate() async {
    final picked = await showDatePicker(
      context: context,
      initialDate: _selectedDate,
      firstDate: DateTime.now(),
      lastDate: DateTime.now().add(const Duration(days: 30)),
    );
    if (picked != null && picked != _selectedDate) {
      setState(() => _selectedDate = picked);
      if (_selectedZone != null) _loadSeats();
    }
  }

  Future<void> _selectStartTime() async {
    final picked = await showTimePicker(
      context: context,
      initialTime: _startTime,
    );
    if (picked != null && picked != _startTime) {
      setState(() => _startTime = picked);
      if (_selectedZone != null) _loadSeats();
    }
  }

  Future<void> _selectEndTime() async {
    final picked = await showTimePicker(
      context: context,
      initialTime: _endTime,
    );
    if (picked != null && picked != _endTime) {
      setState(() => _endTime = picked);
      if (_selectedZone != null) _loadSeats();
    }
  }

  Future<void> _confirmBooking() async {
    if (_selectedSeat == null) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Vui lòng chọn ghế trước')),
      );
      return;
    }

    // TODO: Get actual userId from auth service
    const userId = 'test-user-id';

    final startStr = '${_startTime.hour.toString().padLeft(2, '0')}:${_startTime.minute.toString().padLeft(2, '0')}';
    final endStr = '${_endTime.hour.toString().padLeft(2, '0')}:${_endTime.minute.toString().padLeft(2, '0')}';

    try {
      await _bookingService.createBooking(
        userId: userId,
        seatId: _selectedSeat!.seatId,
        date: _selectedDate,
        start: startStr,
        end: endStr,
      );

      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Đặt ghế ${_selectedSeat!.seatCode} thành công!')),
        );
        _loadSeats();
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Lỗi đặt ghế: $e')),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(_viewMode == 'map' ? 'Chọn khu vực' : 'Chọn ghế'),
        backgroundColor: AppColors.brandColor,
        foregroundColor: Colors.white,
        leading: _viewMode == 'seats'
            ? IconButton(
                icon: const Icon(Icons.arrow_back),
                onPressed: () => setState(() => _viewMode = 'map'),
              )
            : null,
      ),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator())
          : _errorMessage != null
              ? Center(child: Text(_errorMessage!, style: const TextStyle(color: Colors.red)))
              : Column(
                  children: [
                    // Date & time selector (always visible)
                    _buildDateTimeSelector(),
                    // Main content
                    Expanded(
                      child: _viewMode == 'map'
                          ? _buildFloorPlanView()
                          : _buildSeatGridView(),
                    ),
                    // Booking button
                    if (_selectedSeat != null && _viewMode == 'seats') _buildBookingButton(),
                  ],
                ),
    );
  }

  Widget _buildDateTimeSelector() {
    final dateStr = DateFormat('dd/MM/yyyy').format(_selectedDate);
    final startStr = _startTime.format(context);
    final endStr = _endTime.format(context);

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
      color: Colors.grey[100],
      child: Row(
        children: [
          Expanded(
            child: OutlinedButton.icon(
              icon: const Icon(Icons.calendar_today, size: 16),
              label: Text(dateStr, style: const TextStyle(fontSize: 12)),
              onPressed: _selectDate,
            ),
          ),
          const SizedBox(width: 4),
          Expanded(
            child: OutlinedButton.icon(
              icon: const Icon(Icons.access_time, size: 16),
              label: Text(startStr, style: const TextStyle(fontSize: 12)),
              onPressed: _selectStartTime,
            ),
          ),
          const Padding(
            padding: EdgeInsets.symmetric(horizontal: 4),
            child: Text('-'),
          ),
          Expanded(
            child: OutlinedButton.icon(
              icon: const Icon(Icons.access_time, size: 16),
              label: Text(endStr, style: const TextStyle(fontSize: 12)),
              onPressed: _selectEndTime,
            ),
          ),
        ],
      ),
    );
  }

  /// Sơ đồ mặt bằng hiển thị các Area và Zone
  Widget _buildFloorPlanView() {
    return Column(
      children: [
        // Area tabs
        if (_areas.length > 1)
          Container(
            height: 50,
            color: Colors.grey[200],
            child: ListView.builder(
              scrollDirection: Axis.horizontal,
              itemCount: _areas.length,
              itemBuilder: (context, index) {
                final area = _areas[index];
                final isSelected = _selectedArea?.areaId == area.areaId;
                return GestureDetector(
                  onTap: () {
                    setState(() => _selectedArea = area);
                    _loadZones();
                  },
                  child: Container(
                    padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 12),
                    decoration: BoxDecoration(
                      color: isSelected ? AppColors.brandColor : Colors.transparent,
                      borderRadius: BorderRadius.circular(8),
                    ),
                    margin: const EdgeInsets.symmetric(horizontal: 4, vertical: 6),
                    child: Text(
                      area.areaName,
                      style: TextStyle(
                        color: isSelected ? Colors.white : Colors.black,
                        fontWeight: isSelected ? FontWeight.bold : FontWeight.normal,
                      ),
                    ),
                  ),
                );
              },
            ),
          ),
        // Zone layout
        Expanded(
          child: _zones.isEmpty
              ? const Center(child: Text('Không có zone trong khu vực này'))
              : InteractiveViewer(
                  boundaryMargin: const EdgeInsets.all(100),
                  minScale: 0.5,
                  maxScale: 3.0,
                  child: Stack(
                    children: [
                      // Background
                      Container(
                        width: 600,
                        height: 500,
                        color: Colors.grey[100],
                      ),
                      // Zones
                      ..._zones.map((zone) => _buildZoneBox(zone)),
                    ],
                  ),
                ),
        ),
        // Instructions
        Container(
          padding: const EdgeInsets.all(12),
          color: Colors.blue[50],
          child: const Row(
            children: [
              Icon(Icons.touch_app, color: Colors.blue),
              SizedBox(width: 8),
              Text('Chạm vào vùng để xem và đặt ghế', style: TextStyle(color: Colors.blue)),
            ],
          ),
        ),
      ],
    );
  }

  Widget _buildZoneBox(Zone zone) {
    final isSelected = _selectedZone?.zoneId == zone.zoneId;

    // Parse color if provided
    Color zoneColor = Colors.blue;
    if (zone.color != null && zone.color!.isNotEmpty) {
      try {
        final colorStr = zone.color!.replaceFirst('#', '');
        zoneColor = Color(int.parse('FF$colorStr', radix: 16));
      } catch (e) {
        // Use default color
      }
    }

    return Positioned(
      left: zone.positionX.toDouble(),
      top: zone.positionY.toDouble(),
      child: GestureDetector(
        onTap: () => _onZoneTap(zone),
        child: Container(
          width: zone.width.toDouble(),
          height: zone.height.toDouble(),
          decoration: BoxDecoration(
            color: zoneColor.withOpacity(isSelected ? 0.8 : 0.3),
            border: Border.all(
              color: isSelected ? Colors.black : zoneColor,
              width: isSelected ? 3 : 2,
            ),
            borderRadius: BorderRadius.circular(8),
          ),
          child: Stack(
            children: [
              // Zone name
              Positioned(
                left: 8,
                top: 8,
                right: 8,
                child: Text(
                  zone.zoneName,
                  style: TextStyle(
                    color: isSelected ? Colors.white : Colors.black87,
                    fontWeight: FontWeight.bold,
                    fontSize: 14,
                  ),
                  overflow: TextOverflow.ellipsis,
                ),
              ),
              // Power outlet indicator
              if (zone.hasPowerOutlet)
                const Positioned(
                  right: 8,
                  top: 8,
                  child: Icon(Icons.power, size: 18, color: Colors.green),
                ),
              // Tap indicator
              const Center(
                child: Icon(Icons.touch_app, size: 32, color: Colors.white54),
              ),
            ],
          ),
        ),
      ),
    );
  }

  /// Hiển thị lưới ghế khi đã chọn zone
  Widget _buildSeatGridView() {
    if (_isLoadingSeats) {
      return const Center(child: CircularProgressIndicator());
    }

    if (_seats.isEmpty) {
      return Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            const Icon(Icons.event_seat, size: 64, color: Colors.grey),
            const SizedBox(height: 16),
            Text(
              'Không có ghế trong ${_selectedZone?.zoneName ?? "khu vực này"}',
              style: const TextStyle(color: Colors.grey),
            ),
          ],
        ),
      );
    }

    // Group seats by row
    final Map<int, List<Seat>> seatsByRow = {};
    for (final seat in _seats) {
      seatsByRow.putIfAbsent(seat.rowNumber, () => []).add(seat);
    }
    final sortedRows = seatsByRow.keys.toList()..sort();

    return SingleChildScrollView(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // Zone info
          Container(
            padding: const EdgeInsets.all(12),
            margin: const EdgeInsets.only(bottom: 16),
            decoration: BoxDecoration(
              color: Colors.grey[100],
              borderRadius: BorderRadius.circular(8),
            ),
            child: Row(
              children: [
                const Icon(Icons.location_on, color: Colors.blue),
                const SizedBox(width: 8),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        _selectedZone?.zoneName ?? '',
                        style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16),
                      ),
                      if (_selectedZone?.hasPowerOutlet == true)
                        const Row(
                          children: [
                            Icon(Icons.power, size: 14, color: Colors.green),
                            SizedBox(width: 4),
                            Text('Có ổ cắm điện', style: TextStyle(fontSize: 12, color: Colors.green)),
                          ],
                        ),
                    ],
                  ),
                ),
                Text('${_seats.where((s) => s.isAvailable).length}/${_seats.length} ghế trống'),
              ],
            ),
          ),
          // Legend
          _buildLegend(),
          const SizedBox(height: 16),
          // Seat rows
          ...sortedRows.map((rowNum) {
            final rowSeats = seatsByRow[rowNum]!..sort((a, b) => a.columnNumber.compareTo(b.columnNumber));
            final rowLetter = String.fromCharCode(64 + rowNum);

            return Padding(
              padding: const EdgeInsets.only(bottom: 8),
              child: Row(
                children: [
                  SizedBox(
                    width: 30,
                    child: Text(
                      rowLetter,
                      style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16),
                    ),
                  ),
                  Expanded(
                    child: Wrap(
                      spacing: 4,
                      runSpacing: 4,
                      children: rowSeats.map((seat) => _buildSeatItem(seat)).toList(),
                    ),
                  ),
                ],
              ),
            );
          }),
        ],
      ),
    );
  }

  Widget _buildLegend() {
    return Row(
      mainAxisAlignment: MainAxisAlignment.center,
      children: [
        _buildLegendItem(Colors.green, 'Trống'),
        const SizedBox(width: 16),
        _buildLegendItem(Colors.red, 'Đã đặt'),
        const SizedBox(width: 16),
        _buildLegendItem(AppColors.brandColor, 'Đang chọn'),
      ],
    );
  }

  Widget _buildLegendItem(Color color, String label) {
    return Row(
      children: [
        Container(
          width: 20,
          height: 20,
          decoration: BoxDecoration(
            color: color,
            borderRadius: BorderRadius.circular(4),
          ),
        ),
        const SizedBox(width: 4),
        Text(label, style: const TextStyle(fontSize: 12)),
      ],
    );
  }

  Widget _buildSeatItem(Seat seat) {
    final isSelected = _selectedSeat?.seatId == seat.seatId;
    final color = isSelected
        ? AppColors.brandColor
        : seat.isAvailable
            ? Colors.green
            : Colors.red;

    return GestureDetector(
      onTap: () => _onSeatTap(seat),
      child: Container(
        width: 44,
        height: 44,
        decoration: BoxDecoration(
          color: color,
          borderRadius: BorderRadius.circular(8),
          border: isSelected ? Border.all(color: Colors.black, width: 2) : null,
          boxShadow: isSelected
              ? [BoxShadow(color: Colors.black.withOpacity(0.3), blurRadius: 4)]
              : null,
        ),
        child: Center(
          child: Text(
            seat.columnNumber.toString(),
            style: const TextStyle(
              color: Colors.white,
              fontWeight: FontWeight.bold,
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildBookingButton() {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.white,
        boxShadow: [
          BoxShadow(color: Colors.black.withOpacity(0.1), blurRadius: 4, offset: const Offset(0, -2)),
        ],
      ),
      child: ElevatedButton(
        onPressed: _confirmBooking,
        style: ElevatedButton.styleFrom(
          backgroundColor: AppColors.brandColor,
          foregroundColor: Colors.white,
          padding: const EdgeInsets.symmetric(vertical: 16),
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
        ),
        child: Text(
          'Đặt ghế ${_selectedSeat?.seatCode ?? ""} tại ${_selectedZone?.zoneName ?? ""}',
          style: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
        ),
      ),
    );
  }
}
