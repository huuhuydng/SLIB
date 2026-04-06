import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:provider/provider.dart';
import 'package:slib/assets/colors.dart';
import 'package:slib/services/notification/notification_service.dart';
import 'package:slib/views/widgets/error_display_widget.dart';

class _NotificationCategoryOption {
  final String key;
  final String label;

  const _NotificationCategoryOption(this.key, this.label);
}

class _NotificationGroup {
  final List<NotificationItem> items;
  final String? displayTitle;
  final String? displayContent;

  _NotificationGroup(this.items, {this.displayTitle, this.displayContent});

  NotificationItem get latest => items.first;
  String get title => displayTitle ?? latest.title;
  String get content => displayContent ?? latest.content;
  bool get isGrouped => items.length > 1;
  int get count => items.length;
  bool get hasUnread => items.any((item) => !item.isRead);
}

const _allCategory = _NotificationCategoryOption('ALL', 'Tất cả');
const _categoryOptions = <_NotificationCategoryOption>[
  _allCategory,
  _NotificationCategoryOption('MESSAGE', 'Tin nhắn'),
  _NotificationCategoryOption('PROCESSING', 'Xử lý'),
  _NotificationCategoryOption('REPUTATION', 'Điểm uy tín'),
  _NotificationCategoryOption('BOOKING', 'Đặt chỗ'),
  _NotificationCategoryOption('NEWS', 'Tin tức'),
  _NotificationCategoryOption('SYSTEM', 'Hệ thống'),
];

List<_NotificationGroup> _groupConsecutiveNotifications(
  List<NotificationItem> notifications,
) {
  if (notifications.isEmpty) return [];

  final groups = <_NotificationGroup>[];
  var currentGroup = <NotificationItem>[notifications.first];

  for (var i = 1; i < notifications.length; i++) {
    final current = notifications[i];
    final previous = notifications[i - 1];

    final isSameGroup =
        current.category == previous.category &&
        current.type == previous.type &&
        current.title == previous.title;

    if (isSameGroup) {
      currentGroup.add(current);
    } else {
      groups.add(_NotificationGroup(currentGroup));
      currentGroup = <NotificationItem>[current];
    }
  }

  groups.add(_NotificationGroup(currentGroup));
  return groups;
}

class NotificationScreen extends StatefulWidget {
  const NotificationScreen({super.key});

  @override
  State<NotificationScreen> createState() => _NotificationScreenState();
}

class _NotificationScreenState extends State<NotificationScreen> {
  final Set<String> _expandedGroups = <String>{};
  final ScrollController _categoryScrollController = ScrollController();
  final GlobalKey _categoryScrollViewKey = GlobalKey();
  late final Map<String, GlobalKey> _categoryChipKeys;
  String _selectedCategory = _allCategory.key;

  int _unreadCountForCategory(
    List<NotificationItem> notifications,
    String categoryKey,
  ) {
    return notifications
        .where((item) => item.category == categoryKey && !item.isRead)
        .length;
  }

  @override
  void initState() {
    super.initState();
    _categoryChipKeys = {
      for (final option in _categoryOptions) option.key: GlobalKey(),
    };
    WidgetsBinding.instance.addPostFrameCallback((_) {
      final service = context.read<NotificationService>();
      service.refreshData();
    });
  }

