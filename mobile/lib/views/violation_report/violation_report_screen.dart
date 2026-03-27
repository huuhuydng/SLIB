import 'dart:io';
import 'dart:math';
import 'package:flutter/material.dart';
import 'package:image_picker/image_picker.dart';
import 'package:provider/provider.dart';
import '../../assets/colors.dart';
import '../../models/area.dart';
import '../../models/area_factory.dart';
import '../../models/seat.dart';
import '../../models/zone_occupancy.dart';
import '../../models/zones.dart';
import '../../services/auth/auth_service.dart';
import '../../services/booking/booking_service.dart';
import '../../services/report/seat_status_report_service.dart';
import '../../services/report/violation_report_service.dart';
import '../../views/widgets/error_display_widget.dart';
import 'violation_report_history_screen.dart';

class ViolationReportScreen extends StatefulWidget {
  const ViolationReportScreen({Key? key}) : super(key: key);

  @override
  State<ViolationReportScreen> createState() => _ViolationReportScreenState();
}

class _ViolationReportScreenState extends State<ViolationReportScreen> {
  final _bookingService = BookingService();
  final _violationReportService = ViolationReportService();
  final _seatStatusReportService = SeatStatusReportService();
  final _imagePicker = ImagePicker();

  // State
  bool _isLoading = true;
  bool _hasConfirmedSeat = false;
  String? _errorMessage;

  // Reservation info
  int? _mySeatId;
  int? _myZoneId;
  int? _myAreaId;
  String _mySeatCode = '';
  String _zoneName = '';

  // Floor plan data (giống FloorPlanScreen)
  List<Zone> _zones = [];
  List<AreaFactory> _factories = [];
  Map<int, double> _zoneOccupancy = {};
  Map<int, List<Seat>> _zoneSeats = {};
  double _contentWidth = 300;
  double _contentHeight = 400;

  // Report form
  Seat? _selectedSeat;
  String? _selectedViolationType;
  final _descriptionController = TextEditingController();
  final List<File> _selectedImages = [];
  final int _maxImages = 3;
  bool _isSubmitting = false;

  // Seat status report state
  String? _selectedSeatStatusIssueType;
  final _seatStatusDescriptionController = TextEditingController();
  File? _seatStatusSelectedImage;
  bool _isSeatStatusSubmitting = false;

  final List<Map<String, String>> _seatStatusIssueTypes = const [
    {'value': 'BROKEN', 'label': 'Ghế hỏng'},
    {'value': 'DIRTY', 'label': 'Ghế bẩn'},
    {'value': 'MISSING_EQUIPMENT', 'label': 'Thiếu thiết bị'},
    {'value': 'OTHER', 'label': 'Khác'},
  ];

  final List<Map<String, String>> _violationTypes = [
    {'value': 'NOISE', 'label': 'Gây ồn ào'},
    {'value': 'FEET_ON_SEAT', 'label': 'Gác chân lên ghế/bàn'},
    {'value': 'FOOD_DRINK', 'label': 'Ăn uống trong thư viện'},
    {'value': 'UNAUTHORIZED_USE', 'label': 'Sử dụng ghế không đúng'},
    {'value': 'LEFT_BELONGINGS', 'label': 'Để đồ giữ chỗ'},
    {'value': 'SLEEPING', 'label': 'Ngủ tại chỗ ngồi'},
    {'value': 'OTHER', 'label': 'Khác'},
  ];

  @override
  void initState() {
    super.initState();
    _checkReservation();
  }

  @override
  void dispose() {
    _descriptionController.dispose();
    _seatStatusDescriptionController.dispose();
    super.dispose();
  }

  Future<void> _checkReservation() async {
    setState(() {
      _isLoading = true;
      _errorMessage = null;
    });

    try {
      final authService = Provider.of<AuthService>(context, listen: false);
      final user = authService.currentUser;
      if (user == null) {
        setState(() {
          _errorMessage = 'auth';
          _isLoading = false;
        });
        return;
      }

      final booking = await _bookingService.getUpcomingBooking(user.id);

      if (booking == null) {
        setState(() {
          _hasConfirmedSeat = false;
          _isLoading = false;
        });
        return;
      }

      final status = booking['status'] ?? '';
      if (status != 'CONFIRMED') {
        setState(() {
          _hasConfirmedSeat = false;
          _isLoading = false;
        });
        return;
      }

      _mySeatId = booking['seatId'];
      _myZoneId = booking['zoneId'];
      _myAreaId = booking['areaId'];
      _mySeatCode = booking['seatCode'] ?? '';
      _zoneName = booking['zoneName'] ?? '';

      // Load floor plan cho area hiện tại
      if (_myAreaId != null) {
        await _loadFloorPlan(_myAreaId!);
      }

      setState(() {
        _hasConfirmedSeat = true;
        _isLoading = false;
      });
    } catch (e) {
      setState(() {
        _errorMessage = ErrorDisplayWidget.toVietnamese(e);
        _isLoading = false;
      });
    }
  }

