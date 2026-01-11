import 'package:flutter/material.dart';
import 'package:provider/provider.dart'; // Nhớ import Provider
import 'package:slib/models/user_profile.dart';
import 'package:slib/models/zones.dart';
import 'package:slib/services/auth_service.dart';
import 'package:slib/services/booking_service.dart';
import 'package:slib/views/card/hce_screen.dart';
import 'package:slib/views/home/home_screen.dart';
import 'package:slib/views/home/widgets/booking_zone.dart';
import 'package:slib/views/chat/chat_screen.dart';
import 'package:slib/views/menu/menu_screen.dart';
import 'package:slib/views/widgets/bottom_nav_widget.dart';

class MainScreen extends StatefulWidget {
  const MainScreen({super.key});

  @override
  State<MainScreen> createState() => _MainScreenState();
}

class _MainScreenState extends State<MainScreen> {
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
    HomeScreen(user: _currentUser),
    BookingZoneScreen(zones: _zones),
    const HceCardScreen(),
    const ChatScreen(),
    MenuScreen(user: _currentUser),
  ];

  void _onItemTapped(int index) {
    setState(() {
      _selectedIndex = index;
    });
  }

  @override
  Widget build(BuildContext context) {
    // Lắng nghe sự thay đổi từ AuthService (Ví dụ khi logout thì user = null)
    final authService = context.watch<AuthService>();
    // Cập nhật lại _currentUser nếu AuthService thay đổi
    if (authService.currentUser != _currentUser) {
      _currentUser = authService.currentUser;
    }

    return Scaffold(
      body: IndexedStack(index: _selectedIndex, children: _screens),
      bottomNavigationBar: BottomNavWidget(
        selectedIndex: _selectedIndex,
        onItemTapped: _onItemTapped,
      ),
    );
  }
}
