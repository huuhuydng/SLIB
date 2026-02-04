import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:slib/models/student_profile.dart';
import 'package:slib/services/auth_service.dart';
import 'package:slib/services/student_profile_service.dart';

class LiveStatusDashboard extends StatefulWidget {
  const LiveStatusDashboard({super.key});

  @override
  State<LiveStatusDashboard> createState() => LiveStatusDashboardState();
}

class LiveStatusDashboardState extends State<LiveStatusDashboard> {
  StudentProfile? _studentProfile;
  bool _isLoading = true;

  @override
  void initState() {
    super.initState();
    _loadStudentProfile();
  }

  /// Public method to refresh data - can be called from parent widget
  Future<void> refresh() async {
    if (!mounted) return;
    setState(() => _isLoading = true);
    await _loadStudentProfile();
  }

  Future<void> _loadStudentProfile() async {
    final authService = Provider.of<AuthService>(context, listen: false);
    final profileService = StudentProfileService(authService);
    
    final profile = await profileService.getMyProfile();
    
    if (mounted) {
      setState(() {
        _studentProfile = profile;
        _isLoading = false;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(24),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(0.1),
            blurRadius: 20,
            offset: const Offset(0, 10),
          ),
        ],
      ),
      child: Column(
        children: [
          // Dòng 1: Trạng thái thư viện (Live Occupancy)
          Row(
            children: [
              Container(
                padding: const EdgeInsets.all(10),
                decoration: BoxDecoration(
                  color: Colors.orange.withOpacity(0.1),
                  shape: BoxShape.circle,
                ),
                child: const Icon(Icons.people_alt_rounded, color: Colors.orange),
              ),
              const SizedBox(width: 15),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Text(
                      "Trạng thái thư viện",
                      style: TextStyle(color: Colors.grey, fontSize: 13),
                    ),
                    const SizedBox(height: 4),
                    Row(
                      children: const [
                        Text(
                          "Khá đông đúc",
                          style: TextStyle(
                              fontWeight: FontWeight.bold,
                              fontSize: 16,
                              color: Colors.black87),
                        ),
                        SizedBox(width: 8),
                        Icon(Icons.circle, size: 8, color: Colors.orange),
                      ],
                    ),
                  ],
                ),
              ),
              // % Đông đúc
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                decoration: BoxDecoration(
                  color: Colors.orange,
                  borderRadius: BorderRadius.circular(20),
                ),
                child: const Text(
                  "78% Full",
                  style: TextStyle(
                      color: Colors.white,
                      fontWeight: FontWeight.bold,
                      fontSize: 12),
                ),
              )
            ],
          ),
          const Padding(
            padding: EdgeInsets.symmetric(vertical: 15),
            child: Divider(height: 1),
          ),
          // Dòng 2: Điểm uy tín & Vi phạm (Personal Stats)
          _isLoading
              ? const Center(
                  child: SizedBox(
                    height: 40,
                    child: CircularProgressIndicator(strokeWidth: 2),
                  ),
                )
              : Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    _buildStatItem(
                      label: "Điểm uy tín",
                      value: "${_studentProfile?.reputationScore ?? 100}",
                      valueColor: _getReputationColor(_studentProfile?.reputationScore ?? 100),
                      icon: Icons.shield_outlined,
                    ),
                    Container(width: 1, height: 40, color: Colors.grey[200]),
                    _buildStatItem(
                      label: "Giờ đã học",
                      value: _studentProfile?.formattedStudyHours ?? "0.0h",
                      valueColor: Colors.blue,
                      icon: Icons.timer_outlined,
                    ),
                    Container(width: 1, height: 40, color: Colors.grey[200]),
                    _buildStatItem(
                      label: "Vi phạm",
                      value: "${_studentProfile?.violationCount ?? 0}",
                      valueColor: _getViolationColor(_studentProfile?.violationCount ?? 0),
                      icon: Icons.warning_amber_rounded,
                    ),
                  ],
                )
        ],
      ),
    );
  }

  Color _getReputationColor(int score) {
    if (score >= 80) return Colors.green;
    if (score >= 50) return Colors.orange;
    return Colors.red;
  }

  Color _getViolationColor(int count) {
    if (count == 0) return Colors.green;
    if (count <= 2) return Colors.orange;
    return Colors.red;
  }

  Widget _buildStatItem({
    required String label,
    required String value,
    required Color valueColor,
    required IconData icon,
  }) {
    return Column(
      children: [
        Row(
          children: [
            Icon(icon, size: 14, color: Colors.grey),
            const SizedBox(width: 4),
            Text(label, style: const TextStyle(fontSize: 12, color: Colors.grey)),
          ],
        ),
        const SizedBox(height: 4),
        Text(
          value,
          style: TextStyle(
              fontWeight: FontWeight.bold, fontSize: 18, color: valueColor),
        ),
      ],
    );
  }
}
