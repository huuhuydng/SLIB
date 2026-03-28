import 'package:flutter/material.dart';
import 'package:slib/assets/colors.dart';

class BottomNavWidget extends StatelessWidget {
  // Nhận vào chỉ số đang chọn và hàm xử lý khi bấm
  final int selectedIndex;
  final Function(int) onItemTapped;
  final int chatBadgeCount;

  const BottomNavWidget({
    super.key,
    required this.selectedIndex,
    required this.onItemTapped,
    this.chatBadgeCount = 0,
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
        
        destinations: <Widget>[
          const NavigationDestination(
            icon: Icon(Icons.home_outlined),
            selectedIcon: Icon(Icons.home),
            label: 'Trang chủ',
          ),
          const NavigationDestination(
            icon: Icon(Icons.chair_alt_outlined),
            selectedIcon: Icon(Icons.chair_alt),
            label: 'Đặt chỗ',
          ),
          const NavigationDestination(
            icon: Icon(Icons.nfc_outlined),
            selectedIcon: Icon(Icons.nfc),
            label: 'Thẻ TV',
          ),
          NavigationDestination(
            icon: chatBadgeCount > 0
                ? Badge(
                    label: Text(
                      chatBadgeCount > 99 ? '99+' : '$chatBadgeCount',
                      style: const TextStyle(color: Colors.white, fontSize: 10),
                    ),
                    child: const Icon(Icons.chat_bubble_outline),
                  )
                : const Icon(Icons.chat_bubble_outline),
            selectedIcon: chatBadgeCount > 0
                ? Badge(
                    label: Text(
                      chatBadgeCount > 99 ? '99+' : '$chatBadgeCount',
                      style: const TextStyle(color: Colors.white, fontSize: 10),
                    ),
                    child: const Icon(Icons.chat_bubble),
                  )
                : const Icon(Icons.chat_bubble),
            label: 'Trợ lý AI',
          ),
          const NavigationDestination(
            icon: Icon(Icons.menu),
            selectedIcon: Icon(Icons.menu_open),
            label: 'Thêm',
          ),
        ],
      ),
    );
  }
}