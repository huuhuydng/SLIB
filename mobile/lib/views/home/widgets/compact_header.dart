import 'package:flutter/material.dart';
import 'package:slib/assets/colors.dart';
import 'package:slib/models/user_profile.dart';
import 'package:slib/views/home/widgets/notification_bell_button.dart';

class CompactHeader extends StatelessWidget {
  final UserProfile? user;
  final bool show;

  const CompactHeader({super.key, required this.user, required this.show});

  @override
  Widget build(BuildContext context) {
    final hiddenTop = -(MediaQuery.of(context).padding.top + 72);

    return AnimatedPositioned(
      duration: const Duration(milliseconds: 300),
      curve: Curves.easeInOut,
      top: show ? 0 : hiddenTop,
      left: 0,
      right: 0,
      child: Container(
        decoration: BoxDecoration(
          gradient: LinearGradient(
            begin: Alignment.topLeft,
            end: Alignment.bottomRight,
            colors: [AppColors.brandColor, const Color(0xFFFF9052)],
          ),
          boxShadow: show
              ? [
                  BoxShadow(
                    color: Colors.black.withValues(alpha: 0.1),
                    blurRadius: 10,
                    offset: const Offset(0, 2),
                  ),
                ]
              : const [],
        ),
        child: SafeArea(
          bottom: false,
          child: Padding(
            padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 12),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Expanded(
                  child: Text(
                    "Chào ${user?.fullName ?? 'Bạn'},",
                    style: const TextStyle(
                      fontSize: 16,
                      fontWeight: FontWeight.w600,
                      color: Colors.white,
                    ),
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                  ),
                ),
                const SizedBox(width: 12),
                const NotificationBellButton(compact: true),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