  @override
  void dispose() {
    _categoryScrollController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFFF8F8F8),
      appBar: AppBar(
        title: const Text(
          'Thông báo',
          style: TextStyle(fontWeight: FontWeight.bold, color: Colors.white),
        ),
        backgroundColor: AppColors.brandColor,
        foregroundColor: Colors.white,
        elevation: 0,
      ),
      body: Consumer<NotificationService>(
        builder: (context, service, _) {
          if (service.isLoading) {
            return const Center(
              child: CircularProgressIndicator(color: AppColors.brandColor),
            );
          }

          if (service.notifications.isEmpty) {
            return ErrorDisplayWidget.empty(message: 'Chưa có thông báo nào');
          }

          final filteredNotifications = _selectedCategory == _allCategory.key
              ? service.notifications
              : service.notifications
                    .where((item) => item.category == _selectedCategory)
                    .toList();

          if (filteredNotifications.isEmpty) {
            final selectedLabel = _categoryOptions
                .firstWhere((item) => item.key == _selectedCategory)
                .label;
            return Column(
              children: [
                _buildCategoryFilterBar(service.notifications),
                Expanded(
                  child: ErrorDisplayWidget.empty(
                    message: 'Chưa có thông báo trong mục $selectedLabel',
                  ),
                ),
              ],
            );
          }

          return RefreshIndicator(
            onRefresh: () async {
              _expandedGroups.clear();
              await service.refreshData();
            },
            color: AppColors.brandColor,
            child: ListView(
              padding: const EdgeInsets.only(bottom: 16),
              children: [
                _buildCategoryFilterBar(service.notifications),
                if (_selectedCategory == _allCategory.key)
                  ..._buildAllCategorySections(filteredNotifications, service)
                else
                  ..._buildSingleCategorySection(
                    filteredNotifications,
                    service,
                  ),
              ],
            ),
          );
        },
      ),
    );
  }

  Widget _buildCategoryFilterBar(List<NotificationItem> notifications) {
    final counts = <String, int>{};
    for (final item in notifications) {
      counts[item.category] = (counts[item.category] ?? 0) + 1;
    }

    return Container(
      color: Colors.white,
      padding: const EdgeInsets.fromLTRB(12, 12, 12, 8),
      child: SingleChildScrollView(
        key: _categoryScrollViewKey,
        controller: _categoryScrollController,
        scrollDirection: Axis.horizontal,
        child: Row(
          children: _categoryOptions.map((option) {
            final isSelected = option.key == _selectedCategory;
            final count = option.key == 'ALL'
                ? notifications.length
                : (counts[option.key] ?? 0);

            return Padding(
              key: _categoryChipKeys[option.key],
              padding: const EdgeInsets.only(right: 8),
              child: ChoiceChip(
                label: Text('${option.label} ($count)'),
                selected: isSelected,
                selectedColor: AppColors.brandColor.withValues(alpha: 0.18),
                backgroundColor: const Color(0xFFF3F4F6),
                side: BorderSide(
                  color: isSelected
                      ? AppColors.brandColor
                      : const Color(0xFFE5E7EB),
                ),
                labelStyle: TextStyle(
                  color: isSelected
                      ? AppColors.brandColor
                      : const Color(0xFF4B5563),
                  fontWeight: isSelected ? FontWeight.w700 : FontWeight.w500,
                ),
                onSelected: (_) {
                  setState(() {
                    _selectedCategory = option.key;
                    _expandedGroups.clear();
                  });
                  _scrollToSelectedCategory(option.key);
                },
              ),
            );
          }).toList(),
        ),
      ),
    );
  }

  void _scrollToSelectedCategory(String categoryKey) {
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (!_categoryScrollController.hasClients) return;

      final chipContext = _categoryChipKeys[categoryKey]?.currentContext;
      final scrollViewContext = _categoryScrollViewKey.currentContext;
      if (chipContext == null || scrollViewContext == null) return;

      final chipBox = chipContext.findRenderObject() as RenderBox?;
      final viewportBox = scrollViewContext.findRenderObject() as RenderBox?;
      if (chipBox == null || viewportBox == null) return;

      final currentOffset = _categoryScrollController.offset;
      final chipOffset = chipBox.localToGlobal(
        Offset.zero,
        ancestor: viewportBox,
      );
      final chipLeft = chipOffset.dx;
      final chipRight = chipLeft + chipBox.size.width;
      final viewportWidth = viewportBox.size.width;

      const edgePadding = 24.0;
      var targetOffset = currentOffset;

      if (chipLeft < edgePadding) {
        targetOffset += chipLeft - edgePadding;
      } else if (chipRight > viewportWidth - edgePadding) {
        targetOffset += chipRight - (viewportWidth - edgePadding);
      } else {
        return;
      }

      final clampedTarget = targetOffset.clamp(
        0.0,
        _categoryScrollController.position.maxScrollExtent,
      );

      if ((clampedTarget - currentOffset).abs() < 6) return;

      _categoryScrollController.animateTo(
        clampedTarget,
        duration: const Duration(milliseconds: 220),
        curve: Curves.easeOutCubic,
      );
    });
  }

  List<Widget> _buildAllCategorySections(
    List<NotificationItem> notifications,
    NotificationService service,
  ) {
    final widgets = <Widget>[];

    for (final option in _categoryOptions.where((item) => item.key != 'ALL')) {
      final items = notifications
          .where((notification) => notification.category == option.key)
          .toList();
      if (items.isEmpty) continue;

      widgets.add(
        _CategoryHeader(
          categoryKey: option.key,
          label: option.label,
          unreadCount: _unreadCountForCategory(items, option.key),
          onMarkAllRead: () => _markCategoryAsRead(service, option.key),
        ),
      );

      if (option.key == 'BOOKING') {
        final groupId = '${option.key}-all-booking';
        widgets.add(
          _GroupedNotificationWidget(
            group: _NotificationGroup(
              items,
              displayTitle: '${option.label} (${items.length})',
              displayContent: items.first.content,
            ),
            isExpanded: _expandedGroups.contains(groupId),
            onToggle: () {
              setState(() {
                if (_expandedGroups.contains(groupId)) {
                  _expandedGroups.remove(groupId);
                } else {
                  _expandedGroups.add(groupId);
                }
              });
            },
            onItemTap: _handleNotificationTap,
            onItemDelete: (notification) =>
                _deleteNotification(service, notification),
            onItemDetail: _handleNotificationTap,
          ),
        );
        continue;
      }

      widgets.addAll(
        _buildGroupWidgets(
          _groupConsecutiveNotifications(items),
          service,
          option.key,
        ),
      );
    }

    return widgets;
  }

  List<Widget> _buildSingleCategorySection(
    List<NotificationItem> notifications,
    NotificationService service,
  ) {
    final selectedOption = _categoryOptions.firstWhere(
      (item) => item.key == _selectedCategory,
      orElse: () => _allCategory,
    );

    return [
      _CategoryHeader(
        categoryKey: selectedOption.key,
        label: selectedOption.label,
        unreadCount: _unreadCountForCategory(notifications, selectedOption.key),
        onMarkAllRead: () => _markCategoryAsRead(service, selectedOption.key),
      ),
      ..._buildFlatNotificationWidgets(notifications, service),
    ];
  }

  Future<void> _markCategoryAsRead(
    NotificationService service,
    String categoryKey,
  ) async {
    final updated = await service.markCategoryAsRead(categoryKey);
    if (!mounted || updated <= 0) return;

    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text('Đã đánh dấu $updated thông báo là đã đọc'),
        duration: const Duration(seconds: 1),
      ),
    );
  }

  List<Widget> _buildFlatNotificationWidgets(
    List<NotificationItem> notifications,
    NotificationService service,
  ) {
    return notifications
        .map(
          (notification) => _NotificationItemWidget(
            notification: notification,
            onTap: () => _handleNotificationTap(notification),
            onDelete: () => _deleteNotification(service, notification),
            onDetail: () => _handleNotificationTap(notification),
          ),
        )
        .toList();
  }

  List<Widget> _buildGroupWidgets(
    List<_NotificationGroup> groups,
    NotificationService service,
    String categoryKey,
  ) {
    return List<Widget>.generate(groups.length, (index) {
      final group = groups[index];
      final groupId = '$categoryKey-$index-${group.latest.id}';

      if (!group.isGrouped) {
        return _NotificationItemWidget(
          notification: group.latest,
          onTap: () => _handleNotificationTap(group.latest),
          onDelete: () => _deleteNotification(service, group.latest),
          onDetail: () => _handleNotificationTap(group.latest),
        );
      }

      return _GroupedNotificationWidget(
        group: group,
        isExpanded: _expandedGroups.contains(groupId),
        onToggle: () {
          setState(() {
            if (_expandedGroups.contains(groupId)) {
              _expandedGroups.remove(groupId);
            } else {
              _expandedGroups.add(groupId);
            }
          });
        },
        onItemTap: _handleNotificationTap,
        onItemDelete: (notification) =>
            _deleteNotification(service, notification),
        onItemDetail: _handleNotificationTap,
      );
    });
  }

  Future<void> _deleteNotification(
    NotificationService service,
    NotificationItem notification,
  ) async {
    final success = await service.deleteNotification(notification.id);
    if (mounted && success) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Đã xoá thông báo'),
          duration: Duration(seconds: 1),
        ),
      );
    }
  }

  Future<void> _handleNotificationTap(NotificationItem notification) async {
    await context.read<NotificationService>().openNotificationTarget(
      notification,
    );
  }
}

