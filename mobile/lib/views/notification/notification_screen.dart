import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:slib/assets/colors.dart';
import 'package:slib/services/notification/notification_service.dart';
import 'package:slib/views/profile/booking_history_screen.dart';
import 'package:slib/views/profile/violation_history_screen.dart';
import 'package:intl/intl.dart';
import 'package:slib/views/widgets/error_display_widget.dart';

/// Model gộp nhóm các notification liên tiếp cùng type + title
class _NotificationGroup {
  final List<NotificationItem> items;

  _NotificationGroup(this.items);

  String get type => items.first.type;
  String get title => items.first.title;
  bool get isGrouped => items.length > 1;
  int get count => items.length;
  NotificationItem get latest => items.first;
  bool get hasUnread => items.any((n) => !n.isRead);
}

/// Gộp notification liên tiếp cùng type + title thành group
List<_NotificationGroup> _groupConsecutiveNotifications(List<NotificationItem> notifications) {
  if (notifications.isEmpty) return [];

  final groups = <_NotificationGroup>[];
  List<NotificationItem> currentGroup = [notifications.first];

  for (int i = 1; i < notifications.length; i++) {
    final current = notifications[i];
    final previous = notifications[i - 1];

    if (current.type == previous.type && current.title == previous.title) {
      currentGroup.add(current);
    } else {
      groups.add(_NotificationGroup(currentGroup));
      currentGroup = [current];
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
  final Set<int> _expandedGroups = {};

  @override
  void initState() {
    super.initState();
    // Refresh notifications when screen opens
    WidgetsBinding.instance.addPostFrameCallback((_) {
      context.read<NotificationService>().refreshData();
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.white,
      appBar: AppBar(
        title: const Text(
          'Thông báo',
          style: TextStyle(
            fontWeight: FontWeight.bold,
            color: Colors.white,
          ),
        ),
        backgroundColor: AppColors.brandColor,
        foregroundColor: Colors.white,
        elevation: 0,
        actions: [
          Consumer<NotificationService>(
            builder: (context, service, _) {
              if (service.unreadCount > 0) {
                return TextButton(
                  onPressed: () => service.markAllAsRead(),
                  child: const Text(
                    'Đọc tất cả',
                    style: TextStyle(color: Colors.white),
                  ),
                );
              }
              return const SizedBox.shrink();
            },
          ),
        ],
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

          final groups = _groupConsecutiveNotifications(service.notifications);

          return RefreshIndicator(
            onRefresh: () async {
              _expandedGroups.clear();
              await service.refreshData();
            },
            color: AppColors.brandColor,
            child: ListView.separated(
              padding: const EdgeInsets.symmetric(vertical: 8),
              itemCount: groups.length,
              separatorBuilder: (context, index) => Divider(
                height: 1,
                thickness: 0.5,
                color: Colors.grey[200],
                indent: 72,
                endIndent: 16,
              ),
              itemBuilder: (context, index) {
                final group = groups[index];

                if (!group.isGrouped) {
                  // Notification đơn lẻ — render như cũ
                  return _NotificationItemWidget(
                    notification: group.latest,
                    onTap: () {
                      if (!group.latest.isRead) {
                        service.markAsRead(group.latest.id);
                      }
                      _handleNotificationTap(group.latest);
                    },
                  );
                }

                // Nhóm nhiều notification cùng loại
                final isExpanded = _expandedGroups.contains(index);
                return _GroupedNotificationWidget(
                  group: group,
                  isExpanded: isExpanded,
                  onToggle: () {
                    setState(() {
                      if (isExpanded) {
                        _expandedGroups.remove(index);
                      } else {
                        _expandedGroups.add(index);
                      }
                    });
                  },
                  onItemTap: (notification) {
                    if (!notification.isRead) {
                      service.markAsRead(notification.id);
                    }
                    _handleNotificationTap(notification);
                  },
                );
              },
            ),
          );
        },
      ),
    );
  }

  void _handleNotificationTap(NotificationItem notification) {
    switch (notification.type) {
      case 'BOOKING':
        Navigator.push(
          context,
          MaterialPageRoute(builder: (_) => const BookingHistoryScreen()),
        );
        break;
      case 'VIOLATION':
        Navigator.push(
          context,
          MaterialPageRoute(builder: (_) => const ViolationHistoryScreen()),
        );
        break;
      case 'NEWS':
        debugPrint('Navigate to news: ${notification.referenceId}');
        break;
      default:
        break;
    }
  }
}

/// Widget hiển thị nhóm notification gộp
class _GroupedNotificationWidget extends StatelessWidget {
  final _NotificationGroup group;
  final bool isExpanded;
  final VoidCallback onToggle;
  final void Function(NotificationItem) onItemTap;

  const _GroupedNotificationWidget({
    required this.group,
    required this.isExpanded,
    required this.onToggle,
    required this.onItemTap,
  });

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        // Header row — tap để expand/collapse
        InkWell(
          onTap: onToggle,
          child: Container(
            color: group.hasUnread ? Colors.orange.shade50 : Colors.white,
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
            child: Row(
              crossAxisAlignment: CrossAxisAlignment.center,
              children: [
                // Icon
                _buildIcon(group.type),
                const SizedBox(width: 12),
                // Content
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Row(
                        children: [
                          Expanded(
                            child: Text(
                              '${group.title} (${group.count})',
                              style: TextStyle(
                                fontWeight: group.hasUnread
                                    ? FontWeight.bold
                                    : FontWeight.w500,
                                fontSize: 15,
                                color: Colors.black87,
                              ),
                            ),
                          ),
                          if (group.hasUnread)
                            Container(
                              width: 8,
                              height: 8,
                              decoration: BoxDecoration(
                                color: AppColors.brandColor,
                                shape: BoxShape.circle,
                              ),
                            ),
                        ],
                      ),
                      const SizedBox(height: 4),
                      Text(
                        group.latest.content,
                        style: TextStyle(fontSize: 13, color: Colors.grey[600]),
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                      ),
                      const SizedBox(height: 4),
                      Text(
                        _formatTime(group.latest.createdAt),
                        style: TextStyle(fontSize: 12, color: Colors.grey[400]),
                      ),
                    ],
                  ),
                ),
                const SizedBox(width: 4),
                Icon(
                  isExpanded
                      ? Icons.keyboard_arrow_up_rounded
                      : Icons.keyboard_arrow_down_rounded,
                  color: Colors.grey[400],
                  size: 24,
                ),
              ],
            ),
          ),
        ),
        // Expanded children
        if (isExpanded)
          ...group.items.map((notification) => InkWell(
                onTap: () => onItemTap(notification),
                child: Container(
                  width: double.infinity,
                  decoration: BoxDecoration(
                    color: notification.isRead
                        ? Colors.grey.shade50
                        : Colors.orange.shade50,
                    border: Border(
                      left: BorderSide(
                        color: AppColors.brandColor.withOpacity(0.4),
                        width: 3,
                      ),
                    ),
                  ),
                  padding: const EdgeInsets.only(
                      left: 72, right: 16, top: 10, bottom: 10),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        notification.content,
                        style: TextStyle(
                          fontSize: 13,
                          color: Colors.grey[700],
                        ),
                        maxLines: 2,
                        overflow: TextOverflow.ellipsis,
                      ),
                      const SizedBox(height: 2),
                      Text(
                        _formatTime(notification.createdAt),
                        style: TextStyle(
                          fontSize: 11,
                          color: Colors.grey[400],
                        ),
                      ),
                    ],
                  ),
                ),
              )),
      ],
    );
  }

  Widget _buildIcon(String type) {
    final iconColor = _getColorForType(type);
    return Container(
      width: 44,
      height: 44,
      decoration: BoxDecoration(
        color: iconColor.withOpacity(0.1),
        shape: BoxShape.circle,
      ),
      child: Icon(
        _getIconForType(type),
        color: iconColor,
        size: 22,
      ),
    );
  }

  static IconData _getIconForType(String type) {
    switch (type) {
      case 'BOOKING':
        return Icons.event_seat_rounded;
      case 'REMINDER':
        return Icons.alarm_rounded;
      case 'NEWS':
        return Icons.article_rounded;
      case 'VIOLATION':
        return Icons.warning_rounded;
      case 'CHAT_MESSAGE':
        return Icons.chat_bubble_rounded;
      default:
        return Icons.notifications_rounded;
    }
  }

  static Color _getColorForType(String type) {
    switch (type) {
      case 'BOOKING':
        return Colors.blue;
      case 'REMINDER':
        return Colors.orange;
      case 'NEWS':
        return Colors.green;
      case 'VIOLATION':
        return Colors.red;
      case 'CHAT_MESSAGE':
        return Colors.teal;
      default:
        return AppColors.brandColor;
    }
  }

  static String _formatTime(DateTime dateTime) {
    final localDateTime = dateTime.toLocal();
    final now = DateTime.now();
    final difference = now.difference(localDateTime);

    if (difference.inSeconds < 60) {
      return 'Vừa xong';
    } else if (difference.inMinutes < 60) {
      return '${difference.inMinutes} phút trước';
    } else if (difference.inHours < 24) {
      return '${difference.inHours} giờ trước';
    } else if (difference.inDays < 7) {
      return '${difference.inDays} ngày trước';
    } else {
      return DateFormat('dd/MM/yyyy').format(localDateTime);
    }
  }
}

