import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:slib/assets/colors.dart';
import 'package:slib/main_screen.dart';
import 'package:slib/services/ai/ai_analytics_service.dart';
import 'package:slib/services/auth/auth_service.dart';
import 'package:slib/views/booking/floor_plan_screen.dart';

class AICard extends StatefulWidget {
  const AICard({super.key});

  @override
  State<AICard> createState() => _AICardState();
}

class _AICardState extends State<AICard> {
  AICardData? _aiData;
  bool _isLoading = true;
  bool _hasError = false;

  @override
  void initState() {
    super.initState();
    _loadAIData();
  }

  Future<void> _loadAIData() async {
    try {
      final authService = Provider.of<AuthService>(context, listen: false);
      final user = authService.currentUser;

      if (user == null) {
        setState(() {
          _isLoading = false;
          _hasError = true;
        });
        return;
      }

      final data = await AIAnalyticsService.generateAICardData(user.id);

      if (mounted) {
        setState(() {
          _aiData = data;
          _isLoading = false;
          _hasError = data == null;
        });
      }
    } catch (e) {
      debugPrint('Error loading AI data: $e');
      if (mounted) {
        setState(() {
          _isLoading = false;
          _hasError = true;
        });
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(24),
        gradient: const LinearGradient(
          colors: [Color(0xFF1E293B), Color(0xFF334155)],
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
        ),
        boxShadow: const [
          BoxShadow(
            color: Colors.black26,
            blurRadius: 15,
            offset: Offset(0, 8),
          ),
        ],
      ),
      child: Material(
        color: Colors.transparent,
        child: InkWell(
          onTap: () {},
          borderRadius: BorderRadius.circular(24),
          child: Padding(
            padding: const EdgeInsets.all(20.0),
            child: _isLoading
                ? _buildLoading()
                : _hasError || _aiData == null
                    ? _buildFallback()
                    : _buildContent(_aiData!),
          ),
        ),
      ),
    );
  }

  Widget _buildLoading() {
    return const SizedBox(
      height: 100,
      child: Center(
        child: CircularProgressIndicator(
          color: Colors.white54,
          strokeWidth: 2,
        ),
      ),
    );
  }

  Widget _buildFallback() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        _buildHeader(),
        const SizedBox(height: 16),
        const Text(
          "Dự báo: 14:00 - 16:00 thư viện sẽ vắng",
          style: TextStyle(
            fontWeight: FontWeight.bold,
            color: Colors.white,
            fontSize: 16,
          ),
        ),
        const SizedBox(height: 6),
        const Text(
          "Đây là thời điểm lý tưởng để bạn đặt chỗ tại khu vực yên tĩnh.",
          style: TextStyle(color: Colors.white70, fontSize: 13, height: 1.4),
        ),
        const SizedBox(height: 16),
        _buildActionButton("Đặt chỗ ngay", () {
          _navigateToBooking();
        }),
      ],
    );
  }

  Widget _buildContent(AICardData data) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        _buildHeader(),
        const SizedBox(height: 16),
        Text(
          data.title,
          style: const TextStyle(
            fontWeight: FontWeight.bold,
            color: Colors.white,
            fontSize: 16,
          ),
        ),
        const SizedBox(height: 6),
        Text(
          data.message,
          style: const TextStyle(
            color: Colors.white70,
            fontSize: 13,
            height: 1.4,
          ),
        ),
        const SizedBox(height: 16),
        _buildActionButton(data.actionText ?? "Đặt chỗ ngay", () {
          _navigateToBooking(zoneId: data.zoneId, seatId: data.seatId);
        }),
      ],
    );
  }

  void _navigateToBooking({int? zoneId, int? seatId}) {
    if (zoneId != null) {
      // Có gợi ý zone cụ thể -> mở FloorPlanScreen với zone + seat đó
      Navigator.of(context).push(
        MaterialPageRoute(
          builder: (_) => FloorPlanScreen(
            initialZoneId: zoneId,
            initialSeatId: seatId,
          ),
        ),
      );
    } else {
      // Không có gợi ý cụ thể -> chuyển sang tab Đặt chỗ
      MainScreen.globalKey.currentState?.switchToTab(1);
    }
  }

  Widget _buildHeader() {
    return Row(
      children: [
        const Icon(Icons.auto_awesome, color: Colors.amberAccent, size: 24),
        const SizedBox(width: 10),
        const Text(
          "SLIB Intelligence",
          style: TextStyle(
            color: Colors.white,
            fontWeight: FontWeight.bold,
            fontSize: 14,
          ),
        ),
        const Spacer(),
        Container(
          padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
          decoration: BoxDecoration(
            color: Colors.white.withOpacity(0.1),
            borderRadius: BorderRadius.circular(10),
          ),
          child: const Text(
            "BETA",
            style: TextStyle(color: Colors.white70, fontSize: 10),
          ),
        ),
      ],
    );
  }

  Widget _buildActionButton(String text, VoidCallback onPressed) {
    return SizedBox(
      width: double.infinity,
      child: ElevatedButton(
        onPressed: onPressed,
        style: ElevatedButton.styleFrom(
          backgroundColor: AppColors.brandColor,
          foregroundColor: Colors.white,
          padding: const EdgeInsets.symmetric(vertical: 12),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(12),
          ),
          elevation: 0,
        ),
        child: Text(text),
      ),
    );
  }
}
