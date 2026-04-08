import 'package:flutter/material.dart';
import 'package:slib/assets/colors.dart';

enum HistoryTimeFilter { last7Days, last30Days, all }

extension HistoryTimeFilterLabel on HistoryTimeFilter {
  String get label {
    switch (this) {
      case HistoryTimeFilter.last7Days:
        return '7 ngày';
      case HistoryTimeFilter.last30Days:
        return '30 ngày';
      case HistoryTimeFilter.all:
        return 'Tất cả';
    }
  }
}

Future<HistoryTimeFilter?> showHistoryFilterDialog(
  BuildContext context, {
  required HistoryTimeFilter initialFilter,
}) async {
  HistoryTimeFilter draftFilter = initialFilter;

  return showDialog<HistoryTimeFilter>(
    context: context,
    builder: (context) => StatefulBuilder(
      builder: (context, setState) => AlertDialog(
        backgroundColor: Colors.white,
        surfaceTintColor: Colors.transparent,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(24)),
        title: const Text('Lọc theo thời gian'),
        contentPadding: const EdgeInsets.fromLTRB(12, 12, 12, 0),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: HistoryTimeFilter.values.map((filter) {
            final isSelected = draftFilter == filter;
            return InkWell(
              borderRadius: BorderRadius.circular(12),
              onTap: () {
                setState(() {
                  draftFilter = filter;
                });
              },
              child: Container(
                margin: const EdgeInsets.only(bottom: 8),
                padding: const EdgeInsets.symmetric(
                  horizontal: 12,
                  vertical: 12,
                ),
                decoration: BoxDecoration(
                  color: isSelected ? const Color(0xFFFFF2E9) : Colors.white,
                  borderRadius: BorderRadius.circular(12),
                  border: Border.all(
                    color: isSelected
                        ? AppColors.brandColor
                        : const Color(0xFFE5E7EB),
                  ),
                ),
                child: Row(
                  children: [
                    Expanded(
                      child: Text(
                        filter.label,
                        style: TextStyle(
                          fontWeight: isSelected
                              ? FontWeight.w700
                              : FontWeight.w500,
                          color: isSelected
                              ? AppColors.brandColor
                              : const Color(0xFF111827),
                        ),
                      ),
                    ),
                    Icon(
                      isSelected
                          ? Icons.check_circle_rounded
                          : Icons.radio_button_unchecked_rounded,
                      color: isSelected
                          ? AppColors.brandColor
                          : const Color(0xFF9CA3AF),
                    ),
                  ],
                ),
              ),
            );
          }).toList(),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(),
            style: TextButton.styleFrom(
              foregroundColor: const Color(0xFF8B5E3C),
            ),
            child: const Text('Huỷ'),
          ),
          FilledButton(
            onPressed: () => Navigator.of(context).pop(draftFilter),
            style: FilledButton.styleFrom(
              backgroundColor: AppColors.brandColor,
            ),
            child: const Text('Xác nhận'),
          ),
        ],
      ),
    ),
  );
}

class HistoryListControls extends StatelessWidget {
  final bool isExpanded;
  final ValueChanged<bool> onExpandedChanged;
  final int totalCount;
  final int visibleCount;
  final int hiddenCount;
  final VoidCallback? onRestoreHidden;

  const HistoryListControls({
    super.key,
    required this.isExpanded,
    required this.onExpandedChanged,
    required this.totalCount,
    required this.visibleCount,
    this.hiddenCount = 0,
    this.onRestoreHidden,
  });

  @override
  Widget build(BuildContext context) {
    if (totalCount == 0 && hiddenCount == 0) {
      return const SizedBox.shrink();
    }

    final theme = Theme.of(context);
    final canExpand = totalCount > 10;

    return Container(
      margin: const EdgeInsets.fromLTRB(16, 16, 16, 8),
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(16),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withValues(alpha: 0.04),
            blurRadius: 10,
            offset: const Offset(0, 2),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Wrap(
            spacing: 12,
            runSpacing: 8,
            crossAxisAlignment: WrapCrossAlignment.center,
            children: [
              Text(
                'Hiển thị $visibleCount/$totalCount mục',
                style: theme.textTheme.bodySmall?.copyWith(
                  color: const Color(0xFF6B7280),
                  fontWeight: FontWeight.w600,
                ),
              ),
              if (canExpand)
                InkWell(
                  onTap: () => onExpandedChanged(!isExpanded),
                  borderRadius: BorderRadius.circular(999),
                  child: Padding(
                    padding: const EdgeInsets.symmetric(
                      horizontal: 2,
                      vertical: 2,
                    ),
                    child: Row(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        Text(
                          isExpanded ? 'Thu gọn' : 'Xem thêm',
                          style: const TextStyle(
                            color: AppColors.brandColor,
                            fontWeight: FontWeight.w700,
                            fontSize: 12,
                          ),
                        ),
                        const SizedBox(width: 4),
                        Icon(
                          isExpanded
                              ? Icons.expand_less_rounded
                              : Icons.expand_more_rounded,
                          size: 16,
                          color: AppColors.brandColor,
                        ),
                      ],
                    ),
                  ),
                ),
              if (hiddenCount > 0 && onRestoreHidden != null)
                InkWell(
                  onTap: onRestoreHidden,
                  borderRadius: BorderRadius.circular(999),
                  child: Padding(
                    padding: const EdgeInsets.symmetric(
                      horizontal: 2,
                      vertical: 2,
                    ),
                    child: Row(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        const Icon(
                          Icons.visibility_outlined,
                          size: 15,
                          color: AppColors.brandColor,
                        ),
                        const SizedBox(width: 4),
                        Text(
                          'Hiện lại $hiddenCount mục ẩn',
                          style: const TextStyle(
                            color: AppColors.brandColor,
                            fontWeight: FontWeight.w700,
                            fontSize: 12,
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
            ],
          ),
        ],
      ),
    );
  }
}