/// Widget hiển thị 1 notification đơn lẻ
class _NotificationItemWidget extends StatelessWidget {
  final NotificationItem notification;
  final VoidCallback onTap;

  const _NotificationItemWidget({
    required this.notification,
    required this.onTap,
  });

  IconData _getIconForType(String type) {
    switch (type) {
      case 'BOOKING':
        return Icons.event_seat_rounded;
      case 'REMINDER':
        return Icons.alarm_rounded;
      case 'NEWS':
        return Icons.article_rounded;
      case 'VIOLATION':
        return Icons.warning_rounded;
      case 'CHAT_MESSAGE':
        return Icons.chat_bubble_rounded;
      default:
        return Icons.notifications_rounded;
    }
  }

  Color _getColorForType(String type) {
    switch (type) {
      case 'BOOKING':
        return Colors.blue;
      case 'REMINDER':
        return Colors.orange;
      case 'NEWS':
        return Colors.green;
      case 'VIOLATION':
        return Colors.red;
      case 'CHAT_MESSAGE':
        return Colors.teal;
      default:
        return AppColors.brandColor;
    }
  }

  String _formatTime(DateTime dateTime) {
    final localDateTime = dateTime.toLocal();
    final now = DateTime.now();
    final difference = now.difference(localDateTime);

    if (difference.inSeconds < 60) {
      return 'Vừa xong';
    } else if (difference.inMinutes < 60) {
      return '${difference.inMinutes} phút trước';
    } else if (difference.inHours < 24) {
      return '${difference.inHours} giờ trước';
    } else if (difference.inDays < 7) {
      return '${difference.inDays} ngày trước';
    } else {
      return DateFormat('dd/MM/yyyy').format(localDateTime);
    }
  }

