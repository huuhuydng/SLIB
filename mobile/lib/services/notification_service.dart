import 'dart:async';
import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:http/http.dart' as http;
import 'package:firebase_messaging/firebase_messaging.dart';
import 'package:flutter_local_notifications/flutter_local_notifications.dart';
import 'package:slib/core/constants/api_constants.dart';
import 'package:slib/services/auth_service.dart';

/// Model for notification
class NotificationItem {
  final String id;
  final String title;
  final String content;
  final String type;
  final String? referenceType;
  final String? referenceId;
  final bool isRead;
  final DateTime createdAt;

  NotificationItem({
    required this.id,
    required this.title,
    required this.content,
    required this.type,
    this.referenceType,
    this.referenceId,
    required this.isRead,
    required this.createdAt,
  });

  factory NotificationItem.fromJson(Map<String, dynamic> json) {
    // Server returns UTC time, need to parse properly
    DateTime createdAt = DateTime.now();
    if (json['createdAt'] != null) {
      // Parse as UTC (server time is UTC)
      final parsed = DateTime.parse(json['createdAt']);
      // If no timezone info in string, assume it's UTC
      createdAt = parsed.isUtc ? parsed : DateTime.utc(
        parsed.year, parsed.month, parsed.day,
        parsed.hour, parsed.minute, parsed.second, parsed.millisecond
      );
    }
    
    return NotificationItem(
      id: json['id'] ?? '',
      title: json['title'] ?? '',
      content: json['content'] ?? '',
      type: json['notificationType'] ?? json['type'] ?? 'SYSTEM',
      referenceType: json['referenceType'],
      referenceId: json['referenceId'],
      isRead: json['isRead'] ?? json['read'] ?? false,
      createdAt: createdAt,
    );
  }
}

/// Notification settings model
class NotificationSettings {
  final bool notifyBooking;
  final bool notifyReminder;
  final bool notifyNews;

  NotificationSettings({
    this.notifyBooking = true,
    this.notifyReminder = true,
    this.notifyNews = true,
  });

  factory NotificationSettings.fromJson(Map<String, dynamic> json) {
    return NotificationSettings(
      notifyBooking: json['notifyBooking'] ?? true,
      notifyReminder: json['notifyReminder'] ?? true,
      notifyNews: json['notifyNews'] ?? true,
    );
  }

  Map<String, dynamic> toJson() => {
        'notifyBooking': notifyBooking,
        'notifyReminder': notifyReminder,
        'notifyNews': notifyNews,
      };

  NotificationSettings copyWith({
    bool? notifyBooking,
    bool? notifyReminder,
    bool? notifyNews,
  }) {
    return NotificationSettings(
      notifyBooking: notifyBooking ?? this.notifyBooking,
      notifyReminder: notifyReminder ?? this.notifyReminder,
      notifyNews: notifyNews ?? this.notifyNews,
    );
  }
}

/// Service to handle push notifications with real-time updates
class NotificationService extends ChangeNotifier with WidgetsBindingObserver {
  final AuthService _authService;
  
  // Local notifications plugin for foreground notifications
  final FlutterLocalNotificationsPlugin _localNotifications = FlutterLocalNotificationsPlugin();

  // Periodic refresh timer - refresh every 5 seconds for near real-time updates
  Timer? _refreshTimer;
  static const _refreshInterval = Duration(seconds: 5);

  List<NotificationItem> _notifications = [];
  int _unreadCount = 0;
  NotificationSettings _settings = NotificationSettings();
  bool _isLoading = false;
  bool _isInitialized = false;

  List<NotificationItem> get notifications => _notifications;
  int get unreadCount => _unreadCount;
  NotificationSettings get settings => _settings;
  bool get isLoading => _isLoading;

  NotificationService(this._authService);

  String get _baseUrl => '${ApiConstants.domain}/slib';

  Future<String?> get _token async => await _authService.getToken();

  String? get _userId => _authService.currentUser?.id;

  /// Initialize notification service
  Future<void> initialize() async {
    if (_isInitialized) return;
    
    // Register lifecycle observer
    WidgetsBinding.instance.addObserver(this);
    
    // Initialize local notifications
    await _initializeLocalNotifications();
    
    // Request permission for notifications
    await _requestPermission();

    // Configure Firebase Messaging
    await _configureFirebaseMessaging();

    // Sync FCM token to server (important for receiving notifications)
    if (_userId != null) {
      await _authService.syncFcmToken(_userId!);
    }

    // Load initial data if user is logged in
    if (_userId != null) {
      await refreshData();
    }
    
    // Start periodic refresh
    _startPeriodicRefresh();
    
    _isInitialized = true;
  }

  /// Start periodic refresh of unread count
  void _startPeriodicRefresh() {
    _refreshTimer?.cancel();
    _refreshTimer = Timer.periodic(_refreshInterval, (_) {
      if (_userId != null) {
        refreshUnreadCount();
      }
    });
  }

  /// Stop periodic refresh
  void _stopPeriodicRefresh() {
    _refreshTimer?.cancel();
    _refreshTimer = null;
  }

