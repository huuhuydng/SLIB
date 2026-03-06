import 'dart:async';
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:slib/models/upcoming_booking.dart';
import 'package:slib/services/auth_service.dart';
import 'package:slib/services/booking_service.dart';
import 'package:slib/views/home/widgets/booking_action_dialog.dart';

class UpcomingBookingCard extends StatefulWidget {
  const UpcomingBookingCard({super.key});

  @override
  State<UpcomingBookingCard> createState() => UpcomingBookingCardState();
}

class UpcomingBookingCardState extends State<UpcomingBookingCard> 
    with WidgetsBindingObserver {
  UpcomingBooking? _upcomingBooking;
  bool _isLoading = true;
  bool _hasError = false;
  Timer? _expiryTimer;

  final BookingService _bookingService = BookingService();

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    _loadUpcomingBooking();
  }

  @override
  void dispose() {
    _expiryTimer?.cancel();
    WidgetsBinding.instance.removeObserver(this);
    super.dispose();
  }

  /// Called when app lifecycle changes - refresh when app resumes
  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed) {
      refresh();
    }
  }

  /// Public method to refresh data - can be called from parent widget
  Future<void> refresh() async {
    if (!mounted) return;
    setState(() => _isLoading = true);
    await _loadUpcomingBooking();
  }

  Future<void> _loadUpcomingBooking() async {
    // Cancel any existing expiry timer
    _expiryTimer?.cancel();
    
    final authService = Provider.of<AuthService>(context, listen: false);
    final user = authService.currentUser;

    if (user == null) {
      if (mounted) {
        setState(() {
          _isLoading = false;
          _hasError = true;
        });
      }
      return;
    }

    try {
      final data = await _bookingService.getUpcomingBooking(user.id);
      if (mounted) {
        final booking = data != null ? UpcomingBooking.fromJson(data) : null;
        
        // Set up timer to refresh when booking expires
        if (booking != null) {
          _scheduleExpiryRefresh(booking.endTime);
        }
        
        setState(() {
          _upcomingBooking = booking;
          _isLoading = false;
          _hasError = false;
        });
      }
    } catch (e) {
      debugPrint("Error loading upcoming booking: $e");
      if (mounted) {
        setState(() {
          _isLoading = false;
          _hasError = true;
        });
      }
    }
  }

  /// Schedule a timer to auto-refresh when the booking expires
  void _scheduleExpiryRefresh(DateTime endTime) {
    final now = DateTime.now();
    final duration = endTime.difference(now);
    
    // If booking already expired, don't schedule anything - just let it show empty
    if (duration.isNegative) {
      debugPrint("Booking already expired, not scheduling refresh");
      // Clear the booking so it shows empty state
      if (mounted) {
        setState(() {
          _upcomingBooking = null;
        });
      }
      return;
    }
    
    debugPrint("Scheduling refresh in ${duration.inMinutes} minutes");
    // Schedule refresh when booking expires (add 1 second buffer)
    _expiryTimer = Timer(duration + const Duration(seconds: 1), () {
      if (mounted) {
        debugPrint("Booking expired timer fired, refreshing...");
        refresh();
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    if (_isLoading) {
      return _buildLoadingCard();
    }

    // Only check if booking exists and status is not EXPIRED/CANCEL/COMPLETED
    if (_hasError || _upcomingBooking == null) {
      return _buildEmptyCard();
    }
    
    // Filter out expired/cancelled/completed bookings
    final status = _upcomingBooking!.status.toUpperCase();
    if (status == 'EXPIRED' || status == 'CANCEL' || status == 'COMPLETED') {
      debugPrint("Hiding booking with status: $status");
      return _buildEmptyCard();
    }

    return _buildBookingCard(_upcomingBooking!);
  }

  /// Check if booking is confirmed via NFC (only CONFIRMED status)
  bool _isConfirmed(UpcomingBooking booking) {
    final status = booking.status.toUpperCase();
    return status == 'CONFIRMED';
  }

  Widget _buildLoadingCard() {
    return Container(
      width: double.infinity,
      height: 80,
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: const Color(0xFFEFF6FF),
        borderRadius: BorderRadius.circular(20),
        border: Border.all(color: Colors.blue.withOpacity(0.1)),
      ),
      child: const Center(
        child: SizedBox(
          height: 24,
          width: 24,
          child: CircularProgressIndicator(strokeWidth: 2),
        ),
      ),
    );
  }

  Widget _buildEmptyCard() {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: Colors.grey[100],
        borderRadius: BorderRadius.circular(20),
        border: Border.all(color: Colors.grey.withOpacity(0.2)),
      ),
      child: Row(
        children: [
          Container(
            padding: const EdgeInsets.all(12),
            decoration: BoxDecoration(
              color: Colors.grey[200],
              borderRadius: BorderRadius.circular(12),
            ),
            child: Icon(Icons.event_busy, color: Colors.grey[400], size: 28),
          ),
          const SizedBox(width: 16),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  "Chưa có lịch đặt chỗ",
                  style: TextStyle(
                    color: Colors.grey[600],
                    fontSize: 15,
                    fontWeight: FontWeight.w500,
                  ),
                ),
                const SizedBox(height: 4),
                Text(
                  "Hãy đặt ghế để bắt đầu học tập!",
                  style: TextStyle(color: Colors.grey[400], fontSize: 13),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildBookingCard(UpcomingBooking booking) {
    final bool isActive = booking.isActive; // Đang trong giờ đặt
    final bool isConfirmed = _isConfirmed(booking); // Đã xác nhận NFC (CONFIRMED)
    
    // Xác định màu sắc:
    // - Xám (grey): chưa đến giờ, lịch sắp tới
    // - Vàng (orange): đã đến giờ nhưng chưa xác nhận NFC
    // - Xanh lá (green): đã xác nhận NFC và đang học
    Color primaryColor;
    Color bgColor;
    String statusText;
    
    if (isActive && isConfirmed) {
      // Đang trong giờ + đã xác nhận NFC → Xanh lá, "Đang học"
      primaryColor = Colors.green;
      bgColor = const Color(0xFFE8F5E9);
      statusText = "Đang học";
    } else if (isActive && !isConfirmed) {
      // Đang trong giờ + chưa xác nhận NFC → Vàng, "Chưa xác nhận chỗ ngồi"
      primaryColor = Colors.orange;
      bgColor = const Color(0xFFFFF3E0);
      statusText = "Chưa xác nhận chỗ ngồi";
    } else {
      // Chưa đến giờ → Xám, "Lịch sắp tới"
      primaryColor = Colors.grey;
      bgColor = Colors.grey[100]!;
      statusText = "Lịch sắp tới";
    }

    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: bgColor,
        borderRadius: BorderRadius.circular(20),
        border: Border.all(color: primaryColor.withOpacity(0.2)),
      ),
      child: Row(
        children: [
          // Date column
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
            decoration: BoxDecoration(
              color: Colors.white,
              borderRadius: BorderRadius.circular(14),
              boxShadow: [
                BoxShadow(color: primaryColor.withOpacity(0.1), blurRadius: 10)
              ],
            ),
            child: Column(
              children: [
                Text(
                  booking.dayOfWeek,
                  style: const TextStyle(color: Colors.grey, fontSize: 12),
                ),
                Text(
                  "${booking.dayOfMonth}",
                  style: TextStyle(
                    color: primaryColor,
                    fontWeight: FontWeight.bold,
                    fontSize: 20,
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(width: 16),
          // Booking info
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    Flexible(
                      child: Text(
                        statusText,
                        style: TextStyle(
                          color: primaryColor,
                          fontSize: 12,
                          fontWeight: FontWeight.bold,
                        ),
                        overflow: TextOverflow.ellipsis,
                      ),
                    ),
                    if (isActive) ...[
                      const SizedBox(width: 8),
                      Container(
                        width: 8,
                        height: 8,
                        decoration: BoxDecoration(
                          color: primaryColor,
                          shape: BoxShape.circle,
                          boxShadow: [
                            BoxShadow(
                              color: primaryColor.withOpacity(0.5),
                              blurRadius: 4,
                              spreadRadius: 1,
                            ),
                          ],
                        ),
                      ),
                    ],
                  ],
                ),
                const SizedBox(height: 4),
                Text(
                  "${booking.zoneName} - ${booking.areaName}",
                  style: const TextStyle(
                    fontWeight: FontWeight.bold,
                    fontSize: 16,
                    color: Colors.black87,
                  ),
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                ),
                const SizedBox(height: 4),
                Row(
                  children: [
                    const Icon(Icons.access_time_rounded, size: 14, color: Colors.grey),
                    const SizedBox(width: 4),
                    Text(
                      booking.timeRange,
                      style: const TextStyle(color: Colors.grey, fontSize: 13),
                    ),
                    const SizedBox(width: 10),
                    const Icon(Icons.chair_alt_outlined, size: 14, color: Colors.grey),
                    const SizedBox(width: 4),
                    Text(
                      "Ghế ${booking.seatCode}",
                      style: const TextStyle(color: Colors.grey, fontSize: 13),
                    ),
                  ],
                ),
              ],
            ),
          ),
          // Arrow button - show action dialog
          IconButton(
            onPressed: () {
              BookingActionDialog.show(context, booking, refresh);
            },
            icon: Icon(
              Icons.arrow_forward_ios_rounded,
              size: 18,
              color: primaryColor,
            ),
            style: IconButton.styleFrom(backgroundColor: Colors.white),
          ),
        ],
      ),
    );
  }
}
