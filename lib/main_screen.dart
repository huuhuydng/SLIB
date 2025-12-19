import 'package:flutter/material.dart';
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

  final List<Widget> _screens = [
    const HomeScreen(),
    const BookingZoneScreen(),
    const HceCardScreen(),
    const ChatScreen(),
    const MenuScreen(),
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