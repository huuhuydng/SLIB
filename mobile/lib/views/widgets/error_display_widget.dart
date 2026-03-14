import 'package:flutter/material.dart';
import 'package:slib/assets/colors.dart';

/// Widget hiển thị lỗi thống nhất trong toàn app.
///
/// Sử dụng:
/// ```dart
/// ErrorDisplayWidget(
///   message: 'Không thể tải dữ liệu',
///   onRetry: () => _loadData(),
/// )
/// ```
class ErrorDisplayWidget extends StatelessWidget {
  final String message;
  final String? detail;
  final VoidCallback? onRetry;
  final IconData icon;

  const ErrorDisplayWidget({
    super.key,
    required this.message,
    this.detail,
    this.onRetry,
    this.icon = Icons.error_outline_rounded,
  });

  /// Factory cho lỗi mạng
  factory ErrorDisplayWidget.network({VoidCallback? onRetry}) {
    return ErrorDisplayWidget(
      message: 'Không có kết nối mạng',
      detail: 'Vui lòng kiểm tra kết nối Internet và thử lại',
      icon: Icons.wifi_off_rounded,
      onRetry: onRetry,
    );
  }

  /// Factory cho lỗi server
  factory ErrorDisplayWidget.server({VoidCallback? onRetry}) {
    return ErrorDisplayWidget(
      message: 'Lỗi máy chủ',
      detail: 'Hệ thống đang gặp sự cố, vui lòng thử lại sau',
      icon: Icons.cloud_off_rounded,
      onRetry: onRetry,
    );
  }

  /// Factory cho lỗi không có dữ liệu
  factory ErrorDisplayWidget.empty({String? message}) {
    return ErrorDisplayWidget(
      message: message ?? 'Chưa có dữ liệu',
      icon: Icons.inbox_rounded,
    );
  }

  /// Factory cho lỗi đăng nhập
  factory ErrorDisplayWidget.auth({VoidCallback? onRetry}) {
    return ErrorDisplayWidget(
      message: 'Phiên đăng nhập hết hạn',
      detail: 'Vui lòng đăng nhập lại để tiếp tục',
      icon: Icons.lock_outline_rounded,
      onRetry: onRetry,
    );
  }

  /// Chuyển đổi Exception/error message thành thông báo tiếng Việt
  static String toVietnamese(dynamic error) {
    final msg = error.toString().toLowerCase();

    if (msg.contains('socketexception') ||
        msg.contains('no internet') ||
        msg.contains('failed host lookup') ||
        msg.contains('network is unreachable')) {
      return 'Không có kết nối mạng';
    }
    if (msg.contains('timeout') || msg.contains('timed out')) {
      return 'Kết nối quá chậm, vui lòng thử lại';
    }
    if (msg.contains('401') || msg.contains('unauthorized')) {
      return 'Phiên đăng nhập hết hạn';
    }
    if (msg.contains('403') || msg.contains('forbidden')) {
      return 'Bạn không có quyền truy cập';
    }
    if (msg.contains('404') || msg.contains('not found')) {
      return 'Không tìm thấy dữ liệu';
    }
    if (msg.contains('500') || msg.contains('internal server')) {
      return 'Lỗi máy chủ, vui lòng thử lại sau';
    }
    if (msg.contains('failed to load')) {
      return 'Không thể tải dữ liệu';
    }
    if (msg.contains('connection refused')) {
      return 'Không thể kết nối đến máy chủ';
    }

    return 'Đã xảy ra lỗi, vui lòng thử lại';
  }

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 32),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Container(
              width: 80,
              height: 80,
              decoration: BoxDecoration(
                color: AppColors.brandColor.withValues(alpha: 0.1),
                shape: BoxShape.circle,
              ),
              child: Icon(
                icon,
                size: 40,
                color: AppColors.brandColor,
              ),
            ),
            const SizedBox(height: 20),
            Text(
              message,
              textAlign: TextAlign.center,
              style: const TextStyle(
                fontSize: 16,
                fontWeight: FontWeight.w600,
                color: AppColors.textPrimary,
              ),
            ),
            if (detail != null) ...[
              const SizedBox(height: 8),
              Text(
                detail!,
                textAlign: TextAlign.center,
                style: const TextStyle(
                  fontSize: 13,
                  color: AppColors.textSecondary,
                  height: 1.4,
                ),
              ),
            ],
            if (onRetry != null) ...[
              const SizedBox(height: 24),
              SizedBox(
                width: 160,
                height: 44,
                child: ElevatedButton.icon(
                  onPressed: onRetry,
                  icon: const Icon(Icons.refresh_rounded, size: 18),
                  label: const Text(
                    'Thử lại',
                    style: TextStyle(fontWeight: FontWeight.w600),
                  ),
                  style: ElevatedButton.styleFrom(
                    backgroundColor: AppColors.brandColor,
                    foregroundColor: Colors.white,
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(12),
                    ),
                    elevation: 0,
                  ),
                ),
              ),
            ],
          ],
        ),
      ),
    );
  }
}
