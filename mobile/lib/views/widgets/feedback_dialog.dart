import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:slib/core/constants/api_constants.dart';
import 'package:slib/services/auth/auth_service.dart';

class FeedbackDialog extends StatefulWidget {
  final String title;
  final String subtitle;
  final String? conversationId;
  final String? reservationId;
  final VoidCallback? onSubmitted;
  final VoidCallback? onDismissed;

  const FeedbackDialog({
    super.key,
    required this.title,
    this.subtitle = '',
    this.conversationId,
    this.reservationId,
    this.onSubmitted,
    this.onDismissed,
  });

  @override
  State<FeedbackDialog> createState() => _FeedbackDialogState();
}

const _categoryOptions = [
  {'value': 'FACILITY', 'label': 'Cơ sở vật chất'},
  {'value': 'SERVICE', 'label': 'Dịch vụ'},
  {'value': 'GENERAL', 'label': 'Chung'},
];

class _FeedbackDialogState extends State<FeedbackDialog> {
  int _rating = 0;
  String _selectedCategory = 'GENERAL';
  final _commentController = TextEditingController();
  bool _isSubmitting = false;
  bool _submitted = false;

  @override
  void dispose() {
    _commentController.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    if (_rating == 0) return;
    setState(() => _isSubmitting = true);

    try {
      final authService = Provider.of<AuthService>(context, listen: false);
      final url = Uri.parse('${ApiConstants.domain}/slib/feedbacks');

      final body = <String, dynamic>{
        'rating': _rating,
        'content': _commentController.text.trim(),
        'category': widget.conversationId != null
            ? 'MESSAGE'
            : _selectedCategory,
      };
      if (widget.conversationId != null) {
        body['conversationId'] = widget.conversationId;
      }
      if (widget.reservationId != null) {
        body['reservationId'] = widget.reservationId;
      }

      final response = await authService.authenticatedRequest(
        'POST',
        url,
        headers: {'Content-Type': 'application/json'},
        body: jsonEncode(body),
      );

      if (response.statusCode == 201 || response.statusCode == 200) {
        setState(() => _submitted = true);
        widget.onSubmitted?.call();
      }
    } catch (_) {
      // silently ignore
    } finally {
      if (mounted) setState(() => _isSubmitting = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    if (_submitted) {
      return _buildThankYou();
    }
    return _buildForm();
  }

  Widget _buildThankYou() {
    return Container(
      padding: const EdgeInsets.all(24),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(16),
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          const Icon(
            Icons.check_circle_rounded,
            color: Color(0xFF4CAF50),
            size: 56,
          ),
          const SizedBox(height: 12),
          const Text(
            'Cảm ơn bạn đã đánh giá!',
            style: TextStyle(fontSize: 16, fontWeight: FontWeight.w600),
          ),
          const SizedBox(height: 8),
          Text(
            'Phản hồi của bạn giúp chúng tôi cải thiện dịch vụ.',
            style: TextStyle(fontSize: 13, color: Colors.grey[600]),
            textAlign: TextAlign.center,
          ),
        ],
      ),
    );
  }

