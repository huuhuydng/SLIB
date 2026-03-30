import 'dart:io';
import 'package:flutter/material.dart';
import 'package:image_picker/image_picker.dart';
import 'package:provider/provider.dart';
import '../../services/auth/auth_service.dart';
import '../../services/booking/booking_service.dart';
import '../../services/report/seat_status_report_service.dart';
import '../../views/widgets/error_display_widget.dart';
import 'seat_status_report_history_screen.dart';

class SeatStatusReportScreen extends StatefulWidget {
  const SeatStatusReportScreen({super.key});

  @override
  State<SeatStatusReportScreen> createState() => _SeatStatusReportScreenState();
}

class _SeatStatusReportScreenState extends State<SeatStatusReportScreen> {
  final _bookingService = BookingService();
  final _seatStatusReportService = SeatStatusReportService();
  final _descriptionController = TextEditingController();
  final _imagePicker = ImagePicker();

  bool _isLoading = true;
  bool _isSubmitting = false;
  String? _errorMessage;
  int? _seatId;
  String _seatCode = '';
  String? _selectedIssueType;
  File? _selectedImage;

  final List<Map<String, String>> _issueTypes = const [
    {'value': 'BROKEN', 'label': 'Ghế hỏng'},
    {'value': 'DIRTY', 'label': 'Ghế bẩn'},
    {'value': 'MISSING_EQUIPMENT', 'label': 'Thiếu thiết bị'},
    {'value': 'OTHER', 'label': 'Khác'},
  ];

  @override
  void initState() {
    super.initState();
    _loadCurrentSeat();
  }

  @override
  void dispose() {
    _descriptionController.dispose();
    super.dispose();
  }

  Future<void> _loadCurrentSeat() async {
    setState(() {
      _isLoading = true;
      _errorMessage = null;
    });
    try {
      final authService = Provider.of<AuthService>(context, listen: false);
      final user = authService.currentUser;
      if (user == null) throw Exception('Vui lòng đăng nhập');
      final booking = await _bookingService.getUpcomingBooking(user.id);
      if (booking == null || booking['status'] != 'CONFIRMED') {
        throw Exception(
          'Bạn cần có chỗ ngồi đã xác nhận để gửi báo cáo tình trạng ghế',
        );
      }
      setState(() {
        _seatId = booking['seatId'];
        _seatCode = booking['seatCode'] ?? '';
        _isLoading = false;
      });
    } catch (e) {
      setState(() {
        _errorMessage = ErrorDisplayWidget.toVietnamese(e);
        _isLoading = false;
      });
    }
  }

  Future<void> _pickImage() async {
    final picked = await _imagePicker.pickImage(
      source: ImageSource.gallery,
      imageQuality: 80,
    );
    if (picked != null) {
      setState(() => _selectedImage = File(picked.path));
    }
  }

  Future<void> _submit() async {
    if (_seatId == null) return;
    if (_selectedIssueType == null) {
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(const SnackBar(content: Text('Vui lòng chọn loại sự cố')));
      return;
    }

    setState(() => _isSubmitting = true);
    try {
      final authService = Provider.of<AuthService>(context, listen: false);
      final token = await authService.getValidToken();
      if (token == null) throw Exception('Phiên đăng nhập không hợp lệ');
      await _seatStatusReportService.createReport(
        token: token,
        seatId: _seatId!,
        issueType: _selectedIssueType!,
        description: _descriptionController.text.trim(),
        image: _selectedImage,
      );
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Đã gửi báo cáo tình trạng ghế thành công'),
        ),
      );
      _descriptionController.clear();
      setState(() {
        _selectedIssueType = null;
        _selectedImage = null;
      });
    } catch (e) {
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(SnackBar(content: Text('Không thể gửi báo cáo: $e')));
    } finally {
      if (mounted) setState(() => _isSubmitting = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Báo cáo tình trạng ghế'),
        actions: [
          IconButton(
            icon: const Icon(Icons.history),
            onPressed: () {
              Navigator.push(
                context,
                MaterialPageRoute(
                  builder: (_) => const SeatStatusReportHistoryScreen(),
                ),
              );
            },
          ),
        ],
      ),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator())
          : _errorMessage != null
          ? ErrorDisplayWidget(
              message: _errorMessage!,
              onRetry: _loadCurrentSeat,
            )
          : SingleChildScrollView(
              padding: const EdgeInsets.all(20),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    'Ghế hiện tại: $_seatCode',
                    style: const TextStyle(
                      fontSize: 18,
                      fontWeight: FontWeight.w700,
                    ),
                  ),
                  const SizedBox(height: 16),
                  DropdownButtonFormField<String>(
                    initialValue: _selectedIssueType,
                    decoration: const InputDecoration(
                      labelText: 'Loại sự cố',
                      border: OutlineInputBorder(),
                    ),
                    items: _issueTypes
                        .map(
                          (item) => DropdownMenuItem(
                            value: item['value'],
                            child: Text(item['label']!),
                          ),
                        )
                        .toList(),
                    onChanged: (value) =>
                        setState(() => _selectedIssueType = value),
                  ),
                  const SizedBox(height: 16),
                  TextField(
                    controller: _descriptionController,
                    maxLines: 4,
                    decoration: const InputDecoration(
                      labelText: 'Mô tả chi tiết',
                      hintText: 'Mô tả tình trạng ghế để thủ thư dễ xử lý hơn',
                      border: OutlineInputBorder(),
                    ),
                  ),
                  const SizedBox(height: 16),
                  OutlinedButton.icon(
                    onPressed: _pickImage,
                    icon: const Icon(Icons.image_outlined),
                    label: Text(
                      _selectedImage == null
                          ? 'Chọn hình ảnh (tuỳ chọn)'
                          : 'Đổi hình ảnh',
                    ),
                  ),
                  if (_selectedImage != null) ...[
                    const SizedBox(height: 12),
                    ClipRRect(
                      borderRadius: BorderRadius.circular(12),
                      child: Image.file(
                        _selectedImage!,
                        height: 180,
                        width: double.infinity,
                        fit: BoxFit.cover,
                      ),
                    ),
                  ],
                  const SizedBox(height: 24),
                  SizedBox(
                    width: double.infinity,
                    child: ElevatedButton(
                      onPressed: _isSubmitting ? null : _submit,
                      child: Text(
                        _isSubmitting ? 'Đang gửi...' : 'Gửi báo cáo',
                      ),
                    ),
                  ),
                ],
              ),
            ),
    );
  }
}
