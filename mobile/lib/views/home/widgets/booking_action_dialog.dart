import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:slib/models/upcoming_booking.dart';
import 'package:slib/services/booking/booking_service.dart';
import 'package:slib/views/booking/floor_plan_screen.dart';
import 'package:slib/views/home/widgets/nfc_seat_verification_screen.dart';
import 'package:slib/views/widgets/feedback_dialog.dart';

class BookingActionDialog extends StatefulWidget {
  final UpcomingBooking booking;
  final VoidCallback onActionComplete;

  const BookingActionDialog({
    super.key,
    required this.booking,
    required this.onActionComplete,
  });

  static Future<void> show(
    BuildContext context,
    UpcomingBooking booking,
    VoidCallback onComplete,
  ) {
    return showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (context) =>
          BookingActionDialog(booking: booking, onActionComplete: onComplete),
    );
  }

  @override
  State<BookingActionDialog> createState() => _BookingActionDialogState();
}

class _BookingActionDialogState extends State<BookingActionDialog> {
  final BookingService _bookingService = BookingService();
  bool _isLoading = false;
  bool _isCheckingNfcStatus = true;
  bool _isCheckedIntoLibrary = false;
  bool _canConfirmViaNfcFromBackend = false;
  bool _canLeaveViaNfcFromBackend = false;
  String? _nfcStatusMessage;
  String? _errorMessage;

  @override
  void initState() {
    super.initState();
    _loadSeatNfcStatus();
  }

  /// Check if booking can be cancelled (more than 12 hours before start)
  bool get _canCancel {
    return widget.booking.canCancel;
  }

  bool get _canChangeSeat {
    return widget.booking.canChangeSeat;
  }

  /// Check if booking is already confirmed
  bool get _isAlreadyConfirmed {
    return widget.booking.status.toUpperCase() == 'CONFIRMED';
  }

  Future<void> _loadSeatNfcStatus() async {
    try {
      final data = await _bookingService.getSeatNfcActionStatus(
        widget.booking.reservationId,
      );
      if (!mounted) return;

      setState(() {
        _isCheckingNfcStatus = false;
        _isCheckedIntoLibrary = data['checkedIntoLibrary'] == true;
        _canConfirmViaNfcFromBackend = data['canConfirmSeatWithNfc'] == true;
        _canLeaveViaNfcFromBackend = data['canLeaveSeatWithNfc'] == true;
        _nfcStatusMessage = data['message']?.toString();
      });
    } catch (e) {
      if (!mounted) return;
      setState(() {
        _isCheckingNfcStatus = false;
        _nfcStatusMessage =
            'Không kiểm tra được trạng thái check-in thư viện lúc này.';
      });
    }
  }

