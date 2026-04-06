import 'dart:io';
import 'package:flutter/material.dart';
import 'package:image_picker/image_picker.dart';
import 'package:intl/intl.dart';
import 'package:provider/provider.dart';
import 'package:qr_flutter/qr_flutter.dart';
import 'package:url_launcher/url_launcher.dart';
import 'package:slib/assets/colors.dart';
import 'package:slib/models/user_profile.dart';
import 'package:slib/models/student_profile.dart';
import 'package:slib/services/auth/auth_service.dart';
import 'package:slib/services/user/student_profile_service.dart';
import 'package:slib/views/authentication/change_password_screen.dart';
import 'package:slib/views/widgets/error_display_widget.dart';

class ProfileInfoScreen extends StatefulWidget {
  final UserProfile user;

  const ProfileInfoScreen({super.key, required this.user});

  @override
  State<ProfileInfoScreen> createState() => _ProfileInfoScreenState();
}

class _ProfileInfoScreenState extends State<ProfileInfoScreen> {
  late UserProfile _user;
  bool _isEditing = false;
  bool _isLoading = false;

  late TextEditingController _fullNameController;
  late TextEditingController _phoneController;
  DateTime? _selectedDob;

  File? _selectedImage;
  final ImagePicker _picker = ImagePicker();

  // Student profile stats
  StudentProfile? _studentProfile;

  @override
  void initState() {
    super.initState();
    _user = widget.user;
    _fullNameController = TextEditingController(text: _user.fullName);
    _phoneController = TextEditingController(text: _user.phone ?? '');
    if (_user.dob != null) {
      try {
        _selectedDob = DateTime.parse(_user.dob!);
      } catch (_) {}
    }
    _loadStudentProfile();
  }

  Future<void> _loadStudentProfile() async {
    final authService = context.read<AuthService>();
    final profileService = StudentProfileService(authService);
    final profile = await profileService.getMyProfile();
    if (mounted && profile != null) {
      setState(() {
        _studentProfile = profile;
      });
    }
  }

  @override
  void dispose() {
    _fullNameController.dispose();
    _phoneController.dispose();
    super.dispose();
  }

  Future<void> _openFAP() async {
    final Uri url = Uri.parse('https://fap.fpt.edu.vn');
    if (!await launchUrl(url, mode: LaunchMode.externalApplication)) {
      debugPrint('Không thể mở FAP');
    }
  }

  Future<void> _pickImage() async {
    final XFile? image = await _picker.pickImage(
      source: ImageSource.gallery,
      maxWidth: 512,
      maxHeight: 512,
      imageQuality: 85,
    );
    if (image != null) {
      setState(() {
        _selectedImage = File(image.path);
      });
    }
  }

  Future<void> _selectDate() async {
    final DateTime? picked = await showDatePicker(
      context: context,
      initialDate: _selectedDob ?? DateTime(2000, 1, 1),
      firstDate: DateTime(1950),
      lastDate: DateTime.now(),
      locale: const Locale('vi', 'VN'),
    );
    if (picked != null) {
      setState(() {
        _selectedDob = picked;
      });
    }
  }

  void _navigateToChangePassword() {
    Navigator.push(
      context,
      MaterialPageRoute(
        builder: (_) => const ChangePasswordScreen(isFirstLogin: false),
      ),
    );
  }

