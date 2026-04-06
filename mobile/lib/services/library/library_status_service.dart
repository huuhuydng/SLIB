import 'dart:async';
import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:stomp_dart_client/stomp_dart_client.dart';
import 'package:slib/core/constants/api_constants.dart';
import 'package:slib/services/auth/auth_service.dart';

/// Service quản lý trạng thái thư viện real-time
class LibraryStatusService extends ChangeNotifier {
  final AuthService _authService;

  StompClient? _stompClient;
  bool _wsConnected = false;
  bool _isInitialized = false;
  bool _isInitializing = false;

  // Data
  int _totalSeats = 0;
  int _occupiedSeats = 0;
  double _occupancyRate = 0.0;
  int _currentlyInLibrary = 0;
  bool _isLoading = true;

  // Getters
  int get totalSeats => _totalSeats;
  int get occupiedSeats => _occupiedSeats;
  double get occupancyRate => _occupancyRate;
  int get currentlyInLibrary => _currentlyInLibrary;
  bool get isLoading => _isLoading;

  /// Trạng thái text dựa trên occupancyRate
  String get statusText {
    if (_occupancyRate <= 30) return 'Vắng';
    if (_occupancyRate <= 60) return 'Bình thường';
    if (_occupancyRate <= 80) return 'Khá đông đúc';
    return 'Rất đông';
  }

  /// Color tương ứng
  Color get statusColor {
    if (_occupancyRate <= 30) return Colors.green;
    if (_occupancyRate <= 60) return Colors.blue;
    if (_occupancyRate <= 80) return Colors.orange;
    return Colors.red;
  }

  /// Badge color (cho container % Full)
  Color get badgeColor {
    if (_occupancyRate <= 30) return Colors.green;
    if (_occupancyRate <= 60) return Colors.blue;
    if (_occupancyRate <= 80) return Colors.orange;
    return Colors.red;
  }

  LibraryStatusService(this._authService);

  String get _baseUrl => '${ApiConstants.domain}/slib';

  Future<String?> get _token async => await _authService.getToken();

  /// Initialize: load data + connect WebSocket
  Future<void> initialize() async {
    if (_isInitialized || _isInitializing) return;

    _isInitializing = true;
    try {
      final token = await _token;
      if (token == null || token.isEmpty) {
        debugPrint('[LibraryStatus] Chưa thể initialize vì chưa có token');
        _isLoading = false;
        notifyListeners();
        return;
      }

      await fetchLibraryStatus();
      await _connectWebSocket();
      _isInitialized = true;
    } finally {
      _isInitializing = false;
    }
  }

  /// Fetch library status từ REST API
  Future<void> fetchLibraryStatus() async {
    try {
      final token = await _token;
      if (token == null || token.isEmpty) {
        debugPrint(
          '[LibraryStatus] Không tìm thấy token để tải trạng thái thư viện',
        );
        _isLoading = false;
        notifyListeners();
        return;
      }

      final response = await _authService.authenticatedRequest(
        'GET',
        Uri.parse('$_baseUrl/dashboard/library-status'),
        headers: {'Content-Type': 'application/json'},
      );

      if (response.statusCode == 200) {
        final data = jsonDecode(utf8.decode(response.bodyBytes));
        _totalSeats = (data['totalSeats'] as num?)?.toInt() ?? 0;
        _occupiedSeats = (data['occupiedSeats'] as num?)?.toInt() ?? 0;
        _occupancyRate = (data['occupancyRate'] as num?)?.toDouble() ?? 0.0;
        _currentlyInLibrary =
            (data['currentlyInLibrary'] as num?)?.toInt() ?? 0;
        _isLoading = false;
        notifyListeners();
        return;
      }

      debugPrint(
        '[LibraryStatus] API lỗi ${response.statusCode}: ${utf8.decode(response.bodyBytes)}',
      );
      _isLoading = false;
      notifyListeners();
    } catch (e) {
      debugPrint('[LibraryStatus] Error fetching: $e');
      _isLoading = false;
      notifyListeners();
    }
  }

  /// Connect WebSocket STOMP → subscribe /topic/dashboard
  Future<void> _connectWebSocket() async {
    if (_wsConnected) return;

    try {
      final token = await _token;
      if (token == null || token.isEmpty) {
        debugPrint('[LibraryStatus] Bỏ qua kết nối WebSocket vì chưa có token');
        return;
      }

      String wsUrl = ApiConstants.domain;
      if (wsUrl.startsWith('https://')) {
        wsUrl = wsUrl.replaceFirst('https://', 'wss://');
      } else if (wsUrl.startsWith('http://')) {
        wsUrl = wsUrl.replaceFirst('http://', 'ws://');
      }
      final stompUrl = '$wsUrl/ws/websocket';

      debugPrint('[LibraryStatus] Connecting WebSocket to $stompUrl');

      _stompClient = StompClient(
        config: StompConfig(
          url: stompUrl,
          stompConnectHeaders: {'Authorization': 'Bearer $token'},
          webSocketConnectHeaders: {'ngrok-skip-browser-warning': 'true'},
          onConnect: _onStompConnected,
          onWebSocketError: (error) {
            debugPrint('[LibraryStatus] WebSocket error: $error');
            _wsConnected = false;
          },
          onDisconnect: (_) {
            debugPrint('[LibraryStatus] Disconnected');
            _wsConnected = false;
            // Auto-reconnect sau 5s
            Future.delayed(const Duration(seconds: 5), () {
              if (_isInitialized) _connectWebSocket();
            });
          },
          reconnectDelay: const Duration(seconds: 5),
        ),
      );
      _stompClient!.activate();
    } catch (e) {
      debugPrint('[LibraryStatus] Connection error: $e');
    }
  }

  void _onStompConnected(StompFrame frame) {
    debugPrint('[LibraryStatus] Connected, subscribing to /topic/dashboard');
    _wsConnected = true;

    _stompClient?.subscribe(
      destination: '/topic/dashboard',
      callback: (StompFrame frame) {
        if (frame.body != null) {
          try {
            final data = jsonDecode(frame.body!);
            debugPrint(
              '[LibraryStatus] Dashboard update: ${data['type']} - ${data['action']}',
            );
            // Khi nhận event dashboard update → refresh data từ API
            fetchLibraryStatus();
          } catch (e) {
            debugPrint('[LibraryStatus] Parse error: $e');
          }
        }
      },
    );
  }

  /// Disconnect và cleanup
  void clearData() {
    _stompClient?.deactivate();
    _stompClient = null;
    _wsConnected = false;
    _isInitialized = false;
    _totalSeats = 0;
    _occupiedSeats = 0;
    _occupancyRate = 0.0;
    _currentlyInLibrary = 0;
    _isLoading = true;
    notifyListeners();
  }

  @override
  void dispose() {
    _stompClient?.deactivate();
    _stompClient = null;
    super.dispose();
  }
}
