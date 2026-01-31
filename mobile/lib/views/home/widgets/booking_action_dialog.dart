import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:provider/provider.dart';
import 'package:slib/models/upcoming_booking.dart';
import 'package:slib/services/booking_service.dart';
import 'package:slib/views/home/widgets/nfc_seat_verification_screen.dart';

class BookingActionDialog extends StatefulWidget {
  final UpcomingBooking booking;
  final VoidCallback onActionComplete;

  const BookingActionDialog({
    super.key,
    required this.booking,
    required this.onActionComplete,
  });

  static Future<void> show(BuildContext context, UpcomingBooking booking, VoidCallback onComplete) {
    return showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (context) => BookingActionDialog(
        booking: booking,
        onActionComplete: onComplete,
      ),
    );
  }

  @override
  State<BookingActionDialog> createState() => _BookingActionDialogState();
}

class _BookingActionDialogState extends State<BookingActionDialog> {
  final BookingService _bookingService = BookingService();
  bool _isLoading = false;
  String? _errorMessage;

  /// Check if booking can be cancelled (more than 12 hours before start)
  bool get _canCancel {
    final now = DateTime.now();
    final cancelDeadline = widget.booking.startTime.subtract(const Duration(hours: 12));
    return now.isBefore(cancelDeadline);
  }

  /// Check if booking can be confirmed (within 15 mins before start to end)
  bool get _canConfirm {
    final now = DateTime.now();
    final checkInStart = widget.booking.startTime.subtract(const Duration(minutes: 15));
    return now.isAfter(checkInStart) && now.isBefore(widget.booking.endTime);
  }