class _CategoryHeader extends StatelessWidget {
  final String categoryKey;
  final String label;
  final int unreadCount;
  final VoidCallback? onMarkAllRead;

  const _CategoryHeader({
    required this.categoryKey,
    required this.label,
    this.unreadCount = 0,
    this.onMarkAllRead,
  });

  @override
  Widget build(BuildContext context) {
    final color = _categoryColor(categoryKey);
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 18, 16, 8),
      child: Row(
        children: [
          Expanded(
            child: Row(
              children: [
                Container(
                  width: 10,
                  height: 10,
                  decoration: BoxDecoration(
                    color: color,
                    shape: BoxShape.circle,
                  ),
                ),
                const SizedBox(width: 8),
                Text(
                  label,
                  style: const TextStyle(
                    fontSize: 15,
                    fontWeight: FontWeight.w700,
                    color: Color(0xFF111827),
                  ),
                ),
              ],
            ),
          ),
          if (unreadCount > 0 && onMarkAllRead != null)
            TextButton(
              onPressed: onMarkAllRead,
              style: TextButton.styleFrom(
                foregroundColor: AppColors.brandColor,
                padding: const EdgeInsets.symmetric(
                  horizontal: 10,
                  vertical: 6,
                ),
                minimumSize: Size.zero,
                tapTargetSize: MaterialTapTargetSize.shrinkWrap,
              ),
              child: const Text(
                'Đọc tất cả',
                style: TextStyle(fontWeight: FontWeight.w700, fontSize: 13),
              ),
            ),
        ],
      ),
    );
  }
}