  Future<void> _saveProfile() async {
    setState(() => _isLoading = true);

    try {
      final authService = context.read<AuthService>();
      final profileService = StudentProfileService(authService);

      // Upload avatar nếu có chọn ảnh mới
      String? newAvatarUrl;
      if (_selectedImage != null) {
        newAvatarUrl = await profileService.uploadAvatar(_selectedImage!);
      }

      // Cập nhật thông tin profile (không đổi tên)
      final success = await profileService.updateProfile(
        phone: _phoneController.text.trim().isEmpty
            ? null
            : _phoneController.text.trim(),
        dob: _selectedDob != null
            ? DateFormat('yyyy-MM-dd').format(_selectedDob!)
            : null,
      );

      if (success) {
        // Làm mới dữ liệu user
        await authService.checkLoginStatus();

        if (mounted) {
          setState(() {
            _isEditing = false;
            _selectedImage = null;
            // Cập nhật dữ liệu user local (không đổi tên)
            _user = _user.copyWith(
              phone: _phoneController.text.trim().isEmpty
                  ? null
                  : _phoneController.text.trim(),
              dob: _selectedDob != null
                  ? DateFormat('yyyy-MM-dd').format(_selectedDob!)
                  : null,
              avtUrl: newAvatarUrl ?? _user.avtUrl,
            );
          });
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(
              content: Text('Cập nhật thành công!'),
              backgroundColor: Colors.green,
            ),
          );
        }
      } else {
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(
              content: Text('Cập nhật thất bại, vui lòng thử lại'),
              backgroundColor: Colors.red,
            ),
          );
        }
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(ErrorDisplayWidget.toVietnamese(e)),
            backgroundColor: Colors.red,
          ),
        );
      }
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final bool isTeacher = _user.isTeacher;
    final String profileTitle = isTeacher ? 'Hồ sơ giáo viên' : 'Hồ sơ cá nhân';
    final String userCodeLabel = isTeacher ? 'Mã giáo viên' : 'Mã người dùng';

    return Scaffold(
      backgroundColor: const Color(0xFFF5F7FA),
      appBar: AppBar(
        title: Text(
          profileTitle,
          style: TextStyle(fontWeight: FontWeight.bold, color: Colors.black87),
        ),
        backgroundColor: Colors.white,
        centerTitle: true,
        elevation: 0,
        iconTheme: const IconThemeData(color: Colors.black87),
        actions: [
          if (!_isEditing)
            IconButton(
              icon: const Icon(Icons.edit_outlined),
              onPressed: () => setState(() => _isEditing = true),
            )
          else
            TextButton(
              onPressed: _isLoading
                  ? null
                  : () {
                      setState(() {
                        _isEditing = false;
                        _selectedImage = null;
                        _fullNameController.text = _user.fullName;
                        _phoneController.text = _user.phone ?? '';
                      });
                    },
              child: const Text('Hủy', style: TextStyle(color: Colors.grey)),
            ),
        ],
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(20),
        child: Column(
          children: [
            // 1. THẺ SINH VIÊN ĐIỆN TỬ & QR CODE THẬT
            _buildDigitalStudentCard(),

            const SizedBox(height: 25),

            // 2. THÔNG TIN CÁ NHÂN (Editable)
            _buildSectionTitle("Thông tin cá nhân"),
            const SizedBox(height: 10),
            _buildPersonalInfoSection(),

            const SizedBox(height: 25),

            // 3. THÔNG TIN HỌC VỤ
            _buildSectionTitle("Thông tin học vụ"),
            const SizedBox(height: 10),
            _buildInfoGroup([
              _buildInfoRow(
                Icons.email_outlined,
                "Email",
                _user.email ?? 'Chưa cập nhật',
              ),
              _buildDivider(),
              _buildInfoRow(
                Icons.badge_outlined,
                userCodeLabel,
                _user.studentCode,
              ),
              _buildDivider(),
              InkWell(
                onTap: _openFAP,
                child: Padding(
                  padding: const EdgeInsets.symmetric(
                    horizontal: 20,
                    vertical: 16,
                  ),
                  child: Row(
                    children: [
                      const Icon(
                        Icons.language,
                        color: Colors.orange,
                        size: 22,
                      ),
                      const SizedBox(width: 16),
                      const Expanded(
                        child: Text(
                          "Kiểm tra trên FAP",
                          style: TextStyle(
                            fontSize: 15,
                            fontWeight: FontWeight.bold,
                            color: Colors.orange,
                          ),
                        ),
                      ),
                      Icon(
                        Icons.open_in_new,
                        size: 16,
                        color: Colors.orange.withValues(alpha: 0.7),
                      ),
                    ],
                  ),
                ),
              ),
            ]),

            const SizedBox(height: 25),

            // 4. THỐNG KÊ
            _buildSectionTitle("Thống kê hoạt động"),
            const SizedBox(height: 10),
            Row(
              children: [
                Expanded(
                  child: _buildStatCard(
                    Icons.bookmark_added,
                    Colors.blue,
                    "${_studentProfile?.totalBookings ?? 0}",
                    "Lượt đặt chỗ",
                  ),
                ),
                const SizedBox(width: 15),
                Expanded(
                  child: _buildStatCard(
                    Icons.warning_amber,
                    Colors.orange,
                    "${_studentProfile?.violationCount ?? 0}",
                    "Vi phạm",
                  ),
                ),
              ],
            ),
            const SizedBox(height: 15),
            Row(
              children: [
                Expanded(
                  child: _buildStatCard(
                    Icons.access_time,
                    Colors.green,
                    _studentProfile?.formattedStudyHours ?? "0h",
                    "Giờ học tập",
                  ),
                ),
                const SizedBox(width: 15),
                Expanded(
                  child: _buildStatCard(
                    Icons.star_border,
                    Colors.purple,
                    "${_studentProfile?.reputationScore ?? _user.reputationScore}",
                    "Điểm uy tín",
                  ),
                ),
              ],
            ),
            if ((_studentProfile?.bookingRestriction?.hasNotice ?? false)) ...[
              const SizedBox(height: 16),
              _buildBookingRestrictionCard(
                _studentProfile!.bookingRestriction!,
              ),
            ],

            // Nút lưu khi đang edit
            if (_isEditing) ...[
              const SizedBox(height: 30),
              SizedBox(
                width: double.infinity,
                height: 52,
                child: ElevatedButton(
                  onPressed: _isLoading ? null : _saveProfile,
                  style: ElevatedButton.styleFrom(
                    backgroundColor: AppColors.brandColor,
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(16),
                    ),
                  ),
                  child: _isLoading
                      ? const SizedBox(
                          width: 24,
                          height: 24,
                          child: CircularProgressIndicator(
                            color: Colors.white,
                            strokeWidth: 2,
                          ),
                        )
                      : const Text(
                          'Lưu thay đổi',
                          style: TextStyle(
                            fontSize: 16,
                            fontWeight: FontWeight.bold,
                            color: Colors.white,
                          ),
                        ),
                ),
              ),
            ],

            const SizedBox(height: 30),
          ],
        ),
      ),
    );
  }

  Widget _buildDigitalStudentCard() {
    String firstLetter = _user.fullName.isNotEmpty
        ? _user.fullName[0].toUpperCase()
        : "S";

    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(24),
      decoration: BoxDecoration(
        gradient: LinearGradient(
          colors: [AppColors.brandColor, const Color(0xFFFF9052)],
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
        ),
        borderRadius: BorderRadius.circular(24),
        boxShadow: [
          BoxShadow(
            color: AppColors.brandColor.withValues(alpha: 0.4),
            blurRadius: 20,
            offset: const Offset(0, 10),
          ),
        ],
      ),
      child: Column(
        children: [
          // Avatar với nút edit
          GestureDetector(
            onTap: _isEditing ? _pickImage : null,
            child: Stack(
              children: [
                Container(
                  padding: const EdgeInsets.all(4),
                  decoration: BoxDecoration(
                    shape: BoxShape.circle,
                    border: Border.all(
                      color: Colors.white.withValues(alpha: 0.5),
                      width: 2,
                    ),
                  ),
                  child: CircleAvatar(
                    radius: 40,
                    backgroundColor: Colors.white,
                    backgroundImage: _selectedImage != null
                        ? FileImage(_selectedImage!)
                        : (_user.avtUrl != null && _user.avtUrl!.isNotEmpty
                              ? NetworkImage(_user.avtUrl!) as ImageProvider
                              : null),
                    child:
                        (_selectedImage == null &&
                            (_user.avtUrl == null || _user.avtUrl!.isEmpty))
                        ? Text(
                            firstLetter,
                            style: TextStyle(
                              fontSize: 32,
                              fontWeight: FontWeight.bold,
                              color: AppColors.brandColor,
                            ),
                          )
                        : null,
                  ),
                ),
                if (_isEditing)
                  Positioned(
                    right: 0,
                    bottom: 0,
                    child: Container(
                      padding: const EdgeInsets.all(6),
                      decoration: const BoxDecoration(
                        color: Colors.white,
                        shape: BoxShape.circle,
                      ),
                      child: Icon(
                        Icons.camera_alt,
                        size: 18,
                        color: AppColors.brandColor,
                      ),
                    ),
                  ),
              ],
            ),
          ),
          const SizedBox(height: 16),
          Text(
            _user.fullName.toUpperCase(),
            style: const TextStyle(
              fontSize: 20,
              fontWeight: FontWeight.bold,
              color: Colors.white,
              letterSpacing: 1,
            ),
            textAlign: TextAlign.center,
          ),
          const SizedBox(height: 4),
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 4),
            decoration: BoxDecoration(
              color: Colors.white.withValues(alpha: 0.2),
              borderRadius: BorderRadius.circular(20),
            ),
            child: Text(
              _user.studentCode,
              style: const TextStyle(
                color: Colors.white,
                fontWeight: FontWeight.w600,
                letterSpacing: 1.5,
              ),
            ),
          ),
          const SizedBox(height: 24),

          // QR CODE
          Container(
            padding: const EdgeInsets.all(12),
            decoration: BoxDecoration(
              color: Colors.white,
              borderRadius: BorderRadius.circular(12),
            ),
            child: Column(
              children: [
                QrImageView(
                  data: _user.studentCode,
                  version: QrVersions.auto,
                  size: 140.0,
                  backgroundColor: Colors.white,
                ),
                const SizedBox(height: 8),
                Text(
                  "Mã QR người dùng",
                  style: TextStyle(fontSize: 10, color: Colors.grey[600]),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildPersonalInfoSection() {
    if (_isEditing) {
      return _buildInfoGroup([
        // Họ tên (chỉ xem, không cho sửa)
        _buildInfoRow(Icons.person_outline, "Họ tên", _user.fullName),
        _buildDivider(),
        // Số điện thoại (cho sửa)
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 12),
          child: Row(
            children: [
              Icon(Icons.phone_outlined, color: Colors.grey[400], size: 22),
              const SizedBox(width: 16),
              Expanded(
                child: TextField(
                  controller: _phoneController,
                  keyboardType: TextInputType.phone,
                  decoration: const InputDecoration(
                    labelText: 'Số điện thoại',
                    border: OutlineInputBorder(),
                    isDense: true,
                  ),
                ),
              ),
            ],
          ),
        ),
        _buildDivider(),
        // Ngày sinh (cho sửa)
        InkWell(
          onTap: _selectDate,
          child: Padding(
            padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 16),
            child: Row(
              children: [
                Icon(Icons.cake_outlined, color: Colors.grey[400], size: 22),
                const SizedBox(width: 16),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        'Ngày sinh',
                        style: TextStyle(fontSize: 12, color: Colors.grey[500]),
                      ),
                      const SizedBox(height: 2),
                      Text(
                        _selectedDob != null
                            ? DateFormat('dd/MM/yyyy').format(_selectedDob!)
                            : 'Chạm để chọn',
                        style: const TextStyle(
                          fontSize: 15,
                          fontWeight: FontWeight.w500,
                          color: Colors.black87,
                        ),
                      ),
                    ],
                  ),
                ),
                const Icon(Icons.chevron_right, color: Colors.grey),
              ],
            ),
          ),
        ),
        _buildDivider(),
        // Đổi mật khẩu
        InkWell(
          onTap: _navigateToChangePassword,
          child: Padding(
            padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 16),
            child: Row(
              children: [
                Icon(Icons.lock_outline, color: AppColors.brandColor, size: 22),
                const SizedBox(width: 16),
                Expanded(
                  child: Text(
                    'Đổi mật khẩu',
                    style: TextStyle(
                      fontSize: 15,
                      fontWeight: FontWeight.w600,
                      color: AppColors.brandColor,
                    ),
                  ),
                ),
                Icon(
                  Icons.chevron_right,
                  color: AppColors.brandColor.withValues(alpha: 0.7),
                ),
              ],
            ),
          ),
        ),
      ]);
    }

    // Chế độ xem
    return _buildInfoGroup([
      _buildInfoRow(Icons.person_outline, "Họ tên", _user.fullName),
      _buildDivider(),
      _buildInfoRow(
        Icons.phone_outlined,
        "Số điện thoại",
        _user.phone ?? 'Chưa cập nhật',
      ),
      _buildDivider(),
      _buildInfoRow(
        Icons.cake_outlined,
        "Ngày sinh",
        _user.dob != null
            ? DateFormat('dd/MM/yyyy').format(DateTime.parse(_user.dob!))
            : 'Chưa cập nhật',
      ),
    ]);
  }

  Widget _buildBookingRestrictionCard(BookingRestrictionStatus restriction) {
    final bool isBlocked = restriction.isTemporarilyBlocked;
    final bool isDeniedNow = !restriction.allowedNow;
    final Color accentColor = isBlocked
        ? Colors.red
        : (isDeniedNow ? Colors.orange : Colors.blue);

    String message = restriction.summaryMessage ?? '';
    final remainingText = restriction.remainingText;
    if (isBlocked && remainingText != null) {
      message = '$message Còn khoảng $remainingText.';
    } else if (restriction.blockedUntil != null) {
      message =
          '$message Mở lại sau ${DateFormat('HH:mm dd/MM/yyyy').format(restriction.blockedUntil!)}.';
    }

    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: accentColor.withValues(alpha: 0.08),
        borderRadius: BorderRadius.circular(18),
        border: Border.all(color: accentColor.withValues(alpha: 0.25)),
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            padding: const EdgeInsets.all(10),
            decoration: BoxDecoration(
              color: accentColor.withValues(alpha: 0.12),
              borderRadius: BorderRadius.circular(12),
            ),
            child: Icon(
              isBlocked
                  ? Icons.lock_clock_rounded
                  : (isDeniedNow
                        ? Icons.warning_amber_rounded
                        : Icons.info_outline_rounded),
              color: accentColor,
              size: 22,
            ),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  isBlocked
                      ? 'Đặt chỗ đang bị tạm khóa'
                      : (isDeniedNow
                            ? 'Đặt chỗ đang bị hạn chế'
                            : 'Giới hạn đặt chỗ hiện tại'),
                  style: TextStyle(
                    fontSize: 15,
                    fontWeight: FontWeight.w700,
                    color: accentColor,
                  ),
                ),
                const SizedBox(height: 6),
                Text(
                  message,
                  style: const TextStyle(
                    fontSize: 14,
                    color: Colors.black87,
                    height: 1.45,
                  ),
                ),
                if ((restriction.policyHint?.isNotEmpty ?? false) &&
                    restriction.policyHint !=
                        restriction.restrictionReason) ...[
                  const SizedBox(height: 8),
                  Text(
                    restriction.policyHint!,
                    style: TextStyle(
                      fontSize: 13,
                      color: Colors.grey[700],
                      height: 1.4,
                    ),
                  ),
                ],
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildSectionTitle(String title) {
    return SizedBox(
      width: double.infinity,
      child: Text(
        title,
        style: TextStyle(
          color: Colors.grey[600],
          fontSize: 14,
          fontWeight: FontWeight.w600,
        ),
      ),
    );
  }

  Widget _buildInfoGroup(List<Widget> children) {
    return Container(
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(20),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withValues(alpha: 0.03),
            blurRadius: 15,
            offset: const Offset(0, 5),
          ),
        ],
      ),
      clipBehavior: Clip.hardEdge,
      child: Column(children: children),
    );
  }

  Widget _buildInfoRow(IconData icon, String label, String value) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 16),
      child: Row(
        children: [
          Icon(icon, color: Colors.grey[400], size: 22),
          const SizedBox(width: 16),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  label,
                  style: TextStyle(fontSize: 12, color: Colors.grey[500]),
                ),
                const SizedBox(height: 2),
                Text(
                  value,
                  style: const TextStyle(
                    fontSize: 15,
                    fontWeight: FontWeight.w500,
                    color: Colors.black87,
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildDivider() {
    return Divider(
      height: 1,
      thickness: 1,
      indent: 58,
      endIndent: 0,
      color: Colors.grey[100],
    );
  }

  Widget _buildStatCard(
    IconData icon,
    Color color,
    String value,
    String label,
  ) {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(20),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withValues(alpha: 0.03),
            blurRadius: 10,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            padding: const EdgeInsets.all(8),
            decoration: BoxDecoration(
              color: color.withValues(alpha: 0.1),
              shape: BoxShape.circle,
            ),
            child: Icon(icon, color: color, size: 20),
          ),
          const SizedBox(height: 12),
          Text(
            value,
            style: const TextStyle(
              fontSize: 20,
              fontWeight: FontWeight.bold,
              color: Colors.black87,
            ),
          ),
          const SizedBox(height: 4),
          Text(label, style: TextStyle(fontSize: 12, color: Colors.grey[500])),
        ],
      ),
    );
  }
}