  /// Load floor plan giống FloorPlanScreen._loadZonesAndFactories
  Future<void> _loadFloorPlan(int areaId) async {
    try {
      final results = await Future.wait([
        _bookingService.getZoneOccupancy(areaId),
        _bookingService.getFactoriesByArea(areaId),
        _bookingService.getZonesByArea(areaId),
      ]);

      final zoneOccupancies = results[0] as List<ZoneOccupancy>;
      final factories = results[1] as List<AreaFactory>;
      final zones = results[2] as List<Zone>;

      // Occupancy map
      Map<int, double> occupancy = {};
      for (final zo in zoneOccupancies) {
        occupancy[zo.zoneId] = zo.occupancyRate * 100;
      }

      // Load seats cho mỗi zone
      Map<int, List<Seat>> zoneSeats = {};
      final seatFutures = zones.map((zone) async {
        try {
          final seats = await _bookingService.getSeats(zone.zoneId);
          return MapEntry(zone.zoneId, seats);
        } catch (e) {
          debugPrint('Zone ${zone.zoneId}: Lỗi tải ghế: $e');
          return MapEntry(zone.zoneId, <Seat>[]);
        }
      });
      final seatResults = await Future.wait(seatFutures);
      for (final entry in seatResults) {
        zoneSeats[entry.key] = entry.value;
      }

      _calculateContentSize(zones, factories);

      setState(() {
        _zones = zones;
        _factories = factories;
        _zoneOccupancy = occupancy;
        _zoneSeats = zoneSeats;
      });
    } catch (e) {
      debugPrint('Lỗi tải floor plan: $e');
    }
  }