  Future<void> _handleCancel() async {
    // Show confirmation dialog matching booking confirm screen style
    final confirmed = await showModalBottomSheet<bool>(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (context) => Container(
        padding: const EdgeInsets.all(24),
        decoration: const BoxDecoration(
          color: Color(0xFFF5F7FA),
          borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
        ),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            // Drag handle
            Container(
              width: 40,
              height: 4,
              margin: const EdgeInsets.only(bottom: 20),
              decoration: BoxDecoration(
                color: Colors.grey[300],
                borderRadius: BorderRadius.circular(2),
              ),
            ),
            
            // Icon
            Container(
              padding: const EdgeInsets.all(20),
              decoration: BoxDecoration(
                color: Colors.red.shade50,
                shape: BoxShape.circle,
              ),
              child: Icon(Icons.cancel_outlined, size: 60, color: Colors.red.shade400),
            ),
            const SizedBox(height: 20),
            
            // Title
            const Text(
              "Xác nhận hủy đặt chỗ",
              style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 8),
            Text(
              "Bạn có chắc muốn hủy đặt chỗ này?",
              style: TextStyle(color: Colors.grey[600]),
            ),
            const SizedBox(height: 24),
            
            // Info card
            Container(
              padding: const EdgeInsets.all(20),
              decoration: BoxDecoration(
                color: Colors.white,
                borderRadius: BorderRadius.circular(20),
                boxShadow: [
                  BoxShadow(
                    color: Colors.black.withAlpha(13),
                    blurRadius: 20,
                    offset: const Offset(0, 10),
                  ),
                ],
              ),
              child: Column(
                children: [
                  _buildInfoRow(Icons.location_on_rounded, "Khu vực", widget.booking.zoneName),
                  const Divider(height: 30),
                  _buildInfoRow(Icons.chair_alt_rounded, "Mã ghế", widget.booking.seatCode),
                  const Divider(height: 30),
                  _buildInfoRow(Icons.calendar_month_rounded, "Ngày", DateFormat('EEEE, dd/MM/yyyy', 'vi').format(widget.booking.startTime)),
                  const Divider(height: 30),
                  _buildInfoRow(Icons.access_time_rounded, "Khung giờ", widget.booking.timeRange),
                ],
              ),
            ),
            const SizedBox(height: 24),
            
            // Buttons
            Row(
              children: [
                Expanded(
                  child: OutlinedButton(
                    onPressed: () => Navigator.pop(context, false),
                    style: OutlinedButton.styleFrom(
                      padding: const EdgeInsets.symmetric(vertical: 16),
                      side: BorderSide(color: Colors.grey[400]!),
                      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                    ),
                    child: const Text('Không', style: TextStyle(color: Colors.grey, fontSize: 16)),
                  ),
                ),
                const SizedBox(width: 16),
                Expanded(
                  flex: 2,
                  child: ElevatedButton(
                    onPressed: () => Navigator.pop(context, true),
                    style: ElevatedButton.styleFrom(
                      backgroundColor: Colors.red,
                      padding: const EdgeInsets.symmetric(vertical: 16),
                      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                    ),
                    child: const Text('Xác nhận hủy', style: TextStyle(color: Colors.white, fontSize: 16, fontWeight: FontWeight.bold)),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 16),
          ],
        ),
      ),
    );

    if (confirmed != true) return;

    // Proceed with cancellation
    setState(() {
      _isLoading = true;
      _errorMessage = null;
    });

    try {
      await _bookingService.cancelReservation(widget.booking.reservationId);
      if (mounted) {
        Navigator.pop(context);
        widget.onActionComplete();
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('Đã hủy đặt chỗ thành công'),
            backgroundColor: Colors.green,
          ),
        );
      }
    } catch (e) {
      setState(() {
        _errorMessage = e.toString().replaceFirst('Exception: ', '');
        _isLoading = false;
      });
    }
  }

  Future<void> _handleNfcConfirm() async {
    final bookingService = Provider.of<BookingService>(context, listen: false);
    
    // Store the root navigator context before popping
    final rootContext = Navigator.of(context, rootNavigator: true).context;
    
    // Close this dialog first
    Navigator.pop(context);
    
    // Wait a frame for the dialog to fully close
    await Future.delayed(const Duration(milliseconds: 100));
    
    // Use try-catch to handle any navigation errors
    try {
      final result = await NfcVerificationDialog.show(
        rootContext,
        seatCode: widget.booking.seatCode,
        reservationId: widget.booking.reservationId,
      );
      
      if (result == true) {
        // NFC verification successful - update booking status to CONFIRMED
        try {
          await bookingService.updateStatus(widget.booking.reservationId, "CONFIRMED");
          
          widget.onActionComplete();
          if (rootContext.mounted) {
            ScaffoldMessenger.of(rootContext).showSnackBar(
              const SnackBar(
                content: Text('Xác nhận thành công!'),
                backgroundColor: Colors.green,
              ),
            );
          }
        } catch (e) {
          if (rootContext.mounted) {
            ScaffoldMessenger.of(rootContext).showSnackBar(
              SnackBar(
                content: Text('Lỗi xác nhận: $e'),
                backgroundColor: Colors.red,
              ),
            );
          }
        }
      }
    } catch (e) {
      // Navigation error - dialog was already dismissed or context invalid
      debugPrint('NFC verification navigation error: $e');
    }
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: const BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
      ),
      child: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(20),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // Drag handle
              Center(
                child: Container(
                  width: 40,
                  height: 4,
                  decoration: BoxDecoration(
                    color: Colors.grey[300],
                    borderRadius: BorderRadius.circular(2),
                  ),
                ),
              ),
              const SizedBox(height: 20),
              
              // Booking info header
              Row(
                children: [
                  Container(
                    padding: const EdgeInsets.all(12),
                    decoration: BoxDecoration(
                      color: Colors.blue.withOpacity(0.1),
                      borderRadius: BorderRadius.circular(12),
                    ),
                    child: const Icon(Icons.event_seat, color: Colors.blue),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          'Ghế ${widget.booking.seatCode}',
                          style: const TextStyle(
                            fontSize: 18,
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                        Text(
                          '${widget.booking.zoneName} • ${widget.booking.timeRange}',
                          style: TextStyle(color: Colors.grey[600]),
                        ),
                      ],
                    ),
                  ),
                ],
              ),
              
              if (_errorMessage != null) ...[
                const SizedBox(height: 16),
                Container(
                  padding: const EdgeInsets.all(12),
                  decoration: BoxDecoration(
                    color: Colors.red.withOpacity(0.1),
                    borderRadius: BorderRadius.circular(12),
                  ),
                  child: Row(
                    children: [
                      const Icon(Icons.error_outline, color: Colors.red, size: 20),
                      const SizedBox(width: 8),
                      Expanded(
                        child: Text(
                          _errorMessage!,
                          style: const TextStyle(color: Colors.red, fontSize: 13),
                        ),
                      ),
                    ],
                  ),
                ),
              ],
              
              const SizedBox(height: 24),
              
              // Action buttons
              // 1. Cancel booking button
              _buildActionButton(
                icon: Icons.cancel_outlined,
                label: 'Hủy đặt chỗ',
                subtitle: _canCancel 
                    ? 'Hủy trước 12 giờ để không bị trừ điểm'
                    : 'Không thể hủy (còn dưới 12 giờ)',
                color: Colors.red,
                enabled: _canCancel && !_isLoading,
                isLoading: _isLoading,
                onTap: _handleCancel,
              ),
              
              const SizedBox(height: 12),
              
              // 2. NFC confirm button
              _buildActionButton(
                icon: Icons.nfc,
                label: 'Xác nhận chỗ ngồi',
                subtitle: _canConfirm 
                    ? 'Chạm thẻ NFC trên bàn để check-in'
                    : 'Có thể check-in từ 15 phút trước giờ đặt',
                color: Colors.green,
                enabled: _canConfirm && !_isLoading,
                onTap: _handleNfcConfirm,
              ),
              
              const SizedBox(height: 20),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildActionButton({
    required IconData icon,
    required String label,
    required String subtitle,
    required Color color,
    required bool enabled,
    bool isLoading = false,
    required VoidCallback onTap,
  }) {
    return Material(
      color: enabled ? color.withOpacity(0.1) : Colors.grey[100],
      borderRadius: BorderRadius.circular(16),
      child: InkWell(
        onTap: enabled ? onTap : null,
        borderRadius: BorderRadius.circular(16),
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Row(
            children: [
              Container(
                padding: const EdgeInsets.all(10),
                decoration: BoxDecoration(
                  color: enabled ? color.withOpacity(0.2) : Colors.grey[200],
                  shape: BoxShape.circle,
                ),
                child: isLoading
                    ? SizedBox(
                        width: 24,
                        height: 24,
                        child: CircularProgressIndicator(
                          strokeWidth: 2,
                          color: color,
                        ),
                      )
                    : Icon(icon, color: enabled ? color : Colors.grey),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      label,
                      style: TextStyle(
                        fontSize: 16,
                        fontWeight: FontWeight.bold,
                        color: enabled ? color : Colors.grey,
                      ),
                    ),
                    Text(
                      subtitle,
                      style: TextStyle(
                        fontSize: 12,
                        color: enabled ? Colors.grey[600] : Colors.grey[400],
                      ),
                    ),
                  ],
                ),
              ),
              Icon(
                Icons.chevron_right,
                color: enabled ? color : Colors.grey[300],
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildInfoRow(IconData icon, String label, String value) {
    return Row(
      children: [
        Container(
          padding: const EdgeInsets.all(10),
          decoration: BoxDecoration(
            color: Colors.red.shade50,
            borderRadius: BorderRadius.circular(10),
          ),
          child: Icon(icon, color: Colors.red.shade400, size: 22),
        ),
        const SizedBox(width: 16),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(label, style: TextStyle(color: Colors.grey[600], fontSize: 13)),
              const SizedBox(height: 4),
              Text(value, style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 16)),
            ],
          ),
        ),
      ],
    );
  }
}
