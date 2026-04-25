import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:slib/assets/colors.dart';
import 'package:slib/core/constants/api_constants.dart';
import 'package:slib/models/user_setting.dart';
import 'package:slib/models/user_profile.dart';
import 'package:slib/services/auth/auth_service.dart';
import 'package:slib/services/hce/hce_bridge.dart';
import 'package:slib/services/library/library_status_service.dart';
import 'package:slib/services/notification/notification_service.dart';
import 'package:slib/views/authentication/on_boarding_screen.dart';
import 'package:slib/views/profile/booking_history_screen.dart';
import 'package:slib/views/profile/complaint_history_screen.dart';
import 'package:slib/views/profile/profile_info_screen.dart';
import 'package:slib/views/profile/report_history_screen.dart';
import 'package:slib/views/profile/violation_history_screen.dart';
import 'package:slib/views/support/support_request_history_screen.dart';
import 'package:slib/views/support/support_request_screen.dart';

class SettingScreen extends StatefulWidget {
  final UserProfile? user;
  const SettingScreen({super.key, this.user});

  @override
  State<SettingScreen> createState() => _SettingScreenState();
}

class _SettingScreenState extends State<SettingScreen>
    with WidgetsBindingObserver {
  int _violationCount = 0;
  bool _deviceHceEnabled = false;

  Future<void> _pushScreen(
    Widget screen, {
    bool refreshViolationCount = false,
  }) async {
    await Navigator.push(
      context,
      MaterialPageRoute(builder: (context) => screen),
    );

    if (refreshViolationCount && mounted) {
      await _loadViolationCount();
    }
  }

  @override
  void initState() {
    super.initState();
    _loadViolationCount();
    _loadDeviceHceStatus();
    WidgetsBinding.instance.addObserver(this);
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed) {
      _loadDeviceHceStatus();
    }
  }

  Future<void> _loadDeviceHceStatus() async {
    final isNfcEnabled = await HceBridge.isNfcEnabled();
    final isDefaultPaymentService = await HceBridge.isDefaultPaymentService();
    if (mounted) {
      setState(() {
        _deviceHceEnabled = isNfcEnabled && isDefaultPaymentService;
      });
    }
  }

  Future<void> _loadViolationCount() async {
    final authService = Provider.of<AuthService>(context, listen: false);
    final user = authService.currentUser;
    if (user == null) return;

    int count = 0;
    try {
      final penaltyUrl = Uri.parse(
        "${ApiConstants.activityUrl}/penalties/${user.id}",
      );
      final penaltyRes = await authService.authenticatedRequest(
        'GET',
        penaltyUrl,
      );
      if (penaltyRes.statusCode == 200) {
        final List<dynamic> data = jsonDecode(
          utf8.decode(penaltyRes.bodyBytes),
        );
        count += data.where(_isEffectiveAutomaticPenalty).length;
      }
    } catch (_) {}

    try {
      final violationUrl = Uri.parse(
        "${ApiConstants.violationReportUrl}/against-me",
      );
      final violationRes = await authService.authenticatedRequest(
        'GET',
        violationUrl,
      );
      if (violationRes.statusCode == 200) {
        final List<dynamic> data = jsonDecode(
          utf8.decode(violationRes.bodyBytes),
        );
        count += data.where(_isEffectiveReportedViolation).length;
      }
    } catch (_) {}

    if (mounted) setState(() => _violationCount = count);
  }

  bool _isAutomaticPenalty(dynamic penalty) {
    if (penalty is! Map) return false;
    final type = penalty['transactionType']?.toString() ?? '';
    return type == 'NO_SHOW_PENALTY' ||
        type == 'LATE_CHECKIN_PENALTY' ||
        type == 'CHECK_OUT_LATE_PENALTY';
  }

  bool _hasAcceptedAppeal(dynamic item) {
    if (item is! Map) return false;
    return item['appealStatus']?.toString() == 'ACCEPTED';
  }

  bool _isEffectiveAutomaticPenalty(dynamic penalty) {
    return _isAutomaticPenalty(penalty) && !_hasAcceptedAppeal(penalty);
  }

  bool _isEffectiveReportedViolation(dynamic violation) {
    if (violation is! Map) return false;
    final pointDeducted = ((violation['pointDeducted'] ?? 0) as num).toInt();
    return violation['status'] == 'VERIFIED' &&
        pointDeducted > 0 &&
        !_hasAcceptedAppeal(violation);
  }

  @override
  Widget build(BuildContext context) {
    // 1. LẮNG NGHE DỮ LIỆU TỪ PROVIDER (Tự động cập nhật khi Cache/API thay đổi)
    final authService = context.watch<AuthService>();
    final currentUser =
        authService.currentUser ?? widget.user; // Ưu tiên lấy từ Provider
    final settings = authService.currentSetting;

    return Scaffold(
      backgroundColor: const Color(0xFFF5F7FA),
      appBar: AppBar(
        title: const Text(
          "Tài khoản & Cài đặt",
          style: TextStyle(
            fontWeight: FontWeight.bold,
            fontSize: 18,
            color: Colors.black87,
          ),
        ),
        backgroundColor: Colors.white,
        centerTitle: true,
        elevation: 0,
        scrolledUnderElevation: 0,
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 24),
        child: Column(
          children: [
            // 1. Profile Header
            _buildModernProfileHeader(currentUser),

            const SizedBox(height: 20),

            // 3. Nhóm Cấu hình Thư viện
            _buildSectionTitle("Cấu hình Thư viện"),
            const SizedBox(height: 10),

            // CHECK NULL:
            // - Nếu settings đã có (từ Cache hoặc API): Hiện luôn.
            // - Nếu null (lần đầu cài app chưa có cache): Hiện Loading nhỏ.
            if (settings != null)
              _buildSettingsGroup([
                _buildSwitchTile(
                  icon: Icons.nfc_rounded,
                  iconColor: Colors.deepOrange,
                  title: "Check-in HCE",
                  subtitle: "Sử dụng HCE để check-in",
                  value: _deviceHceEnabled,
                  onChanged: (val) async {
                    await _handleHceToggle(settings, val);
                  },
                ),
                _buildDivider(),
                _buildSwitchTile(
                  icon: Icons.auto_awesome_rounded,
                  iconColor: Colors.purple,
                  title: "Gợi ý thông minh (AI)",
                  subtitle: "Đề xuất chỗ ngồi phù hợp",
                  value: settings.isAiRecommendEnabled,
                  onChanged: (val) {
                    context.read<AuthService>().updateSetting(
                      settings.copyWith(isAiRecommendEnabled: val),
                    );
                  },
                ),
                _buildDivider(),
                _buildSwitchTile(
                  icon: Icons.notifications_none_rounded,
                  iconColor: Colors.blue,
                  title: "Thông báo đẩy",
                  subtitle: "Nhận thông báo từ hệ thống",
                  value: settings.isBookingRemindEnabled,
                  onChanged: (val) {
                    context.read<AuthService>().updateSetting(
                      settings.copyWith(isBookingRemindEnabled: val),
                    );
                  },
                ),
              ])
            else
              const Padding(
                padding: EdgeInsets.all(20),
                child: Center(
                  child: CircularProgressIndicator(color: AppColors.brandColor),
                ),
              ),

            const SizedBox(height: 30),

            // 4. Nhóm Quản lý (Giữ nguyên)
            _buildSectionTitle("Quản lý cá nhân"),
            const SizedBox(height: 10),
            _buildSettingsGroup([
              _buildNavTile(
                icon: Icons.person_outline_rounded,
                iconColor: Colors.blue,
                title: "Thông tin cá nhân",
                onTap: () async {
                  if (currentUser != null) {
                    await _pushScreen(ProfileInfoScreen(user: currentUser));
                  }
                },
              ),
              _buildDivider(),
              _buildNavTile(
                icon: Icons.history_edu_rounded,
                iconColor: Colors.teal,
                title: "Lịch sử đặt chỗ",
                onTap: () async {
                  await _pushScreen(const BookingHistoryScreen());
                },
              ),
              _buildDivider(),
              _buildNavTile(
                icon: Icons.warning_amber_rounded,
                iconColor: Colors.redAccent,
                title: "Lịch sử vi phạm",
                trailingText: "$_violationCount vi phạm",
                onTap: () async {
                  await _pushScreen(
                    const ViolationHistoryScreen(),
                    refreshViolationCount: true,
                  );
                },
              ),
              _buildDivider(),
              _buildNavTile(
                icon: Icons.report_outlined,
                iconColor: Colors.orange,
                title: "Lịch sử báo cáo",
                onTap: () async {
                  await _pushScreen(const ReportHistoryScreen());
                },
              ),
              _buildDivider(),
              _buildNavTile(
                icon: Icons.gavel_rounded,
                iconColor: Colors.deepOrange,
                title: "Lịch sử khiếu nại",
                onTap: () async {
                  await _pushScreen(const ComplaintHistoryScreen());
                },
              ),
            ]),

            const SizedBox(height: 30),

            // 5. Nhóm Hỗ trợ
            _buildSectionTitle("Hỗ trợ"),
            const SizedBox(height: 10),
            _buildSettingsGroup([
              _buildNavTile(
                icon: Icons.support_agent_rounded,
                iconColor: AppColors.brandColor,
                title: "Gửi yêu cầu hỗ trợ",
                subtitle: "Báo lỗi hoặc nhờ thủ thư xử lý vấn đề",
                onTap: () async {
                  await _pushScreen(const SupportRequestScreen());
                },
              ),
              _buildDivider(),
              _buildNavTile(
                icon: Icons.history_rounded,
                iconColor: Colors.indigo,
                title: "Yêu cầu đã gửi",
                subtitle: "Theo dõi trạng thái phản hồi từ thủ thư",
                onTap: () async {
                  await _pushScreen(const SupportRequestHistoryScreen());
                },
              ),
              _buildDivider(),
              _buildNavTile(
                icon: Icons.help_outline_rounded,
                iconColor: Colors.green,
                title: "Hướng dẫn nhanh",
                subtitle: "Check-in, đặt chỗ, điểm uy tín và kháng cáo",
                onTap: _showQuickGuideSheet,
              ),
            ]),

            const SizedBox(height: 40),

            // 6. Nút Đăng xuất
            SizedBox(
              width: double.infinity,
              child: TextButton(
                onPressed: _showLogoutDialog,
                style: TextButton.styleFrom(
                  padding: const EdgeInsets.symmetric(vertical: 16),
                  backgroundColor: const Color(0xFFFFEBEE),
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(16),
                  ),
                ),
                child: const Text(
                  "Đăng xuất",
                  style: TextStyle(
                    color: Colors.red,
                    fontWeight: FontWeight.bold,
                    fontSize: 16,
                  ),
                ),
              ),
            ),
            const SizedBox(height: 30),
          ],
        ),
      ),
    );
  }

  // --- CÁC WIDGET UI (GIỮ NGUYÊN STYLE) ---

  Widget _buildModernProfileHeader(UserProfile? user) {
    final fullName = user?.fullName ?? '';
    final avatarUrl = user?.avtUrl;
    final firstLetter = fullName.isNotEmpty ? fullName[0].toUpperCase() : "S";
    final hasAvatar = avatarUrl != null && avatarUrl.isNotEmpty;

    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        gradient: const LinearGradient(
          colors: [Colors.white, Color(0xFFFFF3E0)],
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
        ),
        borderRadius: BorderRadius.circular(24),
        boxShadow: [
          BoxShadow(
            color: Colors.orange.withValues(alpha: 0.1),
            blurRadius: 20,
            offset: const Offset(0, 10),
          ),
        ],
        border: Border.all(color: Colors.white, width: 2),
      ),
      child: Row(
        children: [
          Container(
            padding: const EdgeInsets.all(4),
            decoration: const BoxDecoration(
              color: Colors.white,
              shape: BoxShape.circle,
              boxShadow: [BoxShadow(color: Colors.black12, blurRadius: 5)],
            ),
            child: CircleAvatar(
              radius: 30,
              backgroundColor: AppColors.brandColor.withValues(alpha: 0.1),
              backgroundImage: hasAvatar ? NetworkImage(avatarUrl) : null,
              child: !hasAvatar
                  ? Text(
                      firstLetter,
                      style: const TextStyle(
                        fontSize: 24,
                        fontWeight: FontWeight.bold,
                        color: AppColors.brandColor,
                      ),
                    )
                  : null,
            ),
          ),
          const SizedBox(width: 16),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  fullName.isNotEmpty ? fullName : "Người dùng SLIB",
                  style: const TextStyle(
                    fontSize: 18,
                    fontWeight: FontWeight.bold,
                    color: Colors.black87,
                  ),
                ),
                const SizedBox(height: 6),
                Container(
                  padding: const EdgeInsets.symmetric(
                    horizontal: 10,
                    vertical: 4,
                  ),
                  decoration: BoxDecoration(
                    color: AppColors.brandColor.withValues(alpha: 0.1),
                    borderRadius: BorderRadius.circular(20),
                  ),
                  child: Text(
                    "MSSV: ${user?.studentCode ?? '...'}",
                    style: const TextStyle(
                      color: AppColors.brandColor,
                      fontSize: 12,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Future<void> _handleHceToggle(UserSetting settings, bool enabled) async {
    if (enabled) {
      final isNfcEnabled = await HceBridge.isNfcEnabled();
      if (!mounted) return;

      if (!isNfcEnabled) {
        _showHceSetupSnack(
          "Vui lòng bật NFC trước, sau đó chọn SLIB trong mục thanh toán không tiếp xúc.",
        );
        await HceBridge.openNfcSettings();
        await _loadDeviceHceStatus();
        return;
      }

      final isDefaultPaymentService = await HceBridge.isDefaultPaymentService();
      if (!mounted) return;

      if (!isDefaultPaymentService) {
        _showHceSetupSnack("Hãy chọn SLIB làm app thanh toán không tiếp xúc.");
        await HceBridge.requestDefaultPaymentService();
        await _loadDeviceHceStatus();
        return;
      }

      final authService = context.read<AuthService>();
      await authService.updateSetting(settings.copyWith(isHceEnabled: true));
      await _loadDeviceHceStatus();
    } else {
      final authService = context.read<AuthService>();
      await authService.updateSetting(settings.copyWith(isHceEnabled: false));
      await _loadDeviceHceStatus();
    }
  }

  void _showHceSetupSnack(String message) {
    if (!mounted) return;
    ScaffoldMessenger.of(context)
      ..hideCurrentSnackBar()
      ..showSnackBar(
        SnackBar(
          content: Text(message),
          backgroundColor: AppColors.brandColor,
          behavior: SnackBarBehavior.floating,
        ),
      );
  }

  Widget _buildSectionTitle(String title) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.only(left: 8),
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

  Widget _buildSettingsGroup(List<Widget> children) {
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

  Widget _buildSwitchTile({
    required IconData icon,
    required Color iconColor,
    required String title,
    required String subtitle,
    required bool value,
    required Function(bool) onChanged,
  }) {
    return SwitchListTile(
      value: value,
      onChanged: onChanged,
      activeThumbColor: Colors.white,
      activeTrackColor: AppColors.brandColor,
      inactiveThumbColor: Colors.white,
      inactiveTrackColor: Colors.grey.shade200,
      tileColor: Colors.white,
      contentPadding: const EdgeInsets.symmetric(horizontal: 20, vertical: 8),
      secondary: Container(
        padding: const EdgeInsets.all(10),
        decoration: BoxDecoration(
          color: iconColor.withValues(alpha: 0.1),
          shape: BoxShape.circle,
        ),
        child: Icon(icon, color: iconColor, size: 22),
      ),
      title: Text(
        title,
        style: const TextStyle(
          fontWeight: FontWeight.w600,
          fontSize: 15,
          color: Colors.black87,
        ),
      ),
      subtitle: Padding(
        padding: const EdgeInsets.only(top: 2),
        child: Text(
          subtitle,
          style: TextStyle(fontSize: 12, color: Colors.grey[500]),
        ),
      ),
    );
  }

  Widget _buildNavTile({
    required IconData icon,
    required Color iconColor,
    required String title,
    String? subtitle,
    String? trailingText,
    required VoidCallback onTap,
  }) {
    return Material(
      color: Colors.white,
      child: InkWell(
        onTap: onTap,
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 16),
          child: Row(
            children: [
              Container(
                padding: const EdgeInsets.all(10),
                decoration: BoxDecoration(
                  color: iconColor.withValues(alpha: 0.1),
                  shape: BoxShape.circle,
                ),
                child: Icon(icon, color: iconColor, size: 22),
              ),
              const SizedBox(width: 16),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      title,
                      style: const TextStyle(
                        fontWeight: FontWeight.w600,
                        fontSize: 15,
                        color: Colors.black87,
                      ),
                    ),
                    if (subtitle != null) ...[
                      const SizedBox(height: 4),
                      Text(
                        subtitle,
                        style: TextStyle(fontSize: 12, color: Colors.grey[500]),
                      ),
                    ],
                  ],
                ),
              ),
              if (trailingText != null)
                Flexible(
                  flex: 0,
                  child: Text(
                    trailingText,
                    style: const TextStyle(
                      color: Colors.grey,
                      fontSize: 13,
                      fontWeight: FontWeight.w500,
                    ),
                  ),
                ),
              if (trailingText != null) const SizedBox(width: 8),
              Icon(
                Icons.arrow_forward_ios_rounded,
                size: 16,
                color: Colors.grey[300],
              ),
            ],
          ),
        ),
      ),
    );
  }

  void _showQuickGuideSheet() {
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (sheetContext) {
        return DraggableScrollableSheet(
          initialChildSize: 0.72,
          minChildSize: 0.45,
          maxChildSize: 0.92,
          builder: (_, scrollController) {
            return Container(
              decoration: const BoxDecoration(
                color: Colors.white,
                borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
              ),
              child: Column(
                children: [
                  Container(
                    width: 40,
                    height: 4,
                    margin: const EdgeInsets.only(top: 12, bottom: 16),
                    decoration: BoxDecoration(
                      color: Colors.grey[300],
                      borderRadius: BorderRadius.circular(2),
                    ),
                  ),
                  Padding(
                    padding: const EdgeInsets.symmetric(horizontal: 20),
                    child: Row(
                      children: [
                        Container(
                          padding: const EdgeInsets.all(10),
                          decoration: BoxDecoration(
                            color: AppColors.brandColor.withValues(alpha: 0.1),
                            shape: BoxShape.circle,
                          ),
                          child: const Icon(
                            Icons.help_outline_rounded,
                            color: AppColors.brandColor,
                          ),
                        ),
                        const SizedBox(width: 12),
                        const Expanded(
                          child: Text(
                            "Hướng dẫn nhanh",
                            style: TextStyle(
                              fontSize: 20,
                              fontWeight: FontWeight.w700,
                              color: Colors.black87,
                            ),
                          ),
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(height: 8),
                  Expanded(
                    child: ListView(
                      controller: scrollController,
                      padding: const EdgeInsets.fromLTRB(20, 8, 20, 28),
                      children: [
                        _buildGuideCard(
                          icon: Icons.nfc_rounded,
                          color: Colors.deepOrange,
                          title: "Check-in và check-out",
                          items: const [
                            "Check-in tại kiosk hoặc NFC trước khi sử dụng thư viện.",
                            "Khi rời thư viện, hãy check-out để tránh bị hệ thống xử lý trễ.",
                            "Nếu quên check-out, hệ thống có thể tự check-out theo cấu hình của thư viện và trừ điểm uy tín.",
                          ],
                        ),
                        _buildGuideCard(
                          icon: Icons.event_seat_rounded,
                          color: Colors.blue,
                          title: "Đặt chỗ ngồi",
                          items: const [
                            "Chọn ngày, khung giờ và ghế còn trống trên sơ đồ.",
                            "Đến đúng giờ và xác nhận ghế theo hướng dẫn trong ứng dụng.",
                            "Nếu không còn nhu cầu, hãy hủy đặt chỗ sớm để nhường ghế cho sinh viên khác.",
                          ],
                        ),
                        _buildGuideCard(
                          icon: Icons.verified_user_rounded,
                          color: Colors.green,
                          title: "Điểm uy tín",
                          items: const [
                            "Điểm uy tín phản ánh việc sử dụng thư viện đúng quy định.",
                            "Các lỗi như check-in trễ, không xác nhận ghế hoặc check-out trễ có thể bị trừ điểm.",
                            "Bạn có thể xem chi tiết trong mục Lịch sử vi phạm.",
                          ],
                        ),
                        _buildGuideCard(
                          icon: Icons.gavel_rounded,
                          color: Colors.purple,
                          title: "Kháng cáo và báo cáo",
                          items: const [
                            "Nếu cho rằng vi phạm bị ghi nhận sai, hãy gửi kháng cáo kèm lý do rõ ràng.",
                            "Báo cáo tình trạng ghế khi phát hiện ghế hỏng hoặc khu vực có vấn đề.",
                            "Theo dõi phản hồi của thủ thư trong lịch sử tương ứng.",
                          ],
                        ),
                        Container(
                          margin: const EdgeInsets.only(top: 6),
                          padding: const EdgeInsets.all(16),
                          decoration: BoxDecoration(
                            color: const Color(0xFFFFF7F2),
                            borderRadius: BorderRadius.circular(16),
                            border: Border.all(
                              color: AppColors.brandColor.withValues(
                                alpha: 0.16,
                              ),
                            ),
                          ),
                          child: Row(
                            children: [
                              const Icon(
                                Icons.support_agent_rounded,
                                color: AppColors.brandColor,
                              ),
                              const SizedBox(width: 12),
                              const Expanded(
                                child: Text(
                                  "Cần hỗ trợ gấp? Gửi yêu cầu hỗ trợ để thủ thư nhận thông báo và xử lý.",
                                  style: TextStyle(
                                    fontSize: 13,
                                    height: 1.45,
                                    color: Color(0xFF5F4B3B),
                                  ),
                                ),
                              ),
                              TextButton(
                                onPressed: () {
                                  Navigator.pop(sheetContext);
                                  _pushScreen(const SupportRequestScreen());
                                },
                                child: const Text("Gửi ngay"),
                              ),
                            ],
                          ),
                        ),
                      ],
                    ),
                  ),
                ],
              ),
            );
          },
        );
      },
    );
  }

  Widget _buildGuideCard({
    required IconData icon,
    required Color color,
    required String title,
    required List<String> items,
  }) {
    return Container(
      margin: const EdgeInsets.only(bottom: 12),
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(18),
        border: Border.all(color: Colors.grey.shade100),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withValues(alpha: 0.03),
            blurRadius: 12,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Container(
                padding: const EdgeInsets.all(9),
                decoration: BoxDecoration(
                  color: color.withValues(alpha: 0.1),
                  shape: BoxShape.circle,
                ),
                child: Icon(icon, color: color, size: 20),
              ),
              const SizedBox(width: 10),
              Expanded(
                child: Text(
                  title,
                  style: const TextStyle(
                    fontSize: 15,
                    fontWeight: FontWeight.w700,
                    color: Colors.black87,
                  ),
                ),
              ),
            ],
          ),
          const SizedBox(height: 12),
          ...items.map(
            (item) => Padding(
              padding: const EdgeInsets.only(bottom: 8),
              child: Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Container(
                    width: 5,
                    height: 5,
                    margin: const EdgeInsets.only(top: 7),
                    decoration: BoxDecoration(
                      color: color.withValues(alpha: 0.65),
                      shape: BoxShape.circle,
                    ),
                  ),
                  const SizedBox(width: 10),
                  Expanded(
                    child: Text(
                      item,
                      style: TextStyle(
                        fontSize: 13,
                        height: 1.45,
                        color: Colors.grey[700],
                      ),
                    ),
                  ),
                ],
              ),
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
      indent: 68,
      endIndent: 0,
      color: Colors.grey[100],
    );
  }

  void _showLogoutDialog() {
    showDialog(
      context: context,
      builder: (dialogContext) => AlertDialog(
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
        title: const Text(
          "Đăng xuất?",
          style: TextStyle(fontWeight: FontWeight.bold),
        ),
        content: const Text(
          "Bạn có chắc chắn muốn đăng xuất khỏi tài khoản không?",
        ),
        actionsPadding: const EdgeInsets.fromLTRB(20, 0, 20, 20),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(dialogContext),
            child: const Text("Hủy", style: TextStyle(color: Colors.grey)),
          ),
          ElevatedButton(
            onPressed: () async {
              // Lưu reference trước khi pop dialog
              final navigator = Navigator.of(context);
              final authService = context.read<AuthService>();
              final notificationService = context.read<NotificationService>();
              final libraryStatusService = context.read<LibraryStatusService>();

              Navigator.pop(dialogContext); // Đóng Dialog

              try {
                await authService.logout();
                notificationService.clearData();
                libraryStatusService.clearData();

                navigator.pushAndRemoveUntil(
                  MaterialPageRoute(
                    builder: (context) => const OnBoardingScreen(),
                  ),
                  (route) => false,
                );
              } catch (e) {
                debugPrint("Lỗi logout: $e");
              }
            },
            style: ElevatedButton.styleFrom(
              backgroundColor: Colors.red,
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(10),
              ),
              elevation: 0,
            ),
            child: const Text(
              "Đăng xuất",
              style: TextStyle(
                color: Colors.white,
                fontWeight: FontWeight.bold,
              ),
            ),
          ),
        ],
      ),
    );
  }
}