class _GroupedNotificationWidget extends StatelessWidget {
  final _NotificationGroup group;
  final bool isExpanded;
  final VoidCallback onToggle;
  final void Function(NotificationItem) onItemTap;
  final void Function(NotificationItem) onItemDelete;
  final void Function(NotificationItem) onItemDetail;

  const _GroupedNotificationWidget({
    required this.group,
    required this.isExpanded,
    required this.onToggle,
    required this.onItemTap,
    required this.onItemDelete,
    required this.onItemDetail,
  });

  @override
  Widget build(BuildContext context) {
    final categoryColor = _categoryColor(group.latest.category);

    return Column(
      children: [
        InkWell(
          onTap: onToggle,
          child: Container(
            decoration: BoxDecoration(
              color: group.hasUnread
                  ? categoryColor.withValues(alpha: 0.1)
                  : Colors.white,
              border: Border(
                left: BorderSide(
                  color: group.hasUnread ? categoryColor : Colors.transparent,
                  width: 4,
                ),
              ),
            ),
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
            child: Row(
              crossAxisAlignment: CrossAxisAlignment.center,
              children: [
                _NotificationLeadingIcon(notification: group.latest),
                const SizedBox(width: 12),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Row(
                        children: [
                          Expanded(
                            child: Text(
                              group.title,
                              style: TextStyle(
                                fontWeight: group.hasUnread
                                    ? FontWeight.bold
                                    : FontWeight.w600,
                                fontSize: 15,
                                color: const Color(0xFF111827),
                              ),
                            ),
                          ),
                          if (group.hasUnread)
                            Container(
                              width: 8,
                              height: 8,
                              decoration: const BoxDecoration(
                                color: AppColors.brandColor,
                                shape: BoxShape.circle,
                              ),
                            ),
                        ],
                      ),
                      const SizedBox(height: 4),
                      Text(
                        group.content,
                        style: const TextStyle(
                          fontSize: 13,
                          color: Color(0xFF6B7280),
                        ),
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                      ),
                      const SizedBox(height: 6),
                      Row(
                        children: [
                          _CategoryPill(notification: group.latest),
                          if (group.hasUnread) ...[
                            const SizedBox(width: 8),
                            const _UnreadPill(),
                          ],
                          const SizedBox(width: 8),
                          Text(
                            _formatTime(group.latest.createdAt),
                            style: const TextStyle(
                              fontSize: 12,
                              color: Color(0xFF9CA3AF),
                            ),
                          ),
                        ],
                      ),
                    ],
                  ),
                ),
                Icon(
                  isExpanded
                      ? Icons.keyboard_arrow_up_rounded
                      : Icons.keyboard_arrow_down_rounded,
                  color: const Color(0xFF9CA3AF),
                ),
              ],
            ),
          ),
        ),
        if (isExpanded)
          ...group.items.map(
            (notification) => Container(
              decoration: BoxDecoration(
                color: notification.isRead
                    ? const Color(0xFFFAFAFA)
                    : categoryColor.withValues(alpha: 0.1),
                border: Border(
                  left: BorderSide(
                    color: notification.isRead
                        ? Colors.transparent
                        : categoryColor,
                    width: 4,
                  ),
                ),
              ),
              child: InkWell(
                onTap: () => onItemTap(notification),
                child: Padding(
                  padding: const EdgeInsets.only(
                    left: 72,
                    right: 4,
                    top: 8,
                    bottom: 8,
                  ),
                  child: Row(
                    children: [
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              notification.content,
                              style: const TextStyle(
                                fontSize: 13,
                                color: Color(0xFF4B5563),
                              ),
                            ),
                            const SizedBox(height: 4),
                            Row(
                              children: [
                                if (!notification.isRead) ...[
                                  const _UnreadPill(
                                    fontSize: 10,
                                    horizontalPadding: 6,
                                    verticalPadding: 2,
                                  ),
                                  const SizedBox(width: 6),
                                ],
                                Text(
                                  _formatTime(notification.createdAt),
                                  style: const TextStyle(
                                    fontSize: 11,
                                    color: Color(0xFF9CA3AF),
                                  ),
                                ),
                              ],
                            ),
                          ],
                        ),
                      ),
                      _buildPopupMenu(notification, onItemDetail, onItemDelete),
                    ],
                  ),
                ),
              ),
            ),
          ),
      ],
    );
  }
}

