import 'package:flutter/material.dart';
import 'package:slib/models/user_profile.dart';
import 'package:slib/services/auth_service.dart';
import 'package:slib/views/card/hce_screen.dart';
import 'package:slib/views/home/home_screen.dart';
import 'package:slib/views/home/widgets/booking_zone.dart';
import 'package:slib/views/others/chat_screen.dart';
import 'package:slib/views/others/menu_screen.dart';
import 'package:slib/views/widgets/bottom_nav_widget.dart';


class MainScreen extends StatefulWidget {
  const MainScreen({super.key});

  @override
  State<MainScreen> createState() => _MainScreenState();
}

class _MainScreenState extends State<MainScreen> {
  int _selectedIndex = 0;
  final AuthService _authService = AuthService();
  UserProfile? _currentUser;

  @override
  void initState() {
    super.initState();
    _loadUserInfo();
  }

  void _loadUserInfo() async {
    final profile = await _authService.getProfile();
    setState(() {
      _currentUser = profile;
    });
  }

  List<Widget> get _screens => [
    HomeScreen(user: _currentUser),
    const BookingZoneScreen(),
    const HceCardScreen(),
    const ChatScreen(),
    // Truyền _currentUser vào MenuScreen
    MenuScreen(user: _currentUser), 
  ];

  void _onItemTapped(int index) {
    setState(() {
      _selectedIndex = index;
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: null,
      body: IndexedStack(
        index: _selectedIndex, 
        children: _screens,
      ),
      
      // GỌI WIDGET RIÊNG TẠI ĐÂY
      bottomNavigationBar: BottomNavWidget(
        selectedIndex: _selectedIndex, 
        onItemTapped: _onItemTapped,  
      ),
    );
  }
}