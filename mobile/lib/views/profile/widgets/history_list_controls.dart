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

class HistoryListControls extends StatelessWidget {
  final HistoryTimeFilter selectedFilter;
  final ValueChanged<HistoryTimeFilter> onFilterChanged;
  final bool isExpanded;
  final ValueChanged<bool> onExpandedChanged;
  final int totalCount;
  final int visibleCount;
  final int hiddenCount;
  final VoidCallback? onRestoreHidden;

  const HistoryListControls({
    super.key,
    required this.selectedFilter,
    required this.onFilterChanged,
    required this.isExpanded,
    required this.onExpandedChanged,
    required this.totalCount,
    required this.visibleCount,
    this.hiddenCount = 0,
    this.onRestoreHidden,
  });

  @override
  Widget build(BuildContext context) {
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
          SingleChildScrollView(
            scrollDirection: Axis.horizontal,
            child: Row(
              children: HistoryTimeFilter.values.map((filter) {
                final selected = filter == selectedFilter;
                return Padding(
                  padding: EdgeInsets.only(
                    right: filter == HistoryTimeFilter.values.last ? 0 : 8,
                  ),
                  child: ChoiceChip(
                    label: Text(filter.label),
                    selected: selected,
                    onSelected: (_) => onFilterChanged(filter),
                    selectedColor: AppColors.brandColor.withValues(alpha: 0.12),
                    labelStyle: TextStyle(
                      color: selected
                          ? AppColors.brandColor
                          : const Color(0xFF6B7280),
                      fontWeight: selected ? FontWeight.w700 : FontWeight.w500,
                    ),
                    side: BorderSide(
                      color: selected
                          ? AppColors.brandColor.withValues(alpha: 0.18)
                          : const Color(0xFFE5E7EB),
                    ),
                    backgroundColor: const Color(0xFFF9FAFB),
                  ),
                );
              }).toList(),
            ),
          ),
          const SizedBox(height: 10),
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
