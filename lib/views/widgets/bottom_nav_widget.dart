import 'package:flutter/material.dart';
import 'package:slib/assets/colors.dart';

class BottomNavWidget extends StatelessWidget {
  // Nhận vào chỉ số đang chọn và hàm xử lý khi bấm
  final int selectedIndex;
  final Function(int) onItemTapped;

  const BottomNavWidget({
    super.key,
    required this.selectedIndex,
    required this.onItemTapped,
  });

  @override
  Widget build(BuildContext context) {
    return NavigationBarTheme(
      data: NavigationBarThemeData(
        // Màu nền của nút khi được chọn (Cam nhạt)
        indicatorColor: AppColors.brandColor.withOpacity(0.15),
        
        // Style chữ khi được chọn
        labelTextStyle: WidgetStateProperty.resolveWith((states) {
          if (states.contains(WidgetState.selected)) {
            return const TextStyle(
              fontSize: 12, 
              fontWeight: FontWeight.bold, 
              color: AppColors.brandColor
            );
          }
          return const TextStyle(fontSize: 12, fontWeight: FontWeight.normal);
        }),
        
        // Style icon khi được chọn
        iconTheme: WidgetStateProperty.resolveWith((states) {
          if (states.contains(WidgetState.selected)) {
            return const IconThemeData(color: AppColors.brandColor);
          }
          return const IconThemeData(color: Colors.grey);
        }),
      ),
      child: NavigationBar(
        selectedIndex: selectedIndex,
        onDestinationSelected: onItemTapped, // Gọi hàm từ cha truyền xuống
        backgroundColor: Colors.white,
        surfaceTintColor: Colors.transparent,
        elevation: 2,
        height: 65,
        
        destinations: const <Widget>[
          NavigationDestination(
            icon: Icon(Icons.home_outlined),
            selectedIcon: Icon(Icons.home),
            label: 'Home',
          ),
          NavigationDestination(
            icon: Icon(Icons.chair_alt_outlined),
            selectedIcon: Icon(Icons.chair_alt),
            label: 'Booking',
          ),
          NavigationDestination(
            icon: Icon(Icons.nfc_outlined),
            selectedIcon: Icon(Icons.nfc),
            label: 'Card',
          ),
          NavigationDestination(
            icon: Icon(Icons.chat_bubble_outline),
            selectedIcon: Icon(Icons.chat_bubble),
            label: 'Chat',
          ),
          NavigationDestination(
            icon: Icon(Icons.menu),
            selectedIcon: Icon(Icons.menu_open),
            label: 'Menu',
          ),
        ],
      ),
    );
  }
}