class _NotificationItemWidget extends StatelessWidget {
  final NotificationItem notification;
  final VoidCallback onTap;
  final VoidCallback onDelete;
  final VoidCallback onDetail;

  const _NotificationItemWidget({
    required this.notification,
    required this.onTap,
    required this.onDelete,
    required this.onDetail,
  });

  @override
  Widget build(BuildContext context) {
    final categoryColor = _categoryColor(notification.category);

    return InkWell(
      onTap: onTap,
      child: Container(
        decoration: BoxDecoration(
          color: notification.isRead
              ? Colors.white
              : categoryColor.withValues(alpha: 0.12),
          border: Border(
            left: BorderSide(
              color: notification.isRead ? Colors.transparent : categoryColor,
              width: 4,
            ),
          ),
        ),
        padding: const EdgeInsets.only(left: 16, top: 12, bottom: 12, right: 4),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            _NotificationLeadingIcon(notification: notification),
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      Expanded(
                        child: Text(
                          notification.title,
                          style: TextStyle(
                            fontWeight: notification.isRead
                                ? FontWeight.w600
                                : FontWeight.bold,
                            fontSize: 15,
                            color: const Color(0xFF111827),
                          ),
                        ),
                      ),
                      if (!notification.isRead)
                        Container(
                          width: 8,
                          height: 8,
                          decoration: const BoxDecoration(
                            color: AppColors.brandColor,
                            shape: BoxShape.circle,
                          ),
                        ),
                    ],
                  ),
                  const SizedBox(height: 4),
                  Text(
                    notification.content,
                    style: const TextStyle(
                      fontSize: 13,
                      color: Color(0xFF6B7280),
                    ),
                    maxLines: 2,
                    overflow: TextOverflow.ellipsis,
                  ),
                  const SizedBox(height: 6),
                  Row(
                    children: [
                      _CategoryPill(notification: notification),
                      if (!notification.isRead) ...[
                        const SizedBox(width: 8),
                        const _UnreadPill(),
                      ],
                      const SizedBox(width: 8),
                      Text(
                        _formatTime(notification.createdAt),
                        style: const TextStyle(
                          fontSize: 12,
                          color: Color(0xFF9CA3AF),
                        ),
                      ),
                    ],
                  ),
                ],
              ),
            ),
            _buildPopupMenu(notification, (_) => onDetail(), (_) => onDelete()),
          ],
        ),
      ),
    );
  }
}

class _UnreadPill extends StatelessWidget {
  final double fontSize;
  final double horizontalPadding;
  final double verticalPadding;

  const _UnreadPill({
    this.fontSize = 11,
    this.horizontalPadding = 8,
    this.verticalPadding = 3,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: EdgeInsets.symmetric(
        horizontal: horizontalPadding,
        vertical: verticalPadding,
      ),
      decoration: BoxDecoration(
        color: AppColors.brandColor.withValues(alpha: 0.12),
        borderRadius: BorderRadius.circular(999),
      ),
      child: Text(
        'Chưa đọc',
        style: TextStyle(
          color: AppColors.brandColor,
          fontSize: fontSize,
          fontWeight: FontWeight.w700,
        ),
      ),
    );
  }
}