  Future<bool> _ensureNfcActionAllowed({required bool leaveSeat}) async {
    setState(() {
      _isCheckingNfcStatus = true;
      _errorMessage = null;
    });

    await _loadSeatNfcStatus();
    if (!mounted) return false;

    final allowed = leaveSeat
        ? _canLeaveViaNfcFromBackend
        : _canConfirmViaNfcFromBackend;
    if (allowed) {
      return true;
    }

    setState(() {
      _errorMessage = _nfcStatusMessage;
    });
    return false;
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
              child: Icon(
                Icons.cancel_outlined,
                size: 60,
                color: Colors.red.shade400,
              ),
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
                  _buildInfoRow(
                    Icons.location_on_rounded,
                    "Khu vực",
                    widget.booking.zoneName,
                  ),
                  const Divider(height: 30),
                  _buildInfoRow(
                    Icons.chair_alt_rounded,
                    "Mã ghế",
                    widget.booking.seatCode,
                  ),
                  const Divider(height: 30),
                  _buildInfoRow(
                    Icons.calendar_month_rounded,
                    "Ngày",
                    DateFormat(
                      'EEEE, dd/MM/yyyy',
                      'vi',
                    ).format(widget.booking.startTime),
                  ),
                  const Divider(height: 30),
                  _buildInfoRow(
                    Icons.access_time_rounded,
                    "Khung giờ",
                    widget.booking.timeRange,
                  ),
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
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(12),
                      ),
                    ),
                    child: const Text(
                      'Không',
                      style: TextStyle(color: Colors.grey, fontSize: 16),
                    ),
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
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(12),
                      ),
                    ),
                    child: const Text(
                      'Xác nhận hủy',
                      style: TextStyle(
                        color: Colors.white,
                        fontSize: 16,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
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

  Future<void> _handleChangeSeat() async {
    final rootNavigator = Navigator.of(context, rootNavigator: true);
    final rootContext = rootNavigator.context;

    Navigator.pop(context);
    await Future.delayed(const Duration(milliseconds: 100));
    if (!rootContext.mounted) return;

    final result = await Navigator.of(rootContext).push<bool>(
      MaterialPageRoute(
        builder: (_) => FloorPlanScreen(
          initialZoneId: widget.booking.zoneId,
          initialSeatId: widget.booking.seatId,
          initialDate: widget.booking.startTime,
          initialTimeSlot: widget.booking.timeRange,
          replacementReservationId: widget.booking.reservationId,
        ),
      ),
    );

    if (result == true) {
      widget.onActionComplete();
      if (rootContext.mounted) {
        ScaffoldMessenger.of(rootContext).showSnackBar(
          const SnackBar(
            content: Text('Đã đổi ghế thành công'),
            backgroundColor: Colors.green,
          ),
        );
      }
    }
  }

  Future<void> _handleNfcConfirm() async {
    final allowed = await _ensureNfcActionAllowed(leaveSeat: false);
    if (!allowed || !mounted) return;

    // Store the root navigator context before popping
    final rootContext = Navigator.of(context, rootNavigator: true).context;

    // Close this dialog first
    Navigator.pop(context);

    // Wait a frame for the dialog to fully close
    await Future.delayed(const Duration(milliseconds: 100));

    // Use try-catch to handle any navigation errors
    try {
      if (!rootContext.mounted) return;

      final seatCode = widget.booking.seatCode;
      final seatId = widget.booking.seatId;
      final reservationId = widget.booking.reservationId;
      final result = await NfcVerificationDialog.show(
        rootContext,
        seatCode: seatCode,
        seatId: seatId,
        reservationId: reservationId,
      );

      if (result == true) {
        // NFC screen already confirmed via confirmSeatWithNfcUid — just refresh UI
        widget.onActionComplete();
        if (rootContext.mounted) {
          ScaffoldMessenger.of(rootContext).showSnackBar(
            const SnackBar(
              content: Text('Xác nhận thành công!'),
              backgroundColor: Colors.green,
            ),
          );
        }
      }
    } catch (e) {
      // Navigation error - dialog was already dismissed or context invalid
      debugPrint('NFC verification navigation error: $e');
    }
  }

  Future<void> _handleLeaveSeatByNfc() async {
    final allowed = await _ensureNfcActionAllowed(leaveSeat: true);
    if (!allowed || !mounted) return;

    final sheetNavigator = Navigator.of(context);
    final rootNavigator = Navigator.of(context, rootNavigator: true);

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
            Container(
              width: 40,
              height: 4,
              margin: const EdgeInsets.only(bottom: 20),
              decoration: BoxDecoration(
                color: Colors.grey[300],
                borderRadius: BorderRadius.circular(2),
              ),
            ),
            Container(
              padding: const EdgeInsets.all(20),
              decoration: BoxDecoration(
                color: Colors.orange.shade50,
                shape: BoxShape.circle,
              ),
              child: Icon(
                Icons.logout_rounded,
                size: 60,
                color: Colors.orange.shade600,
              ),
            ),
            const SizedBox(height: 20),
            const Text(
              "Xác nhận trả chỗ ngồi",
              style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 8),
            Text(
              "Sau khi xác nhận và quét NFC, ghế sẽ được giải phóng ngay cho người khác đặt.",
              textAlign: TextAlign.center,
              style: TextStyle(color: Colors.grey[600]),
            ),
            const SizedBox(height: 24),
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
                  _buildInfoRow(
                    Icons.location_on_rounded,
                    "Khu vực",
                    widget.booking.zoneName,
                  ),
                  const Divider(height: 30),
                  _buildInfoRow(
                    Icons.chair_alt_rounded,
                    "Mã ghế",
                    widget.booking.seatCode,
                  ),
                  const Divider(height: 30),
                  _buildInfoRow(
                    Icons.access_time_rounded,
                    "Khung giờ",
                    widget.booking.timeRange,
                  ),
                ],
              ),
            ),
            const SizedBox(height: 24),
            Row(
              children: [
                Expanded(
                  child: OutlinedButton(
                    onPressed: () => Navigator.pop(context, false),
                    style: OutlinedButton.styleFrom(
                      padding: const EdgeInsets.symmetric(vertical: 16),
                      side: BorderSide(color: Colors.grey[400]!),
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(12),
                      ),
                    ),
                    child: const Text(
                      'Chưa',
                      style: TextStyle(color: Colors.grey, fontSize: 16),
                    ),
                  ),
                ),
                const SizedBox(width: 16),
                Expanded(
                  flex: 2,
                  child: ElevatedButton(
                    onPressed: () => Navigator.pop(context, true),
                    style: ElevatedButton.styleFrom(
                      backgroundColor: Colors.orange,
                      padding: const EdgeInsets.symmetric(vertical: 16),
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(12),
                      ),
                    ),
                    child: const Text(
                      'Tiếp tục quét NFC',
                      style: TextStyle(
                        color: Colors.white,
                        fontSize: 16,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
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
    if (!mounted) return;

    final rootContext = rootNavigator.context;
    sheetNavigator.pop();
    await Future.delayed(const Duration(milliseconds: 100));

    try {
      if (!rootContext.mounted) return;

      final result = await NfcVerificationDialog.show(
        rootContext,
        seatCode: widget.booking.seatCode,
        seatId: widget.booking.seatId,
        reservationId: widget.booking.reservationId,
        mode: NfcSeatActionMode.leaveSeat,
      );

      if (result == true) {
        widget.onActionComplete();
        if (rootContext.mounted) {
          ScaffoldMessenger.of(rootContext).showSnackBar(
            const SnackBar(
              content: Text('Đã trả chỗ ngồi thành công!'),
              backgroundColor: Colors.green,
            ),
          );
        }
        await Future.delayed(const Duration(milliseconds: 150));
        if (rootContext.mounted) {
          await showSeatFeedbackPopup(
            rootContext,
            reservationId: widget.booking.reservationId,
            zoneName: widget.booking.zoneName,
            seatCode: widget.booking.seatCode,
          );
        }
      }
    } catch (e) {
      debugPrint('NFC leave-seat navigation error: $e');
    }
  }

  @override
  Widget build(BuildContext context) {
    final showNfcActionButton = _isAlreadyConfirmed
        ? _canLeaveViaNfcFromBackend
        : _canConfirmViaNfcFromBackend;

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

              if (widget.booking.layoutChanged) ...[
                Container(
                  width: double.infinity,
                  padding: const EdgeInsets.all(14),
                  decoration: BoxDecoration(
                    color: const Color(0xFFFFF7ED),
                    borderRadius: BorderRadius.circular(14),
                    border: Border.all(color: const Color(0xFFFDBA74)),
                  ),
                  child: Row(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      const Icon(
                        Icons.warning_amber_rounded,
                        color: Color(0xFFEA580C),
                        size: 20,
                      ),
                      const SizedBox(width: 10),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              widget.booking.layoutChangeTitle ??
                                  'Lịch đặt chỗ này vừa bị ảnh hưởng',
                              style: const TextStyle(
                                color: Color(0xFF9A3412),
                                fontWeight: FontWeight.bold,
                                fontSize: 14,
                              ),
                            ),
                            const SizedBox(height: 4),
                            Text(
                              widget.booking.layoutChangeMessage ??
                                  'Thư viện vừa chỉnh sửa sơ đồ. Bạn nên kiểm tra lại và đổi ghế nếu cần.',
                              style: const TextStyle(
                                color: Color(0xFF9A3412),
                                fontSize: 12,
                                height: 1.45,
                              ),
                            ),
                          ],
                        ),
                      ),
                    ],
                  ),
                ),
                const SizedBox(height: 16),
              ],

              // Booking info header
              Row(
                children: [
                  Container(
                    padding: const EdgeInsets.all(12),
                    decoration: BoxDecoration(
                      color: Colors.blue.withValues(alpha: 0.1),
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
                    color: Colors.red.withValues(alpha: 0.1),
                    borderRadius: BorderRadius.circular(12),
                  ),
                  child: Row(
                    children: [
                      const Icon(
                        Icons.error_outline,
                        color: Colors.red,
                        size: 20,
                      ),
                      const SizedBox(width: 8),
                      Expanded(
                        child: Text(
                          _errorMessage!,
                          style: const TextStyle(
                            color: Colors.red,
                            fontSize: 13,
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
              ],

              const SizedBox(height: 24),

              // Action buttons
              if (widget.booking.layoutChanged) ...[
                _buildActionButton(
                  icon: Icons.swap_horiz_rounded,
                  label: 'Đổi sang ghế khác',
                  subtitle: _canChangeSeat
                      ? 'Giữ nguyên ngày và khung giờ, chỉ đổi ghế'
                      : 'Chỉ đổi ghế được trước giờ bắt đầu',
                  color: const Color(0xFFEA580C),
                  enabled: _canChangeSeat && !_isLoading,
                  onTap: _handleChangeSeat,
                ),
                const SizedBox(height: 12),
              ],

              // 1. Cancel booking button
              _buildActionButton(
                icon: Icons.cancel_outlined,
                label: 'Hủy đặt chỗ',
                  subtitle: widget.booking.layoutChanged
                    ? (_canCancel
                          ? 'Bạn được hủy lịch này dù đã gần tới giờ'
                          : 'Không thể hủy khi lịch đã bắt đầu')
                    : (_canCancel
                          ? 'Hủy trước thời hạn cấu hình để không bị trừ điểm'
                          : 'Đã quá thời hạn hủy cho phép'),
                color: Colors.red,
                enabled: _canCancel && !_isLoading,
                isLoading: _isLoading,
                onTap: _handleCancel,
              ),

              const SizedBox(height: 12),

              if (_isCheckingNfcStatus)
                _buildActionButton(
                  icon: _isAlreadyConfirmed
                      ? Icons.logout_rounded
                      : Icons.nfc,
                  label: _isAlreadyConfirmed
                      ? 'Trả chỗ ngồi bằng NFC'
                      : 'Xác nhận chỗ ngồi',
                  subtitle: 'Đang kiểm tra trạng thái check-in thư viện...',
                  color: _isAlreadyConfirmed ? Colors.orange : Colors.green,
                  enabled: false,
                  isLoading: true,
                  onTap: () {},
                )
              else if (showNfcActionButton)
                _buildActionButton(
                  icon: _isAlreadyConfirmed
                      ? Icons.logout_rounded
                      : Icons.nfc,
                  label: _isAlreadyConfirmed
                      ? 'Trả chỗ ngồi bằng NFC'
                      : 'Xác nhận chỗ ngồi',
                  subtitle: _isAlreadyConfirmed
                      ? 'Quét lại NFC đúng ghế để rời chỗ an toàn'
                      : 'Bạn đã check-in thư viện. Chạm NFC trên ghế để xác nhận.',
                  color: _isAlreadyConfirmed ? Colors.orange : Colors.green,
                  enabled: !_isLoading,
                  onTap: _isAlreadyConfirmed
                      ? _handleLeaveSeatByNfc
                      : _handleNfcConfirm,
                )
              else
                _buildNfcRequirementNotice(),

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
      color: enabled ? color.withValues(alpha: 0.1) : Colors.grey[100],
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
                  color: enabled
                      ? color.withValues(alpha: 0.2)
                      : Colors.grey[200],
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

  Widget _buildNfcRequirementNotice() {
    final highlightColor = _isCheckedIntoLibrary
        ? const Color(0xFFB45309)
        : const Color(0xFFB91C1C);
    final backgroundColor = _isCheckedIntoLibrary
        ? const Color(0xFFFFFBEB)
        : const Color(0xFFFEF2F2);
    final borderColor = _isCheckedIntoLibrary
        ? const Color(0xFFFCD34D)
        : const Color(0xFFFECACA);

    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: backgroundColor,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: borderColor),
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(
            _isCheckedIntoLibrary
                ? Icons.access_time_rounded
                : Icons.login_rounded,
            color: highlightColor,
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  _isCheckedIntoLibrary
                      ? 'Chưa thể dùng NFC cho lượt đặt này'
                      : 'Bạn chưa check-in vào thư viện',
                  style: TextStyle(
                    color: highlightColor,
                    fontWeight: FontWeight.w700,
                    fontSize: 14,
                  ),
                ),
                const SizedBox(height: 4),
                Text(
                  _nfcStatusMessage ??
                      'Bạn cần hoàn tất check-in thư viện trước khi dùng NFC cho ghế ngồi.',
                  style: TextStyle(
                    color: highlightColor,
                    fontSize: 12,
                    height: 1.45,
                  ),
                ),
              ],
            ),
          ),
        ],
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
              Text(
                label,
                style: TextStyle(color: Colors.grey[600], fontSize: 13),
              ),
              const SizedBox(height: 4),
              Text(
                value,
                style: const TextStyle(
                  fontWeight: FontWeight.w600,
                  fontSize: 16,
                ),
              ),
            ],
          ),
        ),
      ],
    );
  }
}
