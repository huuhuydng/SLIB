import 'package:flutter/material.dart';
import 'package:provider/provider.dart'; // Nhớ import Provider
import 'package:slib/models/user_profile.dart';
import 'package:slib/models/zones.dart';
import 'package:slib/services/auth/auth_service.dart';
import 'package:slib/services/booking/booking_service.dart';
import 'package:slib/services/notification/notification_service.dart';
import 'package:slib/views/card/hce_screen.dart';
import 'package:slib/views/home/home_screen.dart';
import 'package:slib/views/booking/floor_plan_screen.dart';
import 'package:slib/views/chat/chat_screen.dart';
import 'package:slib/views/menu/setting_screen.dart';
import 'package:slib/views/widgets/bottom_nav_widget.dart';

class MainScreen extends StatefulWidget {
  const MainScreen({super.key});

  // Static key to access MainScreen state from anywhere
  static final GlobalKey<MainScreenState> globalKey =
      GlobalKey<MainScreenState>();

  @override
  State<MainScreen> createState() => MainScreenState();
}

class MainScreenState extends State<MainScreen> {
  int _selectedIndex = 0;

  // Biến này để lưu user lấy từ Provider
  UserProfile? _currentUser;
  List<Zones> _zones = [];

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      _loadUserInfo();
      _loadZones();
    });
  }

  void _loadZones() async {
    final bookingService = context.read<BookingService>();
    final zones = await bookingService.getAllZones();
    debugPrint("Zones fetched: ${zones.length}");
    for (var z in zones) {
      debugPrint(
        "Zone: id=${z.id}, name=${z.name}, desc=${z.description}, hasPower=${z.hasPowerOutlet}",
      );
    }
    setState(() {
      _zones = zones;
    });
  }

  void _loadUserInfo() async {
    // Dùng context.read để lấy AuthService gốc từ main.dart
    final authService = context.read<AuthService>();
    final profile = await authService.getProfile();

    if (mounted) {
      setState(() {
        _currentUser = profile;
      });
    }
  }

  // Danh sách màn hình
  List<Widget> get _screens => [
    HomeScreen(user: _currentUser, isActive: _selectedIndex == 0),
    const FloorPlanScreen(), // NEW: Sơ đồ mặt bằng
    const HceCardScreen(),
    const ChatScreen(),
    SettingScreen(user: _currentUser),
  ];

  void _onItemTapped(int index) {
    // Clear chat badge when switching to chat tab
    if (index == 3) {
      try {
        context.read<NotificationService>().clearChatBadge();
      } catch (_) {}
    }
    setState(() {
      _selectedIndex = index;
    });
  }

  /// Public method to switch tabs programmatically
  void switchToTab(int index) {
    _onItemTapped(index);
  }

  @override
  Widget build(BuildContext context) {
    // Lắng nghe sự thay đổi từ AuthService (Ví dụ khi logout thì user = null)
    final authService = context.watch<AuthService>();
    // Cập nhật lại _currentUser nếu AuthService thay đổi
    if (authService.currentUser != _currentUser) {
      _currentUser = authService.currentUser;
    }

    // Watch notification service for chat badge count
    final chatBadge = context.select<NotificationService, int>(
      (service) => service.unreadChatCount,
    );

    return Scaffold(
      body: IndexedStack(index: _selectedIndex, children: _screens),
      bottomNavigationBar: BottomNavWidget(
        selectedIndex: _selectedIndex,
        onItemTapped: _onItemTapped,
        chatBadgeCount: chatBadge,
      ),
    );
  }
}