class _NotificationLeadingIcon extends StatelessWidget {
  final NotificationItem notification;

  const _NotificationLeadingIcon({required this.notification});

  @override
  Widget build(BuildContext context) {
    final color = _categoryColor(notification.category);
    return Container(
      width: 44,
      height: 44,
      decoration: BoxDecoration(
        color: color.withValues(alpha: 0.12),
        shape: BoxShape.circle,
      ),
      child: Icon(_iconForNotification(notification), color: color, size: 22),
    );
  }
}

class _CategoryPill extends StatelessWidget {
  final NotificationItem notification;

  const _CategoryPill({required this.notification});

  @override
  Widget build(BuildContext context) {
    final color = _categoryColor(notification.category);
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
      decoration: BoxDecoration(
        color: color.withValues(alpha: 0.12),
        borderRadius: BorderRadius.circular(999),
      ),
      child: Text(
        notification.categoryLabel,
        style: TextStyle(
          color: color,
          fontSize: 11,
          fontWeight: FontWeight.w700,
        ),
      ),
    );
  }
}

Widget _buildPopupMenu(
  NotificationItem notification,
  void Function(NotificationItem) onDetail,
  void Function(NotificationItem) onDelete,
) {
  return PopupMenuButton<String>(
    icon: const Icon(Icons.more_vert, color: Color(0xFF9CA3AF), size: 20),
    padding: EdgeInsets.zero,
    constraints: const BoxConstraints(),
    shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
    onSelected: (value) {
      if (value == 'detail') {
        onDetail(notification);
      } else if (value == 'delete') {
        onDelete(notification);
      }
    },
    itemBuilder: (context) {
      final items = <PopupMenuEntry<String>>[];
      if (_canOpenDetail(notification)) {
        items.add(
          const PopupMenuItem<String>(
            value: 'detail',
            child: Row(
              children: [
                Icon(Icons.open_in_new, size: 18),
                SizedBox(width: 8),
                Text('Chi tiết'),
              ],
            ),
          ),
        );
      }

      items.add(
        const PopupMenuItem<String>(
          value: 'delete',
          child: Row(
            children: [
              Icon(Icons.delete_outline, size: 18, color: Colors.red),
              SizedBox(width: 8),
              Text('Xoá', style: TextStyle(color: Colors.red)),
            ],
          ),
        ),
      );

      return items;
    },
  );
}

bool _canOpenDetail(NotificationItem notification) {
  return const {
    'BOOKING',
    'REPUTATION',
    'PROCESSING',
    'NEWS',
  }.contains(notification.category);
}

IconData _iconForNotification(NotificationItem notification) {
  switch (notification.category) {
    case 'MESSAGE':
      return Icons.chat_bubble_rounded;
    case 'PROCESSING':
      return notification.content.toLowerCase().contains('vi phạm')
          ? Icons.warning_rounded
          : Icons.rule_folder_rounded;
    case 'REPUTATION':
      return Icons.workspace_premium_rounded;
    case 'BOOKING':
      return notification.type == 'REMINDER'
          ? Icons.alarm_rounded
          : Icons.event_seat_rounded;
    case 'NEWS':
      return Icons.article_rounded;
    default:
      return Icons.notifications_rounded;
  }
}

Color _categoryColor(String category) {
  switch (category) {
    case 'MESSAGE':
      return Colors.teal;
    case 'PROCESSING':
      return Colors.deepOrange;
    case 'REPUTATION':
      return Colors.indigo;
    case 'BOOKING':
      return Colors.blue;
    case 'NEWS':
      return Colors.green;
    default:
      return AppColors.brandColor;
  }
}

String _formatTime(DateTime dateTime) {
  final now = DateTime.now();
  final difference = now.difference(dateTime);

  if (difference.isNegative || difference.inSeconds < 60) {
    return 'Vừa xong';
  }
  if (difference.inMinutes < 60) {
    return '${difference.inMinutes} phút trước';
  }
  if (difference.inHours < 24) {
    return '${difference.inHours} giờ trước';
  }
  if (difference.inDays < 7) {
    return '${difference.inDays} ngày trước';
  }
  return DateFormat('dd/MM/yyyy').format(dateTime);
}