  /// Tính kích thước content - giống FloorPlanScreen
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
    _contentWidth = maxX + 40;
    _contentHeight = maxY + 40;
  }

  Color _getZoneColor(double occupancy) {
    if (occupancy >= 90) return const Color(0xFFE74C3C);
    if (occupancy >= 50) return const Color(0xFFF39C12);
    return const Color(0xFF27AE60);
  }

  // ====== Seat tap logic cho violation ======

  void _onSeatTap(Seat seat) {
    if (seat.isUnavailable) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text('Ghế ${seat.seatCode} đang bảo trì'),
          backgroundColor: Colors.grey[600],
          duration: const Duration(seconds: 2),
        ),
      );
      return;
    }

    if (seat.seatId == _mySeatId) {
      // Tap ghế mình → mở bottom sheet báo cáo tình trạng ghế
      setState(() => _selectedSeat = seat);
      _showSeatStatusReportBottomSheet(seat);
      return;
    }

    if (seat.seatStatus == 'AVAILABLE') {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text('Ghế ${seat.seatCode} đang không có ai ngồi'),
          backgroundColor: Colors.grey[600],
          duration: const Duration(seconds: 2),
        ),
      );
      return;
    }

    // Ghế có người → mở bottom sheet báo cáo
    setState(() => _selectedSeat = seat);
    _showReportBottomSheet(seat);
  }

  // ====== Bottom sheet báo cáo ======

  void _showReportBottomSheet(Seat seat) {
    _selectedViolationType = null;
    _descriptionController.clear();
    _selectedImages.clear();

    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (ctx) => StatefulBuilder(
        builder: (context, setSheetState) {
          return Container(
            constraints: BoxConstraints(
              maxHeight: MediaQuery.of(context).size.height * 0.85,
            ),
            decoration: const BoxDecoration(
              color: Colors.white,
              borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
            ),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                Container(
                  margin: const EdgeInsets.only(top: 12),
                  width: 40, height: 4,
                  decoration: BoxDecoration(color: Colors.grey[300], borderRadius: BorderRadius.circular(2)),
                ),
                Padding(
                  padding: const EdgeInsets.fromLTRB(20, 16, 20, 0),
                  child: Row(
                    children: [
                      Container(
                        padding: const EdgeInsets.all(10),
                        decoration: BoxDecoration(
                          color: AppColors.brandColor.withOpacity(0.1),
                          borderRadius: BorderRadius.circular(12),
                        ),
                        child: const Icon(Icons.report_outlined, color: AppColors.brandColor, size: 24),
                      ),
                      const SizedBox(width: 12),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            const Text('Báo cáo vi phạm',
                              style: TextStyle(fontSize: 18, fontWeight: FontWeight.w700, color: AppColors.textPrimary)),
                            const SizedBox(height: 2),
                            Text('Ghế ${seat.seatCode}',
                              style: TextStyle(fontSize: 14, color: Colors.grey[600])),
                          ],
                        ),
                      ),
                      IconButton(
                        icon: const Icon(Icons.close, color: Color(0xFF666666)),
                        onPressed: () => Navigator.pop(ctx),
                      ),
                    ],
                  ),
                ),
                const Divider(height: 24),
                Flexible(
                  child: SingleChildScrollView(
                    padding: const EdgeInsets.fromLTRB(20, 0, 20, 20),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        const Text('Loại vi phạm',
                          style: TextStyle(fontSize: 14, fontWeight: FontWeight.w600, color: Color(0xFF333333))),
                        const SizedBox(height: 8),
                        DropdownButtonFormField<String>(
                          value: _selectedViolationType,
                          decoration: _dropdownDecoration('Chọn loại vi phạm'),
                          items: _violationTypes.map((type) =>
                            DropdownMenuItem<String>(
                              value: type['value'],
                              child: Text(type['label']!, style: const TextStyle(fontSize: 14)),
                            ),
                          ).toList(),
                          onChanged: (value) {
                            setSheetState(() => _selectedViolationType = value);
                            setState(() => _selectedViolationType = value);
                          },
                        ),
                        const SizedBox(height: 16),
                        const Text('Mô tả chi tiết (không bắt buộc)',
                          style: TextStyle(fontSize: 14, fontWeight: FontWeight.w600, color: Color(0xFF333333))),
                        const SizedBox(height: 8),
                        TextField(
                          controller: _descriptionController,
                          maxLines: 3,
                          maxLength: 300,
                          decoration: InputDecoration(
                            hintText: 'Mô tả thêm về tình trạng vi phạm...',
                            hintStyle: TextStyle(color: Colors.grey[400], fontSize: 14),
                            border: OutlineInputBorder(borderRadius: BorderRadius.circular(12), borderSide: BorderSide(color: Colors.grey[300]!)),
                            enabledBorder: OutlineInputBorder(borderRadius: BorderRadius.circular(12), borderSide: BorderSide(color: Colors.grey[300]!)),
                            focusedBorder: OutlineInputBorder(borderRadius: BorderRadius.circular(12), borderSide: const BorderSide(color: AppColors.brandColor, width: 1.5)),
                            contentPadding: const EdgeInsets.all(14),
                            counterStyle: TextStyle(color: Colors.grey[400], fontSize: 12),
                          ),
                          style: const TextStyle(fontSize: 14, height: 1.5, color: Color(0xFF333333)),
                        ),
                        const SizedBox(height: 16),
                        Text('Ảnh bằng chứng (không bắt buộc, tối đa $_maxImages ảnh)',
                          style: const TextStyle(fontSize: 14, fontWeight: FontWeight.w600, color: Color(0xFF333333))),
                        const SizedBox(height: 12),
                        Wrap(
                          spacing: 10, runSpacing: 10,
                          children: [
                            ..._selectedImages.asMap().entries.map((entry) =>
                              _buildImageThumbnail(entry.key, setSheetState)),
                            if (_selectedImages.length < _maxImages) _buildAddImageButton(setSheetState),
                          ],
                        ),
                        const SizedBox(height: 24),
                        SizedBox(
                          width: double.infinity, height: 50,
                          child: ElevatedButton(
                            onPressed: (_selectedViolationType != null && !_isSubmitting)
                                ? () => _submitReport(ctx) : null,
                            style: ElevatedButton.styleFrom(
                              backgroundColor: AppColors.brandColor,
                              foregroundColor: Colors.white,
                              disabledBackgroundColor: const Color(0xFFF5F5F5),
                              disabledForegroundColor: Colors.grey[400],
                              elevation: 0,
                              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
                            ),
                            child: _isSubmitting
                                ? const SizedBox(width: 22, height: 22,
                                    child: CircularProgressIndicator(color: Colors.white, strokeWidth: 2.5))
                                : const Text('Gửi báo cáo vi phạm',
                                    style: TextStyle(fontSize: 16, fontWeight: FontWeight.w600)),
                          ),
                        ),
                        SizedBox(height: MediaQuery.of(context).viewInsets.bottom),
                      ],
                    ),
                  ),
                ),
              ],
            ),
          );
        },
      ),
    );
  }

  InputDecoration _dropdownDecoration(String hint) {
    return InputDecoration(
      hintText: hint,
      hintStyle: TextStyle(color: Colors.grey[400], fontSize: 14),
      border: OutlineInputBorder(borderRadius: BorderRadius.circular(12), borderSide: BorderSide(color: Colors.grey[300]!)),
      enabledBorder: OutlineInputBorder(borderRadius: BorderRadius.circular(12), borderSide: BorderSide(color: Colors.grey[300]!)),
      focusedBorder: OutlineInputBorder(borderRadius: BorderRadius.circular(12), borderSide: const BorderSide(color: AppColors.brandColor, width: 1.5)),
      contentPadding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
    );
  }

  Widget _buildImageThumbnail(int index, StateSetter setSheetState) {
    return Stack(
      children: [
        ClipRRect(
          borderRadius: BorderRadius.circular(12),
          child: Image.file(_selectedImages[index], width: 72, height: 72, fit: BoxFit.cover),
        ),
        Positioned(
          top: -2, right: -2,
          child: GestureDetector(
            onTap: () {
              setState(() => _selectedImages.removeAt(index));
              setSheetState(() {});
            },
            child: Container(
              padding: const EdgeInsets.all(3),
              decoration: BoxDecoration(
                color: AppColors.brandColor.withOpacity(0.85),
                shape: BoxShape.circle,
                border: Border.all(color: Colors.white, width: 1.5),
              ),
              child: const Icon(Icons.close, color: Colors.white, size: 12),
            ),
          ),
        ),
      ],
    );
  }

  Widget _buildAddImageButton(StateSetter setSheetState) {
    return GestureDetector(
      onTap: () => _pickImages(setSheetState),
      child: Container(
        width: 72, height: 72,
        decoration: BoxDecoration(
          color: AppColors.brandColor.withOpacity(0.08),
          borderRadius: BorderRadius.circular(12),
          border: Border.all(color: AppColors.brandColor.withOpacity(0.3)),
        ),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(Icons.add_a_photo_outlined, color: AppColors.brandColor.withOpacity(0.7), size: 22),
            const SizedBox(height: 4),
            Text('Thêm ảnh', style: TextStyle(fontSize: 10, color: Colors.grey[600], fontWeight: FontWeight.w500)),
          ],
        ),
      ),
    );
  }

  Future<void> _pickImages(StateSetter setSheetState) async {
    if (_selectedImages.length >= _maxImages) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Tối đa $_maxImages ảnh'), backgroundColor: AppColors.brandColor),
      );
      return;
    }
    showModalBottomSheet(
      context: context,
      backgroundColor: Colors.transparent,
      builder: (ctx) => Container(
        decoration: const BoxDecoration(color: Colors.white, borderRadius: BorderRadius.vertical(top: Radius.circular(20))),
        padding: const EdgeInsets.symmetric(vertical: 20),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Container(width: 40, height: 4,
              decoration: BoxDecoration(color: Colors.grey[300], borderRadius: BorderRadius.circular(2))),
            const SizedBox(height: 16),
            const Text('Chọn ảnh bằng chứng',
              style: TextStyle(fontSize: 18, fontWeight: FontWeight.w600)),
            const SizedBox(height: 16),
            ListTile(
              leading: Container(
                padding: const EdgeInsets.all(10),
                decoration: BoxDecoration(color: AppColors.brandColor.withOpacity(0.1), borderRadius: BorderRadius.circular(12)),
                child: const Icon(Icons.camera_alt, color: AppColors.brandColor),
              ),
              title: const Text('Chụp ảnh'),
              onTap: () async {
                Navigator.pop(ctx);
                final XFile? photo = await _imagePicker.pickImage(source: ImageSource.camera, imageQuality: 80);
                if (photo != null) {
                  setState(() => _selectedImages.add(File(photo.path)));
                  setSheetState(() {});
                }
              },
            ),
            ListTile(
              leading: Container(
                padding: const EdgeInsets.all(10),
                decoration: BoxDecoration(color: AppColors.brandColor.withOpacity(0.1), borderRadius: BorderRadius.circular(12)),
                child: const Icon(Icons.photo_library, color: AppColors.brandColor),
              ),
              title: const Text('Chọn từ thư viện'),
              onTap: () async {
                Navigator.pop(ctx);
                final remaining = _maxImages - _selectedImages.length;
                final List<XFile> photos = await _imagePicker.pickMultiImage(imageQuality: 80, limit: remaining);
                if (photos.isNotEmpty) {
                  setState(() {
                    for (final photo in photos) {
                      if (_selectedImages.length < _maxImages) _selectedImages.add(File(photo.path));
                    }
                  });
                  setSheetState(() {});
                }
              },
            ),
            const SizedBox(height: 16),
          ],
        ),
      ),
    );
  }

  Future<void> _submitReport(BuildContext bottomSheetContext) async {
    if (_selectedSeat == null || _selectedViolationType == null) return;

    // Lấy token trước
    final authService = Provider.of<AuthService>(context, listen: false);
    final token = await authService.getToken();
    if (token == null) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Chưa đăng nhập'), backgroundColor: AppColors.error),
        );
      }
      return;
    }

    // Lưu thông tin cần gửi trước khi đóng bottom sheet
    final seatId = _selectedSeat!.seatId;
    final violationType = _selectedViolationType!;
    final description = _descriptionController.text.trim().isNotEmpty ? _descriptionController.text.trim() : null;
    final images = _selectedImages.isNotEmpty ? List<File>.from(_selectedImages) : null;

    // Đóng bottom sheet + hiện thành công NGAY LẬP TỨC
    if (mounted) {
      Navigator.pop(bottomSheetContext);
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Đã gửi báo cáo vi phạm! Thủ thư sẽ xử lý sớm nhất'),
          backgroundColor: AppColors.success,
          duration: Duration(seconds: 3),
        ),
      );
    }

    // Gửi request chạy ngầm phía sau (fire-and-forget)
    _violationReportService.createReport(
      token: token,
      seatId: seatId,
      violationType: violationType,
      description: description,
      images: images,
    ).then((_) {
      debugPrint('[ViolationReport] Báo cáo đã gửi thành công lên server');
    }).catchError((e) {
      debugPrint('[ViolationReport] Lỗi gửi báo cáo: $e');
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('Lỗi gửi báo cáo: ${e.toString()}'),
            backgroundColor: AppColors.error,
            duration: const Duration(seconds: 5),
          ),
        );
      }
    });
  }

  // ====== SEAT STATUS REPORT BOTTOM SHEET ======

  void _showSeatStatusReportBottomSheet(Seat seat) {
    _selectedSeatStatusIssueType = null;
    _seatStatusDescriptionController.clear();
    _seatStatusSelectedImage = null;

    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (ctx) => StatefulBuilder(
        builder: (context, setSheetState) {
          return Container(
            constraints: BoxConstraints(
              maxHeight: MediaQuery.of(context).size.height * 0.85,
            ),
            decoration: const BoxDecoration(
              color: Colors.white,
              borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
            ),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                Container(
                  margin: const EdgeInsets.only(top: 12),
                  width: 40, height: 4,
                  decoration: BoxDecoration(color: Colors.grey[300], borderRadius: BorderRadius.circular(2)),
                ),
                Padding(
                  padding: const EdgeInsets.fromLTRB(20, 16, 20, 0),
                  child: Row(
                    children: [
                      Container(
                        padding: const EdgeInsets.all(10),
                        decoration: BoxDecoration(
                          color: Colors.blue.withOpacity(0.1),
                          borderRadius: BorderRadius.circular(12),
                        ),
                        child: const Icon(Icons.build_outlined, color: Colors.blue, size: 24),
                      ),
                      const SizedBox(width: 12),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            const Text('Báo cáo tình trạng ghế',
                              style: TextStyle(fontSize: 18, fontWeight: FontWeight.w700, color: AppColors.textPrimary)),
                            const SizedBox(height: 2),
                            Text('Ghế ${seat.seatCode} (ghế của bạn)',
                              style: TextStyle(fontSize: 14, color: Colors.grey[600])),
                          ],
                        ),
                      ),
                      IconButton(
                        icon: const Icon(Icons.close, color: Color(0xFF666666)),
                        onPressed: () => Navigator.pop(ctx),
                      ),
                    ],
                  ),
                ),
                const Divider(height: 24),
                Flexible(
                  child: SingleChildScrollView(
                    padding: const EdgeInsets.fromLTRB(20, 0, 20, 20),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        const Text('Loại sự cố',
                          style: TextStyle(fontSize: 14, fontWeight: FontWeight.w600, color: Color(0xFF333333))),
                        const SizedBox(height: 8),
                        DropdownButtonFormField<String>(
                          value: _selectedSeatStatusIssueType,
                          decoration: _dropdownDecoration('Chọn loại sự cố'),
                          items: _seatStatusIssueTypes.map((type) =>
                            DropdownMenuItem<String>(
                              value: type['value'],
                              child: Text(type['label']!, style: const TextStyle(fontSize: 14)),
                            ),
                          ).toList(),
                          onChanged: (value) {
                            setSheetState(() => _selectedSeatStatusIssueType = value);
                            setState(() => _selectedSeatStatusIssueType = value);
                          },
                        ),
                        const SizedBox(height: 16),
                        const Text('Mô tả chi tiết (không bắt buộc)',
                          style: TextStyle(fontSize: 14, fontWeight: FontWeight.w600, color: Color(0xFF333333))),
                        const SizedBox(height: 8),
                        TextField(
                          controller: _seatStatusDescriptionController,
                          maxLines: 3,
                          maxLength: 300,
                          decoration: InputDecoration(
                            hintText: 'Mô tả tình trạng ghế để thủ thư dễ xử lý hơn...',
                            hintStyle: TextStyle(color: Colors.grey[400], fontSize: 14),
                            border: OutlineInputBorder(borderRadius: BorderRadius.circular(12), borderSide: BorderSide(color: Colors.grey[300]!)),
                            enabledBorder: OutlineInputBorder(borderRadius: BorderRadius.circular(12), borderSide: BorderSide(color: Colors.grey[300]!)),
                            focusedBorder: OutlineInputBorder(borderRadius: BorderRadius.circular(12), borderSide: const BorderSide(color: Colors.blue, width: 1.5)),
                            contentPadding: const EdgeInsets.all(14),
                            counterStyle: TextStyle(color: Colors.grey[400], fontSize: 12),
                          ),
                          style: const TextStyle(fontSize: 14, height: 1.5, color: Color(0xFF333333)),
                        ),
                        const SizedBox(height: 16),
                        const Text('Ảnh minh họa (không bắt buộc)',
                          style: TextStyle(fontSize: 14, fontWeight: FontWeight.w600, color: Color(0xFF333333))),
                        const SizedBox(height: 12),
                        if (_seatStatusSelectedImage != null)
                          Stack(
                            children: [
                              ClipRRect(
                                borderRadius: BorderRadius.circular(12),
                                child: Image.file(_seatStatusSelectedImage!, width: 100, height: 100, fit: BoxFit.cover),
                              ),
                              Positioned(
                                top: -2, right: -2,
                                child: GestureDetector(
                                  onTap: () {
                                    setState(() => _seatStatusSelectedImage = null);
                                    setSheetState(() {});
                                  },
                                  child: Container(
                                    padding: const EdgeInsets.all(3),
                                    decoration: BoxDecoration(
                                      color: Colors.red.withOpacity(0.85),
                                      shape: BoxShape.circle,
                                      border: Border.all(color: Colors.white, width: 1.5),
                                    ),
                                    child: const Icon(Icons.close, color: Colors.white, size: 12),
                                  ),
                                ),
                              ),
                            ],
                          )
                        else
                          GestureDetector(
                            onTap: () async {
                              final picked = await _imagePicker.pickImage(
                                source: ImageSource.gallery,
                                imageQuality: 80,
                              );
                              if (picked != null) {
                                setState(() => _seatStatusSelectedImage = File(picked.path));
                                setSheetState(() {});
                              }
                            },
                            child: Container(
                              width: 100, height: 100,
                              decoration: BoxDecoration(
                                color: Colors.blue.withOpacity(0.08),
                                borderRadius: BorderRadius.circular(12),
                                border: Border.all(color: Colors.blue.withOpacity(0.3)),
                              ),
                              child: Column(
                                mainAxisAlignment: MainAxisAlignment.center,
                                children: [
                                  Icon(Icons.add_a_photo_outlined, color: Colors.blue.withOpacity(0.7), size: 28),
                                  const SizedBox(height: 4),
                                  Text('Thêm ảnh', style: TextStyle(fontSize: 11, color: Colors.grey[600], fontWeight: FontWeight.w500)),
                                ],
                              ),
                            ),
                          ),
                        const SizedBox(height: 24),
                        SizedBox(
                          width: double.infinity, height: 50,
                          child: ElevatedButton(
                            onPressed: (_selectedSeatStatusIssueType != null && !_isSeatStatusSubmitting)
                                ? () => _submitSeatStatusReport(ctx) : null,
                            style: ElevatedButton.styleFrom(
                              backgroundColor: Colors.blue,
                              foregroundColor: Colors.white,
                              disabledBackgroundColor: const Color(0xFFF5F5F5),
                              disabledForegroundColor: Colors.grey[400],
                              elevation: 0,
                              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
                            ),
                            child: _isSeatStatusSubmitting
                                ? const SizedBox(width: 22, height: 22,
                                    child: CircularProgressIndicator(color: Colors.white, strokeWidth: 2.5))
                                : const Text('Gửi báo cáo tình trạng',
                                    style: TextStyle(fontSize: 16, fontWeight: FontWeight.w600)),
                          ),
                        ),
                        SizedBox(height: MediaQuery.of(context).viewInsets.bottom),
                      ],
                    ),
                  ),
                ),
              ],
            ),
          );
        },
      ),
    );
  }

  Future<void> _submitSeatStatusReport(BuildContext bottomSheetContext) async {
    if (_selectedSeat == null || _selectedSeatStatusIssueType == null) return;

    final authService = Provider.of<AuthService>(context, listen: false);
    final token = await authService.getToken();
    if (token == null) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Chưa đăng nhập'), backgroundColor: AppColors.error),
        );
      }
      return;
    }

    final seatId = _selectedSeat!.seatId;
    final issueType = _selectedSeatStatusIssueType!;
    final description = _seatStatusDescriptionController.text.trim().isNotEmpty
        ? _seatStatusDescriptionController.text.trim()
        : null;
    final image = _seatStatusSelectedImage;

    // Đóng bottom sheet + hiện thông báo ngay
    if (mounted) {
      Navigator.pop(bottomSheetContext);
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Đã gửi báo cáo tình trạng ghế! Thủ thư sẽ xử lý sớm nhất'),
          backgroundColor: AppColors.success,
          duration: Duration(seconds: 3),
        ),
      );
    }

    // Gửi request chạy ngầm (fire-and-forget)
    _seatStatusReportService.createReport(
      token: token,
      seatId: seatId,
      issueType: issueType,
      description: description,
      image: image,
    ).then((_) {
      debugPrint('[SeatStatusReport] Báo cáo tình trạng ghế đã gửi thành công');
    }).catchError((e) {
      debugPrint('[SeatStatusReport] Lỗi gửi báo cáo: $e');
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('Lỗi gửi báo cáo tình trạng: ${e.toString()}'),
            backgroundColor: AppColors.error,
            duration: const Duration(seconds: 5),
          ),
        );
      }
    });
  }

  // ====== BUILD UI ======

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.white,
      body: Column(
        children: [
          _buildHeader(),
          Expanded(
            child: _isLoading
                ? const Center(child: CircularProgressIndicator(color: AppColors.brandColor))
                : _errorMessage != null
                    ? _buildErrorState()
                    : !_hasConfirmedSeat
                        ? _buildNotConfirmedState()
                        : _buildFloorPlanView(),
          ),
        ],
      ),
    );
  }

  Widget _buildHeader() {
    return Container(
      decoration: BoxDecoration(
        color: AppColors.brandColor,
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(0.1),
            blurRadius: 4,
            offset: const Offset(0, 2),
          ),
        ],
      ),
      child: SafeArea(
        bottom: false,
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 4, vertical: 8),
          child: Row(
            children: [
              IconButton(
                icon: const Icon(Icons.arrow_back_ios, color: Colors.white, size: 20),
                onPressed: () => Navigator.pop(context),
              ),
              const Expanded(
                child: Text('Báo cáo',
                  style: TextStyle(color: Colors.white, fontSize: 18, fontWeight: FontWeight.w600)),
              ),
              TextButton(
                onPressed: () => Navigator.push(context,
                  MaterialPageRoute(builder: (context) => const ViolationReportHistoryScreen())),
                child: const Text('Lịch sử',
                  style: TextStyle(color: Colors.white, fontSize: 14, fontWeight: FontWeight.w600)),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildNotConfirmedState() {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(32),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Container(
              padding: const EdgeInsets.all(24),
              decoration: BoxDecoration(
                color: AppColors.brandColor.withOpacity(0.1),
                shape: BoxShape.circle,
                boxShadow: [BoxShadow(color: AppColors.brandColor.withOpacity(0.15), blurRadius: 20, spreadRadius: 5)],
              ),
              child: const Icon(Icons.event_seat_outlined, size: 56, color: AppColors.brandColor),
            ),
            const SizedBox(height: 24),
            const Text('Chưa xác nhận ghế ngồi',
              style: TextStyle(fontSize: 20, fontWeight: FontWeight.w700, color: AppColors.textPrimary)),
            const SizedBox(height: 12),
            Text(
              'Bạn cần xác nhận ghế ngồi (check-in) trước khi có thể báo cáo vi phạm tại các ghế xung quanh.',
              style: TextStyle(fontSize: 15, color: Colors.grey[600], height: 1.5),
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: 32),
            OutlinedButton.icon(
              onPressed: () => Navigator.pop(context),
              icon: const Icon(Icons.arrow_back),
              label: const Text('Quay lại'),
              style: OutlinedButton.styleFrom(
                foregroundColor: AppColors.brandColor,
                side: const BorderSide(color: AppColors.brandColor),
                padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 12),
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildErrorState() {
    if (_errorMessage == 'auth') {
      return ErrorDisplayWidget.auth(onRetry: _checkReservation);
    }
    return ErrorDisplayWidget(
      message: _errorMessage ?? 'Đã xảy ra lỗi',
      onRetry: _checkReservation,
    );
  }

  // ====== FLOOR PLAN VIEW - giống hệt FloorPlanScreen ======

  Widget _buildFloorPlanView() {
    return Column(
      children: [
        // Hướng dẫn
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
          child: Text(
            'Chạm vào ghế đỏ (đã đặt) để báo cáo vi phạm. Chạm vào ghế cam (ghế của bạn) để báo cáo tình trạng ghế.',
            style: TextStyle(fontSize: 13, color: Colors.grey[600]),
            textAlign: TextAlign.center,
          ),
        ),

        // Legend
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 16),
          child: Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              _legend(Colors.green, 'Ghế trống'),
              const SizedBox(width: 10),
              _legend(Colors.red[400]!, 'Đã đặt'),
              const SizedBox(width: 10),
              _legend(AppColors.brandColor, 'Ghế của bạn'),
              const SizedBox(width: 10),
              _legend(Colors.grey[400]!, 'Bảo trì'),
            ],
          ),
        ),

        const SizedBox(height: 8),

        // Floor plan - giống hệt FloorPlanScreen._buildFloorPlan
        Expanded(
          child: _buildFloorPlan(),
        ),
      ],
    );
  }

  Widget _legend(Color c, String l) => Row(
    children: [
      Container(width: 14, height: 14,
        decoration: BoxDecoration(color: c, borderRadius: BorderRadius.circular(4))),
      const SizedBox(width: 4),
      Text(l, style: const TextStyle(fontSize: 11)),
    ],
  );

  /// Floor plan - copy y hệt FloorPlanScreen._buildFloorPlan
  Widget _buildFloorPlan() {
    final screenWidth = MediaQuery.of(context).size.width;
    final screenHeight = MediaQuery.of(context).size.height;

    return InteractiveViewer(
      constrained: false,
      boundaryMargin: EdgeInsets.all(max(screenWidth, screenHeight) * 2),
      minScale: 0.02,
      maxScale: 5.0,
      panEnabled: true,
      scaleEnabled: true,
      child: Container(
        width: _contentWidth + 40,
        height: _contentHeight + 40,
        padding: const EdgeInsets.all(20),
        decoration: BoxDecoration(
          color: Colors.grey[100],
        ),
        child: Stack(
          children: [
            // Grid background
            CustomPaint(
              size: Size(_contentWidth, _contentHeight),
              painter: _GridPainter(),
            ),
            // Factories (obstacles)
            ..._factories.map((f) => _buildFactoryWidget(f, 1.0)),
            // Zones with seats
            ..._zones.map((z) => _buildZoneWidget(z, 1.0)),
          ],
        ),
      ),
    );
  }

  /// Factory widget - copy y hệt FloorPlanScreen._buildFactoryWidget
  Widget _buildFactoryWidget(AreaFactory factory, double scale) {
    final x = factory.positionX * scale;
    final y = factory.positionY * scale;
    final w = factory.width * scale;
    final h = factory.height * scale;

    return Positioned(
      left: x,
      top: y,
      child: Container(
        width: w,
        height: h,
        decoration: BoxDecoration(
          color: const Color(0xFFBDC3C7),
          borderRadius: BorderRadius.circular(8),
          border: Border.all(color: Colors.grey[600]!, width: 1),
        ),
        child: Center(
          child: Padding(
            padding: const EdgeInsets.all(4),
            child: Text(
              factory.factoryName,
              style: TextStyle(color: Colors.grey[700], fontSize: 11, fontWeight: FontWeight.w500),
              textAlign: TextAlign.center,
              overflow: TextOverflow.ellipsis,
              maxLines: 2,
            ),
          ),
        ),
      ),
    );
  }

  /// Zone widget - copy y hệt FloorPlanScreen._buildZoneWidget
  /// Chỉ thay đổi: ghế mình = brandColor, onTap ghế = _onSeatTap (violation)
  Widget _buildZoneWidget(Zone zone, double scale) {
    final occupancy = _zoneOccupancy[zone.zoneId] ?? 0;
    final color = _getZoneColor(occupancy);
    final seats = _zoneSeats[zone.zoneId] ?? [];

    final sortedSeats = List<Seat>.from(seats)
      ..sort((a, b) {
        if (a.rowNumber != b.rowNumber) return a.rowNumber.compareTo(b.rowNumber);
        return a.columnNumber.compareTo(b.columnNumber);
      });

    final availableSeats = sortedSeats.where((s) => s.seatStatus == 'AVAILABLE').length;
    final totalSeats = sortedSeats.length;

    final x = zone.positionX * scale;
    final y = zone.positionY * scale;
    final w = zone.width * scale;
    final h = zone.height * scale;

    return Positioned(
      left: x,
      top: y,
      child: Container(
        width: w,
        height: h,
        decoration: BoxDecoration(
          color: color.withAlpha(50),
          borderRadius: BorderRadius.circular(8),
          border: Border.all(color: color, width: 2),
          boxShadow: [BoxShadow(color: color.withAlpha(40), blurRadius: 4, offset: const Offset(0, 2))],
        ),
        child: Column(
          children: [
            // Header
            Container(
              width: double.infinity,
              padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 3),
              decoration: BoxDecoration(
                color: color,
                borderRadius: const BorderRadius.only(topLeft: Radius.circular(6), topRight: Radius.circular(6)),
              ),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Expanded(
                    child: Text(zone.zoneName,
                      style: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 10),
                      overflow: TextOverflow.ellipsis),
                  ),
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 4, vertical: 1),
                    decoration: BoxDecoration(
                      color: Colors.white.withAlpha(50),
                      borderRadius: BorderRadius.circular(8),
                    ),
                    child: Row(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        const Icon(Icons.event_seat, color: Colors.white, size: 10),
                        const SizedBox(width: 2),
                        Text('$availableSeats/$totalSeats',
                          style: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 9)),
                      ],
                    ),
                  ),
                ],
              ),
            ),
            // Seat grid
            Expanded(
              child: totalSeats == 0
                  ? Center(
                      child: Column(
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          Icon(Icons.event_seat_outlined, color: color.withAlpha(150), size: 24),
                          const SizedBox(height: 4),
                          Text('Chưa có ghế', style: TextStyle(color: color.withAlpha(180), fontSize: 10)),
                        ],
                      ),
                    )
                  : Padding(
                      padding: const EdgeInsets.all(4),
                      child: SingleChildScrollView(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: _buildSeatRows(sortedSeats, color, w, h, zone),
                        ),
                      ),
                    ),
            ),
          ],
        ),
      ),
    );
  }

  /// Seat rows - copy y hệt FloorPlanScreen._buildSeatRows
  /// Chỉ thay đổi: ghế mình = brandColor, onTap = _onSeatTap (violation)
  List<Widget> _buildSeatRows(List<Seat> seats, Color color, double zoneWidth, double zoneHeight, Zone zone) {
    Map<int, List<Seat>> seatsByRow = {};
    for (final seat in seats) {
      seatsByRow.putIfAbsent(seat.rowNumber, () => []).add(seat);
    }

    String getRowLabel(int rowNumber) {
      return String.fromCharCode('A'.codeUnitAt(0) + rowNumber - 1);
    }

    final numRows = seatsByRow.length;
    final maxSeatsPerRow = seatsByRow.values.map((r) => r.length).reduce((a, b) => a > b ? a : b);

    const fixedSeatSize = 35.0;

    final availableWidth = zoneWidth - 16;
    final availableHeight = zoneHeight - 50;

    final totalSeatWidthInRow = fixedSeatSize * maxSeatsPerRow;
    final hSpacing = (availableWidth - totalSeatWidthInRow) / (maxSeatsPerRow + 1);

    final totalSeatHeightInCol = fixedSeatSize * numRows;
    final vSpacing = (availableHeight - totalSeatHeightInCol) / (numRows + 1);

    List<Widget> rows = [];
    final sortedRowNumbers = seatsByRow.keys.toList()..sort();

    for (int i = 0; i < sortedRowNumbers.length; i++) {
      final rowNum = sortedRowNumbers[i];
      final rowSeats = seatsByRow[rowNum]!;
      rowSeats.sort((a, b) => a.columnNumber.compareTo(b.columnNumber));
      final rowLabel = getRowLabel(rowNum);

      rows.add(
        Padding(
          padding: EdgeInsets.only(
            top: i == 0 ? max(vSpacing, 4.0) : 0,
            bottom: max(vSpacing, 4.0),
          ),
          child: Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: rowSeats.asMap().entries.map((entry) {
              final j = entry.key;
              final seat = entry.value;
              final seatLabel = '$rowLabel${seat.columnNumber}';
              final isMySeat = seat.seatId == _mySeatId;
              final isAvailable = seat.seatStatus == 'AVAILABLE';

              // Màu ghế: ghế mình = brandColor, ghế trống = green, ghế đã đặt = red, bảo trì = grey
              Color seatColor;
              Color borderColor;
              if (isMySeat) {
                seatColor = AppColors.brandColor;
                borderColor = AppColors.brandColor;
              } else if (seat.isUnavailable) {
                seatColor = Colors.grey[400]!;
                borderColor = Colors.grey[600]!;
              } else if (isAvailable) {
                seatColor = Colors.green;
                borderColor = Colors.green[700]!;
              } else {
                seatColor = Colors.red[400]!;
                borderColor = Colors.red[700]!;
              }

              return Padding(
                padding: EdgeInsets.only(
                  left: j == 0 ? max(hSpacing, 4.0) : max(hSpacing / 2, 2.0),
                  right: j == rowSeats.length - 1 ? max(hSpacing, 4.0) : max(hSpacing / 2, 2.0),
                ),
                child: GestureDetector(
                  onTap: () => _onSeatTap(seat),
                  child: Tooltip(
                    message: isMySeat
                        ? '${seat.seatCode} - Ghế của bạn'
                        : '${seat.seatCode} - ${isAvailable ? "Trống" : "Đã đặt"}',
                    child: Container(
                      width: fixedSeatSize,
                      height: fixedSeatSize,
                      decoration: BoxDecoration(
                        color: seatColor,
                        borderRadius: BorderRadius.circular(6),
                        border: Border.all(color: borderColor, width: 1.5),
                        boxShadow: isMySeat
                            ? [BoxShadow(color: AppColors.brandColor.withAlpha(100), blurRadius: 6, spreadRadius: 1)]
                            : null,
                      ),
                      child: Center(
                        child: Text(
                          seatLabel,
                          style: const TextStyle(color: Colors.white, fontSize: 10, fontWeight: FontWeight.bold),
                        ),
                      ),
                    ),
                  ),
                ),
              );
            }).toList(),
          ),
        ),
      );
    }

    return rows;
  }
}

/// Grid painter - copy y hệt FloorPlanScreen._GridPainter
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