  /// Handle app lifecycle changes
  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    switch (state) {
      case AppLifecycleState.resumed:
        // App came to foreground - refresh data
        if (_userId != null) {
          refreshUnreadCount();
        }
        _startPeriodicRefresh();
        break;
      case AppLifecycleState.paused:
      case AppLifecycleState.inactive:
      case AppLifecycleState.detached:
      case AppLifecycleState.hidden:
        // App went to background - stop timer
        _stopPeriodicRefresh();
        break;
    }
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    _stopPeriodicRefresh();
    super.dispose();
  }

  Future<void> _initializeLocalNotifications() async {
    // Android initialization
    const androidSettings = AndroidInitializationSettings('@mipmap/ic_launcher');
    
    // iOS initialization
    const iosSettings = DarwinInitializationSettings(
      requestAlertPermission: true,
      requestBadgePermission: true,
      requestSoundPermission: true,
    );
    
    const initSettings = InitializationSettings(
      android: androidSettings,
      iOS: iosSettings,
    );
    
    await _localNotifications.initialize(
      initSettings,
      onDidReceiveNotificationResponse: (NotificationResponse response) {
        debugPrint('Local notification tapped: ${response.payload}');
        // Handle notification tap
      },
    );
    
    // Create notification channel for Android
    const androidChannel = AndroidNotificationChannel(
      'slib_notifications',
      'SLIB',
      description: 'Thông báo từ thư viện SLIB',
      importance: Importance.high,
    );
    
    await _localNotifications
        .resolvePlatformSpecificImplementation<AndroidFlutterLocalNotificationsPlugin>()
        ?.createNotificationChannel(androidChannel);
  }

  Future<void> _requestPermission() async {
    try {
      final messaging = FirebaseMessaging.instance;
      final settings = await messaging.requestPermission(
        alert: true,
        badge: true,
        sound: true,
        announcement: false,
        carPlay: false,
        criticalAlert: false,
        provisional: false,
      );
      debugPrint('Notification permission: ${settings.authorizationStatus}');
    } catch (e) {
      debugPrint('Error requesting notification permission: $e');
    }
  }

  Future<void> _configureFirebaseMessaging() async {
    // Handle foreground messages - show local notification and update count
    FirebaseMessaging.onMessage.listen((RemoteMessage message) {
      _handleIncomingNotification(message);
      _showLocalNotification(message);
    });

    // Handle background/terminated message opens
    FirebaseMessaging.onMessageOpenedApp.listen((RemoteMessage message) {
      debugPrint('Message opened app: ${message.notification?.title}');
      _handleNotificationTap(message);
    });

    // Check if app was opened from a notification
    final initialMessage = await FirebaseMessaging.instance.getInitialMessage();
    if (initialMessage != null) {
      _handleNotificationTap(initialMessage);
    }
  }

  /// Show local notification when app is in foreground
  Future<void> _showLocalNotification(RemoteMessage message) async {
    final notification = message.notification;
    if (notification == null) return;

    const androidDetails = AndroidNotificationDetails(
      'slib_notifications',
      'SLIB',
      channelDescription: 'Thông báo từ thư viện SLIB',
      importance: Importance.high,
      priority: Priority.high,
      showWhen: true,
      icon: '@mipmap/ic_launcher',
    );
    
    const iosDetails = DarwinNotificationDetails(
      presentAlert: true,
      presentBadge: true,
      presentSound: true,
    );
    
    const details = NotificationDetails(
      android: androidDetails,
      iOS: iosDetails,
    );
    
    await _localNotifications.show(
      DateTime.now().millisecondsSinceEpoch ~/ 1000,
      notification.title ?? 'Thông báo',
      notification.body ?? '',
      details,
      payload: message.data['notificationId'],
    );
  }

  void _handleIncomingNotification(RemoteMessage message) {
    // Add to local list for immediate display
    final notification = NotificationItem(
      id: message.data['notificationId'] ?? DateTime.now().millisecondsSinceEpoch.toString(),
      title: message.notification?.title ?? 'Thông báo',
      content: message.notification?.body ?? '',
      type: message.data['type'] ?? 'SYSTEM',
      referenceType: message.data['referenceType'],
      referenceId: message.data['referenceId'],
      isRead: false,
      createdAt: DateTime.now(),
    );

    _notifications.insert(0, notification);
    notifyListeners();
    
    // Refresh unread count from server (source of truth)
    refreshUnreadCount();
  }

  void _handleNotificationTap(RemoteMessage message) {
    // Refresh data when notification is tapped
    refreshData();
    
    // Navigate based on notification type
    final type = message.data['type'];
    final referenceId = message.data['referenceId'];

    debugPrint('Notification tapped - type: $type, referenceId: $referenceId');
    // Navigation logic can be added here based on type
  }

  /// Refresh all notification data
  Future<void> refreshData() async {
    if (_userId == null) return;

    _isLoading = true;
    notifyListeners();

    await Future.wait([
      fetchNotifications(),
      refreshUnreadCount(),
      fetchSettings(),
    ]);

    _isLoading = false;
    notifyListeners();
  }

  /// Fetch notifications from server
  Future<void> fetchNotifications({int limit = 50}) async {
    if (_userId == null) return;

    try {
      final token = await _token;
      if (token == null) return;

      final response = await http.get(
        Uri.parse('$_baseUrl/notifications/user/$_userId?limit=$limit'),
        headers: {
          'Content-Type': 'application/json',
          'Authorization': 'Bearer $token',
        },
      );

      if (response.statusCode == 200) {
        final List<dynamic> jsonList = jsonDecode(utf8.decode(response.bodyBytes));
        _notifications = jsonList.map((e) => NotificationItem.fromJson(e)).toList();
        notifyListeners();
      }
    } catch (e) {
      debugPrint('Error fetching notifications: $e');
    }
  }

  /// Refresh unread count
  Future<void> refreshUnreadCount() async {
    if (_userId == null) return;

    try {
      final token = await _token;
      if (token == null) return;

      final response = await http.get(
        Uri.parse('$_baseUrl/notifications/unread-count/$_userId'),
        headers: {
          'Content-Type': 'application/json',
          'Authorization': 'Bearer $token',
        },
      );

      if (response.statusCode == 200) {
        final data = jsonDecode(utf8.decode(response.bodyBytes));
        final newCount = (data['count'] as num?)?.toInt() ?? 0;
        
        // Always update from server (source of truth)
        _unreadCount = newCount;
        notifyListeners();
      }
    } catch (e) {
      debugPrint('Error fetching unread count: $e');
    }
  }

  /// Mark notification as read
  Future<void> markAsRead(String notificationId) async {
    try {
      final token = await _token;
      if (token == null) return;

      final response = await http.put(
        Uri.parse('$_baseUrl/notifications/mark-read/$notificationId'),
        headers: {
          'Content-Type': 'application/json',
          'Authorization': 'Bearer $token',
        },
      );

      if (response.statusCode == 200) {
        // Update local state
        final index = _notifications.indexWhere((n) => n.id == notificationId);
        if (index != -1 && !_notifications[index].isRead) {
          _notifications[index] = NotificationItem(
            id: _notifications[index].id,
            title: _notifications[index].title,
            content: _notifications[index].content,
            type: _notifications[index].type,
            referenceType: _notifications[index].referenceType,
            referenceId: _notifications[index].referenceId,
            isRead: true,
            createdAt: _notifications[index].createdAt,
          );
          _unreadCount = (_unreadCount - 1).clamp(0, _unreadCount);
          notifyListeners();
        }
      }
    } catch (e) {
      debugPrint('Error marking notification as read: $e');
    }
  }

  /// Mark all notifications as read
  Future<void> markAllAsRead() async {
    if (_userId == null) return;

    try {
      final token = await _token;
      if (token == null) return;

      final response = await http.put(
        Uri.parse('$_baseUrl/notifications/mark-all-read/$_userId'),
        headers: {
          'Content-Type': 'application/json',
          'Authorization': 'Bearer $token',
        },
      );

      if (response.statusCode == 200) {
        // Update local state
        _notifications = _notifications.map((n) => NotificationItem(
          id: n.id,
          title: n.title,
          content: n.content,
          type: n.type,
          referenceType: n.referenceType,
          referenceId: n.referenceId,
          isRead: true,
          createdAt: n.createdAt,
        )).toList();
        _unreadCount = 0;
        notifyListeners();
      }
    } catch (e) {
      debugPrint('Error marking all as read: $e');
    }
  }

  /// Fetch notification settings
  Future<void> fetchSettings() async {
    if (_userId == null) return;

    try {
      final token = await _token;
      if (token == null) return;

      final response = await http.get(
        Uri.parse('$_baseUrl/notifications/settings/$_userId'),
        headers: {
          'Content-Type': 'application/json',
          'Authorization': 'Bearer $token',
        },
      );

      if (response.statusCode == 200) {
        final data = jsonDecode(utf8.decode(response.bodyBytes));
        _settings = NotificationSettings.fromJson(data);
        notifyListeners();
      }
    } catch (e) {
      debugPrint('Error fetching settings: $e');
    }
  }

  /// Update notification settings
  Future<bool> updateSettings(NotificationSettings newSettings) async {
    if (_userId == null) return false;

    try {
      final token = await _token;
      if (token == null) return false;

      final response = await http.put(
        Uri.parse('$_baseUrl/notifications/settings/$_userId'),
        headers: {
          'Content-Type': 'application/json',
          'Authorization': 'Bearer $token',
        },
        body: jsonEncode(newSettings.toJson()),
      );

      if (response.statusCode == 200) {
        _settings = newSettings;
        notifyListeners();
        return true;
      }
      return false;
    } catch (e) {
      debugPrint('Error updating settings: $e');
      return false;
    }
  }

  /// Clear all data on logout
  void clearData() {
    _stopPeriodicRefresh();
    _notifications = [];
    _unreadCount = 0;
    _settings = NotificationSettings();
    _isInitialized = false;
    notifyListeners();
  }
}
