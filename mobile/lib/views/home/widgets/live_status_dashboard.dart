import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:http/http.dart' as http;
import 'package:provider/provider.dart';
import 'package:stomp_dart_client/stomp_dart_client.dart';
import 'package:slib/core/constants/api_constants.dart';
import 'package:slib/models/student_profile.dart';
import 'package:slib/services/auth/auth_service.dart';
import 'package:slib/services/library/library_status_service.dart';
import 'package:slib/services/user/student_profile_service.dart';

class LiveStatusDashboard extends StatefulWidget {
  const LiveStatusDashboard({super.key});

  @override
  State<LiveStatusDashboard> createState() => LiveStatusDashboardState();
}

class LiveStatusDashboardState extends State<LiveStatusDashboard> {
  StudentProfile? _studentProfile;
  double _realStudyHours = 0.0;
  bool _isLoading = true;
  StompClient? _stompClient;
  bool _wsConnected = false;

  @override
  void initState() {
    super.initState();
    _loadStudentProfile();
    _connectWebSocket();
    // Tự động fetch trạng thái thư viện khi vào trang
    WidgetsBinding.instance.addPostFrameCallback((_) {
      Provider.of<LibraryStatusService>(context, listen: false).initialize();
    });
  }

  @override
  void dispose() {
    _stompClient?.deactivate();
    _stompClient = null;
    super.dispose();
  }

  /// Connect STOMP WebSocket → subscribe /topic/dashboard
  /// Khi reservation COMPLETED/EXPIRED → ReservationScheduler gửi event AUTO_STATUS_CHANGE
  /// → reload giờ học realtime
  void _connectWebSocket() {
    if (_wsConnected) return;
    try {
      String wsUrl = ApiConstants.domain;
      if (wsUrl.startsWith('https://')) {
        wsUrl = wsUrl.replaceFirst('https://', 'wss://');
      } else if (wsUrl.startsWith('http://')) {
        wsUrl = wsUrl.replaceFirst('http://', 'ws://');
      }
      final stompUrl = '$wsUrl/ws/websocket';

      _stompClient = StompClient(
        config: StompConfig(
          url: stompUrl,
          webSocketConnectHeaders: {
            'ngrok-skip-browser-warning': 'true',
          },
          onConnect: (StompFrame frame) {
            debugPrint('[LiveStatus] WebSocket connected');
            _wsConnected = true;
            _stompClient?.subscribe(
              destination: '/topic/dashboard',
              callback: (StompFrame frame) {
                if (frame.body != null) {
                  try {
                    final data = jsonDecode(frame.body!);
                    if (data['action'] == 'AUTO_STATUS_CHANGE' || data['action'] == 'AUTO_EXPIRED') {
                      debugPrint('[LiveStatus] Reservation status changed → reloading study hours');
                      _loadStudentProfile();
                    }
                  } catch (e) {
                    debugPrint('[LiveStatus] Parse error: $e');
                  }
                }
              },
            );
          },
          onWebSocketError: (error) {
            debugPrint('[LiveStatus] WebSocket error: $error');
            _wsConnected = false;
          },
          onDisconnect: (_) {
            _wsConnected = false;
            // Auto-reconnect sau 5s
            Future.delayed(const Duration(seconds: 5), () {
              if (mounted) _connectWebSocket();
            });
          },
          reconnectDelay: const Duration(seconds: 5),
        ),
      );
      _stompClient!.activate();
    } catch (e) {
      debugPrint('[LiveStatus] WebSocket connection error: $e');
    }
  }

  /// Public method to refresh data - can be called from parent widget
  Future<void> refresh() async {
    if (!mounted) return;
    setState(() => _isLoading = true);
    await Future.wait([
      _loadStudentProfile(),
      Provider.of<LibraryStatusService>(context, listen: false).fetchLibraryStatus(),
    ]);
  }

  Future<void> _loadStudentProfile() async {
    final authService = Provider.of<AuthService>(context, listen: false);
    final profileService = StudentProfileService(authService);
    
    final profile = await profileService.getMyProfile();
    
    // Lấy totalStudyHours realtime từ Activity API (tính từ reservation COMPLETED)
    double realHours = profile?.totalStudyHours ?? 0.0;
    try {
      final token = await authService.getToken();
      final user = authService.currentUser;
      if (token != null && user != null) {
        final response = await http.get(
          Uri.parse('${ApiConstants.activityUrl}/user/${user.id}'),
          headers: {
            'Content-Type': 'application/json',
            'Authorization': 'Bearer $token',
          },
        );
        if (response.statusCode == 200) {
          final data = jsonDecode(utf8.decode(response.bodyBytes));
          realHours = (data['totalStudyHours'] ?? 0).toDouble();
        }
      }
    } catch (e) {
      debugPrint('[LiveStatus] Error loading study hours: $e');
    }
    
    if (mounted) {
      setState(() {
        _studentProfile = profile;
        _realStudyHours = realHours;
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
          // Dòng 1: Trạng thái thư viện (Live Occupancy) - real-time từ WebSocket
          Consumer<LibraryStatusService>(
            builder: (context, libraryStatus, _) {
              return Row(
                children: [
                  Container(
                    padding: const EdgeInsets.all(10),
                    decoration: BoxDecoration(
                      color: libraryStatus.statusColor.withOpacity(0.1),
                      shape: BoxShape.circle,
                    ),
                    child: Icon(Icons.people_alt_rounded, color: libraryStatus.statusColor),
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
                          children: [
                            Text(
                              libraryStatus.isLoading ? "Đang tải..." : libraryStatus.statusText,
                              style: const TextStyle(
                                  fontWeight: FontWeight.bold,
                                  fontSize: 16,
                                  color: Colors.black87),
                            ),
                            const SizedBox(width: 8),
                            Icon(Icons.circle, size: 8, color: libraryStatus.statusColor),
                          ],
                        ),
                      ],
                    ),
                  ),
                  // % Đông đúc - dynamic
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                    decoration: BoxDecoration(
                      color: libraryStatus.badgeColor,
                      borderRadius: BorderRadius.circular(20),
                    ),
                    child: Text(
                      libraryStatus.isLoading
                          ? "..."
                          : "${libraryStatus.occupancyRate.round()}% Full",
                      style: const TextStyle(
                          color: Colors.white,
                          fontWeight: FontWeight.bold,
                          fontSize: 12),
                    ),
                  )
                ],
              );
            },
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
                      value: "${_realStudyHours.toStringAsFixed(1)}h",
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
