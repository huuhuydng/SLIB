import 'dart:io';
import 'package:flutter/material.dart';
import 'package:image_picker/image_picker.dart';
import 'package:provider/provider.dart';
import '../../services/auth/auth_service.dart';
import '../../services/support/support_request_service.dart';
import '../widgets/error_display_widget.dart';
import 'support_request_history_screen.dart';

class SupportRequestScreen extends StatefulWidget {
  const SupportRequestScreen({Key? key}) : super(key: key);

  @override
  State<SupportRequestScreen> createState() => _SupportRequestScreenState();
}

class _SupportRequestScreenState extends State<SupportRequestScreen> {
  final _descriptionController = TextEditingController();
  final _supportRequestService = SupportRequestService();
  final _imagePicker = ImagePicker();
  final List<File> _selectedImages = [];
  bool _isSubmitting = false;
  final int _maxImages = 5;

  @override
  void initState() {
    super.initState();
    _descriptionController.addListener(() {
      setState(() {}); // Rebuild để cập nhật trạng thái nút gửi
    });
  }

  @override
  void dispose() {
    _descriptionController.dispose();
    super.dispose();
  }

  bool get _canSubmit => _descriptionController.text.trim().isNotEmpty;

  Future<void> _pickImages() async {
    if (_selectedImages.length >= _maxImages) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text('Tối đa $_maxImages ảnh'),
          backgroundColor: Colors.orange,
        ),
      );
      return;
    }

    showModalBottomSheet(
      context: context,
      backgroundColor: Colors.transparent,
      builder: (ctx) => Container(
        decoration: const BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
        ),
        padding: const EdgeInsets.symmetric(vertical: 20),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Container(
              width: 40, height: 4,
              decoration: BoxDecoration(
                color: Colors.grey[300],
                borderRadius: BorderRadius.circular(2),
              ),
            ),
            const SizedBox(height: 16),
            const Text(
              'Chọn ảnh',
              style: TextStyle(fontSize: 18, fontWeight: FontWeight.w600),
            ),
            const SizedBox(height: 16),
            ListTile(
              leading: Container(
                padding: const EdgeInsets.all(10),
                decoration: BoxDecoration(
                  color: const Color(0xFFFFF7F2),
                  borderRadius: BorderRadius.circular(12),
                ),
                child: const Icon(Icons.camera_alt, color: Color(0xFFFF751F)),
              ),
              title: const Text('Chụp ảnh'),
              onTap: () async {
                Navigator.pop(ctx);
                final XFile? photo = await _imagePicker.pickImage(
                  source: ImageSource.camera,
                  imageQuality: 80,
                );
                if (photo != null) {
                  setState(() => _selectedImages.add(File(photo.path)));
                }
              },
            ),
            ListTile(
              leading: Container(
                padding: const EdgeInsets.all(10),
                decoration: BoxDecoration(
                  color: const Color(0xFFFFF7F2),
                  borderRadius: BorderRadius.circular(12),
                ),
                child: const Icon(Icons.photo_library, color: Color(0xFFFF751F)),
              ),
              title: const Text('Chọn từ thư viện'),
              onTap: () async {
                Navigator.pop(ctx);
                final remaining = _maxImages - _selectedImages.length;
                final List<XFile> photos = await _imagePicker.pickMultiImage(
                  imageQuality: 80,
                  limit: remaining,
                );
                if (photos.isNotEmpty) {
                  setState(() {
                    for (final photo in photos) {
                      if (_selectedImages.length < _maxImages) {
                        _selectedImages.add(File(photo.path));
                      }
                    }
                  });
                }
              },
            ),
            const SizedBox(height: 16),
          ],
        ),
      ),
    );
  }

  void _removeImage(int index) {
    setState(() => _selectedImages.removeAt(index));
  }

  Future<void> _submitRequest() async {
    if (!_canSubmit) return;

    setState(() => _isSubmitting = true);

    try {
      final authService = Provider.of<AuthService>(context, listen: false);
      final token = await authService.getToken();
      if (token == null) throw Exception('Chưa đăng nhập');

      await _supportRequestService.createRequest(
        token: token,
        description: _descriptionController.text.trim(),
        images: _selectedImages.isNotEmpty ? _selectedImages : null,
      );

      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('Gửi yêu cầu trợ giúp thành công! Thủ thư sẽ liên hệ bạn sớm nhất'),
            backgroundColor: Color(0xFF4CAF50),
            duration: Duration(seconds: 3),
          ),
        );
        // Quay về và mở màn hình lịch sử
        Navigator.pop(context, true);
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('Gửi thất bại: ${ErrorDisplayWidget.toVietnamese(e)}'),
            backgroundColor: Colors.red,
          ),
        );
      }
    } finally {
      if (mounted) setState(() => _isSubmitting = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFFF5F5F5),
      body: Column(
        children: [
          _buildHeader(),
          Expanded(
            child: GestureDetector(
              onTap: () => FocusScope.of(context).unfocus(),
              child: SingleChildScrollView(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    _buildTitleSection(),
                    _buildFormCard(),
                  ],
                ),
              ),
            ),
          ),
          _buildBottomButton(),
        ],
      ),
    );
  }

  /// Header gradient hồng
  Widget _buildHeader() {
    return Container(
      decoration: const BoxDecoration(
        gradient: LinearGradient(
          colors: [Color(0xFFFFB87A), Color(0xFFFFF7F2), Color(0xFFF5F5F5)],
          begin: Alignment.topCenter,
          end: Alignment.bottomCenter,
        ),
      ),
      child: SafeArea(
        bottom: false,
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 4, vertical: 8),
          child: Row(
            children: [
              IconButton(
                icon: const Icon(Icons.arrow_back_ios, color: Color(0xFF333333), size: 20),
                onPressed: () => Navigator.pop(context),
              ),
              const Expanded(
                child: Text(
                  'Gửi yêu cầu trợ giúp',
                  style: TextStyle(
                    color: Color(0xFF1A1A1A),
                    fontSize: 18,
                    fontWeight: FontWeight.w600,
                  ),
                ),
              ),
              IconButton(
                icon: const Icon(Icons.home_outlined, color: Color(0xFF333333), size: 24),
                onPressed: () => Navigator.of(context).popUntil((route) => route.isFirst),
              ),
            ],
          ),
        ),
      ),
    );
  }

  /// Tiêu đề + thông tin hướng dẫn
  Widget _buildTitleSection() {
    return Padding(
      padding: const EdgeInsets.fromLTRB(20, 16, 20, 8),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text(
            'Bạn cần trợ giúp thêm?',
            style: TextStyle(
              fontSize: 20,
              fontWeight: FontWeight.w700,
              color: Color(0xFF1A1A1A),
            ),
          ),
          const SizedBox(height: 12),
          Text(
            'Thông tin vấn đề càng chi tiết thì SLIB càng hỗ trợ bạn nhanh chóng, hiệu quả hơn',
            style: TextStyle(
              fontSize: 14,
              color: Colors.grey[600],
              height: 1.5,
            ),
          ),
        ],
      ),
    );
  }

  /// Form card chính (mô tả + ảnh)
  Widget _buildFormCard() {
    return Container(
      margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(16),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(0.04),
            blurRadius: 10,
            offset: const Offset(0, 2),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // Mô tả vấn đề
          _buildDescriptionField(),
          const SizedBox(height: 20),
          // Thêm ảnh
          _buildImageSection(),
        ],
      ),
    );
  }

  Widget _buildDescriptionField() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        // TextField với label floating
        TextField(
          controller: _descriptionController,
          maxLines: 5,
          maxLength: 400,
          decoration: InputDecoration(
            labelText: 'Mô tả vấn đề',
            labelStyle: TextStyle(color: Colors.grey[600], fontSize: 14),
            floatingLabelBehavior: FloatingLabelBehavior.always,
            hintText: 'Vui lòng mô tả chi tiết vấn đề của bạn',
            hintStyle: TextStyle(color: Colors.grey[400], fontSize: 14, height: 1.5),
            border: OutlineInputBorder(
              borderRadius: BorderRadius.circular(12),
              borderSide: BorderSide(color: Colors.grey[300]!),
            ),
            enabledBorder: OutlineInputBorder(
              borderRadius: BorderRadius.circular(12),
              borderSide: BorderSide(color: Colors.grey[300]!),
            ),
            focusedBorder: OutlineInputBorder(
              borderRadius: BorderRadius.circular(12),
              borderSide: const BorderSide(color: Color(0xFFFF751F), width: 1.5),
            ),
            contentPadding: const EdgeInsets.all(14),
            counterStyle: TextStyle(color: Colors.grey[400], fontSize: 12),
          ),
          style: const TextStyle(fontSize: 14, height: 1.5, color: Color(0xFF333333)),
        ),
      ],
    );
  }

  Widget _buildImageSection() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          'Thêm ảnh mô tả vấn đề/lỗi để được hỗ trợ nhanh hơn',
          style: TextStyle(
            fontSize: 14,
            color: Colors.grey[700],
            fontWeight: FontWeight.w500,
          ),
        ),
        const SizedBox(height: 12),

        // Hiển thị ảnh đã chọn + nút thêm ảnh
        Wrap(
          spacing: 10,
          runSpacing: 10,
          children: [
            // Các ảnh đã chọn
            ..._selectedImages.asMap().entries.map((entry) =>
              _buildImageThumbnail(entry.key),
            ),
            // Nút thêm ảnh
            if (_selectedImages.length < _maxImages)
              _buildAddImageButton(),
          ],
        ),
      ],
    );
  }

  Widget _buildImageThumbnail(int index) {
    return Stack(
      children: [
        ClipRRect(
          borderRadius: BorderRadius.circular(12),
          child: Image.file(
            _selectedImages[index],
            width: 72,
            height: 72,
            fit: BoxFit.cover,
          ),
        ),
        Positioned(
          top: -2,
          right: -2,
          child: GestureDetector(
            onTap: () => _removeImage(index),
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
    );
  }

  Widget _buildAddImageButton() {
    return GestureDetector(
      onTap: _pickImages,
      child: Container(
        width: 72,
        height: 72,
        decoration: BoxDecoration(
          color: const Color(0xFFFFF7F2).withOpacity(0.5),
          borderRadius: BorderRadius.circular(12),
          border: Border.all(color: const Color(0xFFFF751F).withOpacity(0.3)),
        ),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(Icons.add_a_photo_outlined, color: const Color(0xFFFF751F).withOpacity(0.7), size: 22),
            const SizedBox(height: 4),
            Text(
              'Thêm ảnh',
              style: TextStyle(
                fontSize: 10,
                color: Colors.grey[600],
                fontWeight: FontWeight.w500,
              ),
            ),
          ],
        ),
      ),
    );
  }

  /// Nút gửi ở bottom - mờ khi chưa nhập mô tả
  Widget _buildBottomButton() {
    return Container(
      padding: EdgeInsets.fromLTRB(20, 12, 20, MediaQuery.of(context).padding.bottom + 12),
      decoration: BoxDecoration(
        color: Colors.white,
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(0.05),
            blurRadius: 10,
            offset: const Offset(0, -2),
          ),
        ],
      ),
      child: SizedBox(
        width: double.infinity,
        height: 50,
        child: ElevatedButton(
          onPressed: (_canSubmit && !_isSubmitting) ? _submitRequest : null,
          style: ElevatedButton.styleFrom(
            backgroundColor: const Color(0xFFFF751F),
            foregroundColor: Colors.white,
            disabledBackgroundColor: const Color(0xFFF5F5F5),
            disabledForegroundColor: Colors.grey[400],
            elevation: 0,
            shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(25)),
          ),
          child: _isSubmitting
              ? const SizedBox(
                  width: 22, height: 22,
                  child: CircularProgressIndicator(color: Colors.white, strokeWidth: 2.5),
                )
              : Text(
                  'Gửi yêu cầu trợ giúp',
                  style: TextStyle(
                    fontSize: 16,
                    fontWeight: FontWeight.w600,
                    color: _canSubmit ? Colors.white : Colors.grey[400],
                  ),
                ),
        ),
      ),
    );
  }
}