  @override
  Widget build(BuildContext context) {
    final iconColor = _getColorForType(notification.type);

    return InkWell(
      onTap: onTap,
      child: Container(
        color: notification.isRead ? Colors.white : Colors.orange.shade50,
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Icon
            Container(
              width: 44,
              height: 44,
              decoration: BoxDecoration(
                color: iconColor.withOpacity(0.1),
                shape: BoxShape.circle,
              ),
              child: Icon(
                _getIconForType(notification.type),
                color: iconColor,
                size: 22,
              ),
            ),
            const SizedBox(width: 12),
            // Content
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
                                ? FontWeight.w500
                                : FontWeight.bold,
                            fontSize: 15,
                            color: Colors.black87,
                          ),
                        ),
                      ),
                      if (!notification.isRead)
                        Container(
                          width: 8,
                          height: 8,
                          decoration: BoxDecoration(
                            color: AppColors.brandColor,
                            shape: BoxShape.circle,
                          ),
                        ),
                    ],
                  ),
                  const SizedBox(height: 4),
                  Text(
                    notification.content,
                    style: TextStyle(
                      fontSize: 13,
                      color: Colors.grey[600],
                    ),
                    maxLines: 2,
                    overflow: TextOverflow.ellipsis,
                  ),
                  const SizedBox(height: 4),
                  Text(
                    _formatTime(notification.createdAt),
                    style: TextStyle(
                      fontSize: 12,
                      color: Colors.grey[400],
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}