  Widget _buildForm() {
    return Container(
      padding: const EdgeInsets.all(24),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(16),
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Text(
            widget.title,
            style: const TextStyle(fontSize: 17, fontWeight: FontWeight.w700),
            textAlign: TextAlign.center,
          ),
          if (widget.subtitle.isNotEmpty) ...[
            const SizedBox(height: 6),
            Text(
              widget.subtitle,
              style: TextStyle(fontSize: 13, color: Colors.grey[600]),
              textAlign: TextAlign.center,
            ),
          ],
          const SizedBox(height: 20),
          Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: List.generate(5, (i) {
              final starIndex = i + 1;
              return GestureDetector(
                onTap: () => setState(() => _rating = starIndex),
                child: Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 6),
                  child: Icon(
                    starIndex <= _rating
                        ? Icons.star_rounded
                        : Icons.star_outline_rounded,
                    size: 40,
                    color: starIndex <= _rating
                        ? const Color(0xFFFF751F)
                        : Colors.grey[350],
                  ),
                ),
              );
            }),
          ),
          if (_rating > 0) ...[
            const SizedBox(height: 8),
            Text(
              _ratingLabel(_rating),
              style: const TextStyle(
                fontSize: 13,
                fontWeight: FontWeight.w500,
                color: Color(0xFFFF751F),
              ),
            ),
          ],
          if (widget.conversationId == null) ...[
            const SizedBox(height: 16),
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 12),
              decoration: BoxDecoration(
                color: Colors.grey[50],
                borderRadius: BorderRadius.circular(12),
                border: Border.all(color: Colors.grey[200]!),
              ),
              child: DropdownButtonHideUnderline(
                child: DropdownButton<String>(
                  value: _selectedCategory,
                  isExpanded: true,
                  icon: Icon(
                    Icons.keyboard_arrow_down_rounded,
                    color: Colors.grey[500],
                  ),
                  style: TextStyle(fontSize: 14, color: Colors.grey[800]),
                  items: _categoryOptions.map((opt) {
                    return DropdownMenuItem<String>(
                      value: opt['value'],
                      child: Text(opt['label']!),
                    );
                  }).toList(),
                  onChanged: (val) {
                    if (val != null) setState(() => _selectedCategory = val);
                  },
                ),
              ),
            ),
          ],
          const SizedBox(height: 16),
          TextField(
            controller: _commentController,
            maxLines: 3,
            maxLength: 500,
            decoration: InputDecoration(
              hintText: 'Chia sẻ thêm ý kiến của bạn (không bắt buộc)',
              hintStyle: TextStyle(fontSize: 13, color: Colors.grey[400]),
              filled: true,
              fillColor: Colors.grey[50],
              border: OutlineInputBorder(
                borderRadius: BorderRadius.circular(12),
                borderSide: BorderSide(color: Colors.grey[200]!),
              ),
              enabledBorder: OutlineInputBorder(
                borderRadius: BorderRadius.circular(12),
                borderSide: BorderSide(color: Colors.grey[200]!),
              ),
              focusedBorder: OutlineInputBorder(
                borderRadius: BorderRadius.circular(12),
                borderSide: const BorderSide(color: Color(0xFFFF751F)),
              ),
              contentPadding: const EdgeInsets.all(14),
              counterStyle: TextStyle(fontSize: 11, color: Colors.grey[400]),
            ),
          ),
          const SizedBox(height: 16),
          SizedBox(
            width: double.infinity,
            child: ElevatedButton(
              onPressed: _rating > 0 && !_isSubmitting ? _submit : null,
              style: ElevatedButton.styleFrom(
                backgroundColor: const Color(0xFFFF751F),
                foregroundColor: Colors.white,
                disabledBackgroundColor: Colors.grey[300],
                padding: const EdgeInsets.symmetric(vertical: 14),
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(12),
                ),
                elevation: 0,
              ),
              child: _isSubmitting
                  ? const SizedBox(
                      height: 20,
                      width: 20,
                      child: CircularProgressIndicator(
                        strokeWidth: 2,
                        color: Colors.white,
                      ),
                    )
                  : const Text(
                      'Gửi đánh giá',
                      style: TextStyle(
                        fontSize: 15,
                        fontWeight: FontWeight.w600,
                      ),
                    ),
            ),
          ),
          const SizedBox(height: 8),
          TextButton(
            onPressed: () {
              widget.onDismissed?.call();
            },
            child: Text(
              'Bỏ qua',
              style: TextStyle(fontSize: 13, color: Colors.grey[500]),
            ),
          ),
        ],
      ),
    );
  }

  String _ratingLabel(int rating) {
    switch (rating) {
      case 1:
        return 'Rất tệ';
      case 2:
        return 'Tệ';
      case 3:
        return 'Bình thường';
      case 4:
        return 'Tốt';
      case 5:
        return 'Tuyệt vời';
      default:
        return '';
    }
  }
}

/// Show a feedback popup dialog (used for post-checkout prompt on home screen)
Future<void> showFeedbackPopup(
  BuildContext context, {
  required String title,
  String subtitle = '',
  String? conversationId,
  String? reservationId,
  VoidCallback? onSubmitted,
  VoidCallback? onDismissed,
}) {
  return showDialog(
    context: context,
    barrierDismissible: false,
    builder: (ctx) => Dialog(
      backgroundColor: Colors.transparent,
      insetPadding: const EdgeInsets.symmetric(horizontal: 24),
      child: FeedbackDialog(
        title: title,
        subtitle: subtitle,
        conversationId: conversationId,
        reservationId: reservationId,
        onSubmitted: () {
          onSubmitted?.call();
          Future.delayed(const Duration(seconds: 2), () {
            if (!ctx.mounted) return;
            final navigator = Navigator.of(ctx);
            if (navigator.canPop()) {
              navigator.pop();
            }
          });
        },
        onDismissed: () {
          onDismissed?.call();
          if (!ctx.mounted) return;
          Navigator.of(ctx).pop();
        },
      ),
    ),
  );
}
